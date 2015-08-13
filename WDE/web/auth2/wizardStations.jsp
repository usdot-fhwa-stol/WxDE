<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription" %>
<%@ page import="org.apache.commons.lang3.StringEscapeUtils"%>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription"/>
<jsp:setProperty name="oSubscription" property="*"/>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
        <jsp:param value="Select Stations" name="title"/>
    </jsp:include>
    <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

    <!-- Page specific Javascript files -->
    <script src="/script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="/script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="/script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="/script/WizardStations.js" language="javascript" type="text/javascript"></script>

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
    </style>

    <script type="text/javascript">
        $(document).ready(function () {
            $('#dataPage, #dataPage a').addClass('active');
        });
    </script>
</head>

<body onload='onLoad("<%= StringEscapeUtils.escapeHtml4(oSubscription.getContributors()) %>")'>
<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

<div class="container">

    <div id="titleArea" class='pull-right'>
        <div id="timeUTC"></div>
    </div>

    <div id="linkArea2" class="col-5" style="margin-top:-15px;">
        <table id="tblTossItems" role="presentation">
            <caption style="display: none;">Stations</caption>
            <tr>
                <td><strong>All Stations</strong></td>
            </tr>
            <tr>
                <td>
                    <select title="All Stations" id="listAll" style="width: 484px;" size="10" multiple="1"
                            ondblclick="Add()"></select>
                </td>
            </tr>
            <tr>
                <td align="center">
                    <br/>
                    &nbsp;&nbsp;<input id="btnAdd" type="button" value="&nbsp;\/&nbsp;" onclick="Add()"/>
                    &nbsp;&nbsp;<input id="btnRemove" type="button" value="&nbsp;/\&nbsp;" onclick="Remove()"/>
                </td>
            </tr>
            <tr>
                <td><strong>Selected Stations</strong></td>
            </tr>
            <tr>
                <td>
                    <select id="listSel" title="Selected Stations" style="width: 484px;" size="10" multiple="1"
                            ondblclick="Remove()"></select>
                </td>
            </tr>
            <tr>
                <td class="btnNext">
                    <br>
                    <button id="btnNext" type="button" class="btn-dark" onclick="Validate()">
                        Next Page
                        <img src="/image/icons/light/fa-arrow-right.png" alt="Right Arrow" style="margin-bottom: -1px"/>
                    </button>
                </td>
            </tr>
        </table>

        <form action='<%= response.encodeURL("/auth/wizardObsTypes.jsp") %>' method="post">
            <input id="stations" name="stations" type="hidden" value=""/>
        </form>
    </div>
    <div id="instructions" class="col-4" style="margin:0; margin-top:-15px;">
        <h3>Instructions</h3>

        <p>
            Each entry in the station list consists of four fields: contributor, station category
            (p for permanent, T for transportable and M for mobile), station code, and whether the
            station carries recent observations (0 for no observations, 1 for having observations quality
            checked using the native WxDE algorithms, 2 for having observations using the VDT quality
            checking algorithms, and 3 for having observations using both).
            <br/>
            <br/>
            Select one or more stations from the <b><i>All Stations</i></b> listbox
            and move them to the <b><i>Selected Stations</i></b> listbox by pressing the
            <input type="button" value="&nbsp;\/&nbsp;" disabled="disabled"> button.
            To remove a station from the
            <b><i>Selected Stations</i></b> listbox, select it and press the
            <input type="button" value="&nbsp;/\&nbsp;" disabled="disabled"> button.
            <br/>
            <br/>
            You can select more than one item in the list by pressing the <i>Ctrl</i> or
            <i>Shift</i> key when making your selection.
            <br/>
            <br/>
            Besides using the mouse, you can also use the tab key or shift-tab key combination to traverse elements on
            the page. Once a list is in focus, use the the Up and Down arrow keys to select stations. Use
            the spacebar to add/remove stations after station is highlighted in one of the two lists.
            <br/>
            <br/>
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

<script type="text/javascript">
</script>
</body>
</html>
