<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="FAQ" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
    <!-- Custom Stylesheet -->
    <link href="/style/faq-styles.css" rel="stylesheet" media="screen">
	<script type="text/javascript" src="/script/jquery.nicescroll.min.js"></script>
	<script type="text/javascript" src="/script/faq-scripts.js"></script>
    
    <style>
    	.highlightQuestion {
    		background: #AAA;
    		color: #efefef !important;
    		text-shadow: 0 -1px 0 #999;
    	}
    	.icon-chevron-sign-right {
    		margin-top: 2px;
    	}
    </style>
</head>

<body id="aboutPage" class="faq-page">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div id="pageBody" class="container">

		<a href="#" class="skip-link logical-placement" data-skip="skip-faq">Skip over FAQ</a>

		<h1>Frequently Asked Questions</h1>
		
		<div id="faqSideNav" class="col-2" style="margin-left: 10px;">
			<h3>Questions</h3>
			<div id="snapTrigger"></div>
			<div id="quesNav"></div>
			<a href="#" class="skip-to-section" data-skip="mainFooter" aria-hidden="true">Skip to footer</a>
		</div>
		
		<div id="faqMain" class="col-5" style="margin-left: 30px;">
			<h3>Answers</h3>
			<ul style="margin:0;" id="qa-list"></ul>
		</div>
		
		<div class="clearfix"></div>
		<br>
		<a href="#" class="skip-to-section" style="bottom: 100px;" data-skip="wdeMenu" aria-hidden="true">Skip to local navigation</a>
	</div>

	<a href="#" id="skip-faq" tabindex="-1"></a>
		
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</body>
</html>
