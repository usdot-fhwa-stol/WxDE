package wde.dao.orm;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;
import wde.dao.WdeDbSessionManager;
import wde.data.Feedback;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class FeedbackDaoImpl implements FeedbackDao {

    private static final Logger logger = Logger.getLogger(FeedbackDaoImpl.class);

    @Override
    public void insertNewFeedback(Feedback feedback) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        Date date = new Date();
        long milis = date.getTime();
        Timestamp timeStamp = new Timestamp(milis);

        feedback.setDateCreated(date);
        feedback.setTsCreated(timeStamp);

        try {
            session.insert("wde.FeedbackMapper.insertNewFeedback", feedback);
        } catch (PersistenceException ex) {
            logger.error(ex);
        } finally {
            session.close();
        }
    }

    @Override
    public List<Feedback> getFeedbacks() {
        SqlSessionFactory sqlMapper = WdeDbSessionManager
                .getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Feedback> retList = null;
        try {
            retList = session
                    .selectList("wde.FeedbackMapper.selectFeedbacks");
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public void deleteFeedback(String id) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        try {
            session.delete("wde.FeedbackMapper.deleteFeedback", id);
        } finally {
            session.close();
        }
    }
}
