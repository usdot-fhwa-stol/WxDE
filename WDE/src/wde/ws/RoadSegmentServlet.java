package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerator;
import wde.comp.MetroResults;
import wde.cs.ext.NDFD;
import wde.cs.ext.RTMA;
import wde.dao.UnitConv;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.data.osm.SegIterator;
import wde.metadata.ObsType;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns = "/RoadLayer/*")
public class RoadSegmentServlet extends LayerServlet
{

  public static final String SHOW_ALL_ROADS = "roads";

  private final Roads m_oRoads = Roads.getInstance();
  private final int m_nSnapTolerance = 400;

  private final NDFD m_Ndfd = NDFD.getInstance();
  private final RTMA m_Rtma = RTMA.getInstance();
  private final MetroResults m_oMetroResults = MetroResults.getInstance();

  private String m_sAlertObsTypeCondition = "";

  private final int[] m_nWarningObsTypes =
  {
    1, 2, 3, 5, 6, 7, 9, 10, 11, 13, 14, 15, 16, 17, 19
  };
  private final int[] m_nWatchObsTypes =
  {
    4, 8, 12, 18, 20
  };

  private final int[] m_nRtmaObstypes =
  {
    575, 554, 5733, 5101, 56105, 56108, 56104
  };

  private final int[] m_nNdfdObstypes =
  {
    575, 5733, 581
  };

  private final int[] m_nMetroResultsObstypes =
  {
	  51137, 51138, 51165
  };
  
  private static final Logger m_oLogger = Logger.getLogger(RoadSegmentServlet.class);

  /**
   * The inference module currently produces essPrecipSituation (),
   * essVisibilitySituation (), and essMobileObservation () pavement."
   */
  private final String m_sBaseSelect
          = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",o.obstime\n"
          + ",o.latitude \n"
          + ",o.longitude \n"
          + ",o.obstypeid \n"
          + ",o.sourceid \n"
          + ",o.confValue \n"
          + ",o.sensorid \n"
          + ",o.qchCharFlag \n";

  private final String m_sBaseFromWhere
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON s.platformid = p.id\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "WHERE\n"
          + "p.contribid<>4\n"
          + "AND o.latitude >= ?\n"
          + "AND o.latitude <= ?\n"
          + "AND o.longitude >= ?\n"
          + "AND o.longitude <= ?\n"
          + "AND s.distgroup IN (" + DISTGROUP_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n";

  private final String m_sQueryWithoutObsTemplate;

  private final String m_sQueryWithObsTemplate
          = m_sBaseSelect
          + ",o.value\n"
          + m_sBaseFromWhere
          + "AND o.obstypeid = ?\n"
          + "ORDER BY platformid, obstime desc";

  private final String m_sPlatformObsQuery = m_sBaseObsSelect + m_sBaseObsFrom + m_sBaseObsWhere + m_sBaseObsOrderBy;

  public RoadSegmentServlet() throws NamingException
  {
    super(true, 11);
    m_oQueryUsersMicros = true;

    StringBuilder oConditionBuilder = new StringBuilder("AND o.obstypeid IN (");

    for (int nObstypeId : m_nWarningObsTypes)
      oConditionBuilder.append(nObstypeId).append(",");

    for (int nObstypeId : m_nWatchObsTypes)
      oConditionBuilder.append(nObstypeId).append(",");

    oConditionBuilder.setCharAt(oConditionBuilder.length() - 1, ')');
    oConditionBuilder.append("\n");

    Arrays.sort(m_nWarningObsTypes);
    Arrays.sort(m_nWatchObsTypes);

    m_sAlertObsTypeCondition = oConditionBuilder.toString();

    m_sQueryWithoutObsTemplate
            = m_sBaseSelect
            + ",null AS value\n"
            + m_sBaseFromWhere
            + m_sAlertObsTypeCondition
            + "ORDER BY platformid, obstime desc";
  }

