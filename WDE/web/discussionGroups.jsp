<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=Edge" />
    
	<title>Weather Data Environment - Discussion Group</title>
	
    <!-- normalize, CSS reset for uniform style across all major browsers -->
    <link href="/style/normalize.css" rel="stylesheet">
    
	<link href="/style/wxde-main-style.css" rel="stylesheet" media="screen">
	<link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css" />
	<script src="/script/jquery/jquery-1.9.1.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>
	<script src="/script/jquery/superfish.js"></script>
    
	<link href="/style/jquery/superfish.css" rel="stylesheet" media="screen">
    
    <!-- CSS stylesheet and hacks for IE versions lower than 8 -->
    <!--[if lte IE 7]>
        <link href="/style/IE-styles.css" rel="stylesheet">
    <![endif]-->
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container">
		<h1>Discussion Groups</h1>
		<br />
		<p> <em>Content Here</em> </p>
	</div>
	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
