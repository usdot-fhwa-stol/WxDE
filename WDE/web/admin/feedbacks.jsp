<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");//HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
 %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Feedbacks" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
		
		<!-- CSS stylesheet for the Reports Table -->
		<link href="/style/reports-table.css" rel="stylesheet" media="all">
		
		<style>
			.feedback-menu {
				position: absolute;
				display: none;
				background: #fff;
				color: #888;
				border: 1px solid #AAA;
				cursor: pointer;
			}
			.feedback-menu span {
				display: block;
				padding: 8px 12px;
			}
			.feedback-menu span:hover {
				background: #AAA;
				color: #FFF;
			}
			.popup {
				display: none;
				position: absolute;
				width: auto;
				height: auto;
				padding: 0 10px 10px 10px;
				background: #FFF;
				color: #555;
				border: 2px solid #AAA;
			}
			.popup span {
				color: #888;
			}
		</style>
		<script src="/script/escape.js" type="text/javascript"></script>
		<script src="/script/feedbacks.js" type="text/javascript"></script>
		<script src="/script/simpleSorter.js" type="text/javascript"></script>
		<script src="/script/feedbackDescription.js" type="text/javascript"></script>
		
		<!-- right click menu script -->
		<script type="text/javascript">
			$(document).ready(function() {
				$("#noFeedbacks").hide();
	        	var feedback = new Object();
	        	
				var $menu = $(".feedback-menu"),
					onAction = navigator.userAgent.match(/msie/i) ? "mousedown" : "contextmenu";
				
		        $(".reports-table").on( onAction, "tr", function(event) {
		        	event.preventDefault();

		        	var $this = $(this);
		        	
		        	var rightClick = {
		        		open : function() {
				        	var tempLink = $this.find(".page-link").first().attr("href");
				        	window.location = tempLink;
				        	$menu.hide();
		        		},
		        		deleteRow : function() {
				        	
				        	feedback.feedbackId = $this.find("#feedbackID").text();
				        	
				        	$.ajax({
				        		headers: { 
					                'Accept': 'application/json',
					                'Content-Type': 'application/json' 
				            	},
				        		url : '<%= response.encodeURL("/resources/admin/feedback/delete")%>',
				        		data : JSON.stringify(feedback),
				        		type : 'post',
				        		success : function() {
				        			alert("Row Deleted!");
				        		},
				        		error : function() {
				        			alert("Deletion Failed!");
				        		}
				        	});
				        	
				        	$menu.hide();
		        		},
		        		view : function(){
				        	$this.find(".btn-dark").click();
					        $menu.hide();
		        		}
		        	};
		        	
		        	var popup = {
		        		show : function() {
		        			$('.popup').css({
			        			top : event.pageY - 5,
			        			left: event.pageX - 5,
		        			}).show(0);
		        		},
		        		hide : function() {
		        			$('.popup').hide(0);
		        		},
		        		header : function(text, subText) {
		        			$('.popup').find('p').html(
		        					'<b>' + text + '</b>' +
		        					'<br>' +
		        					'<span>' + subText + '</span>'
		        			);
		        		}
		        	};
		        	
		        	$('#cancel').on("click", function() {
		        		popup.hide();
		        	});
		        	
					if (!$this.hasClass("headers"))
			        	if ( event.button == 2) {
			        		$menu.css({
			        			top : event.pageY - 5,
			        			left: event.pageX - 5,
			        		}).toggle();
				        	
					        $("#openPage").on("click", function() {
					        	rightClick.open();
					        });
					        $("#deleteRow").on("click", function() {
					        	popup.show();
					        	$menu.hide();
					        	popup.header("Delete Row?", "This is irreversable");
					        	$('#ok').click( function() {
						        	rightClick.deleteRow();
						        	popup.hide();
					        	});
					        });
					        $("#viewPage").on("click", function() {
					        	rightClick.view();
					        });
					        
					        return false;
			        	};
		        });
		        
		        $menu.on("mouseleave", function(){ $(this).hide(); });
		        
			});
		</script>
	</head>
	
	<body id="adminPage">
		<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
		<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
		
		<div class="container">
			<h1>
				Feedbacks
				<img id="loading" src="/image/loading-dark.gif" alt="loading">
			</h1>
			
<!-- 			<p><em>* Click column headers containing vertically aligned carets ( <img src="/image/icons/dark/fa-sort.png" alt="Sort Icon" />) to sort.</em></p> -->
			
			<table class="reports-table" id="sortable-table">
				<caption style="display: none;">Reports</caption>
			  <thead>
				<tr class="headers">
				  <th id="" class="">
				    Type
				  </th>
				  <th id="sort-date" class="">
				    Feedback ID
<!-- 				    <span class="icon-stack"> -->
<!-- 				      <img src="/image/icons/light/fa-sort.png" alt="Sort Icon" /> -->
<!-- 				    </span> -->
				  </th>
				  <th id="" class="">Submitted By</th>
				  <th id="" class="">Section</th>
				  <th id="" class="">Description</th>
				  <th id="" class="">Date</th>
				</tr>
			  </thead>
			  <tbody>
			  </tbody>
			</table>
	        <div id="noFeedbacks" style="padding-left:400px;padding-top:20px;">No feedbacks to display.</div>
		</div>	
		
		<div id="descriptionModal" title="View Feedback" style="display:none;">
			<p id="descriptionWriter" style="margin-left: 20px">
			</p>
		</div>
		
		<!-- right click menu -->
		<div class="feedback-menu">
			<span id="openPage">Go to Page</span>
			<span id="deleteRow">Delete Row</span>
			<span id="viewPage">View Feedback</span>
		</div>
		
		<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
		
		<div class="popup">
		<p><b>Confirm</b></p>
		<button class="btn-dark" id="ok">Ok</button>
		<button class="btn-light" id="cancel">Cancel</button>
		</div>
		
	</body>
</html>