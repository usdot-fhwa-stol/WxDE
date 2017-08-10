<%@page import="org.owasp.encoder.Encode"%>
<script>
    $(document).ready(function () {

        // use custom tooltip; disable animations for now to work around lack of refresh method on tooltip
        $("#feedbackForm").tooltip({
            show: false,
            hide: false
        });

        // validate the comment form when it is submitted
        $("#feedbackForm").validate({
            rules: {
                txtEmail: {
                    required: true,
                    email: true
                },
                txtPageUrl: {
                    required: true
                },
                txtDescription: {
                    required: true
                }
            },
            messages: {
                txtEmail: {
                    required: "Email is required",
                    email: "Please, enter a valid Email address"
                },
                txtPageUrl: {
                    required: "Page URL is required"
                },
                txtDescription: {
                    required: "Bug description is required"
                }
            },
            showErrors: function (map, list) {
                // there's probably a way to simplify this
                var focussed = document.activeElement;
                if (focussed && $(focussed).is("input, textarea")) {
                    $(this.currentForm).tooltip("close", {currentTarget: focussed}, true);
                }
                this.currentElements.removeAttr("title").removeClass("ui-state-highlight");
                $.each(list, function (index, error) {
                    $(error.element).attr("title", error.message).addClass("ui-state-highlight");
                });
                if (focussed && $(focussed).is("input, textarea")) {
                    $(this.currentForm).tooltip("open", {target: focussed});
                }
            },
            submitHandler: function () {

                var feedbackInfo = new Object();
                feedbackInfo.name = $("#txtName").val();
                feedbackInfo.email = $("#txtEmail").val();
                feedbackInfo.section = $("#txtPageUrl").val();
                feedbackInfo.description = $("#txtDescription").val();
                var commentType = $("#rdCommentType input[type=radio]:checked")[0].id;
                feedbackInfo.feedbackType = new Object();
                if (commentType == "radio1") {
                    feedbackInfo.feedbackType.id = "1";
                } else if (commentType == "radio2") {
                    feedbackInfo.feedbackType.id = "2";
                } else {
                    if ($('#rdBugType1').is(':checked')) {
                        feedbackInfo.feedbackType.id = "3";
                    } else if ($('#rdBugType2').is(':checked')) {
                        feedbackInfo.feedbackType.id = "4";
                    } else {
                        feedbackInfo.feedbackType.id = "5";
                    }
                }
                $.ajax({
                    url: "<%= response.encodeURL("/resources/feedback")%>",
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify(feedbackInfo),
                    success: function () {
                    },
                    error: function () {
                    }
                });

                $("#dialogFeedback").dialog("close");
            }
        });

        $("#dialogFeedback").dialog({
            autoOpen: false,
            height: "auto",
            width: "auto",
            modal: true,
            open: function () {
                $("#txtPageUrl").val($(location).attr('href'));
                $.ajax({
                    url: "<%= response.encodeURL("/resources/user")%>",
                    dataType: "json",
                    success: function (resp) {
                        $("#txtName").val(resp.firstName + " " + resp.lastName);
                        $("#txtEmail").val(resp.email);

                        $("#txtName").attr('readonly', true);
                        $("#txtEmail").attr('readonly', true);
                    }
                });
            },
            buttons: {
                "Submit Feedback": function () {
                    $("#feedbackForm").submit();
                },
                Cancel: function () {
                    $(this).dialog("close");
                }
            }
        });

        $("#rdCommentType").buttonset({
            width: 500
        });

        if ($("#rdCommentType input[type=radio]:checked")[0].id != "radio3") {
            $("#bugSection").hide();
        }

        $("#radio1").click(function () {
            $("#bugSection").hide();
        });
        $("#radio2").click(function () {
            $("#bugSection").hide();
        });
        $("#radio3").click(function () {
            $("#bugSection").show();
        });
    });

    $(document).ready(function () {

        $('.subdrop-trigger > a, .subdrop-trigger > a > img')
                .on('mouseenter', function () {
                    $(this).next('.sub-drop').addClass('open');
                })
                .on('mouseleave', function () {
                    $(this).next('.sub-drop').removeClass('open');
                })
                .on('focus', function () {
                    $(this).next('.sub-drop').addClass('open');
                })
                .on('blur', function () {
                    $(this).next('.sub-drop').removeClass('open');
                });

        $('.sub-drop a')
                .on('focus', function () {
                    $(this).closest('.sub-drop').addClass('open');
                })
                .on('blur', function () {
                    $(this).closest('.sub-drop').removeClass('open');
                })
                .on('mouseenter', function () {
                    $(this).closest('.sub-drop').addClass('open');
                })
                .on('mouseleave', function () {
                    $(this).closest('.sub-drop').removeClass('open');
                });

        $('.drop-trigger > a, .drop-list a')
                .on('focus', function () {
                    $(this).next('.drop-list').addClass('open');
                })
                .on('blur', function () {
                    $(this).next('.drop-list').removeClass('open');
                })
                .on('mouseover', function () {
                    $(this).next('.drop-list').addClass('open');
                })
                .on('mouseleave', function () {
                    $('.drop-list').removeClass('open');
                });

        $('.drop-list a')
                .focus(function () {
                    $(this).closest('.drop-list').addClass('open');
                })
                .blur(function () {
                    $(this).closest('.drop-list').removeClass('open');
                })
                .on('mouseenter', function () {
                    $(this).closest('.drop-list').addClass('open');
                });

        // 		$('.drop-list').on('hover', function() {
        // 			$(this).addClass('open');
        // 		});

        // 		$('.drop-list').on('mouseout', function() {
        // 			$(this).removeClass('open');
        // 		});

        $('.drop-list img').on('mouseleave', function () {
            $(this).closest('.drop-list').addClass('open');
        });
    });
