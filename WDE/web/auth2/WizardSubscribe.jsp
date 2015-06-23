<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription"%>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Create Subscription" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<!-- Page specific CSS stylesheets -->
	<link href="/style/WizardSubscribe.css" rel="stylesheet" media="screen" />
		
	<!-- Page specific CSS stylesheets -->
	<script src="/script/xml.js" type="text/javascript"></script>
	<script src="/script/Listbox.js" type="text/javascript"></script>
	<script src="/script/Common.js" type="text/javascript"></script>
	<script src="/script/WizardSubscribe.js" type="text/javascript"></script>
	<style>
	.container a {
		color: #006699;
		text-decoration: none;
	}
	
	.container a:hover {
		text-decoration: underline;
	}
	
	.tblHdr,.tblFld {
		text-align: justify !important;
		padding: 10px 5px 10px 15px !important;
		font-size: 1.1em !important;
	}
	
	table {
		border-collapse: separate !important;
	}
	
	.btnNext {
		padding-left: 0px !important;
	}
	.error {
		color: #FF0000 !important;
	}
	</style>
	<script type="text/javascript">
		$(function() {
		    $("#subscriptionForm").validate({
		        rules: {
		        	name: "required",
		        	description: "required"
		        },
		        messages: {
		        	name: "Name is required",
		        	description: "Description is required"
				},
				submitHandler: function() { 
		        	console.log(document.forms[0].action);
		        	var a = document.forms[0].action;
		        	var tok = a.split("/");
		        	if (tok[tok.length - 1] == "WizardSubResults.jsp") {
			        	Validate();
		        	}
		        }
			});
		});
	</script>
</head>

<body onload="onLoad();" id="dataPage">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
		<div id="titleArea" class='pull-right'>
			<div id="timeUTC"></div>
		</div>
		<h1 id="pageTitle">Create Subscription</h1>
		<br>
		<div id="linkArea2" class="col-5" style="margin-top: -15px;">
		<form name="subscriptionForm" id="subscriptionForm" method="POST" action="WizardSubResults.jsp">
		<div>
			<label for="name" style="width:120px;font-weight:bold;">Name *</label> 
			<input id="name" name="name" type="text" style="width:300px;">
		</div>
		<div>
			<label for="description" style="width:120px;font-weight:bold;">Description *</label> 
			<input id="description" name="description" type="text" style="width: 300px;">
		</div>
		<div>
			<label for="listCycle" style="width:120px;font-weight:bold;">Interval (in minutes)</label> 
			<select id="cycle" name="cycle" style="width: 300px;">
				<option value="5">5</option>
				<option value="10">10</option>
				<option value="15">15</option>
				<option value="20" selected="selected">20</option>
				<option value="25">25</option>
				<option value="30">30</option>
			</select>
		</div>
		<div>
			<label for="listFormat" style="width:120px;font-weight:bold;">Output Format</label> 
			<select id="format" name="format" style="width: 300px;">
				<option value="CMML">CMML</option>
				<option value="CSV" selected="selected">CSV</option>
				<option value="KML">KML</option>
				<option value="XML">XML</option>
			</select>
		</div>
		<div align="center">
			<input type="radio" name="subScope" value="public"> Public &nbsp; 
			<input type="radio" name="subScope" value="private" checked="checked"> Private
		</div>
		<div align="center">
			<button class="btn-dark" id="btnNext" type="submit"> Subscribe</button>
			<input id="jsessionid" type="hidden" value="<%=request.getSession().getId()%>" />
		</div>
		</form>
		<br>
		</div>
		<div id="instructions" class="col-4"
			style="margin: 0; margin-top: -15px;">
			<h3>Instructions</h3>
			<p>
				<em>* All fields are required.</em><br/><br/>
				Please enter a name and a description for the subscription. <br/><br/>
				You cal select the interval to run the subscription as well as 
				the output format for the subscription results. <br/><br/>
				CMML - Canadian Meteorological Markup Language <br/>
              	CSV - Comma-Separated Value <br/>
              	KML - Keyhold Markup Language <br/>
              	XML - eXtensible Markup Language <br/><br/>
				By default, a subscription is private to the user 
				who creates it.  You can also select Public  
				so it becomes available to all users.
			</p>
			<div id="statusMessage" class="msg"
				style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
				&nbsp;</div>
		</div>
		<div class='clearfix'></div>
		<br>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</body>
</html>
