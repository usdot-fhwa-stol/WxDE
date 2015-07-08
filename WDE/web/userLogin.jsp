<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Login" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
    <style>
    	.cant-access-account { margin-left: 0 !important; }
    </style>
</head>

<body id="userPage">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	<jsp:include page="/inc/userLoginInclude.html"></jsp:include>	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	<script type="text/javascript">
	    $(document).ready(function() {
	    	
	    	$('#j_username').focus();
	    	
			$("#register").click(function() {
	   			window.location.replace("/userRegistration.jsp");
	   		});
		});
	</script>
</body>
</html>
