<%@page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,wde.qeds.*"%>
<jsp:useBean id="oFcstSubscription" scope="session" class="wde.qeds.FcstSubscription" />
<jsp:setProperty name="oFcstSubscription" property="*" />
<%
	FcstSubscriptions oFcstSubscriptions = FcstSubscriptions.getInstance();
        
	String fileNameBase = "queryResults";
	response.setContentType("text/csv; charset=UTF-8");
	response.setHeader("Content-Disposition", "attachment;filename=queryResults.csv");

	PrintWriter oWriter = response.getWriter();
	oFcstSubscriptions.getResults(request, oWriter, oFcstSubscription);

	// finish writing the output
	oWriter.flush();
	oWriter.close();
%>
