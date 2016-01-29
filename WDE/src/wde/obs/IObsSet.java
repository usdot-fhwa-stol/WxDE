// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IObsSet.java
 */
package wde.obs;

import java.util.List;

/**
 * Provides an interface for observation sets, to allow standard access, and
 * modification.
 */
public interface IObsSet extends List<IObs> {
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Observation type contained in the {@code ObsSet}.
     */
    int getObsType();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return queue state attribute.
     */
    int getState();

    /**
     * <b> Mutator </b>
     * <p>
     * Sets the queue state attribute.
     * </p>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param nState queue state
     */
    void setState(int nState);

    /**
     * <b> Mutator </b>
     * <p>
     * Tells the observation manager not to filter old observations.
     * </p>
     * <p>
     * Extensions must implement this method.
     * </p>
     */
    void ignoreTime();

    /**
     * Creates a new {@link Observation} instance with the supplied properties, then
     * add it to the list.
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param nSensorId  id of the observation sensor.
     * @param lTimestamp timestamp corresponding to the time of observation.
     * @param nLat       latitude of the observation sensor.
     * @param nLon       longitude of the observation sensor.
     * @param tElev      elevation of the observing sensor.
     * @param dValue     observation value.
     */
    void addObs(int nSourceId, int nSensorId, long lTimestamp, long recvTime, int nLat, int nLon,
                short tElev, double dValue);

    /**
     * Creates a new {@link Observation} instance with the supplied properties, then
     * add it to the list.
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param nSourceId   id of the data source.
     * @param nSensorId   id of the observation sensor.
     * @param lTimestamp  timestamp corresponding to the time of observation.
     * @param nLat        latitude of the observation sensor.
     * @param nLon        longitude of the observation sensor.
     * @param tElev       elevation of the observing sensor.
     * @param dValue      observation value.
     * @param nRun        bit-field showing which quality checking algorithms to run
     *                    on this observation.
     * @param nFlags      bit-field showing whether the corresponding quality check
     *                    algorithm passed or failed.
     * @param fConfidence quality confidence level.
     */
    void addObs(int nSourceId, int nSensorId, long lTimestamp, long recvTime, int nLat, int nLon,
                short tElev, double dValue, char[] qcFlags, float fConfidence);

//    /**
//     * <b> Accessor </b>
//     * Returns the {@code IObs} object in the observation set at the supplied
//     * index.
//     * <p>
//     * Extensions must implement this method.
//     * </p>
//     *
//     * @param nIndex index of the observation of interest.
//     * @return the object at index {@code nIndex}.
//     */
//    T get(int nIndex);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return the size of the observation set.
     */
    int size();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Observation set serial identifier.
     */
    int serial();
}
