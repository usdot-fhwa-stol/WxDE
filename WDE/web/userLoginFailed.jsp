<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Login Failed" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
</head>

<body id="userPage">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container">
	
		<h1>Login Failure</h1>
		
		<div>
			<p style="margin:10px;">	
				You have entered an invalid <strong>user name</strong> or <strong>password</strong>.<br>
				Please <a href="<%= response.encodeURL("/auth/loginRedirect.jsp")%>" class="login-a-link" title="Login">login</a> with your correct user credentials.
			</p>
			<br />
			<a class="btn-block btn-dark btn-login" id="tryAgain" href="/auth/loginRedirect.jsp">
				<i class="icon-signin"></i>
				Login</a>
			
			<a href="<%= response.encodeURL("/userAccountRetrieval.jsp")%>" class="cant-access-account">
				Can't access your account?
			</a>
		</div>
		
		<br />
	</div>
	
	<div class="clearfix"></div>
	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
