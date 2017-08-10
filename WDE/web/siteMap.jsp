<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="About" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

</head>

<body id="aboutPage">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div id="pageBody" class="container">

		<a href="#" class="skip-link logical-placement" data-skip="skip-sitemap">Skip over Sitemap</a>

		<h1>Site Map</h1>
		
		<ul class="site-map">
			
			<li><a href="<%= response.encodeURL("/")%>">Home</a></li>
			<li>Data
				<ul>
					<li><a href="<%= response.encodeURL("/summaryMap.jsp")%>">Summary Map</a></li>
					<li>Observations
						<ul>
							<li><a href="<%= response.encodeURL("/auth2/wizardContributor.jsp")%>">Contributor</a></li>
							<li><a href="<%= response.encodeURL("/auth/wizardGeospatial.jsp")%>">Coordinates</a></li>
						</ul>
					</li>
					<li><a href="<%= response.encodeURL("/auth2/Subscriptions.jsp")%>">Data Subscriptions</a></li>
					<li>Metadata
						<ul>
							<li><a href="<%= response.encodeURL("/auth2/metadata.jsp")%>">Files</a></li>
							<li><a href="<%= response.encodeURL("/auth2/dataSource.jsp")%>">Tables</a></li>
						</ul>
					</li>
				</ul>
			</li>
			
			<li><a href="<%= response.encodeURL("/wdeMap.jsp")%>">WxDE Map</a></li>
			
			<li>About
				<ul>
					<li><a href="<%= response.encodeURL("/newsPage.jsp")%>">News</a></li>
					<li><a href="<%= response.encodeURL("/changeLog.jsp")%>">Change Logs</a></li>
					<li><a href="<%= response.encodeURL("/termsOfUse.jsp")%>">Terms of Use</a></li>
					<li><a href="<%= response.encodeURL("/frequentlyAskedQuestions.jsp")%>">Frequently Asked Questions</a></li>
				</ul>
			</li>
			
			<li><a href="<%= response.encodeURL("/auth/loginRedirect.jsp")%>">Login</a>
				<ul>
					<li><a href="<%= response.encodeURL("/userRegistration.jsp")%>">Registration</a></li>
					<li><a href="<%= response.encodeURL("/userAccountRetrieval.jsp")%>">Can't access your account?</a></li>				
				</ul>
			</li>
			
			<li><a href="<%= response.encodeURL("/privacyPolicy.jsp")%>">Privacy Policy</a></li>
			
		</ul>
		
	</div>

	<a href="#" id="skip-sitemap" tabindex="-1"></a>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