</script>

<a href="#" class="skip-link" id="accessibility_open">Accessibility controls</a>
<a href="" class="skip-link" data-skip="menu-nav">Skip to local navigation</a>

<!-- top navigation w/ WxDE and FHWA logo -->
<div class="navbar navbar-fixed-top fhwa-gradient-color" id="top-nav">
    <a class="navbar-brand pull-left" href="/">
        <img src="/image/minified-map-ui/WxDE-logo-white.png" class="wxde-logo" alt="WxDE Logo"/>
    </a>

    <div class="navbar-brand pull-right ie-fix-fhwa fhwa-logo-container">
        <img src="/image/minified-map-ui/fhwa-logo.png" class="fhwa-logo" alt="Federal Highway Administration Logo"/>

        <div class="fhwa-links-container">
            <a href="http://www.fhwa.dot.gov/" target="_blank" title="Federal Highway Administration Website Link">
                <img src="/image/minified-map-ui/fhwa-typography.png" alt="FHWA Logo"/>
            </a>

            <p class="tfhrc-container">
                <a href="http://www.fhwa.dot.gov/research/tfhrc/" target="_blank">
                    Turner-Fairbank Highway Research Center
                </a>
            </p>
        </div>
    </div>
</div>

<div id="accessibility-menu" class="mini-ui" style="height: 0;"></div>

