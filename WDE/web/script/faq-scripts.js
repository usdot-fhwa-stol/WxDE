(function() {
	//trigger Questions nav to fixed when overflowed
	$(window).on("scroll", snapNav);
	
	var queslinkID = "";
	
	$.ajax({
		url:'faq.xml?' + csrf_nonce_param,
		dataType: 'xml',
		success: loadQuestions,
		cache: false
	});

	
	//load the questions only for the navigation ( left side of panel )
	function loadQuestions(xml) {

		var $xml = $(xml), i = 1;
		$xml.find("faqcategory").each( function() {

			var strCategory = "ul-" + $(this).attr("id");

			$("#quesNav").append('<a href="#" class="faq-category" id="cat-' + i++ + '"><span class="pull-right arrow-right">></span>' + $(this).attr("catname") + '</a><ul class="faq-questions" id="' + strCategory  + '"></ul>' );

			$(this).find("faq").each( function() {

				var	$this = $(this),
					strID = $this.find("id").text();
				$("#" + strCategory).append(
						'<li><a href="#id=' + strID +'" class="quesLink" data-id="' + strID + '">'
						+ '<span class="bullet">&#149;</span> ' + $this.find("question").text()
						+ "</a></li>"
						);
				$("#qa-list").append(
						  '<li id="id=' + strID + '" class="ans-' + strID + ' answers">'
						+ "<em>" + $this.find("question").text() + "</em>"
						+ '<p style="font-size: 14px;">' + $this.find("answer").text() + "</p>"
						+ "</li>"
						);

			});

		});

		i++;
		
		// Initialize accordion effect for faq and category
		$('.faq-questions').slideUp(0);
		$('.faq-questions').first().slideDown(0);
		$('.faq-category').first().find('.arrow-right').addClass('open');
		//eof Initialization
		
		//accordion effect for left panel questions + check for overflow
		$('.faq-category').on('click', function(evt){
			
			evt.preventDefault();
			
			var $this = $(this),
				$queslist = $this.next('.faq-questions');
		
			$queslist.siblings('.faq-questions').slideUp( function() {
				$(this).css({ "max-height" : 500 });
			});
			
			//select only visible question list :: Needs testing to see if it works in ie8
			if ( $queslist.is(":visible") )
				$queslist.slideUp( function() {
					$(this).css({ "max-height" : 500 });
				});
			else
				$queslist.slideDown( function() {
					resizeFaq($(this), $this.attr("id"));
				});

		    	$this.siblings('.faq-category').find('.arrow-right').removeClass('open');
    			$this.find('.arrow-right').toggleClass('open');
    			
		});
		
		//resize the questions left panel menu when it overflows the window
		function resizeFaq($faqs, category) {   
				
			var $window = $(window).height() - 100,
				$quesHeight = $faqs.position().top +  $faqs.outerHeight(true);

				if ( $quesHeight > $window ) {
					$faqs.css({ "max-height" : $window / 2 + 100 });
				} else {
					$faqs.css({ "max-height" : "1000px" });
				}
			
			queslinkID = category;
		}
		
		//restores max-height of element
		function restoreFaq($faqs) {
			$faqs.css({ "max-height" : 800 + 'px'});
		}
		
		//initialize scroller
		$('.faq-questions').niceScroll({
			styler:"fb",
			cursorcolor:"#000"
		});
		
		//resize left panel questions to avoid overflowing
		//Check Performance issue

		
		var $window = $(window).height() - 200,
			$faqs = $('.faq-questions'),
			$quesHeight = null;
		
		$(window)
			.bind("resize", function() {
				$faqs.getNiceScroll().resize();
				$quesHeight = $faqs.position().top +  $faqs.outerHeight(true);
				if ( $quesHeight > $window ) {
					$faqs.css("max-height", $window - 220 + 'px');
				} else {
					$faqs.css("max-height", "auto");
				}
			});
		
		$faqs.bind("mouseenter", function() {
			$faqs.getNiceScroll().resize();
		});

		$('.quesLink').on('click', function() {
			var queslink = $(this), 
				data = queslink.data('id');
			$('.faq-active-arrow').remove();
			$('.highlightQuestion').removeClass('highlightQuestion');
			queslink.addClass('highlightQuestion')
			.prepend('<img src="/image/icons/dark/fa-arrow-right.png" alt="Right Icon" class="faq-active-arrow" style="float: right" />');
			$('.highlightAnswer').removeClass('highlightAnswer');
			$('.ans-' + data).addClass('highlightAnswer');
		});

	}// eof loadQuestions()
	
	//allow the faq category navigation to snap when the window is scroll pass it's location
	function snapNav() {
		var windowTop = $(window).scrollTop(),
			divTop = $('#snapTrigger').offset().top;
		
		if (windowTop > divTop)
			$('#quesNav').addClass('snapNav');
		else
			$('#quesNav').removeClass('snapNav');
  	}

	
})(jQuery);