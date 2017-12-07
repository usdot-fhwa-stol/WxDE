package wde.comp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.obs.ObsSet;
import wde.obs.Observation;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;

/**
 *
 * @author scot.lange
 */
public class RoadWeatherAlerts extends AsyncQ<IObsSet>
{

  private static final Logger g_oLogger = Logger.getLogger(RoadWeatherAlerts.class);

  private final WDEMgr g_oWdeManager = WDEMgr.getInstance();
  private ObsMgr g_oObsMgr = ObsMgr.getInstance();
  private static final RoadWeatherAlerts g_oInstance = new RoadWeatherAlerts();

  private Observation m_oSearchObs = new Observation();

  private final ArrayList<IObs> m_oObsCache = new ArrayList<IObs>();
  private final long m_lMaxCacheObsAge = 1000 * 60 * 60;
  private final long m_lPurgeInterval = 1000 * 30 * 30;
  private long m_lLastPurge = System.currentTimeMillis();

  private final ArrayList<AlertRule> m_oCurrentObsRules = new ArrayList<AlertRule>();
  private final ArrayList<AlertRule> m_oForecastObsRules = new ArrayList<AlertRule>();

  private int[] m_nObstypesToCache;

  /**
   * Sort obs by time, obstype, latitude then longitude.
   */
  private static final Comparator<IObs> g_oObsetCacheSorter = new Comparator<IObs>()
  {
    @Override
    public int compare(IObs o1, IObs o2)
    {
      int nReturn = Long.compare(o1.getObsTimeLong(), o2.getObsTimeLong());
      if (nReturn == 0)
        nReturn = Integer.compare(o1.getObsTypeId(), o2.getObsTypeId());
      if (nReturn == 0)
        nReturn = Integer.compare(o1.getLatitude(), o2.getLatitude());
      if (nReturn == 0)
        nReturn = Integer.compare(o1.getLongitude(), o2.getLongitude());
      return nReturn;
    }
  };

  private RoadWeatherAlerts()
  {

    Config oConfig = ConfigSvc.getInstance().getConfig(RoadWeatherAlerts.class.getName());

    //Parse the configured rules
    for (String sRule : oConfig.getStringArray("rule"))
    {
      //both,precipType,3,precipIntensity,1,precipAlert,122 // light rain warning, id 1
      String[] sRuleComponents = sRule.split(",");

      if (sRuleComponents.length % 2 == 0 || sRuleComponents.length < 5)
      {
        g_oLogger.warn("Rule has wrong number of elements [" + sRule + "]");
        continue; // expecting an odd number of elements.
      }

      boolean bIsForecastRule = false;
      boolean bIsCurrentObsRule = false;

      String sRuleType = sRuleComponents[0].toLowerCase();
      if (sRuleType.equalsIgnoreCase("both"))
      {
        bIsCurrentObsRule = true;
        bIsForecastRule = true;
      }
      else if (sRuleType.equalsIgnoreCase("obs"))
        bIsCurrentObsRule = true;
      else if (sRuleType.equalsIgnoreCase("fcst"))
        bIsForecastRule = true;

      if (!bIsCurrentObsRule && !bIsForecastRule)
      {
        g_oLogger.warn("Unrecognized rule type [" + sRuleType + "]");
        continue;
      }

      // subtract one for the rule type entry at the beginning,
      // divide by two for the obstype/value pairs,
      // and then subtract 1 because the last pair is for the alert being generated
      int nConditionCount = (sRuleComponents.length - 1) / 2 - 1;

      String sAlertObstype = sRuleComponents[nConditionCount * 2 + 1];
      int nAlertObstype = oConfig.getInt("obstype." + sAlertObstype, -1);

      if (nAlertObstype < 0)
      {
        g_oLogger.warn("Unrecognized alert obst type [" + sAlertObstype + "]");
        continue;
      }

      AlertRule oAlertRule = new AlertRule();
      oAlertRule.setAlertObstypeId(nAlertObstype);

      boolean bIsRuleValid = true;

      int nConditionIndex = nConditionCount;
      while (--nConditionIndex >= 0)
      {
        String sObsType = sRuleComponents[1 + 2 * nConditionIndex];
        String sObsValue = sRuleComponents[2 + 2 * nConditionIndex];

        int nObsTypeId = oConfig.getInt("obstype." + sObsType, -1);
        if (nObsTypeId < 0)
        {
          bIsRuleValid = false;
          g_oLogger.warn("Unrecognized rule obst type [" + sObsType + "]");
          break;
        }

        double dObsValue;
        try
        {
          dObsValue = Double.parseDouble(sObsValue);
        }
        catch (Exception oEx)
        {
          //unable to parse
          g_oLogger.error("Unable to parse rule condition obs value: " + sObsValue, oEx);
          bIsRuleValid = false;
          break;
        }

        AlertCondition oAlertCondition = new AlertCondition();
        oAlertCondition.setObstypeId(nObsTypeId);
        oAlertCondition.setValue(dObsValue);

        oAlertRule.getConditions().add(oAlertCondition);
      }

      if (!bIsRuleValid)
        continue;

      if (bIsCurrentObsRule)
        m_oCurrentObsRules.add(oAlertRule);
      if (bIsForecastRule)
        m_oForecastObsRules.add(oAlertRule);
    }

    //loop through all of the rules and their conditions to determine what obstypes
    //will need to be cached
    ArrayList<AlertRule> oAllRules = new ArrayList<AlertRule>();
    oAllRules.addAll(m_oCurrentObsRules);
    oAllRules.addAll(m_oForecastObsRules);

    ArrayList<Integer> oRuleObstypes = new ArrayList<Integer>();

    for (AlertRule oAlertRule : oAllRules)
    {
      for (AlertCondition oAlertCondition : oAlertRule.getConditions())
      {
        int nObstypeIndex = Collections.binarySearch(oRuleObstypes, oAlertCondition.getObstypeId());
        if (nObstypeIndex < 0)
          oRuleObstypes.add(~nObstypeIndex, oAlertCondition.getObstypeId());
      }
    }

    m_nObstypesToCache = new int[oRuleObstypes.size()];

    int nObstypeIndex = oRuleObstypes.size();
    while (--nObstypeIndex >= 0)
      m_nObstypesToCache[nObstypeIndex] = oRuleObstypes.get(nObstypeIndex);

    g_oWdeManager.register(getClass().getName(), this);
  }

