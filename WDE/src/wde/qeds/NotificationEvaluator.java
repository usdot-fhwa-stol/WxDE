package wde.qeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import wde.dao.NotificationDao;
import wde.dao.orm.UserDao;
import wde.dao.orm.UserDaoImpl;
import wde.data.Email;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
public class NotificationEvaluator
{
  private final UserDao m_oUserDao = new UserDaoImpl();
  private boolean m_bVerboseDebug = true;
  private static final Logger g_oLogger = Logger.getLogger(NotificationEvaluator.class);

  public void processNotifications(List<SubObs> oNotificationObsList)
  {
    if(oNotificationObsList == null || oNotificationObsList.isEmpty())
    {
      g_oLogger.info("processNotifications called with no obs. Returning.");
      return;
    }

    List<Notification> oNotificationList = NotificationDao.getInstance().getNotifications();
    g_oLogger.info("Processing " + oNotificationList.size() + " notifications with " + oNotificationObsList.size() + " obs");


    List<SubObs> oNotificationAreaObsList = new ArrayList<>();

    for(Notification oNotification : oNotificationList)
    {
      g_oLogger.info("Processing notification " + oNotification);
      boolean bNotificationAlreadyTriggered = oNotification.isTriggered();


      Set<Integer> oNotificationObstypes = new HashSet<>();

      for(NotificationCondition oCondition : oNotification.getConditions())
        oNotificationObstypes.add(oCondition.getObstypeId());

      int nMinLat = Integer.MAX_VALUE;
      int nMaxLat = Integer.MIN_VALUE;
      int nMinLon = Integer.MAX_VALUE;
      int nMaxLon = Integer.MIN_VALUE;

      Set<Integer> oObsListObstypes = new HashSet<>();

      //Get a list of just obs in the area
      oNotificationAreaObsList.clear();
      for(SubObs oSubObs : oNotificationObsList)
      {
        int nLat = MathUtil.toMicro(oSubObs.m_dLat);
        int nLon = MathUtil.toMicro(oSubObs.m_dLon);

        nMinLat = Integer.min(nLat, nMinLat);
        nMaxLat = Integer.max(nLat, nMaxLat);

        nMinLon = Integer.min(nLon, nMinLon);
        nMaxLon = Integer.max(nLon, nMaxLon);

        oObsListObstypes.add(oSubObs.m_nObsTypeId);

        if( oNotificationObstypes.contains(oSubObs.m_nObsTypeId) &&
            nLat >= oNotification.getLat1Micros() &&
            nLat <= oNotification.getLat2Micros()&&
            nLon >= oNotification.getLon1Micros()&&
            nLon <= oNotification.getLon2Micros())
        {
          oNotificationAreaObsList.add(oSubObs);
        }
      }

      StringBuilder oObstypeBuilder = new StringBuilder(oObsListObstypes.size() * 3);
      for(Iterator<Integer> iObstypeIdItr = oObsListObstypes.iterator(); iObstypeIdItr.hasNext();)
        oObstypeBuilder.append(",").append(iObstypeIdItr.next());

      g_oLogger.info("Obslist summary- minLat: "  + nMinLat + " maxLat: " + nMaxLat + " minLon: "  + nMinLon + " maxLon: " + nMaxLon + ", Obstypes: " +  oObstypeBuilder.toString());

      if(oNotificationAreaObsList.isEmpty())
      {
        g_oLogger.info("No obs in notification area. Skipping");
        continue;
      }

      StringBuilder oObsListBuilder = m_bVerboseDebug ? new StringBuilder(500) : null;

      //calculate the mean/mode per obstype first so that it will only
      //be calculated once even if multiple conditions use the same
      //obstype
      Map<Integer, List<Double>> oObstypeModes = new HashMap<>();
      Map<Integer, Double> oObstypeAverages = new HashMap<>();
      Map<Integer, Double> oObstypeMins = new HashMap<>();
      Map<Integer, Double> oObstypeMaxes = new HashMap<>();
      for(Integer nObstypeId : oNotificationObstypes)
      {
        g_oLogger.info("Processing obstype " + nObstypeId);

        if(oObsListBuilder != null)
          oObsListBuilder.setLength(0);

        int nModeCount = 0;
        final Map<Double, Integer> oCountMap = new HashMap<>();

        double dMin = Double.MAX_VALUE;
        double dMax = Double.MIN_VALUE;
        double dSum = 0;
        int nObsCount = 0;

        for(SubObs oSubObs : oNotificationAreaObsList)
        {
          if(oSubObs.m_nObsTypeId != nObstypeId)
            continue;


          double dValue = oNotification.isUsingMetric() ? oSubObs.m_dValue : oSubObs.m_dEnglishValue;
          dSum += dValue;
          ++nObsCount;

          if(oObsListBuilder != null)
            oObsListBuilder.append(",").append(dValue);

          dMin = Double.min(dValue, dMin);
          dMax = Double.max(dValue, dMax);

          int nCount = oCountMap.getOrDefault(dValue, 1);

          oCountMap.put(dValue, nCount);

          if (nCount > nModeCount)
              nModeCount = nCount;
        }

        if(nObsCount == 0)
        {
          g_oLogger.info("No obs for obstype " + nObstypeId);
          continue;
        }

        double dMean = dSum / nObsCount;
        oObstypeAverages.put(nObstypeId, dMean);
        oObstypeMaxes.put(nObstypeId, dMax);
        oObstypeMins.put(nObstypeId, dMin);

        List<Double> oModeList = new ArrayList<>();
        if(nModeCount > 1)
        {
          for(Map.Entry<Double, Integer> oCountEntry : oCountMap.entrySet())
          {
            if(oCountEntry.getValue() == nModeCount)
              oModeList.add(oCountEntry.getKey());
          }
        }
        g_oLogger.info("Mode count was one. Not adding any values to mode list.");

        oObstypeModes.put(nObstypeId, oModeList);


        if(oObsListBuilder != null)
          g_oLogger.info("Obs processed: " + oObsListBuilder.toString());

        g_oLogger.info("Sum: " + dSum + ", Count: " + nObsCount + ", Mean: " + dMean + ", Min: " + dMin + ", Max: " + dMax + ", Mode Count: " + nModeCount + ", Modes: " + oModeList);
      }

      //reuse the same arrays for the most common cases
      //mean will always have one, mode will most likely have two,
      //and any will always have two (one for min, one for max)
      //If a multi-modal result has more than two, then a new array will
      //used
      Double[] dOneValue = new Double[1];
      Double[] dTwoValues = new Double[2];

      for(NotificationCondition oCondition : oNotification.getConditions())
      {
        g_oLogger.info("Processing condition  " + oCondition);
        int nObstypeId = oCondition.getObstypeId();
        Double[] dValues;
        switch(oCondition.getFilter())
        {
          case Mode:
            if(!oObstypeModes.containsKey(nObstypeId))
              continue;

            List<Double> oModeList = oObstypeModes.get(nObstypeId);
            switch(oModeList.size())
            {
              case 1:
                dValues = dOneValue;
                break;
              case 2:
                dValues = dTwoValues;
                break;
              default:
                dValues = new Double[oModeList.size()];
                break;
            }
            oModeList.toArray(dValues);
            break;
          case Mean:
            if(!oObstypeAverages.containsKey(nObstypeId))
              continue;

            dValues = dOneValue;
            dValues[0] = oObstypeAverages.get(nObstypeId);
            break;
          case Any:
            if(!(oObstypeMaxes.containsKey(nObstypeId) && oObstypeMins.containsKey(nObstypeId)))
              continue;

            dValues = dTwoValues;
            dValues[0] = oObstypeMaxes.get(nObstypeId);
            dValues[1] = oObstypeMins.get(nObstypeId);
            break;
          default:
            continue;
        }

        if(m_bVerboseDebug)
          g_oLogger.info("Condition Values: " + Arrays.toString(dValues));

        if(oCondition.isTriggered())
        {
          //see if the values no longer match the notification trigger, and
          //if it doesn't then reset the triggered status if it has moved
          //outside the tolerance range
          //all values must clear to reset the condition
          boolean bClear = true;
          for(double dValue : dValues)
          {
            if(!bClear)
              break;
            boolean bMatches = oCondition.getOperator().matches(dValue, oCondition.getValue());
            if( bMatches)
            {
              if(m_bVerboseDebug)
                g_oLogger.info("Value still matches condition.");

              bClear = false;
            }
            else
            {
              double dDiff = Math.abs(dValue - oCondition.getValue());
              if(dDiff >= oCondition.getTolerance())
              {
                if(m_bVerboseDebug)
                  g_oLogger.info("Value no longer matches, and diff " + dDiff + " is >= tolerance value " + oCondition.getTolerance());
              }
              else
              {
                if(m_bVerboseDebug)
                  g_oLogger.info("Value no longer matches, but diff " + dDiff + " is less than tolerance value " + oCondition.getTolerance());

                bClear = false;
              }
            }
          }

          if(m_bVerboseDebug)
            g_oLogger.info(bClear ? "Clearing." : "Won't clear");

          oCondition.setTriggered(!bClear);
        }
        else
        {
          for(double dValue : dValues)
          {
            if(oCondition.getOperator().matches(dValue, oCondition.getValue()))
            {
              if(m_bVerboseDebug)
                g_oLogger.info("Found matching value. Will trigger");
              //see if a value matches the condition to trigger a notification
              //only one value has to match to trigger
              oCondition.setTriggered(true);
              break;
            }
          }
        }
        if(m_bVerboseDebug)
          g_oLogger.info("Ending trigger status is " + oCondition.isTriggered());
      }

      //A notification is triggered when all conditions are met, and then reset when
      //all of them clear. If it was already triggered, then any triggered condition
      //will keep the whole notification triggered. If it wasn't already triggered, then
      //all conditions must be triggered to trigger the notification

      boolean bNotificationNowTriggered = bNotificationAlreadyTriggered ? oNotification.hasTriggeredCondition() : oNotification.hasAllConditionslTriggered();
      oNotification.setTriggered(bNotificationNowTriggered);

      //There is nothing updated to save if there are no conditions currently triggered,
      //and the notification wasn't previously triggered
      if(bNotificationAlreadyTriggered || bNotificationNowTriggered || oNotification.hasTriggeredCondition())
      {
        if(m_bVerboseDebug)
          g_oLogger.info("Updating notification.");
        NotificationDao.getInstance().updateNotification(oNotification);
      }

      if(bNotificationNowTriggered && ! bNotificationAlreadyTriggered)
      {
        g_oLogger.info("Notification was triggered. Sending email.");
        Email oEmail = new Email();
        oEmail.setSubject("WDE Notification");
        oEmail.setBody(oNotification.getMessage());
        oEmail.setTo(m_oUserDao.getUser(oNotification.getUsername()).getEmail());
        wde.util.Notification.send(oEmail);
      }
    }
    g_oLogger.info("Finished");
  }
}
