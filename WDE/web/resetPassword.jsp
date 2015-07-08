<%@page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Reset Password" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
	    
		<h1>
			Reset Password
			<img id="loading" style="display: none; height: 22px; width: 22px; opacity: .5" src="/image/loading-dark.gif" alt="loading"/>
		</h1>

    	<div id="stage" class="col-10">
    		<form class="new-password-form" method="post">
				<label>New Password: </label>
				<input type="password" name="newPassword" id="newPassword" class="password-box" />
				<br>
				<label>Confirm Password: </label>
				<input type="password" name="confirmPassword" id="confirmPassword" class="password-box" />
    			
    			<button id="submitNewPassword" class="btn-dark">
    				<i class="icon-check"></i>
    				Submit</button>
    		</form>
    		<br><br>
    		<!-- TODO: Remove these error indicators when jquery.validate usage is approved -->
    		<p class="error" id="emptyBox" style="display:none"><em>Please fill out both boxes.</em></p>
    		<p class="error" id="matchBox" style="display:none"><em>The passwords must match exactly.</em></p>
		
		</div>
		<div class='clearfix'></div>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
	<script src="/script/resetPassword.js" type="text/javascript"></script>
	
</body>
</html>
