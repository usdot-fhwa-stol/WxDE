<%@page contentType="text/html; charset=UTF-8" language="java" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Password Required" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/GetPassword.js" type="text/javascript"></script>
	
	<style>	
	.container a {
		color:#006699;
		text-decoration:none;
	}
	.container a:hover {
		text-decoration:underline;
	}
	.tblHdr, .tblFld{
		text-align: justify !important;
		padding:10px 5px 10px 15px !important;
		font-size: 1.1em !important;
	}
	table{
		border-collapse: separate !important;
	}
	.btnNext{
		padding-left: 0px !important;
	}
	.passwordBox *{
		 font-size:14px;
	}
	</style>
	
    <script type="text/javascript">
    	$(document).ready(function() {
    		$('#dataPage, #dataPage a').addClass('active');
    	});
    </script>
</head>

<body onload="onLoad()">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
	    
		<h1>Password Required</h1>

    	<div id="passwordArea" class="col-10" style="margin-top:-15px;">
			<p>
				<em>
					* This subscription is protected by a password.
					Please supply the proper password to gain access.
				</em>
			</p>
          	
			<div class="passwordBox">
				<p>
					<label>Password: </label>
					
					<input id="txtPassword" type="password" onkeypress="return DoEnterKey(this, event, Validate)"/>
		            
		            <br>
		            
		            <button class="btn-light" onclick="goBack()" style="margin-left: 0;">
		            	<i class="icon-circle-arrow-left"></i> 
		            	Back</button>
		            
					<button type="button" id="btn_OK" class="btn-dark" onclick="Validate()" style="margin-left:0; margin-top: 40px;">
						<i class="icon-ok-sign"></i> 
					 	Submit</button>
				</p>
				
          		<div id="statusMessage" style="margin:0;padding:0;">&nbsp;</div>
          	
			</div>
		</div>

		<div class='clearfix'></div>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
	<script type="text/javascript">
		function goBack() { window.history.back(); }
	</script>
</body>
</html>
