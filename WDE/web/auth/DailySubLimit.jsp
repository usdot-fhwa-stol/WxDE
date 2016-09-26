<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Daily Subscription Limit" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
    <style>
    	.col-2 p{
    		color: #922;
    	}
    </style>
    
    <script type="text/javascript">
    	$(document).ready(function() {
    		$('#dataPage, #dataPage a').addClass('active');
    	});
    </script>
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container clearfix">
		<h1>Daily Subscription Limit</h1>
		<br>
		<div class="col-4" id="instructions">
			<h3>Limit Reached</h3>
			<p>
                            The daily limit of subscriptions has been met.<br> 
                            Please try again tomorrow.
			</p>
			<div id="statusMessage">&nbsp;</div>
		</div>
	</div>
	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
