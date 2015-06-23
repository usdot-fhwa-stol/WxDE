<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Change Logs" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
    <style type="text/css">
    	.news-item {
    		margin-bottom: 40px;
    		padding-bottom: 40px;
    		border-bottom: 1px dotted #BBB;
    	}
    </style>
</head>

<body id="aboutPage">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	
	<div class="container">
		<h1>Change Logs</h1>
		
		<div id="news" style="padding: 0 2.5%;"></div>
	</div>
	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	<script type="text/javascript">
	
		$(document).ready(function() {

			var posts = [];
			
			$.ajax({
				type: 'GET',
				url: '/changeLog.xml',
				dataType: 'xml',
				success: function(xml) {
					loadPosts(xml);
					displayPosts(posts);
				},
				//fix for disabling caching of ajax requests by older browsers
				cache: false
			});// eof ajax changeLog
			
			//push each changeLog into posts[]
			function loadPosts(xml) {
				
				$(xml).find('changelogs').each( function() {
					$(this).find('changelog').each( function() {
						if ($(this).find('url').text() === '/changeLog.jsp')
							posts.push({
								date : $(this).find('date').text(),
								heading : $(this).find('heading').text(),
								text : $(this).find('news').text()
							});
					});
				});
				
				return posts;

			};//eof loadPosts()
			
			//display each changelog inside posts[]
			function displayPosts( posts ) {
				
				//index for creating ids for each news item
				var i = 1;
				
				$.each( posts, function( key, value) {
					$('#news').append(
					  '<div id="news-' + i++ + '" class="news-item">'
						+ '<h3 class="blog-heading">' + value.heading + '</h3><p class="pull-right">' + value.date + '</p><br>'
			    			+ '<p>' + value.text + '</p>'
					  + '</div>'
					);
				});
				
				posts = null;
				return posts;
			}
			
		});// eof $
	</script>
</body>
</html>
