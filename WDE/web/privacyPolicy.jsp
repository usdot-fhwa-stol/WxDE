<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Privacy Policy" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
	<script type="text/javascript">
		$(document).ready(function() {
			$('#aboutPage, #aboutPage a').addClass('active');
		});
	</script>
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
		<h1>Privacy Policy</h1>


		<p>
			The Weather Data Environment (WxDE) program requires that
			contributors and users of data and data processing tools abide by
			certain rules of engagement. The data and content on this site are
			licensed under the <a 
				href="http://creativecommons.org/licenses/by-sa/3.0/legalcode">
				Creative Commons Attribution-ShareAlike 3.0 Unported license </a>
		</p>
		<br>
		<h3>User Rights</h3>
		<p>Those who use data and data processing tools distributed by the
			WxDE program have the following right to use data and data processing
			tools for performance measurement and management, system operations
			and management, and research and development efforts, including
			improving proprietary or non-proprietary software.</p>
		<h3>User Responsibilities</h3>
		<p>Those who use data and data processing tools distributed by the
			WxDE program have the following responsibilities:</p>


		<ol>
			<li>Where the contributed materials have been used to any
				extent to enable, verify, supplement or validate performance
				measurement, analysis, research or software development, to fully
				reference the WxDE program and the contributions of the individuals
				in all subsequent and related publications or public events,
				specifically:<br />
				<ol style="list-style-type: lower-alpha;">
					<li>a. In publications, reference the WxDE Web site and the
						date accessed, data and/or data processing tools (by name and
						version number), and the individual contributors identified on the
						reference template associated with each data and/or data
						processing tool.</li>
					<li>b. In presentations or other oral communication, by noting
						the data and/or data processing tool by name and version number,
						and communicating the address of the WxDE Web site.</li>
				</ol>
			</li>
			<li>To report anomalies, errors or other questionable data
				elements using the Feedback feature of the WxDE Web site, referencing
				the specific data or data processing tool by name and version
				number.</li>
			<li>To refrain from duplication and dissemination of the data
				and data processing tools to third parties.</li>
			<li>To abide by any additional restrictions and limitations
				associated with the organizations contributing data to the WxDE.
				These restrictions and limitations can be reviewed on the WxDE Data
				Sources page.</li>
		</ol>
		<h3>Contributor Rights</h3>
		<p>All contributors to the WxDE sign a Data Usage Agreement that
			provides authorization for the WxDE to collect and distribute data
			from that contributor. See the WxDE Data Usage Agreement for the
			standard Data Usage Agreement text. Individual contributors may place
			additional restrictions or have additional disclaimers associated
			with the use of their data. See the WxDE Data Contributors page for
			information on these additional restrictions.</p>

		<h3>US Department of Transportation Understanding of Data Rights
			and Privacy Protection</h3>
		<p>It is the United States Department of Transportation's (USDOT's) understanding
			that it has the right to distribute the data that are available on
			the WxDE Web site. These rights were obtained through a number of
			individual agreements, references to which are available on the WxDE
			Data Contributors page.</p>
		<p>It is the intent of the USDOT that data residing on the
			WxDE Web site do not contain any personally identifiable information
			(PII) or other sensitive information. If there are privacy concerns
			for any data available on the WxDE Web site, please contact the WxDE
			Site Manager (use the "Contact Us" feature on the WxDE Web site) and
			your concerns will be appropriately addressed.</p>
		<p>Information on WxDE registered users is used only for purposes
			related to WxDE Web site operations. Summary statistics on WxDE
			Web site usage are gathered for program evaluation purposes.
			Statistics on general categories of users may be gathered (e.g.,
			academic, private sector, government) but individual user statistics
			are not gathered. All registration information is secured through
			administrative processes.</p>
		<p>
			<a class=".back-to-top btn-dark" href="#"
				style="display: inline-block; padding: 5px 10px; font-size: 14px; text-decoration: none;">
				<i class="icon-circle-arrow-up"></i> Back to top
			</a>
		</p>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

	<script type="text/javascript">
		$(document).ready(function() {
			$('.back-to-top').click(function(event) {
				event.preventDefault();
				$('html, body').animate({
					scrollTop : 0
				});
				return false;
			});
		});
	</script>
</body>
</html>