  public static RoadWeatherAlerts getInstance()
  {
    return g_oInstance;
  }

  @Override
  public void run(IObsSet iObsSet)
  {
    if (Arrays.binarySearch(m_nObstypesToCache, iObsSet.getObsType()) >= 0)
    {
      synchronized (this)
      {
        // add the obs to the platform and set the latest update timestamp
        int nObsIndex = iObsSet.size();
        g_oLogger.trace("Adding " + nObsIndex + " obs of type " + iObsSet.getObsType() + " to cache.");
        while (nObsIndex-- > 0)
        {
          IObs oObs = iObsSet.get(nObsIndex);
          int nObsCacheIndex = Collections.binarySearch(m_oObsCache, oObs, g_oObsetCacheSorter);
          if (nObsCacheIndex < 0)
            nObsCacheIndex = ~nObsCacheIndex;
          m_oObsCache.add(nObsCacheIndex, oObs);
        }

        evaluateRules(iObsSet);
        purgeCache();
      }
    }
    else
      g_oLogger.trace("Skipping obs set with type " + iObsSet.getObsType());

    // queue the obs set for the next process
    g_oWdeManager.queue(iObsSet);
  }

  /**
   * Instead of having both the purge method and the process methods lock
   * the
   */
  private void purgeCache()
  {
    long lCurrentTime = System.currentTimeMillis();
    if (lCurrentTime - m_lPurgeInterval < m_lLastPurge)
    {
      g_oLogger.trace("Skipping purge");
      return;
    }
    g_oLogger.debug("Purging cache. Current cache size is " + m_oObsCache.size());

    m_lLastPurge = lCurrentTime;

    long lCacheCutoffTime = lCurrentTime - m_lMaxCacheObsAge;

    int nPurgeCount = 0;
    Iterator<IObs> iObsItr = m_oObsCache.iterator();
    while (iObsItr.hasNext())
    {
      IObs iObs = iObsItr.next();

      //The cache is ordered by time, so once we find the first entry
      //new enough to keep we can break because the rest will be even newer
      if (iObs.getRecvTimeLong() < lCacheCutoffTime)
      {
        iObsItr.remove();
        ++nPurgeCount;
      }
      else
        break;
    }
    g_oLogger.debug("Purged " + nPurgeCount + " cached obs");
  }