  @Override
  protected void buildLayerResponseContent(JsonGenerator oOutputGenerator, LatLngBounds oLastBounds, PlatformRequest oPlatformRequest, int nZoomLevel) throws SQLException, IOException
  {
    Object oAllRoadsSetting = oPlatformRequest.getSession().getAttribute(SHOW_ALL_ROADS);
    oPlatformRequest.setRequestObsType(-1);
    boolean bAllRoads = !(oAllRoadsSetting != null && oAllRoadsSetting.toString().equalsIgnoreCase("0"));

    ArrayList<Integer> oWarningRoadIds = new ArrayList<Integer>();
    ArrayList<Integer> oWatchRoadIds = new ArrayList<Integer>();

    try (Connection oConnection = m_oDatasource.getConnection())
    {
      try (PreparedStatement oStmt = prepareLayerStatement(oConnection, nZoomLevel, oPlatformRequest))
      {
        try (ResultSet oResult = oStmt.executeQuery())
        {
          while (oResult.next())
          {
            int nLat = oResult.getInt("latitude");
            int nLng = oResult.getInt("longitude");
            int nObstypeId = oResult.getInt("obstypeId");

            Road oRoad = m_oRoads.getLink(m_nSnapTolerance, nLng, nLat);

            if (oRoad == null)
              continue;

            if (Arrays.binarySearch(m_nWatchObsTypes, nObstypeId) >= 0)
              oWatchRoadIds.add(oRoad.m_nId);
            else
              oWarningRoadIds.add(oRoad.m_nId);
          }

        }
      }
    }
    catch (Exception oEx)
    {
		if (!oEx.getMessage().contains("relation ") && !oEx.getMessage().contains("does not exist"))
			m_oLogger.error("", oEx);
    }

    Collections.sort(oWarningRoadIds);

    LatLngBounds currentRequestBounds = oPlatformRequest.getRequestBounds();
    ArrayList<Road> roadList = new ArrayList<Road>();
    m_oRoads.getLinks(roadList, m_nSnapTolerance, currentRequestBounds.getEast(), currentRequestBounds.getNorth(), currentRequestBounds.getWest(), currentRequestBounds.getSouth());

    for (Road oRoad : roadList)
    {

      if (oLastBounds != null && oLastBounds.intersects(oRoad.m_nYmax, oRoad.m_nXmax, oRoad.m_nYmin, oRoad.m_nXmin))
        continue;

      if (!currentRequestBounds.intersects(oRoad.m_nYmax, oRoad.m_nXmax, oRoad.m_nYmin, oRoad.m_nXmin))
        continue;

      boolean bWatch = Collections.binarySearch(oWatchRoadIds, oRoad.m_nId) >= 0;

      //if there is already a watch, just set warning to and don't actually look it up
      boolean bWarning = bWatch ? false : Collections.binarySearch(oWarningRoadIds, oRoad.m_nId) >= 0;

      if (!bAllRoads && !(bWarning || bWatch))
        continue;

      serializeRoadProperties(oOutputGenerator, oRoad, bWatch ? "2" : bWarning ? "1" : "0");

      //empty metric/english obs values
      oOutputGenerator.writeString("");
      oOutputGenerator.writeString("");

      serializeRoadPoints(oOutputGenerator, oRoad);
    }
  }

