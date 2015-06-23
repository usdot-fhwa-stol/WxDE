<%@ page contentType="text/xml; charset=UTF-8" language="java" import="java.io.*,java.util.*,wde.emc.*,wde.dao.*,wde.metadata.*,wde.qeds.*"%>
<%
    int nRows = 0;
	StringBuilder sBuffer = new StringBuilder();

	String[] sContribIds = request.getParameterValues("contribId");
    if (sContribIds != null && sContribIds.length > 0)
	{
		ArrayList<IPlatform> platforms = PlatformDao.getInstance().getActivePlatforms();
		ArrayList<PlatformObs> oPlatforms = PlatformMonitor.getInstance().getPlatforms();
		HashMap<String, PlatformObs> platformMap = new HashMap<String, PlatformObs>();
		for (PlatformObs oPlatform : oPlatforms)
		    platformMap.put(oPlatform.getPlatform().getPlatformCode(), oPlatform);

		Collections.sort(platforms);

		int nIndex = sContribIds.length;
		while (nIndex-- > 0)
		{
			int nStationIndex = platforms.size();
			while (nStationIndex-- > 0)
			{
				IPlatform iPlatform = platforms.get(nStationIndex);
				
				if (iPlatform.getContribId() == Integer.parseInt(sContribIds[nIndex]))
				{
					int wxdeObs = 0;
					int vdtObs = 0;

					PlatformObs oPlatform = platformMap.get(iPlatform.getPlatformCode());
					if (oPlatform == null) {
					    wxdeObs = 0;
					    vdtObs = 0;
					}
					else {
					    wxdeObs = (oPlatform.hasWxDEObs()) ? 1 : 0;
					    vdtObs = (oPlatform.hasVDTObs()) ? 2 : 0;
					}
					
	                int ho = wxdeObs + vdtObs;
				    
					sBuffer.append("  <station ");
					sBuffer.append("id=\"");
					sBuffer.append(iPlatform.getId());
					sBuffer.append("\" hasObs=\"");
					sBuffer.append(ho);
					sBuffer.append("\" stationCode=\"");
					sBuffer.append(iPlatform.getPlatformCode());
					sBuffer.append("\" category=\"");
					sBuffer.append(iPlatform.getCategory());
					sBuffer.append("\" contribId=\"");
					sBuffer.append(iPlatform.getContribId());
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
