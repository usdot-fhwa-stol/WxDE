// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Units.java
 */
package wde.dao;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The {@code Units} class keeps track of {@see UnitConv}'s stored in the
 * database and wraps both Forward and Reverse conversions.
 * <p>
 * Implements the {@code ILockFactory} interface to allow
 * {@see UnitConv}'s to be modified in a mutually exclusive fashion through
 * the use of {@link StripeLock} containers.
 * </p>
 * <p>
 * This is a singleton class who's instance can be retrieved by the
 * {@see Units#getInstance} method.
 * </p>
 *
 * @see UnitConvF
 * @see UnitConvR
 */
public class Units implements ILockFactory<UnitConv> {
    private static final Logger logger = Logger.getLogger(Units.class);

    /**
     * Allows queries of {@see UnitConv}'s from the database in a formatted
     * manner.
     */
    private static String UNIT_QUERY = "SELECT u.id, u.srcUnit, u.dstUnit, " +
            "u.multiplier, u.divisor, u.\"offset\" FROM conf.unit u ORDER BY u.id";

    /**
     * The singleton instance of Units.
     */
    private static Units g_oInstance = new Units();
    /**
     * The list of unit-conversions retrieved from the database specified in the
     * Units config file. Both the forward ({@see UnitConvF}) and reverse
     * ({@see UnitConvR}) versions of these conversions are contained in the
     * list.
     */
    private ArrayList<UnitConv> m_oUnits = new ArrayList<UnitConv>();
    /**
     * Log of {@see UnitConv}'s missing from the list of unit-conversions upon
     * request.
     */
    private ArrayList<UnitConv> m_oMissingUnits = new ArrayList<UnitConv>();
    /**
     * The thread lock to provide mutually exclusive access to UnitConv objects.
     */
    private StripeLock<UnitConv> m_oLock = new StripeLock<UnitConv>(this, 5);
    /**
     * Default conversion, used when requested conversions cannot be found.
     */
    private UnitConv m_oDefaultUnit = new UnitConv();

    /**
     * Default Constructor connects to the database specified in the Units
     * config file to query the units contained there. It then iterates through
     * the units stored in the database, adding both forward and reverse
     * conversions to ({@code m_oUnits}) the list of units contained in the
     * {@code Units} class.
     */
    private Units() {
            // get the unit conversion configuration
            WDEMgr wdeMgr = WDEMgr.getInstance();
            Config oConfig = ConfigSvc.getInstance().getConfig(this);

            // get the database connection pool
            DataSource iDataSource =
                    wdeMgr.getDataSource(oConfig.getString("datasource", null));

            if (iDataSource == null)
                return;
        try (
        Connection iConnection = iDataSource.getConnection();
        PreparedStatement ps = iConnection.prepareStatement(UNIT_QUERY);
        ResultSet rs = ps.executeQuery();
        ){
            while (rs.next()) {
                // create and save the forward direction unit conversion
                UnitConv oUnitConv = new UnitConvF
                        (
                                rs.getString(2),
                                rs.getString(3),
                                rs.getDouble(4),
                                rs.getDouble(5),
                                rs.getDouble(6)
                        );

                int nIndex = Collections.binarySearch(m_oUnits, oUnitConv);
                if (nIndex < 0)
                    m_oUnits.add(~nIndex, oUnitConv);

                // now create the reverse unit conversion
                oUnitConv = new UnitConvR
                        (
                                // reverse the from and to unit labels
                                rs.getString(3),
                                rs.getString(2),
                                rs.getDouble(4),
                                rs.getDouble(5),
                                rs.getDouble(6)
                        );

                nIndex = Collections.binarySearch(m_oUnits, oUnitConv);
                if (nIndex < 0)
                    m_oUnits.add(~nIndex, oUnitConv);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        logger.info("Completing constructor, array size: " + Integer.toString(m_oUnits.size()));
    }

    /**
     * Retrieves the singleton instance of {@code Units}.
     *
     * @return The instance of {@code Units}.
     */
    public static Units getInstance() {
        return g_oInstance;
    }

    /**
     * This method searches the list of conversion for the conversion
     * between the supplied units. If it can't find the conversion, it then adds
     * it to the list of missing units if not already there.
     *
     * @param sFromUnit The unit to convert from.
     * @param sToUnit   The unit to convert to.
     * @return The default conversion when either of the supplied units are
     * null, if the supplied units are the same, or if the conversion is not
     * stored in the conversion list. Else it returns the queried conversion.
     */
    public UnitConv getConversion(String sFromUnit, String sToUnit) {
        // return the identity conversion when the unit labels are the same
        if (sFromUnit == null || sToUnit == null ||
                sFromUnit.compareTo(sToUnit) == 0) {
            // report when one unit is null and the other is not
            if (sFromUnit == null && sToUnit != null ||
                    sFromUnit != null && sToUnit == null) {
                logger.info("getConversion for sFromUnit: " + sFromUnit + " sToUnit: " + sToUnit);
            }

            return m_oDefaultUnit;
        }

        // search the unit conversion list for an appropriate convertor
        UnitConv oUnitConv = m_oLock.readLock();
        oUnitConv.setLabels(sFromUnit, sToUnit);

        int nIndex = Collections.binarySearch(m_oUnits, oUnitConv);
        if (nIndex >= 0)
            oUnitConv = m_oUnits.get(nIndex);

        m_oLock.readUnlock();

        if (nIndex < 0) {
            // log missing unit conversions
            synchronized (this) {
                nIndex = Collections.binarySearch(m_oMissingUnits, oUnitConv);
                if (nIndex < 0) {
                    logger.info("getConversion for sFromUnit: " + sFromUnit + " sToUnit: " + sToUnit);

                    // a new unit conversion object needs to be created
                    // because it did not exist and the search object changes
                    m_oMissingUnits.
                            add(~nIndex, new UnitConv(sFromUnit, sToUnit));
                }
            }

            oUnitConv = m_oDefaultUnit;
        }

        return oUnitConv;
    }

    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link UnitConv} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link UnitConv}
     * @see ILockFactory
     * @see StripeLock
     */
    public UnitConv getLock() {
        return new UnitConv();
    }

    /**
     * Wraps forward conversions which are conversions of the form:
     * <blockquote>
     * from-units -> to-units
     * </blockquote>
     * It extends {@see UnitConv} implementing the {@see UnitConv#convert}
     * method.
     *
     * @see UnitConv
     * @see UnitConvR
     */
    private class UnitConvF extends UnitConv {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code UnitConvF}
         * </p>
         */
        protected UnitConvF() {
        }


        /**
         * Calls the {@see UnitConv} constructor to setup the units. Initializes
         * the Conversion factors.
         *
         * @param sFromUnit Units to convert from.
         * @param sToUnit   Units to convert to.
         * @param dMultiply Multiplication factor.
         * @param dDivide   Division factor.
         * @param dAdd      Addition factor.
         */
        protected UnitConvF(String sFromUnit, String sToUnit,
                            double dMultiply, double dDivide, double dAdd) {
            super(sFromUnit, sToUnit);
            m_dAdd = dAdd;
            m_dDivide = dDivide;
            m_dMultiply = dMultiply;
        }


        /**
         * Overrides {@see UnitConv#convert} and performs a forward conversion
         * of the form:
         * <blockquote>
         * [(Value * Multiplication Factor)/ Division Factor] + Addition Factor
         * </blockquote>
         *
         * @param dValue The value to convert.
         * @return The newly converted value.
         */
        @Override
        public double convert(double dValue) {
            return (dValue * m_dMultiply / m_dDivide + m_dAdd);
        }
    }

    /**
     * Wraps reverse conversions. Extends {@code UnitConvF} implementing the
     * {@see UnitConv#convert} method in such a way that the conversions are
     * of the form:
     * <blockquote>
     * to-units -> from-units
     * </blockquote>
     *
     * @see UnitConv
     * @see UnitConvF
     */
    private class UnitConvR extends UnitConvF {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code UnitConvR}
         * </p>
         */
        protected UnitConvR() {
        }

        /**
         * Calls the {@see UnitConvF} constructor to setup the units and
         * conversion factors.
         *
         * @param sFromUnit Units to convert from.
         * @param sToUnit   Units to convert to.
         * @param dMultiply Multiplication factor.
         * @param dDivide   Division factor.
         * @param dAdd      Addition factor.
         */
        protected UnitConvR(String sFromUnit, String sToUnit,
                            double dMultiply, double dDivide, double dAdd) {
            super(sFromUnit, sToUnit, dMultiply, dDivide, dAdd);
        }

        /**
         * Overrides {@see UnitConv#convert} and performs a forward conversion
         * of the form:
         * <blockquote>
         * [(Value - Addition Factor)* Division Factor] / Multiplication Factor
         * </blockquote>
         *
         * @param dValue The value to convert.
         * @return The newly converted value.
         */
        @Override
        public double convert(double dValue) {
            return ((dValue - m_dAdd) * m_dDivide / m_dMultiply);
        }
    }
}
