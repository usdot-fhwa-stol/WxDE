package wde.dao.orm;

import wde.data.Feedback;

import java.util.List;


public interface FeedbackDao {

    public void insertNewFeedback(Feedback feedback);

    public List<Feedback> getFeedbacks();

    public void deleteFeedback(String id);

}
