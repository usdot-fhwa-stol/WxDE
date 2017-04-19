<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.FcstSubscription" %>
<jsp:useBean id="oFcstSubscription" scope="session" class="wde.qeds.FcstSubscription"/>
<jsp:setProperty name="oFcstSubscription" property="*"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
        <jsp:param value="Forecast Observation Types Wizard" name="title"/>
    </jsp:include>
    <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

    <!-- Page specific Stylesheet -->
    <link href="/style/WizardFcstObsTypes.css" rel="stylesheet" media="all">

    <!-- Page specific JavaScript -->
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/WizardFcstObsTypes.js" type="text/javascript"></script>

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
                    <th id="hdrObs"><label for="listObsTypes">All Observation Types</label></th>
                    <th>&nbsp;</th>
                    <th id="hdrSelObs"><label for="listSelObsTypes">Selected Observation Types</label></th>
                </tr>
                <tr>
                    <td>
                        <select id="listObsTypes" size="10"></select>
                    </td>
                    <td>
	                &nbsp;&nbsp;<input id="btnAdd" type="button" value="&gt;&gt;" onclick="Add()"/>
	                <br/>
	                <br/>
	                &nbsp;&nbsp;<input id="btnRemove" type="button" value="&lt;&lt;" onclick="Remove()"/>
	            </td>
                    <td>
                        <select id="listSelObsTypes" style="width: 210px;" size="10"></select>
                    </td>
                </tr>
            </table>
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
                   value="<%= response.encodeURL("/auth/FcstQueryResults.jsp") %>"/>
            <% if (request.getUserPrincipal() != null && !request.isUserInRole("wde_limited")) { %>
            <input id="subscribeUrl" type="hidden"
                   value="<%= response.encodeURL("/auth/WizardFcstSubscribe.jsp") %>"/>
            <% } %>
            <input id="obs" name="obsType" type="hidden" value=""/>
        </form>
    </div>
    <div id="instructions" class="col-4"
         style="margin: 0; margin-top: -15px;">
        <h3>Instructions</h3>

        <p>
            Specify the observation types to retrieve by selecting them one at a time
            from the <b><i>All Observation Types</i></b> listbox and clicking the
            <span class="button">&nbsp;<input class="btn-wizard" type="button" value="&gt;&gt;" disabled="disabled">&nbsp;</span> button
            to move them to the <b><i>Selected Observation Types</i></b> listbox.
            To remove a specific observation type from the  <b><i>Selected Observation Types</i></b> 
            listbox, click the <span class="button">&nbsp;<input type="button" value="&lt;&lt;" disabled="disabled">&nbsp;</span> button.
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
