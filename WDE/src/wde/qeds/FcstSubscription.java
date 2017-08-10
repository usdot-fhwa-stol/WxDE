package wde.qeds;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Logger;
import wde.util.MathUtil;


/**
 * Provides filtering parameters which can be set and used to gather
 * only the observations that meet these defined parameters.
 *
 * @author bryan.krueger
 */
public class FcstSubscription extends Subscription{
    private static final Logger logger = Logger.getLogger(FcstSubscription.class);

    /**
     * Creates a new default inssrtance of {@code Subscription}.
     */
    public FcstSubscription()
	 {
		 m_nCycle = 60;
    }



    /**
     * Extracts the data associated with the subscription id specified by the
     * provided subscription result set. This subscription id is used to execute
     * the queries associated with the provided prepared statements.
     *
     * @param oSubscription result set of a subscription query.
	 * @param oGetSubObs
     * @throws java.lang.Exception
     */
    public void deserialize(ResultSet oSubscription,
                            PreparedStatement oGetSubObs) throws Exception {
        m_nId = oSubscription.getInt(1);

        // set the subscription parameters
        m_dLat1 = MathUtil.fromMicro(oSubscription.getInt(2));
        if (oSubscription.wasNull())
            m_dLat1 = -Double.MAX_VALUE;

        m_dLng1 = MathUtil.fromMicro(oSubscription.getInt(3));
        if (oSubscription.wasNull())
            m_dLng1 = -Double.MAX_VALUE;

        m_dLat2 = MathUtil.fromMicro(oSubscription.getInt(4));
        if (oSubscription.wasNull())
            m_dLat2 = Double.MAX_VALUE;

        m_dLng2 = MathUtil.fromMicro(oSubscription.getInt(5));
        if (oSubscription.wasNull())
            m_dLng2 = Double.MAX_VALUE;

        int nObsType = oSubscription.getInt(6);

      if (nObsType == 0)
		  {
			  oGetSubObs.setInt(1, m_nId);
			  int nRows = 0;
			  ResultSet oRs = oGetSubObs.executeQuery();
			  oRs.last();
			  nRows = oRs.getRow();
			  oRs.beforeFirst();
			  int nCount = 0;
			  m_nObsTypes = new int[nRows];
			  while (oRs.next())
			  {
				  m_nObsTypes[nCount++] = oRs.getInt(1);
			  }
		  }
        m_dMin = oSubscription.getDouble(7);
        if (oSubscription.wasNull())
            m_dMin = Double.NEGATIVE_INFINITY;

        m_dMax = oSubscription.getDouble(8);
        if (oSubscription.wasNull())
            m_dMax = Double.POSITIVE_INFINITY;

        m_nRun = oSubscription.getInt(9);
        m_nFlag = oSubscription.getInt(10);

        m_sOutputFormat = oSubscription.getString(11);

        m_oRadius = null;
        m_oContribIds.clear();
        m_oPlatformIds.clear();
    }
}