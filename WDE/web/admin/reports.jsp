<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*, java.text.*, java.util.*" %>
<%
	response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");//HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
 %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>

	    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
	    	<jsp:param value="Reports" name="title"/>
	    </jsp:include>
		<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

		<link href="/style/reports-table.css" rel="stylesheet" media="all">
	</head>
	
	<body id="adminPage">
		<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
		<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
		
		<div class="container">
			<h1>Reports</h1>
			
			<p><em>* Click column headers containing vertically aligned carets ( <img src="/image/icons/dark/fa-sort.png" alt="Sort Icon" />) to sort.</em></p>
			
			<table class="reports-table" id="sortable-table">
				<caption style="display: none;">Reports</caption>
			  <thead>
				<tr>
				  <th id="sort-name" class="sortable">File Name
<!-- 				    <span class="icon-stack"> -->
<!-- 				      <i class="icon-sort"></i> -->
<!-- 				    </span> -->
					<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" />
				  </th>
				  <th id="sort-date" class="sortable">Last Modified Date
<!-- 				    <span class="icon-stack"> -->
<!-- 				      <i class="icon-sort"></i> -->
<!-- 				    </span> -->
					<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" />
				  </th>
				  <th id="sort-size" class="sortable">File Size
<!-- 				    <span class="icon-stack"> -->
<!-- 				      <i class="icon-sort"></i> -->
<!-- 				    </span> -->
					<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" />
				  </th>
				  <th class="align-center">Download File</th>
				</tr>
			  </thead>
			  <tbody>
				<%//start scriplet for reading and displaying files in "n" directory
				  String strPath = application.getRealPath("../data/reports/");		
				  File files = new File(strPath);
				  
				  Date datify = new Date();
				  SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss"); 
				  
				  String[] strFileName = files.list(),
					       strLinkName;
				  
				  File[] objFile = files.listFiles();
				  
				  for( int i = 0; i < strFileName.length; i++ ) {
				    strLinkName = strFileName[i].split("\\.");
				    datify = new Date(objFile[i].lastModified());
				 %>
					<tr>
					  <td><%= strLinkName[0] %></td>
					  <td class="align-center">
					  	<%= datify %>
					  </td>
					  <td class="align-center"><%= Math.round(Double.valueOf(objFile[i].length() / 1000000.0) * 100.0) / 100.0 %> MB</td>
					  <td><a href="../data/reports/<%= strFileName[i] %>" class="btn-dark align-center"><img src="/image/icons/light/fa-download.png" alt="Download Icon" style="margin-bottom: -1px;" /> Download</a></td>
					</tr>
				 <%
					}
				 %>
			   </tbody>
			 </table>
		</div>		
		
		<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
		<script type="text/javascript">
		    var table = $('#sortable-table');
		    
		    $('#sort-name, #sort-date, #sort-size')
		    	.css("cursor", "pointer")
		        .wrapInner('<span class="explode" title="Click to sort this column"/>')
		        .each(function(){
				    
		            var th = $(this),
		                thIndex = th.index(),
		                inverse = true;
// 		                caret = "";

		            th.bind("click", function() {
		                
		                table.find('td').filter(function(){
		                    
		                    return $(this).index() === thIndex;
		                    
		                }).sortElements(function(a, b){
		                    
		                    return $.text([a]) > $.text([b]) ?
		                        inverse ? -1 : 1
		                        : inverse ? 1 : -1;
		                    
		                }, function(){
		                    
		                    // parentNode is the element we want to move
		                    return this.parentNode; 
		                    
		                });
		                
// 		                if(inverse == false)
// 		                	caret = "icon-sort-up"
// 		                else
// 		                	caret = "icon-sort-down"
		                
// 		                $(this).find(".icon-stack").html(
// 		                	'<i class="icon-sort" style="color: #AAA;"></i>'+
// 		                	'<i class="' + caret + '"></i>'
// 		                );
		                
		                inverse = !inverse;
		            		            		
		            });
		            
	        	});
		</script>
		<script src="/script/simpleSorter.js" type="text/javascript"></script>
	</body>
</html>