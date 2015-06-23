<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription"/>
<jsp:setProperty name="oSubscription" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
        <jsp:param value="Observation Types Wizard" name="title"/>
    </jsp:include>
    <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

    <!-- Page specific Stylesheet -->
    <link href="/style/WizardObsTypes.css" rel="stylesheet" media="all">

    <!-- Page specific JavaScript -->
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/WizardObsTypes.js" type="text/javascript"></script>

    <script type="text/javascript">
        $(document).ready(function () {
            $('#dataPage, #dataPage a').addClass('active');
        });
    </script>
</head>

<body onload="onLoad()">
<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

<div class="container">

    <div id="titleArea" class='pull-right'>
        <div id="timeUTC"></div>
    </div>

    <h1>Observation Types</h1>

    <div class="col-5" style="margin-top: -15px;">
        <div id="obsArea">
            <table id="tblObs" border="0">
                <caption style="display: none;">Area</caption>
                <tr>
                    <th id="hdrObs"><label for="listObsTypes">Observation Type</label></th>
                    <th id="hdrMin"><label for="txtMin">Minimum</label></th>
                    <th id="hdrMax"><label for="txtMax">Maximum</label></th>
                </tr>
                <tr>
                    <td>
                        <select id="listObsTypes" onchange="ObsTypeChanged()"></select>
                    </td>
                    <td>
                        <input id="txtMin" type="text" size="10"
                               onkeypress="return NumbersOnly(this, event)"/></td>
                    <td>
                        <input id="txtMax" type="text" size="10"
                               onkeypress="return NumbersOnly(this, event)"/></td>
                </tr>
            </table>
        </div>

        <div id="qcTestsArea">
            <h4 id="hdrTestArea">Quality Checks</h4>
            <table id="tblTests" class="qualityCheckFlags"></table>
        </div>

        <div id="hourlyMessageArea"
             style="width: 90%; margin-top: 48px; font-weight: bold;">
            <br> If you need more continuous data, it is recommended that
            you create a subscription instead of running a report.
        </div>

        <br>
        <button class="btn-dark" type="button" id="runQuery"
                onclick="RunQuery()">
            <img src="/image/icons/light/fa-file-alt.png" alt="Report Icon" style="margin-bottom: -1px"/>
            Run Report
        </button>
        <% if (request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
        <button class="btn-dark" type="button" id="subscript"
                onclick="Subscribe()">
            <img src="/image/icons/light/fa-add.png" alt="Subscribe Icon" style="margin-bottom: -1px"/>
            Subscribe
        </button>
        <% } %>

        <form action="" method="post">
            <input id="runQueryUrl" type="hidden"
                   value="<%= response.encodeURL("/auth/WizardTimeRange.jsp") %>"/>
            <% if (request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
            <input id="subscribeUrl" type="hidden"
                   value="<%= response.encodeURL("/auth2/WizardSubscribe.jsp") %>"/>
            <% } %>
            <input id="obs" name="obs" type="hidden" value=""/>
            <input id="flags" name="flags" type="hidden" value=""/>
        </form>
    </div>
    <div id="instructions" class="col-4"
         style="margin: 0; margin-top: -15px;">
        <h3>Instructions</h3>

        <p>
            Specify the observation type to retrieve by selecting it from the <b><i>Observation
            Type</i></b> listbox, or leave it on "All Observations" to retrieve all
            observations. <br/> <br/> When you select a specific observation
            type, you will be presented with optional entry fields for the
            minimum and maximum values, as well as listboxes for each of the
            Quality Checks that are valid for that observation type. Supplying
            values for the minimum and/or maximum will filter the observations
            retrieved to those values that are within the specified range. <br/>
            <br/> You can also filter an observation based on its Quality
            Checks by selecting "P" (Pass) or "N" (Not Pass) from its respective
            drop-down list.
            <br/>
            <br/>
            Besides using the mouse, you can also use the tab key or shift-tab key combination to
            traverse elements on the page.
            Once a list is in focus, use the the Up and Down arrow keys to make your selection. You
            can also gently press the space bar to display the dropdown list.
            <br/>
            <br/>
        </p>

        <div id="statusMessage" class="msg"
             style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
            &nbsp;</div>
    </div>
    <div class='clearfix'></div>
    <br>
</div>

<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
