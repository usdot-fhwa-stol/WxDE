package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import org.codehaus.jackson.JsonGenerator;
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

  private final Roads m_oRoads = Roads.getInstance();
  private final int m_nSnapTolerance = 200000;

  public RoadSegmentServlet() throws NamingException
  {
    super(true, 6);
    m_oQueryUsersMicros = true;
  }

  private final String m_sBaseSelect
          = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",o.obstime\n"
          + ",o.latitude \n"
          + ",o.longitude \n";

  private final String m_sBaseFromWhere
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON s.platformid = p.id\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "WHERE\n"
          + "p.contribid<>4\n"
          //    + "AND p.category IN ('M')\n"
          //            + "AND o.obstypeid IN ()\n"
          + "AND o.latitude >= ?\n"
          + "AND o.latitude <= ?\n"
          + "AND o.longitude >= ?\n"
          + "AND o.longitude <= ?\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n";

  private final String m_sQueryWithoutObsTemplate
          = m_sBaseSelect
          + ",null AS value\n"
          + m_sBaseFromWhere
          + "ORDER BY platformid, obstime desc";

  private final String m_sQueryWithObsTemplate
          = m_sBaseSelect
          + ",o.value\n"
          + m_sBaseFromWhere
          + "AND o.obstypeid = ?\n"
          + "ORDER BY platformid, obstime desc";

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

      if (oLastRequestBounds != null && oLastRequestBounds.intersects(nLat, nLng))
        continue;

      if (oRoad == null)
        continue;

      int nRoadIdIndex = Collections.binarySearch(oReturnedRoadIdList, oRoad.m_nId);
      if (nRoadIdIndex >= 0)
        continue;
      else
        oReturnedRoadIdList.add(~nRoadIdIndex, oRoad.m_nId);

      oJsonGenerator.writeNumber(oRoad.m_nId);
      oJsonGenerator.writeString(oRoad.m_sName);
      oJsonGenerator.writeNumber(MathUtil.fromMicro(oRoad.m_nYmid));
      oJsonGenerator.writeNumber(MathUtil.fromMicro(oRoad.m_nXmid));

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

      oJsonGenerator.writeStartArray();
      SegIterator oSegItr = oRoad.iterator();
      while (oSegItr.hasNext())
      {
        int[] nPoint = oSegItr.next();
        oJsonGenerator.writeStartArray();
        oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[1]));
        oJsonGenerator.writeNumber(MathUtil.fromMicro(nPoint[0]));
        oJsonGenerator.writeEndArray();
      }
      oJsonGenerator.writeEndArray();
    }
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
  protected PreparedStatement prepareObsStatement(Connection oConnection, ObsRequest oObsRequest) throws SQLException
  {
    //Multiple platforms could have snappped to the same road segment. Re-run the
    //platform query and build a list of platform Ids that snap to the selected
    //road. Then call the parent function to query for obs with the list of
    //platform ids

    PlatformRequest oPlatformRequest = new PlatformRequest();
    oPlatformRequest.setRequestBounds(oObsRequest.getRequestBounds());
    oPlatformRequest.setRequestTimestamp(oObsRequest.getRequestTimestamp());

    ArrayList<Integer> oPlatformIdList = new ArrayList<Integer>();

    int nRoadId = oObsRequest.getPlatformIds()[0];

    try (PreparedStatement oStatement = prepareLayerStatement(oConnection, 0, oPlatformRequest))
    {
      try (ResultSet oResult = oStatement.executeQuery())
      {
        while (oResult.next())
        {
          int nPlatformId = oResult.getInt("id");
          int nPlatformIdIndex = Collections.binarySearch(oPlatformIdList, nPlatformId);
          if (nPlatformIdIndex >= 0)
            continue;
          int nLat = oResult.getInt("latitude");
          int nLng = oResult.getInt("longitude");

          Road oRoad = m_oRoads.getLink(m_nSnapTolerance, nLng, nLat);
          if (oRoad != null && oRoad.m_nId == nRoadId)
            oPlatformIdList.add(~nPlatformIdIndex, nPlatformId);
        }
      }
    }

    int[] nPlatormIdArray = new int[oPlatformIdList.size()];
    int nPlatformIdIndex = 0;
    for (Integer nPlatformId : oPlatformIdList)
      nPlatormIdArray[nPlatformIdIndex++] = nPlatformId;

    oObsRequest.setPlatformIds(nPlatormIdArray);
    return super.prepareObsStatement(oConnection, oObsRequest);
  }
}
