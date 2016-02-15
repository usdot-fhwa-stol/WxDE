package wde.compute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wde.compute.algo.ObservationTypes.Mapping;
import wde.dao.ObsTypeDao;
import wde.data.shp.Polyline;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.ObsMgr;
import wde.qchs.Roads;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public abstract class Inference<T extends Inference> implements Comparable<T> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected InferenceSeq m_seq;
    protected int m_seqOrder;
    protected Connection m_oConnection;
    private ObsMgr m_obsMgr = null;

    public Inference() {
    }

    public Inference(InferenceSeq seq, int seqOrder) {
        this.m_seq = seq;
        this.m_seqOrder = seqOrder;
    }

    public ObsMgr getObsMgr() {
        if (m_obsMgr == null) {
            m_obsMgr = ObsMgr.getInstance();
        }

        return m_obsMgr;
    }

    public int[] getObsTypeIds() {
        if (m_seq != null) {
            return m_seq.getObsTypeIds();
        }

        return new int[] { /* null */ };
    }

    public Double getRelatedObsValue(Mapping observation, IObs obs) {
        Double value = Double.NaN;
        try {
            ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();

            for(String obsTypeName : observation.getNames()) {
                int obsTypeId = obsTypeDao.getObsTypeId(obsTypeName);
                if (obsTypeId > 0)
                    continue;

                IObs relatedObs = getRelatedObs(obsTypeId, obs);
                if (relatedObs == null)
                    continue;

                value = relatedObs.getValue();
                break;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            logger.debug("An exception was encountered while attempting to retrieve related observation value.", e);
        }

        return value;
    }

    public IObs getRelatedObs(int obsTypeId, IObs obs) {
        IObs useObs = null;
        try {
            Roads roads = Roads.getInstance();

            if (roads == null)
                throw new Exception("Could not get instance of Roads.");

            Polyline polyline = roads.getLink(100, obs.getLongitude(), obs.getLatitude());
            if (polyline == null)
                throw new Exception("Could not get the midpoint of the road.");
            int[] midpoint = new int[2];
            polyline.getMidPoint(midpoint);

            ArrayList<IObs> obsArrayList = new ArrayList<>();
            getObsMgr().getBackground(
                    obsTypeId,
                    midpoint[0] - 100,
                    midpoint[1] - 100,
                    midpoint[0] + 100,
                    midpoint[1] + 100,
                    obs.getObsTimeLong() - 30 * 60000, /* 30 minutes */
                    obs.getObsTimeLong() + 30 * 60000,
                    obsArrayList);

            IObs selectedObs = null;
            int minDist = Integer.MAX_VALUE;
            for (IObs currObs : obsArrayList) {
                int dist = polyline.snap(100, currObs.getLatitude(), currObs.getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    selectedObs = currObs;
                }
            }

            if (selectedObs == null)
                throw new Exception("Not obs found within minimum distance of link.");

            useObs = selectedObs;

        } catch (Exception e) {
            //e.printStackTrace();
            logger.debug("An exception was encountered while attempting retrieve related observations.", e);
        }

        return useObs;
    }

    void init(InferenceSeq seq, int m_seqOrder) {
        this.m_seq = seq;
        this.m_seqOrder = m_seqOrder;
    }

    /**
     * Abstract method should be defined for extensions.
     * <p>
     * Performs the extensions quality checking algorithm.
     * </p>
     *
     * @param obsTypeId observation type id.
     * @param obs       observation.
     * @param sensor    sensor that recorded the observation.
     * @param obs
     */
    public abstract Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs);

    protected Set<InferenceResult> newInferenceResultSet() {
        return new TreeSet<InferenceResult>();
    }

    @Override
    public int compareTo(T o) {
        return m_seqOrder - o.m_seqOrder;
    }

    protected Logger getLogger() {
        return logger;
    }
}