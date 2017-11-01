<%@ page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,java.sql.*,javax.sql.*,java.util.*,java.util.zip.*,wde.WDEMgr" %>
<%
        String sFile = request.getParameter("file");
	if (sFile == null || sFile.length() == 0)
		return;
        String sContributorID = sFile.substring(0, 2);
        String sYear = sFile.substring(3, 7);
        String sMonth = sFile.substring(7, 9);
        String sDay = sFile.substring(9, 11);

	GregorianCalendar oNow = new GregorianCalendar();
	GregorianCalendar oWhen = new GregorianCalendar(Integer.parseInt(sYear), Integer.parseInt(sMonth) - 1, Integer.parseInt(sDay));
	if (oNow.getTimeInMillis() - oWhen.getTimeInMillis() < 172800000)
		return;

	String sQuery = "SELECT o.obstypeid, o.sensorid, o.obstime, o.latitude, " + 
		"o.longitude, o.elevation, o.value, o.confvalue, o.qchcharflag " +
		"FROM obs.\"obs_" + sYear + "-" + sMonth + "-" + sDay + "\" o, meta.sensor s, meta.platform p " +
		"WHERE s.id=o.sensorid AND s.distgroup=2 AND s.platformid=p.id " +
		"AND p.contribid=" + Integer.parseInt(sContributorID) + " ORDER BY o.obstime, o.obstypeid, o.sensorid;";

        PrintWriter oPrintWriter = new PrintWriter(new GZIPOutputStream(response.getOutputStream()));
        oPrintWriter.println("obstypeid,sensorid,obstime,latitude,longitude,elevation,value,confvalue,qcharflag");
        
		DataSource iDataSource = WDEMgr.getInstance().getDataSource("java:comp/env/jdbc/wxde");
		if (iDataSource == null)
			return;
    
		try(Connection iConnection = iDataSource.getConnection();
		Statement iQuery = iConnection.createStatement();
		ResultSet iResultSet = iQuery.executeQuery(sQuery);
            )
    {

        
		while (iResultSet.next())
        	{
	            for (int i = 1; i < 10; i++)
        	    {
                	if (i > 1)
	                    oPrintWriter.print(",");
        	        oPrintWriter.print(iResultSet.getString(i));
	            }
        	    oPrintWriter.println();
	        }

    
	}
	catch (Exception oException)
	{
	}
      
	oPrintWriter.println("END OF RECORDS");
	oPrintWriter.close();
%>
