package wde.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import wde.dao.NotificationDao;
import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;
import wde.qeds.Notification;
import wde.qeds.NotificationCondition;

/**
 *
 * @author scot.lange
 */
@Path("notifications")
public class NotificationsResource
{

  private final JsonFactory m_oJsonFactory = new JsonFactory();

  private ObsTypeDao obstypeDao = ObsTypeDao.getInstance();

  @Context
  protected UriInfo uriInfo;
  @Context
  protected HttpServletRequest req;
  @Context
  protected HttpServletResponse resp;

  private static final NotificationDao notificationsDao = NotificationDao.getInstance();

  @GET
  @Produces(
  {
    MediaType.APPLICATION_JSON
  })
  public Response getNotifications()
  {
    return Response.ok(getNotificationJsonStream(notificationsDao.getUserNotifications(req.getUserPrincipal().getName()))).build();
  }

  @POST
  @Consumes(
  {
    MediaType.APPLICATION_JSON
  })
  @Produces(
  {
    MediaType.APPLICATION_JSON
  })
  public Response addNotification(Notification notification)
  {
    if (req.getUserPrincipal() != null)
      notification.setUsername(req.getUserPrincipal().getName());
    notificationsDao.insertNotification(notification);

    return Response.ok(getNotificationJsonStream(notification)).build();
  }

  @DELETE
  @Path("{id}")
  public Response deleteNotification(@PathParam("id") int id)
  {

    return (notificationsDao.deleteNotification(id) ? Response.ok() : Response.serverError()).build();
  }

  private void serializeNotification(JsonGenerator jsonGen, Notification notification) throws IOException
  {
    jsonGen.writeStartObject();
    jsonGen.writeNumberField("id", notification.getId());
    jsonGen.writeNumberField("lat1", notification.getLat1());
    jsonGen.writeNumberField("lon1", notification.getLon1());
    jsonGen.writeNumberField("lat2", notification.getLat2());
    jsonGen.writeNumberField("lon2", notification.getLon2());
    jsonGen.writeStringField("message", notification.getMessage());
    jsonGen.writeBooleanField("triggered", notification.isTriggered());
    jsonGen.writeBooleanField("usingMetric", notification.isUsingMetric());

    DecimalFormat formatter = new DecimalFormat("0.##");
    jsonGen.writeArrayFieldStart("conditions");
    for (NotificationCondition condition : notification.getConditions())
    {
      ObsType obstype = obstypeDao.getObsType(condition.getObstypeId());
      jsonGen.writeStartObject();
      jsonGen.writeStringField("filter", condition.getFilter().name());
      jsonGen.writeStringField("operator", condition.getOperator().name());
      jsonGen.writeNumberField("obstypeId", condition.getObstypeId());
      jsonGen.writeStringField("tolerance", formatter.format(condition.getTolerance()));
      jsonGen.writeBooleanField("triggered", condition.isTriggered());

      if (obstype != null)
        jsonGen.writeStringField("obstypeName", obstype.getObsType());

      jsonGen.writeStringField("value", formatter.format(condition.getValue()));
      jsonGen.writeEndObject();
    }
    jsonGen.writeEndArray();

    jsonGen.writeEndObject();
  }

  private StreamingOutput getNotificationJsonStream(final Notification notification)
  {

    return new StreamingOutput()
    {
      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException
      {

        try (JsonGenerator jsonGen = m_oJsonFactory.createJsonGenerator(os, JsonEncoding.UTF8))
        {
          serializeNotification(jsonGen, notification);
        }

      }
    };
  }

  private StreamingOutput getNotificationJsonStream(final List<Notification> notifications)
  {

    return new StreamingOutput()
    {
      @Override
      public void write(OutputStream os) throws IOException, WebApplicationException
      {

        try (JsonGenerator jsonGen = m_oJsonFactory.createJsonGenerator(os, JsonEncoding.UTF8))
        {
          jsonGen.writeStartArray();
          for (Notification notification : notifications)
          {
            serializeNotification(jsonGen, notification);
          }
          jsonGen.writeEndArray();
        }

      }
    };
  }

}
