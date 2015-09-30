/**
 * @file RTMARange.java
 */
package wde.qchs.algo;


import wde.metadata.ISensor;
import wde.obs.IObs;


/**
 * Tests observation values to be within the tolerance of the modeled value.
 *
 * <p>
 * Extends {@code LikeInstrument} to inherit observation type-specific delta.
 * </p>
 */
public class RTMA extends LikeInstrument
{
	/**
	 * Service for requesting RTMA forecast values
	 */
	protected wde.qchs.RTMA m_oRTMA = wde.qchs.RTMA.getInstance();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code RTMA}
	 * </p>
	 */
	public RTMA()
	{
	}
	

	/**
	 * Checks the observation by comparing the observation value to the model
	 * value. If the value falls within the observation type threshold, 
	 * then the observation passes the check.
	 *
	 * @param nObsTypeId observation type.
	 * @param iSensor recording sensor.
	 * @param iObs observation in question.
	 * @param oResult results of the check, after returning from this method.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		double dModel = m_oRTMA.getReading(nObsTypeId, iObs.getObsTimeLong(), 
			iObs.getLatitude(), iObs.getLongitude());

		if (dModel == Double.NaN) // no model data available
			return;

		double dValue = iObs.getValue();
		if (dValue >= dModel - m_dSdMax && dValue <= dModel + m_dSdMax)
		{
			oResult.setPass(true);
			oResult.setConfidence(1.0);
		}
		oResult.setRun(); // indicate the test was run
	}
}
