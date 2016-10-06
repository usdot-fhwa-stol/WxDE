<%@page import="org.owasp.encoder.Encode"%>
<script>
    $(document).ready(function () {

        $("#wdeMenu").superfish();

        // use custom tooltip; disable animations for now to work around lack of refresh method on tooltip
        $("#feedbackForm").tooltip({
            show: false,
            hide: false
        });

        // validate the comment form when it is submitted
        $("#feedbackForm").validate({
            onfocusout: true,
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
                feedbackInfo.kaptcha = $("#kaptcha").val();

                $.ajax({
                    url: "/resources/feedback",
                    type: "POST",
                    contentType: "application/json",
                    data: JSON.stringify(feedbackInfo),
                    dataType: 'json',
                    success: function (data) {
                        console.log('data', data);

                        status = data.status2;
                        message = data.message;

                        if (status !== "Success") {
                            console.log('Error encountered: ' + message);
                        }

                        $("#dialogFeedback").dialog("close");
                    },
                    error: function (xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
                    }
                });
            }
        });

        $("#dialogFeedback").dialog({
            autoOpen: false,
            height: 600,
            width: 550,
            modal: true,
            open: function() {
                $("#txtPageUrl").val($(location).attr('href'));
                $('#kaptchaImage').attr('src', '/kaptcha.jpg?' + Math.floor(Math.random()*100) );

                $.ajax({
                    url: "/resources/user",
                    dataType: "json",
                    success: function (data) {
                        console.log('data', data);

                        if (data == null) {
                            return;
                        }

                        $("#txtName").val(data.firstName + " " + data.lastName);
                        $("#txtEmail").val(data.email);
                        $("#txtName").attr('readonly', true);
                        $("#txtEmail").attr('readonly', true);
                    },
                    error: function(xhr, ajaxOptions, thrownError) {
                        console.log(xhr.status);
                        console.log(thrownError);
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

        $('.non-link-cursor').bind('click', function (event) {
            event.preventDefault();
        });
    });
</script>

<a href="#" class="skip-link" data-skip="wdeMenu">Skip to Local Navigation</a>

<ul class="sf-menu" id="wdeMenu" style="float: none;">
    <li id="homePage-li" class="current main-nav"><a href="/"><img src="/image/icons/light/fa-home.png"
                                                                class="main-menu-icons" id="home-link" alt="Home"/> Home</a>
    </li>
    <li id="dataPage-li" class="current main-nav"><a href="#" class="non-link-cursor" alt="Data">
        <img src="/image/icons/light/fa-cloud-download.png" class="main-menu-icons" alt="Data Icon"/>
        Data</a>
        <ul>
            <li class="current"><a href="/summaryMap.jsp">Summary Map</a></li>
            <li>
                <a href="#" class="non-link-cursor" title="Observations">Observations</a>
                <ul>
                    <% if (request.getUserPrincipal() == null || request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
                    <li><a href="/auth2/wizardContributor.jsp">
                        Contributor
                        <% if (request.getUserPrincipal() == null) {%>
                        <!-- 									<i class="icon-lock" style="margin-left:16px;"></i> -->
                        <img src="/image/icons/light/fa-lock.png" style="margin-left: 17px;" alt="Locked"/>
                        <% } %>
                    </a></li>
                    <% } %>
                    <li><a href="/auth/wizardGeospatial.jsp">
                        Coordinates
                        <% if (request.getUserPrincipal() == null) {%>
                        <!-- 									<i class="icon-lock" style="margin-left:12px;"></i> -->
                        <img src="/image/icons/light/fa-lock.png" style="margin-left: 12px;" alt="Locked"/>
                        <% } %>
                    </a></li>
                </ul>
            </li>
            <% if (request.getUserPrincipal() == null || request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
            <li><a href="/auth2/Subscriptions.jsp" title="Data Subscriptions">
                Data Subscriptions
                <% if (request.getUserPrincipal() == null) {%>
                <!-- 							<i class="icon-lock" style="margin-left:12px;"></i> -->
                <img src="/image/icons/light/fa-lock.png" style="margin-left: 12px;" alt="Locked"/>
                <% } %>
            </a></li>
            <li><a href="#">Metadata</a>
                <ul>
                    <li><a href="/auth2/metadata.jsp">
                        Files
                        <% if (request.getUserPrincipal() == null) {%>
                        <!-- 									<i class="icon-lock" style="margin-left:17px;"></i> -->
                        <img src="/image/icons/light/fa-lock.png" style="margin-left: 17px;" alt="Locked"/>
                        <% } %>
                    </a></li>
                    <li><a href="/auth2/dataSource.jsp">
                        Tables
                        <% if (request.getUserPrincipal() == null) {%>
                        <!-- 									<i class="icon-lock" style="margin-left:12px;"></i> -->
                        <img src="/image/icons/light/fa-lock.png" style="margin-left: 12px;" alt="Locked"/>
                        <% } %>
                    </a></li>
                </ul>
            </li>
            <% } %>
        </ul>
    </li>

    <li id="aboutPage" class="current"><a href="#" class="non-link-cursor">
        <img src="/image/icons/light/fa-question-o.png" class="main-menu-icons" alt="Question"/>
        About</a>
        <ul>
            <li class="current"><a href="/newsPage.jsp">News</a></li>
            <li class="current"><a href="/changeLog.jsp">Change Logs</a></li>
            <li class="current"><a href="/termsOfUse.jsp">Terms of Use</a></li>
            <li class="current"><a href="/frequentlyAskedQuestions.jsp">FAQ</a></li>
            <li class="current"><a href="/siteMap.jsp">Site Map</a></li>
        </ul>
    </li>

    <li class="current">
        <a href="#" onclick="$( '#dialogFeedback' ).dialog( 'open' );">
            <img src="/image/icons/light/fa-pencil.png" class="main-menu-icons" style="height:16px; width:16px;"
                 alt="Feedback"/>
            Feedback</a>
    </li>

    <% if (request.getUserPrincipal() != null) { %>
    <li id="userPage" class="current main-nav" style="float: right;">
        <a href="#" class="non-link-cursor">
            <img src="/image/icons/light/fa-user.png" class="main-menu-icons"
                 title="<%= Encode.forHtmlAttribute( request.getUserPrincipal().getName() ) %>" alt="User"/>
            <%= Encode.forHtml( request.getUserPrincipal().getName()) %>
        </a>
        <ul>
            <% if (!request.isUserInRole("wde_limited")) { %>
            <li class="current"><a href="/auth2/userEdit.jsp">Manage</a></li>
            <% } %>
            <li class="current"><a href="/auth/logout.jsp">Logout</a></li>
        </ul>
    </li>
    <% if (request.isUserInRole("wde_admin")) { %>
    <li id="adminPage" class="current main-nav" style="float: right;">
        <a href="#" class="non-link-cursor">
            <img src="/image/icons/light/fa-cogs.png" class="main-menu-icons" alt="Admin"/>
            Admin
        </a>
        <ul>
            <li class="current"><a href="/admin/feedbacks.jsp" title="User Feedback">User Feedback</a></li>
            <li class="current"><a href="/admin/reports.jsp" title="Reports">Reports</a></li>
            <li class="current"><a href="/wdeMap.jsp?lat=46&lon=-108&zoom=4" title="Full Data Map">Full Data Map</a>
            </li>
        </ul>
    </li>
    <% } %>
    <% } else { %>
    <li class="current main-nav" id="userPage" style="float: right;"><a href="/auth/loginRedirect.jsp" title="Login">
        <img src="/image/icons/light/fa-signin.png" class="main-menu-icons" style="height:14px; width:14px;"
             alt="Signin"/>
        Login</a>
        <ul>
            <li class="current"><a href="/userRegistration.jsp" title="Registration">Registration</a>
        </ul>
    </li>
    <% } %>
</ul>


<div id="dialogFeedback" title="Please provide your name (optional), business email, and your feedback"
     style="display:none">
    <form id="feedbackForm" class="cmxform" method="get" action="">

        <legend style="display: none;">Feedback</legend>

        <div id="rdCommentType">
            <fieldset id="rdCommentTypeFieldSet" style="border-width: 0px 0px 0px 0px; margin: 0px 0px 0px 0px;">
                <legend style="display: none;">Comment Type</legend>

                <input type="radio" id="radio1" name="rdCommentType" checked="checked"/><label
                    for="radio1">Feedback</label>
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
                <%--<label for="bugReportFieldSet">Bug Report *</label>--%>
            <fieldset id="bugReportFieldSet">
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

        <div>
            <label for="kaptcha-div" style="float: left;">Verification Code *</label>
            <div id="kaptcha-div" style="display: inline">
                <input width="200px" type="text" value="" name="kaptcha" id="kaptcha">
                <br>
                <img width="200px" id="kaptchaImage" src="/kaptcha.jpg">
                <br>
                <small>Can't read the image? Click it to get a new one.</small>
                <script type="text/javascript">
                    $(function(){
                        $('#kaptchaImage').click(function () { $(this).attr('src', '/kaptcha.jpg?' + Math.floor(Math.random()*100) ); })
                    });
                </script>
            </div>
        </div>

    </form>
</div>