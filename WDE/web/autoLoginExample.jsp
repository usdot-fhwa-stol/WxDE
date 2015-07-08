<%@page contentType="text/html; charset=UTF-8" language="java" import="java.net.URLEncoder,wde.security.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>RDE Test</title>
</head>
<body>
	<%
		// substitute your RDEGuest password on the WxDE Instance here
		String password = "test123";
		String msg = Encryption.getTimeVariantMessage(password);
		String key = URLEncoder.encode(Encryption.encryptToString(msg), "UTF-8");
		System.out.println("key from JSP: " + key);
	%>	
	
	<!-- This is the URL for the test instance and requires VPN to access -->
	<a href="http://10.10.10.28/resources/rde?key=<%=key%>&lat=45&long=-93&radius=50">login</a>
</body>
</html>