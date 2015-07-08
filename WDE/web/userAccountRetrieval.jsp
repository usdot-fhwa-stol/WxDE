<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Account Retrieval" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
	<link href="/style/user-accounts-retrieval.css" rel="stylesheet" media="screen">
    
</head>

<body id="userPage">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container" style="min-height: 320px;">
	
		<h1>Retrieve Account Information
			<img id="loading" src="/image/loading-dark.gif" alt="loading"></h1>
		
		<div id="retrievalSteps">
		
			<div class="steps-headers" id="stepsHeaders">
				<h2 id="step1">STEP 1</h2>
				<h2 id="step2">STEP 2</h2>
				<h2 id="step3">STEP 3</h2>
			</div>
			
			<div class="clearfix"></div>
			
			<div class="steps-div" id="steps">
			
				
				<div>
				
					<p>What did you forget?</p>
					
					<button id="forgotPassword" class="btn-dark">I Forgot my Password</button>
					
					<button id="forgotUserID" class="btn-dark">I Forgot my User ID</button>
					
				</div>
				
				
			</div><!-- eof stepOne -->
				
			<div class="steps-div" id="steps">
					
				<div>
				
					<p>Enter your Business Email:</p>
					
					<form id="emailForm">
					
						<p  id="userEmail">
							<input class="input-email" type="text" name="email" />
						</p>
					
						<button class='btn-light btn-back' id='backStep' type="reset">
<!-- 							<i class='icon icon-circle-arrow-left'></i> -->
							<img src="/image/icons/dark/fa-arrow-left.png" alt="Back Icon"
								style="margin-bottom: -2px;" />
							Back</button>
						
						<button class="btn-dark btn-submit" id="submitEmail" type="submit">
<!-- 							<i class='icon icon-share-sign'></i> -->
							Submit
							<img src="/image/icons/light/fa-arrow-right.png" alt="Submit Icon"
								style="margin-bottom: -1px;" />
						</button>
					</form>
					
					<p id="errorMsg" style="color:#881111;"></p>
					
				</div>
			
			</div><!-- eof stepTwo -->
				
			<div class="steps-div" id="steps">
			
				<div>
					<p id="retrievalMsg"></p>
					
					<button class='btn-dark btn-back' id='backStep2'>
<!-- 						<i class='icon icon-circle-arrow-left'></i> -->
						<img src="/image/icons/light/fa-arrow-left.png" alt="Back Icon"
							style="margin-bottom: -1px;" />
						Try Again</button>
						
					<a class='btn-dark btn-back' id="backLogin" href="/userLogin.jsp">
<!-- 						<i class='icon icon-signin'></i> -->
						<img src="/image/icons/light/fa-signin-shaded.png" alt="Login Icon" />
						Login</a>
				</div>
			
			</div><!-- eof stepThree -->
			
		</div>
		<br />
	</div>
	
	<div class="clearfix"></div>
	<script type="text/javascript" src="/script/retrievalSteps.js"></script>

	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

	
</body>
</html>
