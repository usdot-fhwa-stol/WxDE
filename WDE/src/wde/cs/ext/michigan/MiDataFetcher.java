package wde.cs.ext.michigan;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 * Calls the MI AVL webservice and translates its response, a JSON array of
 * objects, into a CSV file. Each row in the CSV file represents an object
 * from the array made up of the fields of interest.
 * <p>
 * Arguments passed to run:
 * {@code <client-name> <api-key> <signature> <remote-url> <output-file> }
 *</p>
 * <p>
 * {@code client-name, api-key, and signature} are the credentials to access the service.
 * <br>
 * {@code remote-url} is the URL to the webservice endpoint that we are calling
 * <br>
 * {@code output-file} is the file where the CSV output will be written
 * </p>
 * @author scot.lange
 */
public class MiDataFetcher
{

  private static final JsonFactory m_oJsonFactory = new JsonFactory();

  public static void main(String[] args) throws IOException, GeneralSecurityException
  {
//
//client: WxDE
//apiKey: wxdeatms
//secretKey: OCtjR9eje1ezS0cz
//

//    args = new String[]
//    {
//      "WxDE", "wxdeatms", "OCtjR9eje1ezS0cz",
//      "https://mdot.delcan.net/atms-rs/avl/snowplow/iteris",
//      "E:/opt/apache-tomcat-7.0.63.test/webapps/ROOT/MI/mi.csv"
//    };

    if (args != null && args.length != 5)
    {
      System.out.println("Required args: <client-name> <api-key> <signature> <remote-url> <output-file>");
      System.exit(0);
    }

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String clientName = args[0];
    String apiKey = args[1];
    String secretKey = args[2];
    String remoteUrl = args[3];
    File outputFile = new File(args[4]).getCanonicalFile();

    outputFile.getParentFile().mkdirs();

    Calendar rightNow = Calendar.getInstance();
    int offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
    rightNow.add(Calendar.MILLISECOND, -(offset));

    String timestamp = String.valueOf(rightNow.getTimeInMillis());
    String data = timestamp + apiKey;
    System.out.println("data: " + data);

    //Using the javax.crypto API to produce a HmacSHA256 signature
    String generateHmacSHA256Signature = generateHmacSHA256Signature(data, secretKey);
    System.out.println("Signature: " + generateHmacSHA256Signature);

    //The signature must be URL encoded AFTER it is signed.
    String urlEncodedSign = URLEncoder.encode(generateHmacSHA256Signature, "UTF-8");
    System.out.println("Url encoded value: " + urlEncodedSign);

    String standardQueryParams = "?user=" + clientName + "&timestamp=" + timestamp + "&apiKey=" + apiKey + "&signature=" + urlEncodedSign;

    //Get Snowplow data
    String urlWithParams = remoteUrl + standardQueryParams;
    System.out.println(urlWithParams);

    ClientResponse snowplowResponse = Client.create().resource(urlWithParams)
            .accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

    List<String> fields = Arrays.asList(
            "timestamp",
            "name",
            "latitude",
            "longitude",
            "speed",
            "heading",
            "surfaceTemp",
            "airTemp",
            "visibilitySituation",
            "precipSituation",
            "windSituation" //            "adjacentSnow",
            //            "roadwaySnow"
            ,
             "vehicleId"
    //"pavementObservation",
    //"plowLane",
    //"treatLaneOne",
    //"treatLaneTwo",
    //"solidRate",
    //"actualSolidRate",
    //"actualDirectLiquidRate",
    //"directLiquidRate",
    //"prewetRate",
    //"liquidRate",
    //"solidMaterial",
    //"liquidMaterial",
    //"frontPlow",
    //"leftPlow",
    //"rightPlow",
    //"bellyPlow",
    //"currentLiquidMaterial",
    //        "currentSolidMaterial",
    );

    Set<String> fieldSet = new HashSet<>(fields);
    Map<String, String> currentRow = new HashMap<>();

    int rowCount = 0;
    try (JsonParser parser = m_oJsonFactory.createJsonParser(new BufferedInputStream(snowplowResponse.getEntityInputStream()));
            PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(outputFile))))
    {
      //write out the header
      boolean firstHeader = true;
      for (String fieldName : fields)
      {
        if (firstHeader)
          firstHeader = false;
        else
          out.print(",");

        out.print(fieldName);

      }
      out.println();

      //process JSON tokens. For each object encountered iterate through
      //all of its fields, capture any values in our field list, and then write out
      //a row
      JsonToken token;
      while ((token = parser.nextToken()) != null)
      {
        if (JsonToken.START_OBJECT.equals(token))
        {
          boolean firstRowValue = true;
          while (!JsonToken.END_OBJECT.equals(token = parser.nextToken()))
          {
            if (JsonToken.FIELD_NAME.equals(token))
            {
              String fieldName = parser.getCurrentName();
              token = parser.nextToken();
              if (token == null)
                break;

              //store the value if this is a field we care about
              if (fieldSet.contains(fieldName))
              {
                String fieldValue;
                if (JsonToken.VALUE_NULL.equals(token))
                  fieldValue = null;
                else if ("timestamp".equalsIgnoreCase(fieldName))
                  fieldValue = sdf.format(new Date(parser.getLongValue()));
                else
                  fieldValue = parser.getText();

                currentRow.put(fieldName, fieldValue);
              }
            }
          }

          //write out row
          for (String field : fields)
          {
            if (firstRowValue)
              firstRowValue = false;
            else
              out.print(",");

            String value = currentRow.get(field);
            if (value != null)
              out.print(value);
          }

          out.println();
          currentRow.clear();
          ++rowCount;
        }
      }
    }
    System.out.println("Finished processing " + rowCount + " rows");
  }

  protected static String generateHmacSHA256Signature(String data, String key) throws GeneralSecurityException
  {
    byte[] hmacData;

    if (key == null)
      key = "secretKey";

    try
    {
      SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(secretKey);
      hmacData = mac.doFinal(data.getBytes("UTF-8"));
      return Base64.encodeBase64String(hmacData);
    }
    catch (UnsupportedEncodingException e)
    {
      throw new GeneralSecurityException(e);
    }
  }
}
