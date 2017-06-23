package wde.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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


  public void insertNotification(Notification notification)
  {
    if(m_bDebug)
      logger.info("Inserting notification: " + notification);

    try(Connection con = datasource.getConnection();
            PreparedStatement insertNotifStmt = con.prepareStatement(
                    "INSERT INTO subs.notification (username, message, lat1, lon1, lat2, lon2, usingMetric, triggered) VALUES (?, ?, ?, ?, ?, ?, ? ,?)", Statement.RETURN_GENERATED_KEYS);
            PreparedStatement insertConditionStmt = con.prepareStatement(
                    "INSERT INTO subs.notifcondition (notifId, index, filter, operator, value, obstypeId, tolerance, triggered) VALUES (?, ?, ?, ? ,?, ?, ?, ?)"))
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


      int conditionIndex = -1;
      insertConditionStmt.setInt(1, notification.getId());
      for(NotificationCondition condition : notification.getConditions())
      {
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
            PreparedStatement insertConditionStmt = con.prepareStatement(
                    "UPDATE subs.notifcondition SET filter = ?, operator = ?, value = ?, obstypeid = ?, tolerance = ?, triggered = ? WHERE notifid = ? AND index = ?"))
    {
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

      for(NotificationCondition condition : notification.getConditions())
      {
        if(m_bDebug)
          logger.info("Updating notification condition: " + condition);
        colIndex = 0;

        insertConditionStmt.setString(++colIndex, condition.getFilter().name());
        insertConditionStmt.setString(++colIndex, condition.getOperator().name());
        insertConditionStmt.setDouble(++colIndex, condition.getValue());
        insertConditionStmt.setInt(++colIndex, condition.getObstypeId());
        insertConditionStmt.setDouble(++colIndex, condition.getTolerance());
        insertConditionStmt.setBoolean(++colIndex, condition.isTriggered());

        insertConditionStmt.setInt(++colIndex, notification.getId());
        insertConditionStmt.setInt(++colIndex, condition.getIndex());
        insertConditionStmt.executeUpdate();
      }

    }
    catch(Exception ex)
    {
      logger.error("Error updating notification", ex);
    }
  }

  public void deleteNotification(long notificationId)
  {

  }

  private List<Notification> privGetNotifications(String userName)
  {
      ArrayList<Notification> notifications = new ArrayList<>();

    String notifSelect = "SELECT id, username, message, lat1, lon1, lat2, lon2, usingMetric, triggered FROM subs.notification";
    if(userName != null)
      notifSelect += " WHERE userName = ?";

    try(Connection con = datasource.getConnection();
            PreparedStatement notifStmt = con.prepareStatement(notifSelect);
            PreparedStatement conditionStmt = con.prepareStatement("SELECT notifid, index, filter, operator, value, obstypeid, tolerance, triggered FROM subs.notifcondition WHERE notifid = ? ORDER BY index"))
    {
      if(userName != null)
        notifStmt.setString(1, userName);

      try(ResultSet notificationRs = notifStmt.executeQuery())
      {
        while(notificationRs.next())
        {
          Notification notification = new Notification();
          notifications.add(notification);

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
              //notifid, index, filter, operator, value, obstypeid, tolerance, triggered
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
      }
    }
    catch (SQLException ex)
    {
      logger.error("", ex);
    }
    return notifications;
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