<!-- main top mini navigation menu -->
<div class="navbar navbar-fixed-top nav-top" id="menu-nav">
    <ul class="nav-top-menu">
        <!-- Home : Main Menu Item -->
        <li class="ie-fix-nav"><a href="<%= response.encodeURL("/")%>">
            <!-- 				<i class="icon-home"></i>  -->
            <img src="/image/icons/dark/fa-home.png" class="reduce-top-margin-2" alt="Home Icons"/>
            Home
        </a></li>
        <!-- Data : Main Menu Item -->
        <li class="drop-data drop-trigger">
            <a href="#">
                <!-- 					<i class="icon-cloud-download"></i> -->
                <img src="/image/icons/dark/fa-cloud-download.png" class="reduce-top-margin-2" alt="Data Icons"/>
                Data
                <img src="/image/minified-map-ui/caret-down.png" alt="Down"/>
            </a>
            <ul class="drop drop-list drop-data">
                <li><a href="<%= response.encodeURL("/wdeMap.jsp")%>">Summary Map</a></li>
                <li class="drop-observation subdrop-trigger" class="ie-fix-obs-li">
                    <a href="#">Observations <img src="/image/minified-map-ui/caret-right.png"
                                                  class="reduce-top-margin-2" alt="Right"/></a>

                    <% if (request.getUserPrincipal() == null || request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>

                    <%if (request.getUserPrincipal() == null) { %>

                    <ul class="sub-drop drop-observation">
                        <li><a href="<%= response.encodeURL("/auth2/wizardContributor.jsp")%>" class="ie-fix-li-2">
                            Contributor
                            <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                        </a></li>
                        <li><a href="<%= response.encodeURL("/auth/wizardGeospatial.jsp")%>" class="ie-fix-li-2" style="width: 130px;">
                            Coordinates
                            <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                        </a></li>
                        <li><a href="<%= response.encodeURL("/auth/WizardFcstGeospatial.jsp") %>" class="ie-fix-li-2" style="width: 130px;">
                            Forecast
                            <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                        </a></li>
                    </ul>

                    <% } else { %>

                    <ul class="sub-drop drop-observation user-logged-in">
                        <li><a href="<%= response.encodeURL("/auth2/wizardContributor.jsp")%>" class="ie-fix-li-2">
                            Contributor
                        </a></li>
                        <li><a href="<%= response.encodeURL("/auth/wizardGeospatial.jsp")%>" class="ie-fix-li-2" style="width: 130px;">
                            Coordinates
                        </a></li>
                        <li><a href="<%= response.encodeURL("/auth/WizardFcstGeospatial.jsp") %>" class="ie-fix-li-2" style="width: 130px;">
                            Forecast
                        </a></li>
                    </ul>

                    <% } %>

                    <% } %>

                </li>
                <% if (request.getUserPrincipal() == null || request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
                <li><a href="<%= response.encodeURL("/auth2/Subscriptions.jsp")%>">
                    Data Subscription
                    <% if (request.getUserPrincipal() == null) {%>
                    <!-- 							<i class="icon-lock" style="margin-left:12px;"></i> -->
                    <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                    <% } %>
                </a></li>
                <li class="drop-metadata subdrop-trigger">
                    <a href="#">Metadata <img src="/image/minified-map-ui/caret-right.png" style="margin-left:35px;"
                                              alt="Right Icon"/></a>

                    <% if (request.getUserPrincipal() == null) { %>
                    <ul class="sub-drop drop-metadata">
                        <li><a href="<%= response.encodeURL("/auth2/metadata.jsp")%>" class="ie-fix-li-2">
                            Files
                            <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                        </a></li>
                        <li>
                            <a href="<%= response.encodeURL("/auth2/dataSource.jsp")%>" class="ie-fix-li-2" style="width: 130px;">
                                Tables
                                <img src="/image/icons/dark/fa-lock.png" class="reduce-top-margin-2" alt="Lock Icon"/>
                            </a>
                        </li>
                    </ul>
                    <% } else { %>
                    <ul class="sub-drop drop-metadata user-logged-in">
                        <li><a href="<%= response.encodeURL("/auth2/metadata.jsp")%>" class="ie-fix-li-2">
                            Files
                        </a></li>
                        <li>
                            <a href="<%= response.encodeURL("/auth2/dataSource.jsp")%>" class="ie-fix-li-2" style="width: 130px;">
                                Tables
                            </a>
                        </li>
                    </ul>
                    <% } %>
                </li>
                <li><a href="<%= response.encodeURL("/auth2/archive.jsp") %>">Archive</a></li>
                <% } %>
            </ul>
        </li>
        <!-- About : Main Menu Item -->
        <li class="drop-about drop-trigger">
            <a href="aboutPage.jsp">
                <!-- 					<i class="icon-question-sign"></i>  -->
                <img src="/image/icons/dark/fa-question-o.png" class="reduce-top-margin-2" alt="About Icon"/>
                About <img src="/image/minified-map-ui/caret-down.png" alt="Down Icon"/>
            </a>
            <ul class="drop drop-list drop-about">
                <li class="current"><a href="<%= response.encodeURL("/newsPage.jsp")%>">News</a></li>
                <li class="current"><a href="<%= response.encodeURL("/changeLog.jsp")%>">Change Logs</a></li>
                <li><a href="<%= response.encodeURL("/termsOfUse.jsp")%>">Terms of Use</a></li>
                <li><a href="<%= response.encodeURL("/frequentlyAskedQuestions.jsp")%>">FAQ</a></li>
                <li><a href="<%= response.encodeURL("/siteMap.jsp")%>">Site Map</a></li>
            </ul>
        </li>
        <li><a href="#" onclick="$( '#dialogFeedback' ).dialog( 'open' );">
            <!-- 				<i class="icon icon-edit"></i> -->
            <img src="/image/icons/dark/fa-pencil.png" class="reduce-top-margin-2" alt="Feedback Icon"/>
            Feedback</a></li>
        <li class="drop-user drop-trigger" style="float:right;">
                <% if (request.getUserPrincipal() != null) { %>
            <a href="#">
                <!-- 						<i class="icon-user"></i> -->
                <img src="/image/icons/dark/fa-user.png" class="reduce-top-margin-2" alt="User Icon"/>
                <%= Encode.forHtml(request.getUserPrincipal().getName()) %>
                <img src="/image/minified-map-ui/caret-down.png" alt="Down Icon"/>
            </a>
            <ul class="drop drop-list drop-user">
                <% if (!request.isUserInRole("wde_limited")) { %>
                <li><a href="<%= response.encodeURL("/auth2/userEdit.jsp")%>">Manage</a></li>
                <% } %>
                <li><a href="<%= response.encodeURL("/auth/logout.jsp")%>">Logout</a></li>
            </ul>
                <% if (request.isUserInRole("wde_admin")) { %>
        <li class="drop drop-trigger drop-admin" style="float:right;">
            <a href="#">
                <!-- 							<i class="icon-cogs"></i> -->
                <img src="/image/icons/dark/fa-cogs.png" class="reduce-top-margin-2" alt="Admin Icon"/>
                Admin
                <img src="/image/minified-map-ui/caret-down.png" alt="Down Icon"/>
            </a>
            <ul class="drop drop-list drop-admin">
                <li><a href="<%= response.encodeURL("/admin/feedbacks.jsp")%>">User Feedback</a></li>
                <li><a href="<%= response.encodeURL("/admin/reports.jsp")%>">Reports</a></li>
                <li><a href="<%= response.encodeURL("/wdeMap.jsp?lat=46&lon=-108&zoom=4")%>">Full Data Map</a></li>
            </ul>
        </li>
        <% } %>
        <% } else { %>
        <!-- Login : Main Menu Item -->
        <a href="<%= response.encodeURL("/auth/loginRedirect.jsp")%>">
            <!-- 						<i class="icon-signin"></i> -->
            <img src="/image/icons/dark/fa-signin.png" alt="Login Icon"/>
            Login <img src="/image/minified-map-ui/caret-down.png" alt="Down Icon"/>
        </a>
        <ul class="drop drop-list drop-user">
            <li><a href="<%= response.encodeURL("/userRegistration.jsp")%>">
                Registration
            </a></li>
        </ul>
        <% } %>
        </li>
    </ul>
