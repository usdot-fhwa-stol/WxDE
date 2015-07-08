<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<%
    // Clear out the Subscription object.
    oSubscription.clearAll();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Select Contributo Wizard" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/WizardContributor.js" type="text/javascript"></script>
	
	<style>	
		.container a {
			color:#006699;
			text-decoration:none;
		}
		.container a:hover {
			text-decoration:underline;
		}
		.tblHdr, .tblFld{
			text-align: justify !important;
			padding:10px 5px 10px 15px !important;
			font-size: 1.1em !important;
		}
		table{
			border-collapse: separate !important;
		}
		.btnNext{
			padding-left: 0px !important;
		}	
	</style>
	
    <script type="text/javascript">
    	$(document).ready(function() {
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

    	<div id="linkArea2" class="col-5" style="margin-top:-15px;">
	        <table id="tblTossItems">
				<caption style="display: none;">Select Contributors</caption>
			  <thead>
		         <tr>
		           <th><h3>All Contributors</h3></th>
		           <th>&nbsp;</th>
		           <th><h3>Selected Contributors</h3></th>
		         </tr>
			  </thead>
			  <tbody>
	            <tr>
	              <td>
	                <select id="listAll" title="All Contributors" style="width: 197px;" size="10" multiple="1" ondblclick="Add()"></select>
	              </td>
	              <td>
	                &nbsp;&nbsp;<input id="btnAdd" type="button" value="&gt;&gt;" onclick="Add()"/>
	                <br/>
	                <br/>
	                &nbsp;&nbsp;<input id="btnRemove" type="button" value="&lt;&lt;" onclick="Remove()"/>
	              </td>
	              <td>
	                <select id="listSel" title="Selected Contributors" style="width: 197px;" size="10" multiple="1" ondblclick="Remove()"></select>
	              </td>
	            </tr>
	            <tr>
		          <td colspan="2" class="btnNext">
		            <br>
			        <button id="btnNext" type="button" class="btn-dark" onclick="Validate()">
			        	Next Page 
			        	<img src="/image/icons/light/fa-arrow-right.png" alt="Right Arrow" style="margin-bottom: -1px" />	
		        	</button>
		          </td>
		        </tr>
	          </tbody>
	        </table>
	        
            <form action='<%= response.encodeURL("/auth2/wizardStations.jsp") %>' method="post">
		      <input id="contributors" name="contributors" type="hidden" value=""/>
		    </form>
		</div>
		<div id="instructions" class="col-4" style="margin:0; margin-top:-15px;">
			<h3>Instructions</h3>
			<p>
				Select one or more contributors from the<br/>
				<b><i>All Contributors</i></b> listbox
				and move them to the <b><i>Selected Contributors</i></b> listbox by pressing the
				<span class="button">&nbsp;<input class="btn-wizard" type="button" value="&gt;&gt;" disabled="disabled">&nbsp;</span> button. To remove a contributor from the
				<b><i>Selected Contributors</i></b> listbox, select it and press the
				<span class="button">&nbsp;<input type="button" value="&lt;&lt;" disabled="disabled">&nbsp;</span> button.
				<br/>
				<br/>
				You can select more than one item in the list by pressing the <b><i>Ctrl</i></b> or
				<b><i>Shift</i></b> key when making your selection.  
				<br/>
				<br/>
				Besides using the mouse, you can also use the tab key or shift-tab key combination to traverse elements on
	        	the page.  Once a list is in focus, use the the Up and Down arrow keys to select contributors. Use
	        	the spacebar to add/remove contributors after highlight the contributor in one of the two lists. 
	        	<br/>
	     		<br/>
			</p>
			<div id="statusMessage" class="msg" style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
				&nbsp;
			</div>
	    </div>
		<div class='clearfix'></div>
		<br>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
