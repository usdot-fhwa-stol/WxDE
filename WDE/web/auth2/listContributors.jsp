<%@ page contentType="text/xml; charset=UTF-8" language="java" import="java.io.*,java.util.*,wde.qeds.*"%>
<%
	ArrayList<Contrib> oContribs = new ArrayList<Contrib>();
	Contribs.getInstance().getContribs(oContribs);

	int nRows = oContribs.size();
	if (nRows > 0)
	{
		// write xml declaration and root tag
		PrintWriter oPrintWriter = response.getWriter();
		oPrintWriter.println("<?xml version=\"1.0\" ?>");
		oPrintWriter.println("<contributors numRows=\"" + nRows + "\" >");

		// write the contributor information
		for (int nIndex = 0; nIndex < nRows; nIndex++)
		{
			Contrib oContrib = oContribs.get(nIndex);
			if (oContrib.getDisplay() == 0)
				continue;
	
			oPrintWriter.print("  <contributor id=\"");
			oPrintWriter.print(oContrib.getId());
			oPrintWriter.print("\" name=\"");
			oPrintWriter.print(oContrib.getName());
			oPrintWriter.println("\"/>");
		}

		// complete the xml
		oPrintWriter.println("</contributors>");
	}
%>
