 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.comp;



/**
 * This class represents road alerts. It contains an alert ID and the road ID of
 * the road the alert is issued for.
 * @author aaron.cherney
 */
public class RoadAlert
{
	public int m_nRoadID;
	public int m_nAlertID;
	
	RoadAlert(int nRoadID, int nAlertID)
	{
		m_nRoadID = nRoadID;
		m_nAlertID = nAlertID;
	}
	
	/**
	 * This method returns the Alert Message for the Alert based off of its 
	 * Alert ID.
	 * @return Alert Message
	 */
	public String getRoadAlertMessage()
	{
		switch (m_nAlertID)
		{
			case 13:	return "Alert: Pavement wet";
			case 14:	return "Alert: Pavement snow";
			case 15:	return "Alert: Pavement slick";
			case 17:	return "Alert: Pavement ice";
			default: return "No alert";
		}
	}
}


