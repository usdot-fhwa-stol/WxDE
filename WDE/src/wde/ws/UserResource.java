package wde.ws;

import org.apache.log4j.Logger;
import wde.dao.orm.UserDao;
import wde.dao.orm.UserDaoImpl;
import wde.data.Email;
import wde.data.User;
import wde.data.WxdeError;
import wde.util.Notification;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

@Path("user")
public class UserResource {

    private static final Logger logger = Logger.getLogger(UserResource.class);

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest req;
    @Context
    protected HttpServletResponse resp;

    private UserDao userDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public User getUser() {
        userDao = new UserDaoImpl();
        return userDao.getUser(req.getRemoteUser());
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public WxdeError postNewUser(User user) {

        userDao = new UserDaoImpl();

        // ensure the user id isn't in use, already
        User tempUser = userDao.getUser(user.getUser());
        if (tempUser != null) {
            WxdeError err = new WxdeError();
            err.setErrorMessage("The User ID has already been taken.");
            return err;
        }

        // ensure the email isn't in use, already
        tempUser = userDao.getUserByEmail(user.getEmail());
        if (tempUser != null) {
            WxdeError err = new WxdeError();
            err.setErrorMessage("The email address is already in use.");
            return err;
        }

        user.setGuid(UUID.randomUUID().toString());
        user.setIsVerified(false);

        try {
            userDao.insertNewUser(user);

            Email email = new Email();
            email.setTo(user.getEmail());
            email.setSubject("Weather Data Environment Validation");
            email.setBody("Thank you for registering at Weather Data Environment.\r\n\r\n"
                    + "Click or copy the link in this email and paste it in your browser's\r\n"
                    + "Address Bar to validate your account: \r\n\r\n"
                    + resp.encodeURL(req.getRequestURL().toString().replace("user", "email/verifyEmail/" + user.getGuid())));

            Notification.send(email);
        } catch (Exception ex) {
            logger.error(ex);
        }

        return null;
    }

    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void updateUser(User user) {

        userDao = new UserDaoImpl();

        user.setUser(req.getRemoteUser());
        userDao.updateUser(user);

        Email email = new Email();
        email.setTo(user.getEmail());
        email.setSubject("Weather Data Environment Notification");
        email.setBody("This is a courtesy notification to inform you that user account at the "
                + "Weather Data Environment has recently changed.  No action is required on your part.");

        Notification.send(email);
    }
}