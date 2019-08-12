package wde.ws;

import wde.dao.orm.UserDao;
import wde.dao.orm.UserDaoImpl;
import wde.data.Email;
import wde.data.User;
import wde.util.Notification;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Path("email")
public class EmailResource {

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest req;
    @Context
    protected HttpServletResponse res;
    @Context
    protected ServletContext context;

    private UserDao userDao;

    @GET
    @Path("verifyEmail/{guid}")
    public String verifyEmail(@PathParam("guid") String guid) {

        String url = "/verifyUserEmailFail.jsp";

        userDao = new UserDaoImpl();
        User user = userDao.getUserByGuid(guid);

        if (user != null && !user.getIsVerified()) {
            user.setIsVerified(true);
            user.setGuid(null);
            userDao.updateUserVerified(user);

            url = "/verifyUserEmailSuccess.jsp";
        }

        RequestDispatcher dispatcher = context.getRequestDispatcher(url);

        try {
            dispatcher.forward(req, res);
        } catch (ServletException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("forgotUserId")
    public void forgotUserId(User _user) {

        userDao = new UserDaoImpl();
        User user = userDao.getUserByEmail(_user.getEmail());

        Email email = new Email();
        email.setTo(user.getEmail());
        email.setSubject("Forgotten User ID for the Weather Data Environment");
        email.setBody(
                "You recently requested to retrieve your User ID from the Weather Data Environment system.\r\n\r\n"
                        + "Your account's User ID is: " + user.getUser() + "\r\n\r\n"
                        + "Weather Data Environment - Login: "
                        + req.getRequestURL().toString().replace("resources/email/forgotUserId", "auth/loginRedirect.jsp")
        );

        Notification.send(email);
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("forgotPassword")
    public void forgotPassword(User _user) {
        // Get the user by email
        userDao = new UserDaoImpl();
        User tempUser = userDao.getUserByEmail(_user.getEmail());

        // set the guid
        String passwordGuid = UUID.randomUUID().toString();
        tempUser.setPasswordGuid(passwordGuid);

        // set the date
        tempUser.setDatePassword(new Date());

        User user = userDao.updatePasswordGuid(tempUser);

        Email email = new Email();
        email.setTo(user.getEmail());
        email.setSubject("Forgotten Password for the Weather Data Environment");
        email.setBody(
                "You recently requested to retrieve your Password from the Weather Data Environment system.\r\n\r\n" +
                        "Please click the link and follow the instructions on the page you're redirected to: \r\n\r\n" +
                        res.encodeURL(req.getRequestURL().toString().replace("forgotPassword", "resetPassword/" + user.getPasswordGuid()))
        );

        Notification.send(email);
    }

    @GET
    @Path("resetPassword/{passGuid}")
    public String resetPassword(@PathParam("passGuid") String passwordGuid) {

        String url = "/index.jsp";

        userDao = new UserDaoImpl();
        User user = userDao.getUserByPasswordGuid(passwordGuid);

        if (user == null) {
            url = "/userAccountRetrieval.jsp";
        } else {
            url = "/resetPassword.jsp";
        }

        RequestDispatcher dispatcher = context.getRequestDispatcher(url);

        try {
            dispatcher.forward(req, res);
        } catch (ServletException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    @POST
    @Path("submitPassword")
    public User submitPassword(User _user) {
        userDao = new UserDaoImpl();
        User user = userDao.getUserByPasswordGuid(_user.getPasswordGuid());

        user.setPassword(_user.getPassword());
        user.setPasswordGuid(null);
        user.setDatePassword(null);
        userDao.updateNewPassword(user);

        return user;
    }
}
