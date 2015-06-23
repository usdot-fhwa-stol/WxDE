package wde.dao.orm;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.Subscription;

import java.util.List;

public class SubscriptionDaoImpl implements SubscriptionDao {

    @Override
    public List<Subscription> getSubscriptionsByOwner(String user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Subscription> retList = null;
        try {
            retList = session.selectList("wde.SubscriptionMapper.selectSubscriptionsByOwner", user);
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Subscription> getSubscriptionsByMember(String user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Subscription> retList = null;
        try {
            retList = session.selectList("wde.SubscriptionMapper.selectMemberSubscriptions", user);
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Subscription> getPublicSubscriptions(String user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Subscription> retList = null;
        try {
            retList = session.selectList("wde.SubscriptionMapper.selectPublicSubscriptions", user);
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public List<Subscription> getSubscriptions(String user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        List<Subscription> retList = null;
        try {
            retList = session.selectList("wde.SubscriptionMapper.selectSubscriptions", user);
        } finally {
            session.close();
        }
        return retList;
    }

    @Override
    public Subscription getSubscription(Integer subscriptionId) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        Subscription ret = null;
        try {
            ret = session.selectOne("wde.SubscriptionMapper.selectSubscription", subscriptionId);
        } finally {
            session.close();
        }
        return ret;
    }
}
