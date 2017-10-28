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

    StringBuilder oMetricObsListBuilder = new StringBuilder(100);
    StringBuilder oEnglishObsListBuilder = new StringBuilder(100);

    if (oResult.next())
    {
      do
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
            oMetricObsListBuilder.setLength(0);
            oEnglishObsListBuilder.setLength(0);

            int nNextPlatformId = nPlatformId;
            do
            {
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

        if(!oResult.next())
          break;
      } while (true);
    }
  }

}
