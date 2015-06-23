<%@page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,wde.qeds.*"%>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<%
	Subscriptions oSubscriptions = Subscriptions.getInstance();

	String fileNameBase = "queryResults";
	if (oSubscription.getFormat().equals("CSV")) {
		response.setContentType("text/csv; charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment;filename=queryResults.csv");
	} else if (oSubscription.getFormat().equals("CMML")) {
        response.setContentType("text/xml; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=queryResults.cmml");
	} else if (oSubscription.getFormat().equals("XML")) {
	    response.setContentType("text/xml; charset=UTF-8");
	    response.setHeader("Content-Disposition", "attachment;filename=queryResults.xml");
	}

	PrintWriter oWriter = response.getWriter();
	oSubscriptions.getResults(request, oWriter, oSubscription);

	// finish writing the output
	oWriter.flush();
	oWriter.close();
%>
