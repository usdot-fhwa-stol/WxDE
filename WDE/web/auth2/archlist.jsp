<%@page import="org.owasp.encoder.Encode"%>
<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription,java.util.*,java.io.*" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<%
    String sUsername = request.getUserPrincipal().getName();
    // Clear out the Subscription object.
    oSubscription.clearAll();
    
    Enumeration<String> oNames = request.getParameterNames();
    ArrayList<Integer> paramArray = new ArrayList();
    
    while (oNames.hasMoreElements())
    {
        String sParamName = oNames.nextElement();
        if("org.apache.catalina.filters.CSRF_NONCE".equalsIgnoreCase(sParamName))
          continue;
        Integer oParam = new Integer(sParamName);
        paramArray.add(oParam);
    }
    Integer oBeginInt = null;
    Integer oEndInt = paramArray.remove(paramArray.size() - 1);
    if (paramArray.get(paramArray.size() - 1) > 99)   
        oBeginInt = paramArray.remove(paramArray.size() - 1);
    else
        oBeginInt = oEndInt;
    
    Iterator<Integer> oIt = paramArray.iterator();

    String oBeginString = oBeginInt.toString();
    String oStartYearString = oBeginString.substring(0, 4);
    String oStartMonthString = oBeginString.substring(4, 6);
    String oStartDayString = oBeginString.substring(6, 8);

    int oStartYearInt = Integer.parseInt(oStartYearString);
    int oStartMonthInt = Integer.parseInt(oStartMonthString);
    int oStartDayInt = Integer.parseInt(oStartDayString);

    String oEndString = oEndInt.toString();
    String oEndYearString = oEndString.substring(0, 4);
    String oEndMonthString = oEndString.substring(4, 6);
    String oEndDayString = oEndString.substring(6, 8);

    int oEndYearInt = Integer.parseInt(oEndYearString);
    int oEndMonthInt = Integer.parseInt(oEndMonthString);
    int oEndDayInt = Integer.parseInt(oEndDayString);
    
    GregorianCalendar oCalEndDate = new GregorianCalendar(oEndYearInt, oEndMonthInt, oEndDayInt);
    oCalEndDate.add(Calendar.DAY_OF_MONTH, 1);

    String oPrintLink;
    int oIdNum, oPrintYear, oPrintMonth, oPrintDay;

    String sUrl = request.getRequestURL().substring(0, request.getRequestURL().length() - request.getRequestURI().length());
    
    FileWriter fw = new FileWriter("/opt/apache-tomcat-7.0.63/webapps/ROOT/archive-scripts/" + sUsername + ".sh");
    
    int nTotal = 0;
    while(oIt.hasNext())
    {
        oIdNum = oIt.next();
        GregorianCalendar oCalCurrDate = new GregorianCalendar(oStartYearInt, oStartMonthInt, oStartDayInt);
        while(!(oCalCurrDate.equals(oCalEndDate)))
        {
            //Construct, for every iteration of oIt, the link with correct date
            //Need to extract the year month day from oCalCurrDate
            oPrintYear = oCalCurrDate.get(GregorianCalendar.YEAR);
            oPrintMonth = oCalCurrDate.get(GregorianCalendar.MONTH);
            oPrintDay = oCalCurrDate.get(GregorianCalendar.DAY_OF_MONTH);
            oPrintLink = String.format("wget %s/archdl.jsp?file=%02d-%d%02d%02d.csv.gz\n", sUrl, oIdNum, oPrintYear, oPrintMonth, oPrintDay);
            fw.write(oPrintLink);
            oCalCurrDate.add(Calendar.DAY_OF_MONTH, 1);
            ++nTotal;
        }
    }
    fw.close();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp"></jsp:include>
        <jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
        
        <script src="/script/xml.js" language="javascript" type="text/javascript"></script>
        <script src="/script/Listbox.js" language="javascript" type="text/javascript"></script>
        <script src="/script/Common.js" language="javascript" type="text/javascript"></script>
        <script src="/script/archive.js" type="text/javascript"></script>
        <script src="/script/archlist.js" type="text/javascript"></script>
        
        <link rel="stylesheet" href="/style/listSupport.css">
        
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

    </head>
    <body>
        <jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
              
        <div class="container">
            <p>
                Up to one hundred archive files are displayed on this page. 
                Larger numbers of files may be downloaded with the script instructions below.<br/>
                To write your own script, you must know the contributor id's that you would like to
                receive data from as well as the range of dates for the data. <br/>The contributor id's
                can be found <a href='<%= response.encodeURL("ShowMetadata.jsp?file=contrib.csv")%>'>here</a>.
                <br/>
            </p>
            
            <!--<h3>Customize your script to download multiple archive files:</h3>-->
            
            <p>
                There are a total of <%=nTotal%> files.<br/><br/>
            To download the complete list of specified archive files, download this <a href='<%= response.encodeURL("/archive-scripts/" + Encode.forHtmlAttribute(sUsername) + ".sh" )%>' download>wget script</a>
                and run it in a Linux command line.
            </p>
            
          
            <h2>List of Archive Files</h2>
            <div id="demo"></div>
            <script>            
                document.getElementById("demo").innerHTML = convertArray();
                document.getElementById("wget").innerHTML = wgetVar();
            </script>
        </div>
    </body>
    <jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</html>
