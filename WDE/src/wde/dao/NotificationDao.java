package wde.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import wde.qeds.ConditionFilter;
import wde.qeds.ConditionOperator;
import wde.qeds.Notification;
import wde.qeds.NotificationCondition;

/**
 *
 * @author scot.lange
 */
public class NotificationDao
{
  private static final Logger logger = Logger.getLogger(NotificationDao.class);

  private static NotificationDao instance;

  private boolean m_bDebug = true;

  private DataSource datasource;

  private static final String NOTIF_SELECT = "SELECT id, username, message, lat1, lon1, lat2, lon2, usingMetric, triggered FROM subs.notification";
  private static final String CONDITION_SELECT = "SELECT notifid, index, filter, operator, value, obstypeid, tolerance, triggered FROM subs.notifcondition WHERE notifid = ? ORDER BY index";

  Comparator<NotificationCondition> COMP_BY_INDEX = new Comparator<NotificationCondition>()
  {
    @Override
    public int compare(NotificationCondition o1, NotificationCondition o2)
    {
      return o1.getIndex() - o2.getIndex();
    }
  };

  /**
   * Constructor
   */
  private NotificationDao()
  {
    try
    {
      InitialContext oInitCtx = new InitialContext();
      Context oCtx = (Context) oInitCtx.lookup("java:comp/env");
      DataSource iDatasource = (DataSource) oCtx.lookup("jdbc/wxde");

      datasource = iDatasource;
      oInitCtx.close();
    }
    catch(NamingException oEx)
    {
      throw new RuntimeException(oEx);
    }
  }

  /**
   * @return FederationManagementDao
   */
  public static NotificationDao getInstance()
  {
    if(instance == null)
      instance = new NotificationDao();

    return instance;
  }

  private void insertNotificationConditions(Connection con, List<NotificationCondition> conditions, int notificationId) throws SQLException
  {
    insertNotificationConditions(con, conditions, notificationId, 0, conditions.size());
  }

  private void insertNotificationConditions(Connection con, List<NotificationCondition> conditions, int notificationId, int startIndex, int endIndex) throws SQLException
  {
    try(PreparedStatement insertConditionStmt = con.prepareStatement(
        "INSERT INTO subs.notifcondition (notifId, index, filter, operator, value, obstypeId, tolerance, triggered) VALUES (?, ?, ?, ? ,?, ?, ?, ?)"))
    {

      int colIndex;
      int conditionIndex = -1;
      insertConditionStmt.setInt(1, notificationId);
      for(int i = startIndex; i < endIndex; ++i)
      {
        NotificationCondition condition = conditions.get(i);
        if(m_bDebug)
          logger.info("Inserting notification condition: " + condition);

        colIndex = 1;

        condition.setIndex(++conditionIndex);
        insertConditionStmt.setInt(++colIndex, condition.getIndex());
        insertConditionStmt.setString(++colIndex, condition.getFilter().name());
        insertConditionStmt.setString(++colIndex, condition.getOperator().name());
        insertConditionStmt.setDouble(++colIndex, condition.getValue());
        insertConditionStmt.setInt(++colIndex, condition.getObstypeId());
        insertConditionStmt.setDouble(++colIndex, condition.getTolerance());
        insertConditionStmt.setBoolean(++colIndex, condition.isTriggered());
        insertConditionStmt.executeUpdate();
      }

    }

  }

  public void insertNotification(Notification notification)
  {
    if(m_bDebug)
      logger.info("Inserting notification: " + notification);

    try(Connection con = datasource.getConnection();
        PreparedStatement insertNotifStmt = con.prepareStatement(
            "INSERT INTO subs.notification (username, message, lat1, lon1, lat2, lon2, usingMetric, triggered) VALUES (?, ?, ?, ?, ?, ?, ? ,?)", Statement.RETURN_GENERATED_KEYS))
    {
      int colIndex = 0;
      insertNotifStmt.setString(++colIndex, notification.getUsername());
      insertNotifStmt.setString(++colIndex, notification.getMessage());
      insertNotifStmt.setInt(++colIndex, notification.getLat1Micros());
      insertNotifStmt.setInt(++colIndex, notification.getLon1Micros());
      insertNotifStmt.setInt(++colIndex, notification.getLat2Micros());
      insertNotifStmt.setInt(++colIndex, notification.getLon2Micros());
      insertNotifStmt.setBoolean(++colIndex, notification.isUsingMetric());
      insertNotifStmt.setBoolean(++colIndex, notification.isTriggered());
      insertNotifStmt.executeUpdate();

      ResultSet iResult = insertNotifStmt.getGeneratedKeys();
      if(iResult.next())
        notification.setId(iResult.getInt(1));
      else
        throw new Exception("Failed to get generated key after insert");

      insertNotificationConditions(con, notification.getConditions(), notification.getId());
    }
    catch(Exception ex)
    {
      logger.error("Error inserting notification", ex);
    }
  }

