<%@page import="org.owasp.encoder.Encode"%>
<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription"/>
<jsp:setProperty name="oSubscription" property="*"/>
<%
    // Let queryResults know that the Subscription was created as part of the wizard.
    oSubscription.setWizardRunning(true);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
        <jsp:param value="Time and Format Wizard" name="title"/>
    </jsp:include>
    <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

    <script src="/script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="/script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="/script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="/script/WizardTimeRange.js" type="text/javascript"></script>
    <script type="text/javascript">
        $(function () {

            $.datepicker.setDefaults({
                showOn: "both",
                buttonImage: "/image/theme/calendar.gif",
                buttonImageOnly: true,
                numberOfMonths: 3,
                changeMonth: true,
                changeYear: true,
                maxDate: 0,
                showButtonPanel: true,
                constraintInput: true
            });

            $("#startDate").datepicker({
                buttonText: "Enter start date",
                minDate: "-5Y",
                onClose: function (selectedDate) {
                    $("#endDate").datepicker("option", "minDate", selectedDate);
                    checkHours();
                }
            });

            $("#endDate").datepicker({
                buttonText: "Enter end date",
                onClose: function (selectedDate) {
                    $("#startDate").datepicker("option", "maxDate", selectedDate);
                    checkHours();
                }
            });

            $("#docFormat").buttonset();
            $('label[for="radio1"]').removeClass("ui-state-active");

            $("#btnNext").click(function (event) {
                if ($("#startDate").val() && $("#endDate").val()) {
                    Validate();
                } else {
                    alert("The Start Date and End Date must contain valid dates.");
                }
            });

            $("#startTime").change(function () {
                checkHours();
            });

            $("#endTime").change(function () {
                checkHours();
            });

            //$("#endTime").val("01");

            function checkHours() {
                if ($("#startDate").val() == $("#endDate").val()) {
                    //make sure endTime falls after startTime
                    if ($("#startTime").val() >= $("#endTime").val()) {
                        if ($("#startTime").val() != 23) {
                            // advance one hour
                            var startTimePlusOne = parseInt($("#startTime").val()) + 1;
                            if (startTimePlusOne < 10) {
                                startTimePlusOne = "0" + startTimePlusOne;
                            }
                            $("#endTime").val(startTimePlusOne);

                            alert("The Start must come before the End.  The End Time has been advanced to 1 hour.");

                        } else {
                            // since 11pm has been selected, advance one day
                            var startDate = new Date($("#startDate").val());
                            var dayAfterStartDate = new Date($("#startDate").val());
                            dayAfterStartDate.setDate(startDate.getDate() + 1);

                            $("#endDate").datepicker("setDate", dayAfterStartDate);
                            $("#endTime").val("00");
                            alert("The Start must come before the End.  The End Date has been advanced by one day.");
                        }
                    }
                }
            }

        });
    </script>
    <style>
        .container a {
            color: #006699;
            text-decoration: none;
        }

        .container a:hover {
            text-decoration: underline;
        }

        .tblHdr, .tblFld {
            text-align: justify !important;
            padding: 10px 5px 10px 15px !important;
            font-size: 1.1em !important;
        }

        table {
            border-collapse: separate !important;
        }

        .btnNext {
            padding-left: 0px !important;
        }

        .ui-datepicker-trigger {
            cursor: pointer;
        }
    </style>
</head>

<body onload='onLoad("<%= Encode.forJavaScript(oSubscription.getContributors() )%>")' id="dataPage">
<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

<div class="container">
    <div id="titleArea" class='pull-right'>
        <div id="timeUTC"></div>
    </div>
    <h1>Time and Format</h1>
    <br/>

    <div id="linkArea2" class="col-5" style="margin-top:-15px;">

        <form action='<%= response.encodeURL("queryResults.jsp") %>' method="post" target="_blank">

            <table cellspacing="10" align="center" role="presentation">
                <caption style="display: none;">Time and Format</caption>
                <%--<tr>--%>
                    <%--<td>&nbsp;</td>--%>
                <%--</tr>--%>
                <%--<tr>--%>
                    <%--<td>&nbsp;</td>--%>
                <%--</tr>--%>
                <tr>
                    <td align="right"><label for="startDate">Start Date</label></td>
                    <td><input id="startDate" name="startDate" type="text" size="10"/></td>
                    <td align="right"><label for="startTime">Start Time</label></td>
                    <td>
                        <select id="startTime">
                            <option value="" selected="selected">Choose a starting time...</option>
                            <!-- build option 0000 to 0900 -->
                            <% for (int x = 0; x <= 9; x++) { %>
                            <option value="0<%= x %>">0<%= x %>00</option>
                            <% } %>
                            <!-- build option 1000 to 2300 -->
                            <% for (int x = 10; x <= 23; x++) { %>
                            <option value="<%= x %>"><%= x %>00</option>
                            <% } %>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td align="right"><label for="endDate">End Date</label></td>
                    <td><input id="endDate" name="endDate" type="text" size="10"/></td>
                    <td align="right"><label for="endTime">End Time</label></td>
                    <td>
                        <select id="endTime">
                            <option value="" selected="selected">Choose an ending time...</option>
                            <!-- build option 0000 to 0900 -->
                            <% for (int x = 0; x <= 9; x++) { %>
                            <option value="0<%= x %>">0<%= x %>00</option>
                            <% } %>
                            <!-- build option 1000 to 2300 -->
                            <% for (int x = 10; x <= 23; x++) { %>
                            <option value="<%= x %>"><%= x %>00</option>
                            <% } %>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td align="right"><label for="docFormat">Format</label></td>
                    <td colspan="3" align="left">
                        <div id="docFormat">
                            <fieldset title="Document Format" id="">
                                <legend style="display: none;">Document Format</legend>

                                <input type="radio" id="radio_cmml" name="docFormat" value="CMML"/><label
                                    for="radio_cmml">CMML</label>

                                <input type="radio" id="radio_csv" name="docFormat" value="CSV" checked="checked"/><label
                                    for="radio_csv">CSV</label>

                                <input type="radio" id="radio_xml" name="docFormat" value="XML"/><label
                                    for="radio_xml">XML</label>
                            </fieldset>
                        </div>
                    </td>
                </tr>
            </table>

            <input id="timeRange" name="timeRange" type="hidden" value=""/>
            <input id="format" name="format" type="hidden" value=""/>
        </form>

        <button class="btn-dark" id="btnNext" type="button">
            Download
            <img src="/image/icons/light/fa-arrow-right.png" alt="Right Arrow" style="margin-bottom: -1px"/>
        </button>

    </div>
    <div id="instructions" class="col-4" style="margin:0; margin-top:-15px;">
        <h3>Instructions</h3>

        <p>
            Specify a start and end date and time for the request.
            <br/>
            <br/>
            You may also select the output format for the report. The default is
            a comma separated value report.
            <br/>
            <br/>
            Besides using the mouse, you may also use the tab key or shift-tab key combination to traverse elements
            on the page. For specifying date, type it in the time range boxes using the format MM/DD/YYYY.
            <br/>
            <br/>
            CMML - Canadian Meteorological Markup Language
            <br/>
            CSV - Comma-Separated Value
            <br/>
            XML - eXtensible Markup Language
        </p>

        <div id="statusMessage" class="msg"
             style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
            &nbsp;
            <!-- <img src="/image/close-img.svg" id="close-msg" style="float:right; cursor: pointer; margin-top:2px;" /> -->
        </div>
    </div>
    <div class='clearfix'></div>
    <br>
</div>

<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</body>
</html>