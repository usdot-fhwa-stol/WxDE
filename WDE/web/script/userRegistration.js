$(function(){
	
    $("#cancel").click(function() {
		window.location.replace("/index.jsp");
    });
    
    $.ajax({
        url: "/resources/organizationTypes/",
        dataType: "json",
        success: function(resp) {
            $.each(resp.organizationType, function(index, item) {
            	$("#organizationType").append("<option value="+item.code+">"+item.name+"</option>");
            });
        }
    });
    
    $.ajax({
        url: "/resources/countries/",
        dataType: "json",
        success: function(resp) {
            $.each(resp.country, function(index, item) {
            	$("#country").append("<option value="+item.code+">"+item.name+"</option>");
            });
        }
    });
    
    //TODO - Validate for Valid Characters e.g. A - Z, a - z, 0 - 9
    // validate the registration form when it is submitted
    $("#registerForm").validate({
        rules: {
           userName: {
               required: true,
               minlength: 5
           },
           email: {
               required: true,
               email: true
           },
           password: {
               required:true,
               minlength:7
           },
           password2: {
               required:true, 
               minlength:7,
               equalTo:"#password"
           },
           firstName: "required",
           lastName: "required",
           organization: "required"
        },
        messages: {
           userName: {
               required: "User Name is required",
               minlength: jQuery.format("User Name requires at least {0} characters.")
           },
           email: {
               required: "Email is required.",
               email: "Your email address must be in the format of name@domain.com"
           },
           password: {
               required: "Password is required",
               minlength: jQuery.format("Password requires at least {0} characters.")
           },
           password2: {
               required: "Password Verification is required",
               minlength: jQuery.format("Password Verification requires at least {0} characters."),
               equalTo: "Confirmed password does not match."
           },
           firstName: "First Name is required.",
           lastName: "Last Name is required.",
           organziation: "Organization is required."
        },
        submitHandler: function() {
        	
	        var userInfo = new Object();
	    	
	    	$("#register")
				.addClass("btn-dark-disabled")
				.removeClass("btn-dark")
				.attr("disabled", "disabled")
				.find("span")
				.text('Saving..')
				.siblings("i")
				.removeClass("icon-save")
				.siblings("img")
				.show(0);
	    	
	        userInfo.user = $("#userName").val();
	        userInfo.firstName = $("#firstName").val();
	        userInfo.lastName = $("#lastName").val();
			userInfo.email = $("#email").val();
			userInfo.password = $("#password").val();
			userInfo.organization = $("#organization").val();
			userInfo.organizationType = $("#organizationType").val();
			userInfo.country = $("#country").val();
			
			$.ajax({
			    url: "/resources/user",
			    type: "POST",
			    contentType: "application/json",
			    data: JSON.stringify(userInfo),
			    success: function(event, XMLHttpRequest, ajaxOptions) {
			    	if (event != null && event.errorMessage != null) {
			    		$("#dialogError").text(event.errorMessage);
						$("#dialogError").dialog("open");
			    	} else {
						$("#dialogSuccess").dialog("open");
			    	}
			    },
			    error: function(jqXHR, textStatus, errorThrown) {
			    	$("#dialogError").text("Unkown error occurred on the server.");
					$("#dialogError").dialog("open");
			    }
			});
        }
	});
    
    $( "#dialogSuccess" ).dialog({
		autoOpen: false,
		resizable: false,
        height:140,
        modal: true,
        dialogClass: "no-close",
        title: "Success",
        buttons: {
          "Login": function() {
            $( this ).dialog( "close" );
            window.location.replace("/auth/loginRedirect.jsp");
          }
        }
	});
    
    $( "#dialogError" ).dialog({
		autoOpen: false,
		resizable: false,
        height:140,
        modal: true,
        dialogClass: "no-close",
        title: "Error",
        buttons: {
          OK: function() {  	    	
            $( this ).dialog( "close" );
          }
        }
	});    
});

$(document).ajaxComplete(function(event, xhr, settings) {
	if ( settings.url === "/resources/user" )
  	  //event for the save button
    	$("#register")
			.addClass("btn-dark")
			.removeClass("btn-dark-disabled")
			.removeAttr("disabled")
			.find("span")
			.text('Save')
			.siblings("i")
			.addClass("icon-save")
			.siblings("img")
			.hide(0);
});





