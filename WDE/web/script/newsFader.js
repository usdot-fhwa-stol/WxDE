$(function() {
	
	$.ajax({
		type: 'GET',
		url: '/changeLog.xml?' + csrf_nonce_param,
		dataType: 'xml',
		success: displayNews,
		cache: false
	});
	
	function displayNews(xml) {
		var i = 1,
			z = $(xml).find('changelogs').find('changelog').length,
			newsClass = "",
			linkType = "";

		$(xml).find('changelogs').each( function() {
			$(this).find('changelog').each( function() {
				
				if (i != 1) newsClass = 'news hidden-news';
				else newsClass = 'news';
					
				if ($(this).find('link').text() == 'external')
					linkType = 'exit-link';
				else linkType = "";
					
				if ($(this).find('featured').text() == 'true')
	    			$('#news').append(
					  '<div id="news-' + i++ + '" class="' + newsClass + '">'
						+ '<p class="news-text">'  
						  + '<b class="news-title">' + $(this).find('heading').text() + '</b>'
						  + '<span style="float:right;font-size:.8rem">' + $(this).find('date').text() + '</span><br><br>'
	   	    			  + $(this).find('news').text()
	   	    			+ '</p>'
	   	    			+ '<a data-heading="' + $(this).find('heading').text() + '" '
	   	    			+ 'href="' + $(this).find('url').text() + '?' + csrf_nonce_param + '#'
	   	    			+ $(this).find('heading').text()
	   	    			+ '"class="button-white readmore-button '+ linkType +'" style="z-index: ' + z-- +'; float:right" title="' + $(this).find('heading').text() + '">'
	   	    			+	'READ MORE <span class="sr-only" style="position: absolute; right: 9999px">About ' + $(this).find('heading').text() + '</span>'
	   	    			+ '</a>'
	    			  + '</div>'
	    			);
			});
		});
		
		$('.readmore-button').on('focus', function() {
			$('.news-header').text($(this).data('heading')).css({ right : '0px'});
		})
		.on('blur', function() {
			$('.news-header').css({ right : '9999px'});
		});
		
		$('#news-1').css({'opacity':1});
		
		$.fn.makeItFade=function(playSpeed){
			var slideIndex=0,
				arrSlide=$(this).children(),
				totalSlides=arrSlide.size()-1;
			
			$.fn.fadeAway=function(){
				this.animate({"opacity":0},"slow").animate({"right": 9999}, 0);
			};
			
			$.fn.fadeInto=function(){
				this.animate({"right": 25}, 0).animate({"opacity": 1}, "slow");
			};
			
			$.fn.nextPlease=function(){
				$(this[slideIndex]).fadeAway();
				slideIndex++;
				if(slideIndex>totalSlides)
					slideIndex=0;$(this[slideIndex]).fadeInto();
			};
			
			$.fn.bringSexyBack=function(){
				$(this[slideIndex]).fadeAway();
				slideIndex--;
				if(slideIndex<0)
					slideIndex=totalSlides;
				$(this[slideIndex]).fadeInto();
			};
			
			$("#next").click(function(){
				arrSlide.nextPlease();
			});
			
			$("#prev").click(function(){
				$(arrSlide).bringSexyBack();
			});
			
			$("#pauseOrPlay").on("click", "#play", function(){
				//$(this).hide();$("#pause").show();
				$("#pauseOrPlay").html(
						'<a href="#" title="Pause"><img alt="Pause" src="/image/icons/light/fa-pause.png" id="pause" title="Pause" /></a>'
				);
				$("#play-box").prop("checked",true);
			});
			
			$("#pauseOrPlay").on("click", "#pause", function(){
				//$(this).hide();$("#play").show();
				$("#pauseOrPlay").html(
						'<a href="#" title="Play"><img alt="Play" src="/image/icons/light/fa-play.png" id="play" title="Play" /></a>'
				);
				$("#play-box").prop("checked",false);
			});
			
			setInterval(function(){
				if($("#play-box").is(":checked"))
					$(arrSlide).nextPlease();
				}, playSpeed);
			};
			

	        $('#news').makeItFade(5000);
	        
	    	var strURL = '';
	    	$("#dialog").dialog({
	    		autoOpen : false,
	    		resizable : false,
	    		height : "auto",
	    		width : "auto",
	    		modal : true,
	    		show : {
	    			effect : "fade",
	    			duration : 1E3
	    		},
	    		hide : {
	    			effect : "fade",
	    			duration : 1E3
	    		},
	    		buttons : {
	    			"Continue" : function() {
	    				$(this).dialog("close");
	    				window.open(strURL);
	    			},
	    			Cancel : function() {
	    				$(this).dialog("close");
	    			}
	    		}
	    	});
	    	$(".exit-link").click(
	    			function(e) {
	    				e.preventDefault();
	    				strURL = $(this).attr("href");
	    				$("#link").html(
	    						'<em style="text-decoration: underline;">' + strURL
	    								+ "</em>");
	    				$("#dialog").dialog("open");
	    			});
	};
	
});