  public void updateNotification(Notification notification)
  {
    if(m_bDebug)
      logger.info("Updating notification: " + notification);

    try(Connection con = datasource.getConnection();
        PreparedStatement updateNotifStmt = con.prepareStatement(
            "UPDATE subs.notification SET username = ?, message = ?, lat1 = ?, lon1 = ?, lat2 = ?, lon2 = ?, usingMetric = ?, triggered = ? WHERE id = ?");
        PreparedStatement updateConditionStmt = con.prepareStatement(
            "UPDATE subs.notifcondition SET filter = ?, operator = ?, value = ?, obstypeid = ?, tolerance = ?, triggered = ? WHERE notifid = ? AND index = ?"))
    {
      Notification currNotification = getNotification(notification.getId());
      if(currNotification == null)
      {
        logger.warn("Attempted to update non-existent notification " + notification.getId());
        return;
      }

      int colIndex = 0;
      updateNotifStmt.setString(++colIndex, notification.getUsername());
      updateNotifStmt.setString(++colIndex, notification.getMessage());
      updateNotifStmt.setInt(++colIndex, notification.getLat1Micros());
      updateNotifStmt.setInt(++colIndex, notification.getLon1Micros());
      updateNotifStmt.setInt(++colIndex, notification.getLat2Micros());
      updateNotifStmt.setInt(++colIndex, notification.getLon2Micros());
      updateNotifStmt.setBoolean(++colIndex, notification.isUsingMetric());
      updateNotifStmt.setBoolean(++colIndex, notification.isTriggered());
      updateNotifStmt.setInt(++colIndex, notification.getId());
      updateNotifStmt.executeUpdate();

      List<NotificationCondition> currentConditions = currNotification.getConditions();
      List<NotificationCondition> newConditions = notification.getConditions();

      Collections.sort(currentConditions, COMP_BY_INDEX);
      Collections.sort(newConditions, COMP_BY_INDEX);

      int updateCount = Math.min(currentConditions.size(), newConditions.size());

      for(int i = 0; i < updateCount; ++i)
      {
        NotificationCondition condition = newConditions.get(i);
        if(m_bDebug)
          logger.info("Updating notification condition: " + condition);
        colIndex = 0;

        updateConditionStmt.setString(++colIndex, condition.getFilter().name());
        updateConditionStmt.setString(++colIndex, condition.getOperator().name());
        updateConditionStmt.setDouble(++colIndex, condition.getValue());
        updateConditionStmt.setInt(++colIndex, condition.getObstypeId());
        updateConditionStmt.setDouble(++colIndex, condition.getTolerance());
        updateConditionStmt.setBoolean(++colIndex, condition.isTriggered());

        updateConditionStmt.setInt(++colIndex, notification.getId());
        updateConditionStmt.setInt(++colIndex, condition.getIndex());
        updateConditionStmt.executeUpdate();
      }

      if(newConditions.size() > currentConditions.size())
      { // new set of conditions is larger than old one, so insert ones that weren't handled with update
        insertNotificationConditions(con, newConditions, notification.getId(), currentConditions.size(), newConditions.size());
      }
      else if(newConditions.size() < currentConditions.size())
      { // new set of conditions is smaller than old one, so delete extras
        try(PreparedStatement deleteStmt = con.prepareStatement(
            "DELETE FROM subs.notifcondition WHERE notifid = ? AND index >= ?"))
        {
          deleteStmt.setInt(1, notification.getId());
          deleteStmt.setInt(2, newConditions.size());
        }
      }

    }
    catch(Exception ex)
    {
      logger.error("Error updating notification", ex);
    }
  }

