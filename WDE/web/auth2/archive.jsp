<!--To Do
More elicit instructions for how to write a wget script
Contributor Id's need to be known in order to write a wget script-->

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
    <script src="/script/archive.js" type="text/javascript"></script>
	
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
                    maxDate: "-2D",
                    onClose: function (selectedDate) {
                        $("#endDate").datepicker("option", "minDate", selectedDate);
                        checkHours();
                    }
                });

                $("#endDate").datepicker({
                    buttonText: "Enter end date",
                    maxDate: "-2D",
                    onClose: function (selectedDate) {
                        $("#startDate").datepicker("option", "maxDate", selectedDate);
                        checkHours();
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
    </script>
</head>

<body onload="onLoad()">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
            <div id="titleArea" class='pull-right'>
                <div id="timeUTC"></div>
	    </div>

            
            <div id="linkArea2" class="col-2" style="margin-top:5px;">
                <table id="tblTossItems">
                <caption style="display: none;">Select Contributors</caption>
                    <thead>
                        <tr>
                            <th><h3>Choose Contributors</h3></th>
                            <th>&nbsp;</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                <select id="listAll" title="All Contributors" style="width: 197px;" size="10" multiple="1" ondblclick="Add()"></select>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
		
            <div id="linkArea2" class="col-2" style="margin-top:5px;">
                <center><h3>Choose the Date</h3></center>
                <table cellspacing="10" align="center" role="presentation">
                    <caption style="display: none;">Time and Format</caption>
                    <tr>
                        <td align="right"><label for="startDate">Start Date</label></td>
                        <td><input id="startDate" name="startDate" type="text" size="10"/></td>
                    </tr>
                    <tr>
                        <td align="right"><label for="endDate">End Date</label></td>
                        <td><input id="endDate" name="endDate" type="text" size="10"/></td>
                    </tr>
                    <tr>
                        <td colspan="2" class="btnNext">
                            <br>
                            <br>
                            <br>
                            <br>
                            <center>
                                <button id="btnNext" type="button" class="btn-dark" onclick="Generate()">
                                    Get Files 
                                    <img src="/image/icons/light/fa-arrow-right.png" alt="Right Arrow" style="margin-bottom: -1px" />	
                                </button>
                            </center>
                        </td>
                    </tr>
                </table>
                <input id="timeRange" name="timeRange" type="hidden" value=""/>
                <input id="format" name="format" type="hidden" value=""/>
            </div>
            <div id="linkArea2" class="col-2" style="margin-top:5px;">
                <p style="word-wrap: break-word;">
                    Use Ctrl+Click and Shift+Click to select one or more contributors from the list. <br/><br/>
                    Select the start and end dates by clicking on either the calendar or the text boxes. 
                    The most recent date available is two days before today.<br/><br/>
                    Click the Get Files button to generate a list of archive files to download. Up to 100 files are listed at a time.
                </p>
            </div>
                      
            <div class='clearfix'></div>
            <br>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>

