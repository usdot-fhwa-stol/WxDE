
<%@page import="org.owasp.encoder.Encode"%>
<%@page contentType="text/html; charset=UTF-8" language="java" import="java.net.URLEncoder,wde.security.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<title>RDE Test</title>
	<script src="/script/jquery/jquery-1.9.1.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
	<link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css">
	<script type="text/javascript">
	    $(document).ready(function() {
			$('#login').click(function(e) {

			<%
				String password = "test123";
				String msg = Encryption.getTimeVariantMessage(password);
				String key = Encryption.encryptToString(msg);
				String getKey = URLEncoder.encode(key, "UTF-8");
				System.out.println("key from JSP: " + key);
			%>	
				var rdeInfo = new Object();
				rdeInfo.key = "<%=Encode.forJavaScript(key)%>";
				$.ajax({
					url: "<%= response.encodeURL("http://10.10.10.28/resources/rde")%>",
				    type: "POST",
				    contentType: "application/json",
				    data: JSON.stringify(rdeInfo),
    	            success: function() {
    	            	window.location.replace("<%= response.encodeURL("http://10.10.10.28/auth/wizardGeospatial.jsp?lat=95.45&long=-35.55&radius=30.0")%>" );
                	},
                	error: function() {
    	            	window.location.replace("<%= response.encodeURL("http://10.10.10.28/auth/loginRedirect.jsp")%>");
                	} 
				});
			});
		});
	</script>
</head>
<body>
	<button id="login" type="button">Login</button>
  <a href="<%= response.encodeURL("http://10.10.10.28/resources/rde?key=" + Encode.forHtmlAttribute( getKey ) + "&lat=45&long=-93&radius=50")%>">login</a>
</body>
</html>