  public boolean deleteNotification(int notificationId)
  {
    try(Connection con = datasource.getConnection();
        PreparedStatement updateNotifStmt = con.prepareStatement(
            "DELETE FROM subs.notification WHERE id = ?"))
    {
      updateNotifStmt.setInt(1, notificationId);
      updateNotifStmt.execute();
      return true;
    }
    catch(Exception ex)
    {
      logger.error("error deleting notification " + notificationId, ex);
      return false;
    }
  }

  private void setNotifValues(Notification notification, ResultSet notificationRs, PreparedStatement conditionStmt) throws SQLException
  {
    notification.setId(notificationRs.getInt("id"));
    notification.setUsername(notificationRs.getString("userName"));
    notification.setMessage(notificationRs.getString("message"));
    notification.setTriggered(notificationRs.getBoolean("triggered"));
    notification.setUsingMetric(notificationRs.getBoolean("usingMetric"));

    int lat1 = notificationRs.getInt("lat1");
    int lat2 = notificationRs.getInt("lat2");
    notification.setLat1(Math.min(lat1, lat2));
    notification.setLat2(Math.max(lat1, lat2));

    int lon1 = notificationRs.getInt("lon1");
    int lon2 = notificationRs.getInt("lon2");
    notification.setLon1(Math.min(lon1, lon2));
    notification.setLon2(Math.max(lon1, lon2));

    conditionStmt.setInt(1, notification.getId());
    try(ResultSet conditionRs = conditionStmt.executeQuery())
    {
      ArrayList<NotificationCondition> conditions = new ArrayList<>();
      notification.setConditions(conditions);
      while(conditionRs.next())
      {
        // notifid, index, filter, operator, value, obstypeid, tolerance, triggered
        NotificationCondition condition = new NotificationCondition();
        conditions.add(condition);

        condition.setFilter(ConditionFilter.valueOf(conditionRs.getString("filter")));
        condition.setOperator(ConditionOperator.valueOf(conditionRs.getString("operator")));
        condition.setValue(conditionRs.getDouble("value"));
        condition.setTolerance(conditionRs.getDouble("tolerance"));
        condition.setObstypeId(conditionRs.getInt("obstypeId"));
        condition.setIndex(conditionRs.getInt("index"));
        condition.setTriggered(conditionRs.getBoolean("triggered"));
      }
    }
  }

  private List<Notification> privGetNotifications(String userName)
  {
    ArrayList<Notification> notifications = new ArrayList<>();

    String notifSelect = NOTIF_SELECT;
    if(userName != null)
      notifSelect += " WHERE userName = ?";

    try(Connection con = datasource.getConnection();
        PreparedStatement notifStmt = con.prepareStatement(notifSelect);
        PreparedStatement conditionStmt = con.prepareStatement(CONDITION_SELECT))
    {
      if(userName != null)
        notifStmt.setString(1, userName);

      try(ResultSet notificationRs = notifStmt.executeQuery())
      {
        while(notificationRs.next())
        {
          Notification notification = new Notification();
          notifications.add(notification);
          setNotifValues(notification, notificationRs, conditionStmt);
        }
      }
    }
    catch(SQLException ex)
    {
      logger.error("", ex);
    }
    return notifications;
  }

  public Notification getNotification(int id)
  {

    String notifSelect = NOTIF_SELECT + " WHERE id = ?";

    try(Connection con = datasource.getConnection();
        PreparedStatement notifStmt = con.prepareStatement(notifSelect);
        PreparedStatement conditionStmt = con.prepareStatement(CONDITION_SELECT))
    {
      notifStmt.setInt(1, id);
      try(ResultSet notificationRs = notifStmt.executeQuery())
      {
        if(notificationRs.next())
        {
          Notification notification = new Notification();
          setNotifValues(notification, notificationRs, conditionStmt);
          return notification;
        }
      }
    }
    catch(SQLException ex)
    {
      logger.error("", ex);
    }
    return null;
  }

  public List<Notification> getNotifications()
  {
    return privGetNotifications(null);
  }

  public List<Notification> getUserNotifications(String userName)
  {
    return privGetNotifications(userName);
  }

}
