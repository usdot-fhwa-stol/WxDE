package wde.ws;

import org.postgresql.util.PSQLException;
import wde.dao.orm.FeedbackDao;
import wde.dao.orm.FeedbackDaoImpl;
import wde.data.Feedback;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

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
    public void postNewFeedback(Feedback feedback) {

        feedbackDao = new FeedbackDaoImpl();

        if (request.getRemoteUser() != null) {
            feedback.setUserName(request.getRemoteUser());
        }

        feedbackDao.insertNewFeedback(feedback);
    }
}
