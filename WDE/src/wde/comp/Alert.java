/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.comp;



/**
 * This class represents alerts and warnings for roads. There are 14 different 
 * Alert IDs, the numbering is based off of the Road Condition variable for MERTo.
 * @author aaron.cherney
 */
public class Alert
{
	public int m_nRoadID;
	public int m_nAlertID;
	
	Alert(int nRoadID, int nAlertID)
	{
		m_nRoadID = nRoadID;
		m_nAlertID = nAlertID;
	}
	
	/**
	 * This method returns the Alert Message for the Alert based off of its 
	 * Alert ID.
	 * @return Alert Message
	 */
	public String getAlertMessage()
	{
		switch (m_nAlertID)
		{
			case 2:	return "Alert: Wet road";
			case 3:	return "Alert: Ice/snow on the road";
			case 4:	return "Alert: Mix water/snow on the road";
			case 5:	return "Alert: Dew";
			case 6:	return "Alert:	Melting snow";
			case 7:	return "Alert: Frost";
			case 8:	return "Alert: Icing rain";
			case 12:	return "Warning: Future wet road";
			case 13: return "Warning: Future ice/snow on the road";
			case 14: return "Warning: Future mix water/snow on the road";
			case 15: return "Warning: Future dew";
			case 16: return "Warning: Future melting snow";
			case 17: return "Warning: Future frost";
			case 18: return "Warning: Future icing rain";
			default: return "Dry road";
		}
	}
}