  @Override
  protected void serializeResult(JsonGenerator oJsonGenerator, LatLngBounds oLastRequestBounds, PlatformRequest oCurrentRequest, ResultSet oResult) throws SQLException, IOException
  {
    UnitConv oUnitConverter = null;
    if (hasObs() && oCurrentRequest.hasObsType())
    {
      ObsType oObstype = m_oObsTypeDao.getObsType(oCurrentRequest.getRequestObsType());

      oUnitConverter = m_oUnits.getConversion(oObstype.getObsInternalUnit(), oObstype.getObsEnglishUnit());
    }

    DecimalFormat oValueFormatter = new DecimalFormat("0.##");
    ArrayList<Integer> oReturnedPlatformIdList = new ArrayList<Integer>();
    ArrayList<Integer> oReturnedRoadIdList = new ArrayList<Integer>();
    while (oResult.next())
    {
      //Repeated platforms would snap to the same road segment and get skipped
      //anyway, but we can avoid the repeated snaps by skipping repeated platforms
      //up front
      int nPlatformId = oResult.getInt("id");
      int nPlatformIdIndex = Collections.binarySearch(oReturnedPlatformIdList, nPlatformId);
      if (nPlatformIdIndex >= 0)
        continue;
      else
        oReturnedPlatformIdList.add(~nPlatformIdIndex, nPlatformId);

      int nLat = oResult.getInt("latitude");
      int nLng = oResult.getInt("longitude");

      Road oRoad = m_oRoads.getLink(m_nSnapTolerance, nLng, nLat);

      if (oRoad == null)
        continue;

      if (oLastRequestBounds != null && oLastRequestBounds.intersects(oRoad.m_nYmax, oRoad.m_nXmax, oRoad.m_nYmin, oRoad.m_nXmin))
        continue;

      int nRoadIdIndex = Collections.binarySearch(oReturnedRoadIdList, oRoad.m_nId);
      if (nRoadIdIndex >= 0)
        continue;
      else
        oReturnedRoadIdList.add(~nRoadIdIndex, oRoad.m_nId);

      serializeRoadProperties(oJsonGenerator, oRoad, "0");

      if (oCurrentRequest.hasObsType())
      {
        double dObsValue = oResult.getDouble("value");
        if (!oResult.wasNull())
        {
          oJsonGenerator.writeString(oValueFormatter.format(dObsValue));

          if (oUnitConverter != null)
            oJsonGenerator.writeString(oValueFormatter.format(oUnitConverter.convert(dObsValue)));
        }
        else
        {
          oJsonGenerator.writeString("");
          oJsonGenerator.writeString("");
        }
      }
      else
      {
        oJsonGenerator.writeString("");
        oJsonGenerator.writeString("");
      }

      serializeRoadPoints(oJsonGenerator, oRoad);
    }
  }

  private static void serializeRoadProperties(JsonGenerator oJsonGenerator, Road oRoad, String sStatus) throws IOException
  {
    oJsonGenerator.writeNumber(oRoad.m_nId);
    oJsonGenerator.writeString(oRoad.m_sName);
    oJsonGenerator.writeNumber(MathUtil.fromMicro(oRoad.m_nYmid));
    oJsonGenerator.writeNumber(MathUtil.fromMicro(oRoad.m_nXmid));
    oJsonGenerator.writeString(sStatus);
  }

