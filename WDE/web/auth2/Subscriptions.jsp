<%@page contentType="text/html; charset=UTF-8" language="java" import="java.io.*, java.util.*,wde.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Data Subscriptions" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
	<!-- Page specific JavaScript -->
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    
	<!-- Custom CSS stylesheet for the Reports Table -->
	<link href="/style/reports-table.css" rel="stylesheet" media="all">
		
	<script type="text/javascript">
		$(document).ready(function() {
			$("#noSubs").hide();
			var subscriptionType = new Array("Private","Public","Shared");
			$.ajax({
	            url:'<%= response.encodeURL("/resources/auth/subscriptions/") %>', 
	            dataType: 'json',
	            success: loadTable,
	            cache: false
	        });

			function loadTable(data) {
				var tempArray = new Array();
				if (data == null) {
					$("#noSubs").show();
					return;
				}
				if ($.isArray(data.subscription)) {
					tempArray = data.subscription;
				} else {
					tempArray.push(data.subscription);
				}
				$.each(tempArray, function(index, item) {
					if (item.description != null && item.description.length > 45) {
						item.description = item.description.substring(0, 45) + '...';
					}
					$('#tblSubscriptions')
						.append(
							'<tr class="subscription-row" data-link="SubFolder.jsp?subId=' + item.subscriptionId + '&' + csrf_nonce_param + '" title="Subscription ' + item.subscriptionId + '">'
								+ '<td>' + item.subscriptionId + '</a></td>'
								+ '<td>' + item.name + '</a></td>'
								+ '<td>' + item.description + '</td>'
								+ '<td class="centered">' + subscriptionType[item.isPublic] + '</td>'
								+ '<td class="centered">' + item.user.user + '</td>'
							+ '</tr>');
					}
				);
				
				$('.subscription-row')
					.on('mouseenter', 'td', function() {
						$(this).parent('tr').addClass('highlight');
					})
					.on('mouseout', 'td', function() {
						$(this).parent('tr').removeClass('highlight');
					})
					.on('click', function() {
						window.location = $(this).data('link');
					});
			}
		});
	</script>
</head>

<body onunload="">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
    	<div class="" style="">
	        <table id="tblSubscriptions" class="reports-table">
				<caption style="display: none;">Data Subscriptions</caption>
			  <thead>
				<tr class="">
				  <th>Id</th>
				  <th>Name</th>
				  <th width="300">Description</th>
				  <th>Type</th>
				  <th>Owner</th>
				</tr>
			  </thead>
			  <tbody>
			  </tbody>
	        </table>
	        <div id="noSubs" style="padding-left:400px;padding-top:20px;">You Have No Subscriptions</div>
		</div>
		
		<div class='clearfix'></div>
		<br>
		
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</body>
</html>
