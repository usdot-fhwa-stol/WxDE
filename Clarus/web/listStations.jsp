<%@ page contentType="text/xml; charset=iso-8859-1" language="java" import="java.io.*,java.util.*,clarus.emc.*"%>
<%
    int nRows = 0;
	StringBuilder sBuffer = new StringBuilder();

	String[] sContribIds = request.getParameterValues("contribId");
    if (sContribIds != null && sContribIds.length > 0)
	{
		ArrayList<IStation> oStations = new ArrayList<IStation>();
		Stations.getInstance().getStations(oStations);

		int nIndex = sContribIds.length;
		while (nIndex-- > 0)
		{
			int nStationIndex = oStations.size();
			while (nStationIndex-- > 0)
			{
				IStation iStation = oStations.get(nStationIndex);
				if (iStation.getContribId() == Integer.parseInt(sContribIds[nIndex]))
				{
					sBuffer.append("  <station ");
					sBuffer.append("id=\"");
					sBuffer.append(iStation.getId());
					sBuffer.append("\" stationCode=\"");
					sBuffer.append(iStation.getCode());
					sBuffer.append("\" contribId=\"");
					sBuffer.append(iStation.getContribId());
					sBuffer.append("\"/>\n");

					++nRows;
				}
			}
		}
	}

    PrintWriter oPrintWriter = response.getWriter();
    oPrintWriter.println("<?xml version=\"1.0\" ?>");
    oPrintWriter.print("<stations numRows=\"");

	oPrintWriter.print(nRows);
	if (nRows == 0)
		oPrintWriter.print("\" msg=\"Required parameter missing: contribId");
	oPrintWriter.println("\">");
	
	oPrintWriter.print(sBuffer.toString());
	oPrintWriter.println("</stations>");

	oPrintWriter.flush();
%>
