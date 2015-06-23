package wde.dao.orm;

import wde.data.User;

public interface UserDao {

    public User getUser(String userId);

    public User getUserByGuid(String guid);

    public void insertNewUser(User user);

    public void updateUser(User user);

    public User getUserByEmail(String email);

    public User getGuidByEmail(String email);

    public User getUserByPasswordGuid(String guid);

    public User updatePasswordGuid(User user);

    public void updateUserVerified(User user);

    public void updateNewPassword(User user);
}
