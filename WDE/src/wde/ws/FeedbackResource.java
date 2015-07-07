package wde.ws;

import org.postgresql.util.PSQLException;
import wde.dao.orm.FeedbackDao;
import wde.dao.orm.FeedbackDaoImpl;
import wde.data.Feedback;

import com.google.code.kaptcha.Constants;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("feedback")
public class FeedbackResource {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    ServletContext context;

    private FeedbackDao feedbackDao;

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces("application/json")
    public String postNewFeedback(Feedback feedback) throws IOException {

        //
        // Ensure the captcha provided is of the expected value.
        //
        String kaptchaExpected = (String) request.getSession()
                .getAttribute(Constants.KAPTCHA_SESSION_KEY);
        //String kaptchaReceived = request.getParameter("kaptcha");
        String kaptchaReceived = feedback.getKaptcha();

        if (kaptchaReceived == null || !kaptchaReceived.equalsIgnoreCase(kaptchaExpected)) {
            //response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return "{ \"status2\":\"Error\", \"message\":\"The captcha value provided was not correct.\", \"expected\":\"" + kaptchaExpected + "\", \"received\":\"" + feedback.getKaptcha() + "\" }";
        }

        try {
            feedbackDao = new FeedbackDaoImpl();

            if (request.getRemoteUser() != null) {
                feedback.setUserName(request.getRemoteUser());
            }

            feedbackDao.insertNewFeedback(feedback);

        } catch (Exception ex) {
            return "{ \"status2\":\"Error\", \"message\":\"An error has occurred. Please consult with the system administrator.\" }";
        }

        return "{ \"status2\":\"Success\" }";
    }
}
