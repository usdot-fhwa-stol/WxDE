package wde.compute;

import wde.compute.algo.ObservationTypes.Mapping;
import wde.dao.ObsTypeDao;
import wde.dao.SensorDao;
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

    protected InferenceSeq m_seq;
    protected int m_seqOrder;
    /**
     * Pointer to the sensors cache.
     */
    protected SensorDao sensorDao = SensorDao.getInstance();
    protected Connection m_oConnection;

    /**
     * Pointer to the observation manager instance.
     */
    protected ObsMgr m_oObsMgr = ObsMgr.getInstance();

    public Inference() {
    }

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Qch}
     * </p>
     */
    public Inference(InferenceSeq seq, int seqOrder) {
        this.m_seq = seq;
        this.m_seqOrder = seqOrder;
    }

    public ObsMgr getObsMgr() {
        return m_oObsMgr;
    }

    public int getObsTypeId() {
        if (m_seq != null) {
            return m_seq.getObsTypeId();
        }

        return -1;
    }

    public Double getRelatedObsValue(Mapping observation, IObs obs) {
        Double value = Double.NaN;
        try {
            ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();

            int obsTypeId = obsTypeDao.getObsTypeId(observation.getVdtObsTypeName());
            IObs relatedObs = getRelatedObs(obsTypeId, obs);

            value = relatedObs.getValue();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
}