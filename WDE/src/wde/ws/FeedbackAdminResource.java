package wde.ws;

import wde.dao.orm.FeedbackDao;
import wde.dao.orm.FeedbackDaoImpl;
import wde.data.Feedback;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Path("admin/feedback")
public class FeedbackAdminResource {

    @Context
    HttpServletRequest request;
    @Context
    HttpServletResponse response;
    @Context
    ServletContext context;

    private FeedbackDao feedbackDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Feedback> getFeedbacks() {
        feedbackDao = new FeedbackDaoImpl();
        List<Feedback> feedbacks = feedbackDao.getFeedbacks();
        return feedbacks;
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Path("delete")
    public void DeleteFeedback(Feedback feedback) {
        feedbackDao = new FeedbackDaoImpl();

        feedbackDao.deleteFeedback(feedback.getFeedbackId().toString());
    }
}
