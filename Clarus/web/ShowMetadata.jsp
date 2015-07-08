<%@page contentType="text/plain; charset=iso-8859-1" language="java" import="java.io.*,util.*" %>
<%
	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.ems.EmsMgr");
    String sPath = oConfig.getString("metadata", null);

    try
    {
        BufferedReader oReader = new BufferedReader(
			new FileReader(sPath + "\\" + request.getParameter("file")));
        String sLine;
        while ((sLine = oReader.readLine()) != null)
        {
            out.write(sLine + "\r\n");
        }
        oReader.close();
    }
    catch(Exception oException)
    {
        String sFilename = request.getParameter("file");
        request.getRequestDispatcher("missingResource.jsp?resource=Metadata File&id=" + sFilename).forward(request, response);
    }

%>
