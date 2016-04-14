package wde.ws;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import javax.naming.NamingException;
import org.codehaus.jackson.JsonGenerator;
import wde.dao.UnitConv;
import wde.metadata.ObsType;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
//@WebServlet(urlPatterns = "/RwisLayer/*", loadOnStartup = 1)
public abstract class PointLayerServletBase extends LayerServlet
{

  public PointLayerServletBase(boolean bHasObs, int... nZoomLevels) throws NamingException
  {
    super(bHasObs, nZoomLevels);
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
    while (oResult.next())
    {
      int nPlatformId = oResult.getInt("id");
      int nPlatformIdIndex = Collections.binarySearch(oReturnedPlatformIdList, nPlatformId);
      if (nPlatformIdIndex >= 0)
        continue;
      else
        oReturnedPlatformIdList.add(~nPlatformIdIndex, nPlatformId);
      double dLat;
      double dLng;
      if (m_oQueryUsersMicros)
      {
        dLat = MathUtil.fromMicro(oResult.getInt("latitude"));
        dLng = MathUtil.fromMicro(oResult.getInt("longitude"));
      }
      else
      {
        dLat = oResult.getDouble("latitude");
        dLng = oResult.getDouble("longitude");
      }
      if (oLastRequestBounds != null && oLastRequestBounds.intersects(dLat, dLng))
        continue;

      oJsonGenerator.writeNumber(nPlatformId);
      oJsonGenerator.writeString(oResult.getString("platformcode"));
      oJsonGenerator.writeNumber(dLat);
      oJsonGenerator.writeNumber(dLng);

      if (hasObs())
      {
        if (oCurrentRequest.hasObsType())
        {
          double oObsValue = oResult.getDouble("value");
          if (!oResult.wasNull())
          {
            oJsonGenerator.writeString(oValueFormatter.format(oObsValue));

            if (oUnitConverter != null)
              oJsonGenerator.writeString(oValueFormatter.format(oUnitConverter.convert(oObsValue)));
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
      }
    }
  }

}