  private static void serializeRoadPoints(JsonGenerator oJsonGenerator, Road oRoad) throws IOException
  {
    oJsonGenerator.writeStartArray();
    SegIterator oSegItr = oRoad.iterator();

    if (oSegItr.hasNext())
    {
      int[] nPoint;
      do
      {
        nPoint = oSegItr.next();

        oJsonGenerator.writeStartArray();
        oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[1]));
        oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[0]));
        oJsonGenerator.writeEndArray();
      }
      while (oSegItr.hasNext());

      oJsonGenerator.writeStartArray(); // write end point of final line segment
      oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[3]));
      oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[2]));
      oJsonGenerator.writeEndArray();
    }

    oJsonGenerator.writeEndArray();
  }

  @Override
  protected String getQueryWithObsType()
  {
    return m_sQueryWithObsTemplate;
  }

  @Override
  protected String getQueryWithoutObstype()
  {
    return m_sQueryWithoutObsTemplate;
  }

  @Override
  protected String getPlatformObsQueryTemplate()
  {
    return m_sPlatformObsQuery;
  }

  @Override
  protected void buildObsResponseContent(JsonGenerator oOutputGenerator, ObsRequest oObsRequest) throws Exception
  {
    oOutputGenerator.writeStartObject();

    int nRoadBoundaryPadding = m_nSnapTolerance + 10000;

    DecimalFormat oNumberFormatter = new DecimalFormat("0.##");
    DecimalFormat oConfFormat = new DecimalFormat("##0");
    SimpleDateFormat oDateFormat = new SimpleDateFormat("MM-dd HH:mm");
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    Date oDate = new Date();
    oOutputGenerator.writeArrayFieldStart("obs");

    Road oRequestRoad = m_oRoads.getLink(nRoadBoundaryPadding, (oObsRequest.getRequestBounds().getWest() + oObsRequest.getRequestBounds().getEast()) / 2, (oObsRequest.getRequestBounds().getSouth() + oObsRequest.getRequestBounds().getNorth()) / 2);

    if (oRequestRoad == null)
      return;

    PlatformRequest oPlatformRequest = new PlatformRequest();

    LatLngBounds oPlatformSearchBounds = new LatLngBounds(oRequestRoad.m_nYmax + nRoadBoundaryPadding, oRequestRoad.m_nXmax + nRoadBoundaryPadding, oRequestRoad.m_nYmin - nRoadBoundaryPadding, oRequestRoad.m_nXmin - nRoadBoundaryPadding);
    oPlatformRequest.setRequestBounds(oPlatformSearchBounds);
    oPlatformRequest.setRequestTimestamp(oObsRequest.getRequestTimestamp());
    oPlatformRequest.setDistributionGroups(oObsRequest.getDistributionGroups());

    ArrayList<Integer> oObstypeIdList = new ArrayList<Integer>();

    int nRoadId = oObsRequest.getPlatformIds()[0];

    try (Connection oConnection = m_oDatasource.getConnection())
    {
      try (PreparedStatement oStatement = prepareLayerStatement(oConnection, 0, oPlatformRequest))
      {
        try (ResultSet oResult = oStatement.executeQuery())
        {
          while (oResult.next())
          {
            int nLat = oResult.getInt("latitude");
            int nLng = oResult.getInt("longitude");

            Road oRoad = m_oRoads.getLink(m_nSnapTolerance, nLng, nLat);
            if (oRoad == null || oRoad.m_nId != nRoadId)
              continue;

            serializeObsRecord(oOutputGenerator, oObstypeIdList, oNumberFormatter, oConfFormat, oDateFormat, oDate, oResult);
          }
        }
      }
    }
    catch (Exception ex)
    {
		if (!ex.getMessage().contains("relation ") && !ex.getMessage().contains("does not exist"))
			m_oLogger.error("", ex);
    }
	 
    //check if the request time is in the current hour
    Calendar oCal = Calendar.getInstance();
    oCal.set(Calendar.MILLISECOND, 0);
    oCal.set(Calendar.SECOND, 0);
    oCal.set(Calendar.MINUTE, 0);

    long lCurrentHourStart = oCal.getTimeInMillis();

    if (oObsRequest.getRequestTimestamp() > lCurrentHourStart && (oObsRequest.getRequestTimestamp() - m_lSearchRangeInterval) < (lCurrentHourStart + 1000 * 60 * 60))
    {
      for (int nObstypeId : m_nRtmaObstypes)
      {
        double dValue = m_Rtma.getReading(nObstypeId, lCurrentHourStart, oRequestRoad.m_nYmid, oRequestRoad.m_nXmid);
        if (Double.isNaN(dValue))
          continue;

        serializeObsRecord(oOutputGenerator, oObstypeIdList, oNumberFormatter, oConfFormat, oDateFormat, oDate, nObstypeId, dValue, oObsRequest.getRequestTimestamp(), -1, 0, -1, null);
      }
    }

    for (int nObstypeId : m_nNdfdObstypes)
    {
      double dValue = m_Ndfd.getReading(nObstypeId, oObsRequest.getRequestTimestamp(), oRequestRoad.m_nYmid, oRequestRoad.m_nXmid);
      if (Double.isNaN(dValue))
        continue;

      serializeObsRecord(oOutputGenerator, oObstypeIdList, oNumberFormatter, oConfFormat, oDateFormat, oDate, nObstypeId, dValue, oObsRequest.getRequestTimestamp(), -1, 0, -1, null);
    }
	 
	 for (int nObstypeId : m_nMetroResultsObstypes)
	 {
		double dValue = m_oMetroResults.getReading(nObstypeId, oObsRequest.getRequestTimestamp(), oRequestRoad.m_nYmid, oRequestRoad.m_nXmid);
		if (Double.isNaN(dValue) || (nObstypeId == 51137 && dValue == 1))  //skip when road condition is 1 (dry road)
			continue; 
		
		serializeObsRecord(oOutputGenerator, oObstypeIdList, oNumberFormatter, oConfFormat, oDateFormat, oDate, nObstypeId, dValue, oObsRequest.getRequestTimestamp(), -1, 0, -1, null);
	 }
	 
	 oOutputGenerator.writeEndArray();
	 oOutputGenerator.writeEndObject();
  }

  @Override
  protected boolean includeDescriptionInDetails()
  {
    return false;
  }

}
