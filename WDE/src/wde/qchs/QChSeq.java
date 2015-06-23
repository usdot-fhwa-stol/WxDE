// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QChSeq.java
 */
package wde.qchs;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.algo.QChResult;
import wde.util.QualityCheckFlagUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides a means of sequencing quality check algorithms, and distinguishing
 * these sequences by climate region.
 * <p>
 * Implements {@code Comparable<QChSeq>} to enforce an ordering based on
 * climate id.
 * </p>
 */
public class QChSeq implements Comparable<QChSeq> {
    /**
     * Database query format string.
     */
    private static String QCHSEQ_QUERY = "SELECT seq, bitPosition, " +
            "runAlways, weight, qchconfigId, className " +
            "FROM conf.qchseq WHERE qchseqmgrId = ?";

    /**
     * Separate quality check sequences by climate. Helps enforce an ordering.
     */
    private int m_nClimateId;
    /**
     * Container of quality check algorithms, sorted by sequence id.
     */
    private ArrayList<QCh> m_oQChS;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code QChSeq}
     * </p>
     */
    QChSeq() {
    }


    /**
     * <b> Constructor </b>
     * <p>
     * Sets the attributes for new instances of {@code QChSeq}. Performs the
     * query and populates the sorted qch array.
     * </p>
     *
     * @param nSeqMgrId   sequence manager id - for database query.
     * @param nClimateId  climate id of this sequence.
     * @param iConnection connected to the datasource, and ready for quality
     *                    check sequence query.
     */
    QChSeq(int nSeqMgrId, int nClimateId, Connection iConnection) {
        setClimateId(nClimateId);
        m_oQChS = new ArrayList<QCh>();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // get the list of quality checks for the supplied climate id
            ps = iConnection.prepareStatement(QCHSEQ_QUERY);
            ps.setInt(1, nSeqMgrId);

            // instantiate the configured quality check
            rs = ps.executeQuery();
            while (rs.next()) {
                QCh oQCh =
                        (QCh) Class.forName(rs.getString(6)).newInstance();

                // initialize the quality check
                oQCh.init
                        (
                                rs.getInt(1),
                                rs.getInt(2),
                                rs.getInt(3),
                                rs.getDouble(4),
                                rs.getInt(5),
                                iConnection
                        );

                // save the quality check to the process list
                m_oQChS.add(oQCh);
            }

            Collections.sort(m_oQChS);
        } catch (Exception oException) {
            oException.printStackTrace();
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }


    /**
     * <b> Mutator </b>
     *
     * @param nClimateId new climate id for <i> this </i> {@code QChSeq}.
     */
    void setClimateId(int nClimateId) {
        m_nClimateId = nClimateId;
    }


    /**
     * Runs the quality check algorithms for the provided observation. Sets the
     * observations run and pass bit-fields, and the confidence level for the
     * observation for each qch algorithm.
     *
     * @param nObsTypeId observation type.
     * @param iSensor    sensor corresponding to the observation.
     * @param iObs       observation to quality check.
     * @param oResult    locked result object to allow mutually exclusive access
     *                   to the result to ensure accuracy.
     */
    void check(int nObsTypeId, ISensor iSensor, IObs iObs, QChResult oResult) {
        // initialize the quality check result accumulators
        boolean bContinue = true;
        int nRun = 0;
        int nFlags = 0;
        double dConfidence = 0.0;
        double dWeight = 0.0;
        double dTotalWeight = 0.0;

        // run quality checking algorithms for each obs
        for (int nSeqIndex = 0; nSeqIndex < m_oQChS.size(); nSeqIndex++) {
            QCh oQCh = m_oQChS.get(nSeqIndex);
            // always set the flag bit to indicate an attempted check
            int nBits = 1 << oQCh.m_nBitPosition;
            nFlags |= nBits;

            if (oQCh.m_bRunAlways || bContinue) {
                // clear the result object and set the not-run weight
                oResult.clear();
                dWeight = 0.0;
                oQCh.check(nObsTypeId, iSensor, iObs, oResult);

                // evaluate results when the test successfully runs
                if (oResult.getRun()) {
                    // set the run bit position
                    nRun |= 1 << oQCh.m_nBitPosition;
                    // update the weight to reflect that the check was run
                    dWeight = oQCh.m_dWeight;

                    // notch the flag bit when the test does not pass
                    if (!oResult.getPass()) {
                        nFlags &= ~nBits;
                        // let subsequent checks know that a test has failed
                        bContinue = !oQCh.m_bSignalStop;
                    }
                }

                // accumulate the weighted results for the overall confidence
                dConfidence += dWeight * oResult.getConfidence() *
                        oResult.getConfidence();
                dTotalWeight += dWeight;
            }
        }

        // set the run, pass, and confidence for the obs
        if (dTotalWeight != 0.0)
            dConfidence = Math.sqrt(dConfidence / dTotalWeight);
        else
            dConfidence = 0.0;

        iObs.setConfValue((float) dConfidence);
        iObs.setQchCharFlag(QualityCheckFlagUtil.getQcCharFlags(1, nRun, nFlags));
    }


    /**
     * Compares <i> this </i> {@code QChSeq} with the provided {@code QChSeq}
     * by climate id.
     *
     * @param oQChSeq object to compare with <i> this </i>
     * @return 0 if the values match. > 0 if <i> this </i> is the lesser value.
     */
    public int compareTo(QChSeq oQChSeq) {
        return (m_nClimateId - oQChSeq.m_nClimateId);
    }
}
