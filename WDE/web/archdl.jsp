<%@ page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,java.sql.*,javax.sql.*, java.util.zip.*, wde.WDEMgr" %>
<%
    int nObsTypeId = 0;
	String sObsTypeId = request.getParameter("obsType");
	if (sObsTypeId != null && sObsTypeId.length() > 0)
		nObsTypeId = Integer.parseInt(sObsTypeId);

        String sUrl = request.getRequestURL().toString();
        String[] sParams = request.getParameter("file").split("-");
        String sContributorID = sParams[0];
        String sYear = sParams[1].substring(0, 4);
        String sMonth = sParams[1].substring(4, 6);
        String sDay = sParams[1].substring(6, 8);
        
	String sDataSourceName = "java:comp/env/jdbc/wxde";
	String sQuery = "SELECT o.obstypeid, o.sensorid, o.obstime, o.latitude, " +
                "o.longitude, o.elevation, o.value, o.confvalue, o.qchcharflag " +
                "FROM obs.\"obs_" + sYear + "-" + sMonth + "-" + sDay + "\" o, meta.sensor s, meta.platform p " +
                "WHERE s.id=o.sensorid AND s.distgroup=2 AND s.platformid=p.id " +
                "AND p.contribid=" + Integer.parseInt(sContributorID) + " ORDER BY o.obstime, o.obstypeid, o.sensorid;";

//	Context oInitialContext = new InitialContext();

//	DataSource iDataSource = (DataSource)oInitialContext.lookup(sDataSourceName);
	DataSource iDataSource =
		WDEMgr.getInstance().getDataSource(sDataSourceName);
	if (iDataSource == null)
		return;

	Connection iConnection = iDataSource.getConnection();
	if (iConnection == null)
		return;

	Statement iQuery = iConnection.createStatement();
//	iQuery.setInt(1, nObsTypeId);
	ResultSet iResultSet = iQuery.executeQuery(sQuery);

	// count the number of records
//	int nRows = 0;
        PrintWriter oPrintWriter = new PrintWriter(new GZIPOutputStream(response.getOutputStream()));
        oPrintWriter.println("obstypeid,sensorid,obstime,latitude,longitude,elevation,value,confvalue,qcharflag");
        
	while (iResultSet.next())
        {
            
            for(int i = 1; i <= 9; i++)
            {
                if(i > 1)
                    oPrintWriter.print(",");
                oPrintWriter.print(iResultSet.getString(i));
            }
            oPrintWriter.println();
        }

	iResultSet.close();
	iQuery.close();
	iConnection.close();
      
	oPrintWriter.println("END OF RECORDS");
	oPrintWriter.flush();
	oPrintWriter.close();
%>