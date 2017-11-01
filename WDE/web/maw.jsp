<%@ page contentType="text/plain; charset=UTF-8" language="java" import="java.io.*,java.sql.*,javax.sql.*,java.util.*,wde.data.osm.*,java.text.*,wde.WDEMgr" %>
<%
    //Access the database with specialized query and get lat/long of road and the code integer of the
    //road condition. Use the Observation? class to look up the road with the given lat/long info and 
    //use the integer code and the spec file to determine what data, for each road, goes in the JSON file.
    
    int nObsTypeId = 0;
	String sObsTypeId = request.getParameter("obsType");
	if (sObsTypeId != null && sObsTypeId.length() > 0)
		nObsTypeId = Integer.parseInt(sObsTypeId);
        
	String sDataSourceName = "java:comp/env/jdbc/wxde";
        
        GregorianCalendar oCurrentCal = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat("YYYY-MM-dd-HH");
        String sCurrFormattedDate = df.format(oCurrentCal.getTime());
        String[] dateArray = sCurrFormattedDate.split("-");
        
        oCurrentCal.add(Calendar.HOUR, -1);
        String sBehindFormattedDate = df.format(oCurrentCal.getTime());
        String[] lateArray = sBehindFormattedDate.split("-");
        
        String sQuery = "SELECT obstypeid,latitude,longitude,value FROM obs.\"obs_" + dateArray[0] + "-" + 
                        dateArray[1] + "-" + dateArray[2] + "\" WHERE obstypeid>0 AND obstypeid<14 " +
                        "AND obstime>'" + sCurrFormattedDate.substring(0, 10) + " " + dateArray[3] + ":00:00' " + 
                        "AND obstime<'" + sBehindFormattedDate.substring(0, 10) + " " + lateArray[3] + ":00:00';";
