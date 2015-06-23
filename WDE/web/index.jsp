<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en-US">
<head>
    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Home" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<link href="/style/newsFader.css" rel="stylesheet" />
	
    <!-- homepage slideshow -->
    <link rel="stylesheet" href="/vendor/nivo-slider/themes/bar/bar.css" type="text/css" media="screen" />
    <link rel="stylesheet" href="/vendor/nivo-slider/nivo-slider.css" type="text/css" media="screen" />

</head>

<body id="homePage" class="home-page">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
	<jsp:include page="/inc/mainContent.html"></jsp:include>	
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
    <!-- script for the Latest News module -->
	<script type="text/javascript" src="/script/newsFader.js"></script>
	
	<!-- script for the Image Slider module -->
    <script type="text/javascript" src="/vendor/nivo-slider/jquery.nivo.slider.js"></script>
    <script type="text/javascript">
    // Nivo slideshow settings
    $(window).load(function() {
        $('#slider').nivoSlider({
            effect: 'boxRandom',
            animSpeed: 1000,
            pauseTime: 5000,
            directionNav: true, 
        });
    });
    </script>
</body>
</html>
