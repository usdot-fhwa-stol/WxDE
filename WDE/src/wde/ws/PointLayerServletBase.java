package wde.ws;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
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
    Set<Integer> oReturnedPlatformIds = new HashSet<Integer>();

    StringBuilder oMetricObsListBuilder = new StringBuilder(100);
    StringBuilder oEnglishObsListBuilder = new StringBuilder(100);

    if (oResult.next())
    {
      do
      {
        int nPlatformId = oResult.getInt("id");

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

        //only write the layer if we haven't written it already
        if (!oReturnedPlatformIds.contains(nPlatformId))
        {
          oReturnedPlatformIds.add(nPlatformId);

          //only write the layer if it wasn't covered by the last request
          if(oLastRequestBounds == null || !oLastRequestBounds.intersects(dLat, dLng))
          {
            oJsonGenerator.writeNumber(nPlatformId);
            oJsonGenerator.writeString(oResult.getString("platformcode"));
            oJsonGenerator.writeNumber(dLat);
            oJsonGenerator.writeNumber(dLng);

            if (hasObs())
            {
              if (oCurrentRequest.hasObsType())
              {
                oMetricObsListBuilder.setLength(0);
                oEnglishObsListBuilder.setLength(0);

                int nNextPlatformId = nPlatformId;
                int nLastSensorIndex = Integer.MIN_VALUE;
                do
                {
                  // Results are ordered by sensor index.
                  // Only write the first value for each
                  int nCurrentSensorIndex = oResult.getInt("sensorindex");
                  if(nCurrentSensorIndex == nLastSensorIndex)
                    continue;
                  else
                    nLastSensorIndex = nCurrentSensorIndex;

                  double oObsValue = oResult.getDouble("value");

                  if (!oResult.wasNull())
                  {
                    if(oMetricObsListBuilder.length() > 0)
                      oMetricObsListBuilder.append(", ");

                    oMetricObsListBuilder.append(oValueFormatter.format(oObsValue));

                    if (oUnitConverter != null)
                    {
                      if(oEnglishObsListBuilder.length() > 0)
                        oEnglishObsListBuilder.append(", ");

                      oEnglishObsListBuilder.append(oValueFormatter.format(oUnitConverter.convert(oObsValue)));
                    }
                  }
                }while(oResult.next() && (nNextPlatformId =  oResult.getInt("id")) == nPlatformId);


                oJsonGenerator.writeString(oMetricObsListBuilder.toString());
                oJsonGenerator.writeString(oEnglishObsListBuilder.toString());


                //if we exited the loop because we got a new platform id, then
                //continue for the next platform. If the platform ids are equal,
                //then we exited because there are no more rows.
                if(nNextPlatformId == nPlatformId)
                  break;
                else
                 continue;
              }
              else
              {
                oJsonGenerator.writeString("");
                oJsonGenerator.writeString("");
              }
            }
          }
        }

        if(!oResult.next())
          break;
      } while (true);
    }
  }

}
