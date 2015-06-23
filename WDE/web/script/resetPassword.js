$(function(){
	
	$("#submitNewPassword").on("click", function(){
		
		var	newPassword = $("#newPassword").val().toString();
		//TODO - Does this need to be tested for XXS attack??
	    $("form").validate({
	        rules: {
	           newPassword: {
	               required:true,
	               minlength:7
	           },
	           confirmPassword: {
	               required:true, 
	               minlength:7,
	               equalTo:"#newPassword"
	           }
	        },
	        messages: {
	           newPassword: {
	               required: "New Password is required",
	               minlength: jQuery.format("Password requires at least {0} characters.")
	           },
	           confirmPassword: {
	               required: "Confirm Password is required",
	               minlength: jQuery.format("Password Verification requires at least {0} characters."),
	               equalTo: "Passwords must match."
	           }
	        },
	        submitHandler: function() {
	        	//show loading effect while processing
	        	$("#loading").show();
	        	
				var pGuid = window.location.toString().split("/")[6],
					user = new Object();
				
				user.passwordGuid = pGuid;
				user.password = newPassword;
				
				$.ajax({
					url : "/resources/email/submitPassword",
					type : "POST",
					contentType: "application/JSON",
					data : JSON.stringify(user),
					cache : false,
					success : function() {
						$("#stage").html(
							  "<p>You have successfully changed your account's password.<br>"
							+ "You can now login with your new password.</p>"
							+ "<a href=\"/auth/loginRedirect.jsp\" class=\"btn-login btn-dark\">"
							+ "<i class=\"icon-signin\"></i> Login</a><br><br><br>"
						);
						$("#loading").fadeOut();
					},
					error : function() {
						$("#stage").html(
								  "<p>We were unable to process your request to change password.<br>"
								+ "Please try again.</p>"
								+ "<a href=\"/userAccountRetrieval.jsp\" class=\"cant-access-account\">"
								+ "Can't access your account?</a><br><br><br>"
							);
						$("#loading").fadeOut();
					}
				});
			}
		});
			
	});
	
});





