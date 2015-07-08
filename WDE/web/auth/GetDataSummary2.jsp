<%@page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,wde.qeds.*"%>
<%
	DataSummary dataSummary = DataSummary.getInstance();
	PrintWriter writer = response.getWriter();
	dataSummary.getResults(request, writer);
	
	// finish writing the output
	writer.flush();
	writer.close();
%>
