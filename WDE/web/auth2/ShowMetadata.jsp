<%@page contentType="text/plain; charset=iso-8859-1" language="java" %>
<%@ page import="java.io.*,wde.util.*" %>
<%@ page import="org.apache.log4j.Logger"%>
<%@ page import="org.apache.commons.io.FilenameUtils"%><%@ page import="org.apache.commons.lang3.StringEscapeUtils"%>

<%
	final Logger logger = Logger.getLogger("ShowMetadata.jsp");
	Config oConfig = ConfigSvc.getInstance().getConfig("wde.ems.EmsMgr");
    String sPath = FilenameUtils.normalize(oConfig.getString("metadata", null));
    response.addHeader("X-Content-Type-Options", "nosniff");

    String requestedPath = "";
    String normalizedFilePath = "";

    try
    {
        //
        // Construct the path to the requested file, then normalize it to evaluate traversal
        // directives and end up with the actual path being requested.
        //
        requestedPath = String.format("%s%s", sPath, request.getParameter("file"));
        File requestedFile = new File(requestedPath);
        normalizedFilePath = FilenameUtils.normalize(requestedFile.getAbsolutePath());

        //
        // Check to make sure the requested path starts with our configured path to ensure
        // a file outside of the configured path isn't being requested, such as readable
        // sensitive files.
        //
        if (!normalizedFilePath.startsWith(sPath)) {
            throw new Exception("The requested file was not found.");
        }

        BufferedReader oReader = new BufferedReader(
			new FileReader(sPath + request.getParameter("file")));
        String sLine;
        while ((sLine = oReader.readLine()) != null)
        {
            out.write(sLine + "\r\n");
        }
        oReader.close();
    }
    catch(Exception oException)
    {
    	logger.error(oException.getMessage());
    	System.out.println(oException.getMessage());

        String sFilename = StringEscapeUtils.escapeHtml4(request.getParameter("file"));
        //String sFilename = request.getParameter("file");
        request.getRequestDispatcher("missingResource.jsp?resource=Metadata File&id=" + sFilename).forward(request, response);
    }

%>