//        String sQuery = "SELECT obstypeid,latitude,longitude,value FROM obs.\"obs_2016-06-01\" WHERE obstypeid>0 AND obstypeid<14 AND obstime>'2016-06-01 14:00:00' AND obstime<'2016-06-01 15:00:00'; ";

	DataSource iDataSource = WDEMgr.getInstance().getDataSource(sDataSourceName);
	if (iDataSource == null)
            return;

  try(Connection iConnection = iDataSource.getConnection();
        Statement iQuery = iConnection.createStatement();
          ResultSet iResultSet = iQuery.executeQuery(sQuery))
  {

        //Lookup table
        String[][] oLookupTable = {
        {"precip", "none", "clear", "both", "", "", "0", "0", "0"},
        {"precip", "light rain", "warning", "both", "light rain", "use caution","1","1","1"},
        {"precip", "moderate rain", "warning", "both", "moderate rain", "use caution", "2", "2","1"},
        {"precip", "heavy rain", "warning", "fcst", "", "", "3", "3", "2"},
        {"precip", "heavy rain", "alert", "obs", "heavy rain", "drive slowly and use caution", "3", "3", "2"},
        {"precip", "light mixed", "warning", "both", "light rain/snow mix", "use caution", "21", "4","1"},
        {"precip", "moderate mixed", "warning", "both", "moderate rain/snow mix", "use caution", "22", "5","1"},
        {"precip", "heavy mixed", "warning", "fcst", "", "", "22", "6", "3"},
	{"precip", "heavy mixed", "alert", "obs", "heavy rain/snow mix", "delay travel, seek alternate route, or drive slowly and use extreme caution", "23", "6", "3"},
        {"precip", "light snow", "warning", "both", "light snow", "use caution", "11", "7", "1"},
        {"precip", "moderate snow", "warning", "both", "moderate snow", "use caution", "12", "8","1"},
        {"precip", "heavy snow", "warning", "fcst", "", "", "12", "9", "3"},
        {"precip", "heavy snow", "alert", "obs", "heavy snow", "delay travel, seek alternate route, or drive slowly and use extreme caution", "13", "9", "3"},
        {"pavement", "wet", "clear", "both", "wet roads", "use caution","1","1","1"},
        {"pavement", "snow", "warning", "both", "snowy roads", "delay travel, seek alternate route, or drive slowly and use extreme caution", "3", "2", "3"},
        {"pavement", "slick, snowy", "warning", "fcst", "", "", "20", "3", "3"},
        {"pavement", "hydroplane", "warning", "both", "hydroplaning possible", "use caution", "2", "6","1"},
        {"pavement", "ice possible", "warning", "both", "icy roads possible", "drive slowly and use caution", "10", "8", "2"},
        {"pavement", "slick, icy", "alert", "both", "icy, slick roads", "delay travel, seek alternate route, or drive slowly and use extreme caution", "24", "5", "3"},
        {"visibility", "low", "warning", "both", "low visibility", "drive slowly and use caution", "10","1", "2"},
        {"visibility", "blowing snow", "alert", "both", "blowing snow", "delay travel, seek alternate route, or drive slowly and use extreme caution", "20", "2", "3"}
        };
        
        //Populate a hash table of obstypeid's with observation condition values
        /*Key for TreeMap = Road id   Value for each Key = arrayList of obstypes*/
        TreeMap<Integer, ArrayList<Integer>> oTm = new TreeMap();
        Integer oObstypeid = null;
        Integer oRoadId = null;
        
        while(iResultSet.next())
        {
            //may return null
            Road oRoad = Roads.getInstance().getLink(400, iResultSet.getInt("longitude"), iResultSet.getInt("latitude"));
            if(oRoad != null)
            {
                oRoadId = oRoad.m_nId;
                if(!(oTm.containsKey(oRoadId)))
                    oTm.put(oRoadId, new ArrayList<Integer>());
            
                /*Add to the arrayList that's in the value of the m_nId*/
                
                oObstypeid = new Integer(iResultSet.getInt("obstypeid"));
                
                if(!(oTm.get(oRoadId).contains(oObstypeid)))
                    oTm.get(oRoadId).add(oObstypeid);
                
                Collections.sort(oTm.get(oRoadId));
            }
        }
        
        Set set = oTm.keySet();
        Iterator oIdIterator = set.iterator();
        
        Iterator<Integer> oObsIterator;
        
        PrintWriter oPrintWriter = response.getWriter();
        
         oPrintWriter.write("{\n");
        
        while(oIdIterator.hasNext())
        {
            oPrintWriter.write("    '" + oRoadId + "':{\n");
            oObsIterator = oTm.get(oRoadId).iterator();
            
            while(oObsIterator.hasNext())
            {
                Integer oSegId = oObsIterator.next();
                if(oSegId < 10)
                {
                    oPrintWriter.write("        'maw_precip_action':'" + oLookupTable[oSegId][5] + "'\n");
                    oPrintWriter.write("        'maw_precip_action_code':'" + oLookupTable[oSegId][8] + "'\n");
                    oPrintWriter.write("        'maw_precip_condition':'" + oLookupTable[oSegId][4] + "'\n");
                    oPrintWriter.write("        'maw_precip_condition_code':'" + oLookupTable[oSegId][7] + "'\n");
                    oPrintWriter.write("        'maw_precip_priority':'" + oLookupTable[oSegId][6] + "'\n");
                }
                else if(oSegId >= 10 && oSegId < 13)
                {
                    oPrintWriter.write("        'maw_pavement_action':'" + oLookupTable[oSegId][5] + "'\n");
                    oPrintWriter.write("        'maw_pavement_action_code':'" + oLookupTable[oSegId][8] + "'\n");
                    oPrintWriter.write("        'maw_pavement_condition':'" + oLookupTable[oSegId][4] + "'\n");
                    oPrintWriter.write("        'maw_pavement_condition_code':'" + oLookupTable[oSegId][7] + "'\n");
                    oPrintWriter.write("        'maw_pavement_priority':'" + oLookupTable[oSegId][6] + "'\n");
                }
                else if(oSegId > 18)
                {
                    oPrintWriter.write("        'maw_visibility_action':'" + oLookupTable[oSegId][5] + "'\n");
                    oPrintWriter.write("        'maw_visibility_action_code':'" + oLookupTable[oSegId][8] + "'\n");
                    oPrintWriter.write("        'maw_visibility_condition':'" + oLookupTable[oSegId][4] + "'\n");
                    oPrintWriter.write("        'maw_visibility_condition_code':'" + oLookupTable[oSegId][7] + "'\n");
                    oPrintWriter.write("        'maw_visibility_priority':'" + oLookupTable[oSegId][6] + "'\n");
                    oPrintWriter.write("        'maw_visibility_site_num':'" + oRoadId + "'\n");
                }
            }
            oIdIterator.next();
            oPrintWriter.write("    }");
            if(oIdIterator.hasNext())
                oPrintWriter.write(",");
            oPrintWriter.write("\n");
        }
        
        oPrintWriter.write("}\n");
        oPrintWriter.close();
  }

%>
