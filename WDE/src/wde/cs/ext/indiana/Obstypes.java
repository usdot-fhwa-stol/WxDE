package wde.cs.ext.indiana;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author scot.lange
 */
public class Obstypes
{

  public static Map<String,String> getColumnObstypes()
  {
    String[][] sColumns = new String[][] {
      {"Air Temperature °F", "5733"},
      {"Avg. Wind Direction °", "56105"},
      {"Avg. Wind Speed mph", "56104"},
      {"Dew Point °F", "575"},
      {"Freeze Point #3 °F", "511313"},
      {"Freeze Point Approach  °F", "511313"},
      {"Freeze Point Approach °F", "511313"},
      {"Freeze Point Bridge A °F", "511313"},
      {"Freeze Point Bridge [index]", "511313"},
      {"Freeze Point Bridge °F", "511313"},
      {"Freeze Point °F", "511313"},
      {"Freeze Temp Approach °F", "511313"},
      {"Freeze Temp Bridge °F", "511313"},
      {"Freeze Temperature °F", "511313"},
      {"Freezing Temperature Approach °F", "511313"},
      {"Freezing Temperature °F", "511313"},
      {"Freezing temp.  °F", "511313"},
      {"Freezing temp. °F", "511313"},
      {"Friction Approach  n/a", "5121"},
      {"Friction Approach N/A", "5121"},
      {"Friction Approach f", "5121"},
      {"Friction Bridge N/A", "5121"},
      {"Friction Bridge f", "5121"},
      {"Friction N/A", "5121"},
      {"Friction logic", "5121"},
      {"Gust Wind Direction °", "56109"},
      {"Gust Wind Speed mph", "56108"},
      {"Ice Percentage Approach %", "204"},
      {"Max Air Temperature °F", "576"},
      {"Min Air Temperature °F", "577"},
      {"Precipitation End Time timestamp", "5812"},
      {"Precipitation Situation [logic]", "589"},
      {"Precipitation Start Time timestamp", "5811"},
      {"Precipitation Yes/No [logic]", "586"},
      {"Rel. Humidity %", "581"},
      {"Road Condition # 3 [logic]", "51137"},
      {"Road Condition Approach [logic]", "51137"},
      {"Road Condition Approach logic", "51137"},
      {"Road Condition Approach n/a", "51137"},
      {"Road Condition Bridge A [logic]", "51137"},
      {"Road Condition Bridge [logic]", "51137"},
      {"Road Condition Bridge n/a", "51137"},
      {"Road Condition [logic]", "51137"},
      {"Road Condition logic", "51137"},
      {"Road Temp #3 °F", "51139"},
      {"Road Temp Approach °F", "51139"},
      {"Road Temp Bridge ", "51139"},
      {"Road Temp Bridge °F", "51139"},
      {"Road Temp. Approach °F", "51139"},
      {"Road Temp. Bridge A °F", "51139"},
      {"Road Temp. Bridge °F", "51139"},
      {"Road Temperature ", "51139"},
      {"Road Temperature Approach ", "51139"},
      {"Road Temperature Approach °F", "51139"},
      {"Road Temperature Bridge °F", "51139"},
      {"Road condition logic", "51137"},
      {"Road temperature  I-80 W middle lane °F", "51139"},
      {"Road temperature I-80 W right lane °F", "51139"},
      {"Road temperature NB departure °F", "51139"},
      {"Road temperature US 421 NB Bridge °F", "51139"},
      {"Saline Concentration %", "511311"},
      {"Saline Concentration Approach %", "511311"},
      {"Saline Concentration Bridge  %", "511311"},
      {"Saline Concentration Bridge %", "511311"},
      {"Saline Concentration Bridge [rate]", "511311"},
      {"Saline concent. %", "511311"},
      {"Salt Concentration Approach %", "511311"},
      {"Salt Concentration Bridge %", "511311"},
      {"Spot Wind Direction °", "56107"},
      {"Sub Surface Temperature °F", "51165"},
      {"SubSurface Temperature °F", "51165"},
      {"Subprobe -12\" °F", "51165"},
      {"Subprobe Temp °F", "51165"},
      {"Subprobe temp °F", "51165"},
      {"Subsurface Temperature  °F", "51165"},
      {"Subsurface Temperature 1 °F", "51165"},
      {"Subsurface Temperature Approach °F", "51165"},
      {"Surface Salinity #3 %", "511311"},
      {"Surface Salinity Approach %", "511311"},
      {"Surface Salinity Bridge %", "511311"},
      {"Surface Salinity Bridge A %", "511311"},
      {"Surface Temperature Approach °F", "51138"},
      {"Surface Temperature °F", "51138"},
      {"Visibility m", "5101"},
      {"Water Film  Approach mil", "511310"},
      {"Water Film  mil", "511310"},
      {"Water Film Approach mil", "511310"},
      {"Water Film Height Approach mil", "511310"},
      {"Water Film Height Bridge mil", "511310"},
      {"WaterFilm Height Bridge mil", "511310"},
      {"Waterfilm height mil", "511310"},
      {"Wet Bulb Temperature °F", "574"},
      {"Wind Speed mph", "56106"}};


      HashMap<String,String> oColumnMap = new HashMap<>();
      for(String[] sColumnPair : sColumns)
      {
        String sColumnName = sColumnPair[0];
        String sColumnObstype = sColumnPair[1];

        if(oColumnMap.containsKey(sColumnName))
          throw new IllegalArgumentException("Column name mapped twice");
        else
          oColumnMap.put(sColumnName, sColumnObstype);
      }
      return oColumnMap;
  }

