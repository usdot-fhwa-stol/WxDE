<%@ page import="java.io.Console" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<style type="text/css">
    label {
        display: inline-block;
    }

    #rdCommentType {
        margin-left: 22px;
    }

    #feedbackForm #rdCommentType label {
        width: 160px;
    }

    #feedbackForm label {
        width: 100px;
        vertical-align: top;
    }
</style>
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
                if (commentType == "radio1") {
                    feedbackInfo.feedbackTypeId = "1";
                } else if (commentType == "radio2") {
                    feedbackInfo.feedbackTypeId = "2";
                } else {
                    if ($('#rdBugType1').is(':checked')) {
                        feedbackInfo.feedbackTypeId = "3";
                    } else if ($('#rdBugType2').is(':checked')) {
                        feedbackInfo.feedbackTypeId = "4";
                    } else {
                        feedbackInfo.feedbackTypeId = "5";
                    }
                }

                $.ajax({
                    url: "/resources/feedback",
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
            height: 520,
            width: 550,
            modal: true,
            open: function () {
                $("#txtPageUrl").val($(location).attr('href'));
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
</script>

<table cellpadding="0" cellspacing="0" border="0" width="1000" align="center" bgcolor="#BDD2FF">
    <tbody>
    <tr>
        <td>
            <ul class="sf-menu" id="wdeMenu" style="float: none;">
                <li class="current"><a href="/">Home</a></li>
                <li class="current"><a href="#">Data</a>
                    <ul>
                        <li class="current"><a href="#">Summary Map</a></li>
                        <% if (request.getUserPrincipal() != null) { %>
                        <li class="current">
                            <a href="#">Observations</a>
                            <ul>
                                <li class="current"><a href="#">Contributor</a></li>
                                <li class="current"><a href="#">Coordinates</a></li>
                            </ul>
                        </li>
                        <li class="current"><a href="#">Data Subscriptions</a></li>
                        <li class="current"><a href="#">Metadata</a></li>
                        <li class="current"><a href="#">RDE Data</a></li>
                        <% } else { %>
                        <li>
                            <a href="#">Observations</a>
                            <ul>
                                <li><a href="#">Contributor</a></li>
                                <li><a href="#">Coordinates</a></li>
                            </ul>
                        </li>
                        <li><a href="#">Data Subscriptions</a></li>
                        <li><a href="#">Metadata</a></li>
                        <li><a href="#">RDE Data</a></li>
                        <% } %>
                    </ul>
                </li>
                <li class="current"><a href="#">Community</a>
                    <ul>
                        <li class="current"><a href="#">WxDE Reports &nbsp; Presentations</a></li>
                        <li class="current"><a href="#">Discussion Groups</a></li>
                        <li class="current"><a href="#">External Links</a></li>
                    </ul>
                </li>
                <li class="current"><a href="#">About</a>
                    <ul>
                        <li class="current"><a href="#">History</a></li>
                        <li class="current"><a href="#">Data Sources</a></li>
                        <li class="current"><a href="#">Terms of Use</a></li>
                        <li class="current"><a href="#">FAQ</a></li>
                    </ul>
                </li>

                <li class="current" style="border-right: 1px solid #FFFFFF;">
                    <a href="#" onclick="$( '#dialogFeedback' ).dialog( 'open' );">Feedback</a>
                </li>

                <% if (request.getUserPrincipal() != null) { %>
                <li class="current" style="float: right;">
                    <a href="#"><%= request.getUserPrincipal().getName() %>
                    </a>
                    <ul>
                        <li class="current"><a href="/auth/userEdit.html">Manage</a></li>
                        <li class="current"><a href="/auth/logout.jsp">Logout</a></li>
                    </ul>
                </li>
                <% } else { %>

                <li class="current" style="float: right;"><a href="/auth/loginRedirect.jsp">Login</a>
                    <ul>
                        <li class="current"><a href="/user.html">Registration</a>
                    </ul>
                </li>
                <% } %>
            </ul>
        </td>
    </tr>
    </tbody>
</table>

<div id="dialogFeedback" title="Please provide your name (optional), email address and your feedback">
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
            <label for="txtEmail">E-Mail *</label>
            <input id="txtEmail" name="txtEmail" size="50" class="ui-widget-content" type="email" required/>
        </p>

        <p>
            <label for="txtPageUrl">Section *</label>
            <input id="txtPageUrl" name="txtPageUrl" size="50" class="ui-widget-content" type="text"
                   readonly="readonly"/>
        </p>

        <div id="bugSection">
            <p>
                <%--<label for="rdBugTypeFieldSet">Bug Report *</label>--%>
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

        <%--<p tabindex="0">* Required</p>--%>
    </form>
</div>