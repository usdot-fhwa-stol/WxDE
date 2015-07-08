<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="404 Error" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
    <style>
    	.btn-example {
    		display: inline-block;
    		padding: 4px 10px 5px 10px;
		    background-color: #4d4d4d;
		    background-image: linear-gradient(#6d6d6d, #4d4d4d);
		    background-image: -o-linear-gradient(#6d6d6d, #4d4d4d);
		    background-image: -moz-linear-gradient(#6d6d6d, #4d4d4d);
		    background-image: -webkit-linear-gradient(#6d6d6d, #4d4d4d);
    		color: #fff;
    		text-shadow: 0 -1px 0 #444;
    		border: solid 1px #777;
    		margin-top: 1px;
    	}
    </style>
    
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container">
		<h1>404 Error - Page not found</h1>
		<p>
			The page you were trying to access is either at a different location<br>
			or does not exist in our server any longer.
		 </p>
		 <p>
		 	If you believe you got to this page by mistake,<br>
		 	Please click the <span class="btn-example"><i class="icon-edit"></i> Feedback</span> in the menu to submit<br>
		 	a report to our web developers.
		 </p>
		 <p>
		 	Thank you.
	 	</p>
	</div>
	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
