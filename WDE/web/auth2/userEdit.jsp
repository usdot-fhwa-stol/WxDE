<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");//HTTP 1.1
    response.setHeader("Pragma", "no-cache"); //HTTP 1.0
    response.setDateHeader("Expires", 0); //prevents caching at the proxy server
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
        <jsp:param value="Edit Account" name="title"/>
    </jsp:include>
    <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

    <script>
        $(document).ready(function () {
            $("#cancel").click(function () {
                window.location.replace("<%= response.encodeURL("/index.jsp")%>" );
            });

            $('#register').find('img').toggle(0);

            $.ajax({
                url: "<%= response.encodeURL("/resources/user")%>",
                dataType: "json",
                success: function (resp) {
                    $("#userName").text(resp.user);
                    $("#firstName").val(resp.firstName);
                    $("#lastName").val(resp.lastName);
                    $("#email").val(resp.email);
                    $("#organization").val(resp.organization);
                    $.ajax({
                        url: "<%= response.encodeURL("/resources/countries/")%>",
                        dataType: "json",
                        success: function (countResp) {
                            $('#country').append('<option value="" selected="selected">Please select a country...</option>');
                            $.each(countResp.country, function (index, item) {
                                $("#country").append("<option value=" + item.code + ">" + item.name + "</option>");
                            });
                            $("#country").val(resp.country);
                        }
                    });
                    $.ajax({
                        url: "<%= response.encodeURL("/resources/organizationTypes/")%>",
                        dataType: "json",
                        success: function (orgResp) {
                            $('#organizationType').append('<option selected="selected" value="">Please select a type...</option>');
                            $.each(orgResp.organizationType, function (index, item) {
                                $("#organizationType").append("<option value=" + item.code + ">" + item.name + "</option>");
                            });
                            $("#organizationType").val(resp.organizationType);
                        }
                    });
                }
            });

            // validate the registration form when it is submitted
            $("#registerForm").validate({
                rules: {
                    userName: {
                        required: true,
                        minlength: 3
                    },
                    email: {
                        required: true,
                        email: true
                    },
                    password: {
                        required: true,
                        minlength: 7
                    },
                    password2: {
                        required: true,
                        minlength: 7,
                        equalTo: "#password"
                    },
                    firstName: "required",
                    lastName: "required",
                    organization: "required",
                    organizationType: {
                        required: true
                    },
                    country: {
                        required: true
                    }
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
                    organziation: "Organization is required.",
                    organizationType: "Organization type is required.",
                    country: "Country is required."
                },
                submitHandler: function () {

                    var userInfo = new Object();
                    // Disable Save button while ajax is working
                    $("#register")
                            .addClass("btn-dark-disabled")
                            .removeClass("btn-dark")
                            .attr("disabled", "disabled")
                            .find("span")
                            .text('Saving..')
// 						.siblings("i")
// 						.removeClass("icon-save")
                            .siblings("img")
                            .toggle(0);

                    userInfo.firstName = $("#firstName").val();
                    userInfo.lastName = $("#lastName").val();
                    userInfo.email = $("#email").val();
                    userInfo.password = $("#password").val();
                    userInfo.organization = $("#organization").val();
                    userInfo.organizationType = $("#organizationType").val();
                    userInfo.country = $("#country").val();
                    $.ajax({
                        url: "<%= response.encodeURL("/resources/user")%>",
                        type: "PUT",
                        contentType: "application/json",
                        data: JSON.stringify(userInfo),
                        success: function () {
                            $("#dialogSuccess").dialog("open");
                        },
                        error: function () {
                            $("#dialogError").dialog("open");
                        }
                    });
                }
            });
            $("#dialogSuccess").dialog({
                autoOpen: false,
                resizable: false,
                height: "auto",
                modal: true,
                dialogClass: "no-close",
                title: "Success",
                buttons: {
                    OK: function () {
                        $(this).dialog("close");
                        window.location.replace("<%= response.encodeURL("/auth/loginRedirect.jsp")%>" );
                    }
                }
            });
            $("#dialogError").dialog({
                autoOpen: false,
                resizable: false,
                height: 140,
                modal: true,
                dialogClass: "no-close",
                title: "Error",
                buttons: {
                    OK: function () {
                        $(this).dialog("close");
                    }
                }
            });
        });

        $(document).ajaxComplete(function (event, xhr, settings) {
            if (settings.url === "/resources/user")
            // Re-enable the Save button upon ajax completion
                $("#register")
                        .addClass("btn-dark")
                        .removeClass("btn-dark-disabled")
                        .removeAttr("disabled")
                        .find("span")
                        .text('Save')
//        			.siblings("i")
//        			.addClass("icon-save")
                        .siblings("img")
                        .toggle(0);
        });

    </script>
</head>

<body onunload="" id="userPage">

<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

<div class="container">
    <h1>User Edit</h1>

    <div id="container" style="border:none;">

        <div class="canvas">
            <form action="POST" name="registerForm" id="registerForm">
                <fieldset class="ui-widget ui-widget-content ui-corner-all" style="min-width:450px; max-width:450px;">
                    <legend style="display: none;">User Profile</legend>

                    <div>
                        <label for="userName" style="width: 120px;">User Name: </label>

                        <div id="userName" style="margin-top:-15px;margin-left:125px;"></div>
                    </div>
                    <div>
                        <label for="email" style="width: 120px;">Email: </label>
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
                            <img id="loader" src="/image/loader.gif" alt="loader">
                            <!-- 							<i class="icon-save"></i>  -->
                            <img src="/image/icons/light/fa-save.png" alt="Save Icon" style="margin-bottom:-1px;"/>
                            <span>Save</span>
                        </button>
                        <button class="btn-signin btn-light" type="button" id="cancel" name="cancel">
                            <!-- 							<i class="icon-ban-circle"></i>  -->
                            <img src="/image/icons/dark/fa-ban.png" alt="Cancel Icon" style="margin-bottom:-2px;"/>
                            Cancel
                        </button>
                    </div>
                </fieldset>
            </form>
        </div>
        <div id="dialogSuccess">
            You have successfully updated your information.
        </div>
        <div id="dialogError">
            Unknown error has occurred.
        </div>
    </div>

    <p>
        Please provide all required information.
        <br/>
        <em>* All fields are required.</em>
    </p>

    <div class="clearfix"></div>
</div>
<!-- .container -->

<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
