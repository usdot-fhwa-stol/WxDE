var webService = "",
	userEmail = "",
	errorMsg = $("#errorMsg"),
	currentStep = 0,
	arrStep = $('#retrievalSteps #steps').children(),
	arrHeader = $('#stepsHeaders').children();
// preparations for the app to work properly
// separated for flexibility and readability
initialSettings = function() {
	for (var i = 1; i < arrStep.length; i++)
		$(arrStep[i]).css({marginLeft : '20px', opacity : 0}).hide(0);
	$(arrHeader[0]).css({color : '#555'});
	currentStep = 0;
	$("#forgotPassword, #forgotUserID, #submitEmail").prop("disabled", false);
};

// animate next step
nextStep = function() {
	$(arrStep[currentStep]).animate({marginLeft : '-20px', opacity : 0}, 500).hide(0);
	$(arrHeader[currentStep]).animate({color : '#CCC'}, 500).addClass('active');
	$(arrStep[++currentStep]).delay(500).show(0).animate({marginLeft : '0px', opacity : 1}, 500);
	$(arrHeader[currentStep]).animate({color : '#555'}, 500);
};

// going back a previous step
prevStep = function() {
	$(arrStep[currentStep]).animate({marginLeft : '20px', opacity : 0}, 500).hide(0);
	$(arrHeader[currentStep]).animate({color : '#CCC'}, 500);
	$(arrStep[--currentStep]).delay(600).show(0).animate({marginLeft : '0px', opacity : 1}, 500);
	$(arrHeader[currentStep]).animate({color : '#555'}, 500);
};
// function for validating email format
isEmail = function($email) {
	  var emailReg = /^([\w-\.]+@([\w-]+\.)+[\w-]{2,4})?$/;
	  return( !emailReg.test( $email ) && $email != "" ? false : true );

};
// function for disabling and enabling buttons
disablePhase = function( disable, enable ) {
	$(disable).prop("disabled", true); // avoid double clicking issues
	$(enable).prop("disabled", false); // enabling #backStep button in case it is disabled
};

$(function(){
	
	initialSettings();
	
	$("#forgotPassword").on("click", function(){
		errorMsg.hide();
		nextStep();
		$('input[name="email"]').focus();
		webService = "/resources/email/forgotPassword";
		disablePhase($(this), "#backStep");
	});
	
	$("#forgotUserID").click(function(event){
		errorMsg.hide();
		nextStep();
		$('input[name="email"]').focus();
		webService = "/resources/email/forgotUserId";
		disablePhase($(this), "#backStep");
	});
	
	$('#backStep').on("click", function(event){
		event.preventDefault();
		disablePhase($(this), "#forgotPassword, #forgotUserID");
		prevStep();
	});
	
	$('#backStep2').on("click", function(event){
		event.preventDefault();
		disablePhase($(this), "#submitEmail, #backStep");
		prevStep();
	});
	
	$("#emailForm").on("submit", function(e){
		if (!$('#userEmail').val()) {
		       e.preventDefault();
		   }
	});
	
	$('#submitEmail').click(function(e) {
		
		e.preventDefault();

		//hide buttons to allow fluid loading effect
		$('#backLogin').hide(0);
		$('#backStep2').hide(0).prop("disabled", false);
		
		var emailInput = $(this).closest("form").find("#userEmail").find("input");
		
		userEmail = emailInput.val();
		
		if (!userEmail || !isEmail(userEmail)) {
			errorMsg.fadeIn(0);
			errorMsg.text("Please enter a valid email address.");
			emailInput.focus();
		} else {
			errorMsg.hide(0);
	        var userInfo = new Object();
	        
	        userInfo.email = userEmail;
			
			$.ajax({
				url: webService,
			    type: "POST",
			    contentType: "application/json",
			    data: JSON.stringify(userInfo),
			    success: function() {
				    	$('#retrievalMsg').html(
				    			'An email was sent to '
				    			+ '<strong>' + userEmail+ '</strong>.'
				    			+ '<br>'
				    			+ 'Please check your <em>inbox</em> or <em>spam</em> folder.'
				    	);
				    	$("#retrievalMsg").ready(function() {
			    			$('#backLogin').show(0);
				    		$("#loading").fadeOut();
				    	});
			    },
			    error: function() {
				    	$('#retrievalMsg').html(
				    			'The email '
				    			+ '<strong>' + userEmail+ '</strong>'
				    			+ ' does not exist in our database.<br>'
				    			+ 'Please enter a <strong>valid</strong> email.'
				    	);
				    	$("#retrievalMsg").ready(function() {
				    		$('#backStep2').show(0);
				    		$("#loading").fadeOut();
				    	});
			    }
			});
			$("#submitEmail").prop("disabled", true);
			nextStep();
			$("#loading").show();
		}

	});
	
});





