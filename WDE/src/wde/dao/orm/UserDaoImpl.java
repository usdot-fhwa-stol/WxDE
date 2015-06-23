package wde.dao.orm;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import wde.dao.WdeDbSessionManager;
import wde.data.User;
import wde.util.ShaHasher;


public class UserDaoImpl implements UserDao {

    @Override
    public void insertNewUser(User user) throws PersistenceException {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        //Hash the Password
        user.setPassword(ShaHasher.hash(user.getPassword()));

        try {
            session.insert("wde.UserMapper.insertNewUser", user);
        } catch (PersistenceException ex) {
            throw ex;
        }
        finally {
            session.close();
        }
    }

    @Override
    public User getUser(String userId) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        User user = null;
        try {
            user = session.selectOne("wde.UserMapper.selectUser", userId);
        } finally {
            session.close();
        }
        return user;
    }

    @Override
    public User getUserByGuid(String guid) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        User user = null;
        try {
            user = session.selectOne("wde.UserMapper.selectUserByGuid", guid);
        } finally {
            session.close();
        }
        return user;
    }

    @Override
    public User getUserByEmail(String email) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        User user = null;
        try {
            user = session.selectOne("wde.UserMapper.selectUserByEmail", email);
        } finally {
            session.close();
        }
        return user;
    }

    public User getGuidByEmail(String email) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        User user = null;

        try {
            user = session.selectOne("wde.UserMapper.selectGuidByEmail", email);
        } finally {
            session.close();
        }
        return user;
    }

    @Override
    public User getUserByPasswordGuid(String guid) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);
        User user = null;
        try {
            user = session.selectOne("wde.UserMapper.selectUserByPasswordGuid", guid);
        } finally {
            session.close();
        }
        return user;
    }

    @Override
    public void updateUser(User user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        //Hash the Password
        user.setPassword(ShaHasher.hash(user.getPassword()));

        try {
            session.update("wde.UserMapper.updateUser", user);
        } finally {
            session.close();
        }
    }

    @Override
    public void updateUserVerified(User user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        try {
            session.update("wde.UserMapper.updateUserVerifyEmail", user);
        } finally {
            session.close();
        }
    }

    @Override
    public User updatePasswordGuid(User user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        try {
            session.update("wde.UserMapper.updatePasswordGuid", user);
        } finally {
            session.close();
        }
        return user;
    }

    @Override
    public void updateNewPassword(User user) {
        SqlSessionFactory sqlMapper = WdeDbSessionManager.getSqlSessionFactory();
        SqlSession session = sqlMapper.openSession(true);

        //Hash the Password
        user.setPassword(ShaHasher.hash(user.getPassword()));

        try {
            session.update("wde.UserMapper.updateNewPassword", user);
        } finally {
            session.close();
        }
    }

}
