<%@ page contentType="text/xml; charset=iso-8859-1" language="java" import="java.io.*,java.sql.*,javax.sql.*,clarus.ClarusMgr" %>
<%
    int nObsTypeId = 0;
	String sObsTypeId = request.getParameter("obsType");
	if (sObsTypeId != null && sObsTypeId.length() > 0)
		nObsTypeId = Integer.parseInt(sObsTypeId);

	String sDataSourceName = "java:comp/env/jdbc/clarus_conf";
	String sQuery = "SELECT bitPosition, className FROM qchseqmgr, qchseq " +
		"WHERE obsTypeId = ? AND climateId = 0 AND active = 1 " +
		"AND qchseqmgrId = id ORDER BY bitPosition";

//	Context oInitialContext = new InitialContext();

//	DataSource iDataSource = (DataSource)oInitialContext.lookup(sDataSourceName);
	DataSource iDataSource =
		ClarusMgr.getInstance().getDataSource(sDataSourceName);
	if (iDataSource == null)
		return;

	Connection iConnection = iDataSource.getConnection();
	if (iConnection == null)
		return;

	PreparedStatement iQuery = iConnection.prepareStatement(sQuery);
	iQuery.setInt(1, nObsTypeId);
	ResultSet iResultSet = iQuery.executeQuery();

	// count the number of records
	int nRows = 0;
	while (iResultSet.next())
		++nRows;

    PrintWriter oPrintWriter = response.getWriter();
    oPrintWriter.println("<?xml version=\"1.0\" ?>");
	oPrintWriter.print("<qchs numRows=\"");
	oPrintWriter.print(nRows);
	oPrintWriter.print("\"");
	if (nRows == 0)
		oPrintWriter.print(" msg=\"Required parameter missing: obsType\"");
	oPrintWriter.println(">");
    
	// format the quality check names
	iResultSet.beforeFirst();
	while (iResultSet.next())
    {
        oPrintWriter.print("  <qch bit=\"");
		oPrintWriter.print(iResultSet.getInt(1));
		oPrintWriter.print("\" label=\"");
		oPrintWriter.print(iResultSet.getString(2));
		oPrintWriter.println("\"/>");
    }

	iResultSet.close();
	iQuery.close();
	iConnection.close();
      
	oPrintWriter.println("</qchs>");
	oPrintWriter.flush();
	oPrintWriter.close();
%>
