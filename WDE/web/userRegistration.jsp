<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Registration" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
   
	<script type="text/javascript">
		$(function() {
			
		    $("#cancel").click(function() {
				window.location.replace("<%= response.encodeURL("/index.jsp")%>" );
		    });
		    
		    $.ajax({
		        url: "<%= response.encodeURL("/resources/organizationTypes/")%>",
		        dataType: "json",
		        success: function(resp) {
		            $.each(resp.organizationType, function(index, item) {
		            	$("#organizationType").append("<option value="+item.code+">"+item.name+"</option>");
		            });
		        }
		    });
		    
		    $.ajax({
		        url: "<%= response.encodeURL("/resources/countries/")%>",
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
					    url: "<%= response.encodeURL("/resources/user")%>",
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
		            window.location.replace("<%= response.encodeURL("/auth/loginRedirect.jsp")%>" );
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
	</script>
</head>

<body id="userPage">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
		<h1>User Registration</h1>
	
		<div id="container" style="border:none;">
		
		     <div class="canvas">
		     
		     		<form action="POST" name="registerForm" id="registerForm">
		     		
					<fieldset class="ui-widget ui-widget-content ui-corner-all" style="min-width:450px; max-width:450px;">
					
						<div>
							<label for="userName" style="width: 120px;">User Name: </label>
							<input type="text" id="userName" name="userName" size="41" 
								class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="email" style="width: 120px;">Business Email: </label>
							<input type="text" id="email" name="email" size="41" 
								class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="password" style="width: 120px;">Password: </label>
							<input type="password" autocomplete="off" id="password" name="password" size="41"
						       class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="password2" style="width: 120px;">Verify Password: </label>
						<input type="password" autocomplete="off" id="password2" name="password2" size="41"
						       class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="firstName" style="width: 120px;">First Name: </label>
							<input type="text" id="firstName" name="firstName" size="41"
						       class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="lastName" style="width: 120px;">Last Name: </label>
							<input type="text" id="lastName" name="lastName" size="41"
						       class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="organization" style="width: 120px;">Organization: </label>
							<input type="text" id="organization" name="organization" size="41"
						       class="ui-corner-all" style="width: 297px;">
						</div>
						
						<div>
							<label for="organizationType" style="width: 120px;">Organization Type: </label>
							<select id="organizationType" class="ui-corner-all" style="width: 303px;"></select>
						</div>
						
						<div>
							<label for="country" style="width: 120px;">Country: </label>
							<select id="country" class="ui-corner-all" style="width: 303px;"></select>
						</div>
						<div style="text-align:right; margin-bottom:0;">
							<button class="btn-signin btn-dark" type="submit" id="register" name="register">
		    					<img id="loader" src="/image/loader.gif" alt="loader" />
								<i class="icon-save"></i> 
								<span>Save</span>
								</button>
							<button class="btn-signin btn-light" type="button" id="cancel" name="cancel">
								<i class="icon-ban-circle"></i> 
								Cancel</button>
						</div>
										
					</fieldset>
					
				</form>
				
			</div>
			
			<div id="dialogSuccess">
				You have successfully registered.
				Thank you.
			</div>
			<div id="dialogError"></div>
		</div>
		
		<p>
			Please, provide all required information.
			<br/>
			<em>* All fields are required.</em>
		</p>
		
		<div class="clearfix"></div>
	</div><!-- .container -->

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