  private void evaluateRules(IObsSet iObsSet)
  {
    //don't create the set of alerts until we are going to create at least one alert.
    HashMap<Integer, IObsSet> oAlertObsSets = null;
    int nObsIndex = iObsSet.size();
    while (--nObsIndex >= 0)
    {
      IObs iObs = iObsSet.get(nObsIndex);
      //loop through either the forecast rules or the current-obs rules for ones that use this obs type
      for (AlertRule oRule : iObs.getRecvTimeLong() < iObs.getObsTimeLong() ? m_oForecastObsRules : m_oCurrentObsRules)
      {
        boolean bEvaluateRule = false;
        //Go through the conditions to determine if one of them matches the current obstype.
        for (AlertCondition oCondition : oRule.getConditions())
        {
          if (oCondition.getObstypeId() == iObsSet.getObsType())
          {
            bEvaluateRule = true;
            break;
          }
        }
        if (!bEvaluateRule)
          continue;

        g_oLogger.trace("Evaluating rule to generate alert id " + oRule.getAlertObstypeId());
        //go back through the conditions and actually evaluate them
        boolean bCreateAlert = true;
        for (AlertCondition oCondition : oRule.getConditions())
        {
          if (oCondition.getObstypeId() == iObsSet.getObsType())
          {
            if (iObs.getValue() != oCondition.getValue())
            {
              g_oLogger.trace("New obs value does not match rule condition");
              bCreateAlert = false;
              break;
            }
          }
          else
          {
            //The condition isn't for the current obstype, so try to find it in the cache
            m_oSearchObs.setObsTypeId(oCondition.getObstypeId());
            m_oSearchObs.setLatitude(iObs.getLatitude());
            m_oSearchObs.setLongitude(iObs.getLongitude());
            m_oSearchObs.setObsTimeLong(iObs.getObsTimeLong());

            int nCachedObsIndex = Collections.binarySearch(m_oObsCache, m_oSearchObs, g_oObsetCacheSorter);
            if (nCachedObsIndex < 0)
            {
              g_oLogger.trace("Cannot find cached obs");
              bCreateAlert = false;
              break;

            }
            else if (m_oObsCache.get(nCachedObsIndex).getValue() != oCondition.getValue())
            {
              g_oLogger.trace("Cached value does not match rule condition");
              bCreateAlert = false;
              break;
            }
          }
        }

        if (bCreateAlert)
        {
          g_oLogger.info("Creating alert id " + oRule.getAlertObstypeId());
          if (oAlertObsSets == null)
            oAlertObsSets = new HashMap<Integer, IObsSet>();

          IObsSet iAlertObsSet = oAlertObsSets.get(oRule.getAlertObstypeId());
          if (iAlertObsSet == null)
          {
            iAlertObsSet = g_oObsMgr.getObsSet(oRule.getAlertObstypeId());
            iAlertObsSet = new ObsSet(oRule.getAlertObstypeId());
            oAlertObsSets.put(oRule.getAlertObstypeId(), iAlertObsSet);
          }

          iAlertObsSet.addObs(iObs.getSourceId(), iObs.getSensorId(), iObs.getObsTimeLong(), System.currentTimeMillis(), iObs.getLatitude(), iObs.getLongitude(), (short) iObs.getElevation(), 0);
        }
      }
    }

    //If any alerts were created, send them to the wde manager
    if (oAlertObsSets != null)
    {
      for (IObsSet iAlertObsSet : oAlertObsSets.values())
        g_oWdeManager.queue(iAlertObsSet);
    }
  }

  private static class AlertRule
  {

    private final List<AlertCondition> m_oConditions = new ArrayList<AlertCondition>();
    private int m_nAlertObstypeId;

    /**
     * @return the conditions
     */
    public List<AlertCondition> getConditions()
    {
      return m_oConditions;
    }

    /**
     * @return the alertObstypeId
     */
    public int getAlertObstypeId()
    {
      return m_nAlertObstypeId;
    }

    /**
     * @param alertObstypeId the alertObstypeId to set
     */
    public void setAlertObstypeId(int alertObstypeId)
    {
      this.m_nAlertObstypeId = alertObstypeId;
    }
  }

  private static class AlertCondition
  {

    private int m_nObstypeId;
    private double m_dValue;

    /**
     * @return the value
     */
    public double getValue()
    {
      return m_dValue;
    }

    /**
     * @param value the value to set
     */
    public void setValue(double value)
    {
      this.m_dValue = value;
    }

    /**
     * @return the obstypeId
     */
    public int getObstypeId()
    {
      return m_nObstypeId;
    }

    /**
     * @param obstypeId the obstypeId to set
     */
    public void setObstypeId(int obstypeId)
    {
      this.m_nObstypeId = obstypeId;
    }
  }

  public static void main(String[] args)
  {
    RoadWeatherAlerts alerts = RoadWeatherAlerts.getInstance();

    /**
     *
     * obstype.lowVisibilityWarning=19
     * obstype.lowVisibilityAlert=20
     *
     * obstype.precipType=207
     * obstype.precipIntensity=206
     * obstype.pavementCondition=?
     * obstype.pavementSlickness=?
     * obstype.visibility=5101
     */
    IObsSet oObsSetPrecipType = new ObsSet(207);

    IObsSet oObsSetPrecipIntensity = new ObsSet(206);

    long obstime = System.currentTimeMillis();
    int lat = 326792654;
    int lng = -925296782;

    int sourceId = 1;
    int sensorId = 2;

    oObsSetPrecipType.addObs(sourceId, sensorId, obstime, obstime, lat, lng, (short) 0, 1);
    oObsSetPrecipType.addObs(sourceId, sensorId, obstime - 500, obstime, lat, lng, (short) 0, lng);
    oObsSetPrecipType.addObs(sourceId, sensorId, obstime - 5000, obstime, lat, lng, (short) 0, lng);
    oObsSetPrecipType.addObs(sourceId, sensorId, obstime - 50000, obstime, lat, lng, (short) 0, lng);
    oObsSetPrecipType.addObs(sourceId, sensorId, obstime - 500000, obstime, lat, lng, (short) 0, lng);

    alerts.run(oObsSetPrecipType);

    oObsSetPrecipIntensity.addObs(sourceId, sensorId, obstime, obstime, lat, lng, (short) 0, 1);

    alerts.run(oObsSetPrecipIntensity);

  }
}