</div>

<div id="dialogFeedback" title="Please provide your name (optional), business email, and your feedback"
     style="display:none">
    <form id="feedbackForm" class="cmxform" method="get" action="">

        <div id="rdCommentType">
            <fieldset id="rdCommentTypeFieldSet">
                <legend>Comment Type</legend>

                <input type="radio" id="radio1" name="rdCommentType" checked="checked"/><label for="radio1">Feedback</label>
                <input type="radio" id="radio2" name="rdCommentType"/><label for="radio2">New Feature</label>
                <input type="radio" id="radio3" name="rdCommentType"/><label for="radio3">Bug Report</label>
            </fieldset>
        </div>
        <p>
            <label for="txtName">Name</label>
            <input id="txtName" name="txtName" type="text" size="50" class="ui-widget-content"/>
        </p>

        <p>
            <label for="txtEmail">Business Email *</label>
            <input id="txtEmail" name="txtEmail" size="50" class="ui-widget-content" type="email" required/>
        </p>

        <p>
            <label for="txtPageUrl">Section *</label>
            <input id="txtPageUrl" name="txtPageUrl" size="50" class="ui-widget-content" type="text"
                   readonly="readonly"/>
        </p>

        <div id="bugSection">
            <p>
                <%--<label for="rdBugType">Bug Report *</label>--%>
                <fieldset id="rdBugTypeFieldSet">
                    <legend>Bug Report</legend>
                    <input type="radio" id="rdBugType1" name="rdBugType" checked="checked"/><label for="rdBugType1">User Interface</label>
                    <input type="radio" id="rdBugType2" name="rdBugType"/><label for="rdBugType2">Data</label>
                    <input type="radio" id="rdBugType3" name="rdBugType"/><label for="rdBugType3">Broken Link</label>
                </fieldset>
            </p>
        </div>
        <p>
            <label for="txtDescription">Description *</label>
            <textarea id="txtDescription" name="txtDescription" rows="9" cols="50" class="ui-widget-content"
                      required></textarea>
        </p>
        <br>

        <%--<p>* Required</p>--%>
    </form>
</div>