  public static Map<String,String> getObstypeNames()
  {
    HashMap<String, String> oObsypeNames = new HashMap<>();

    oObsypeNames.put("204", "icePercent");
    oObsypeNames.put("574", "essWetBulbTemp");
    oObsypeNames.put("575", "essDewpointTemp");
    oObsypeNames.put("576", "essMaxTemp");
    oObsypeNames.put("577", "essMinTemp");
    oObsypeNames.put("581", "essRelativeHumidity");
    oObsypeNames.put("586", "essPrecipYesNo");
    oObsypeNames.put("589", "essPrecipSituation");
    oObsypeNames.put("5101", "essVisibility");
    oObsypeNames.put("5121", "essMobileFriction");
    oObsypeNames.put("5733", "essAirTemperature");
    oObsypeNames.put("5811", "essPrecipitationStartTime");
    oObsypeNames.put("5812", "essPrecipitationEndTime");
    oObsypeNames.put("51137", "essSurfaceStatus");
    oObsypeNames.put("51138", "essSurfaceTemperature");
    oObsypeNames.put("51139", "essPavementTemperature");
    oObsypeNames.put("51165", "essSubSurfaceTemperature");
    oObsypeNames.put("56104", "windSensorAvgSpeed");
    oObsypeNames.put("56105", "windSensorAvgDirection");
    oObsypeNames.put("56106", "windSensorSpotSpeed");
    oObsypeNames.put("56107", "windSensorSpotDirection");
    oObsypeNames.put("56108", "windSensorGustSpeed");
    oObsypeNames.put("56109", "windSensorGustDirection");
    oObsypeNames.put("511310", "essSurfaceWaterDepth");
    oObsypeNames.put("511311", "essSurfaceSalinity");
    oObsypeNames.put("511313", "essSurfaceFreezePoint");


    return oObsypeNames;
  }

  public static String[] getOutputObstypes()
  {
    return new String[]
    {
      "5121",
      "5101",
      "574",
      "575",
      "576",
      "577",
      "51165",
      "511313",
      "5733",
      "5811",
      "5812",
      "511311",
      "511310",
      "581",
      "56104",
      "56105",
      "56106",
      "586",
      "56107",
      "589",
      "204",
      "51137",
      "51139",
      "51138",
      "56108",
      "56109"
    };
  }


  public static void outputUniqueObstypeList()
  {
    Set<String> oObstypes = new HashSet<>();
    oObstypes.addAll(getColumnObstypes().values());

    for(String sObstype : oObstypes)
      System.out.println(sObstype);

  }
}
