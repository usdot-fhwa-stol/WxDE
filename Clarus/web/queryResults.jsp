<%@page contentType="text/plain; charset=iso-8859-1" language="java" import="java.io.*,clarus.qeds.*"%>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<%
	Subscriptions oSubscriptions = Subscriptions.getInstance();
	PrintWriter oWriter = response.getWriter();
	oSubscriptions.getResults(oWriter, oSubscription);
	
	// finish writing the output
	oWriter.flush();
	oWriter.close();
%>
