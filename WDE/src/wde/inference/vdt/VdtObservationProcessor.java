package wde.inference.vdt;

import wde.dao.ObsTypeDao;
import wde.dao.ObservationDao;
import wde.data.shp.Polyline;
import wde.inference.InferenceResult;
import wde.inference.InferenceResultProcessor;
import wde.inference.Inferencer;
import wde.inference.InferencerProcessorMapping;
import wde.inference.ObservationProcessor;
import wde.inference.vdt.rwx.PavementConditionInferenceResult;
import wde.inference.vdt.rwx.PavementSlicknessInferenceResult;
import wde.inference.vdt.rwx.PrecipitationIntensityInferenceResult;
import wde.inference.vdt.rwx.PrecipitationTypeInferenceResult;
import wde.inference.vdt.rwx.RwxInferencerFactory;
import wde.inference.vdt.rwx.VisibilityInferenceResult;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.obs.ObsSet;
import wde.obs.Observation;
import wde.qchs.Roads;
import wde.qeds.PlatformMonitor;
import wde.radar.Radar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VdtObservationProcessor extends ObservationProcessor {

    private IObs currentObs;
    private final List<Observation> pseudoObservations = new ArrayList<>();

    public VdtObservationProcessor() {
        super();
    }

    protected void init() {
        try {
            this.addInferencers(
                    new InferencerProcessorMapping[]{
                            new InferencerProcessorMapping<>(
                                    RwxInferencerFactory.newPrecipitationTypeInferencer(),
                                    new AbstractInferenceResultProcessor<PrecipitationTypeInferenceResult>() {
                                        @Override
                                        public void process(PrecipitationTypeInferenceResult result) {
                                            Observation obs = createObsCopy(getCurrentObs());
                                            obs.setObsTypeId(VdtObservationTypes.wde_precip_type.getWdeObsTypeId());
                                            obs.setValue(result.getPrecipitationType().getCode());
                                            obs.setConfValue(result.getConfidence());
                                            obs.setElevation(0);

                                            addPseudoObservation(obs);
                                        }
                                    }
                            ),
                            new InferencerProcessorMapping<>(
                                    RwxInferencerFactory.newPrecipitationIntensityInferencer(),
                                    new AbstractInferenceResultProcessor<PrecipitationIntensityInferenceResult>() {
                                        @Override
                                        public void process(PrecipitationIntensityInferenceResult result) {
                                            Observation obs = createObsCopy(getCurrentObs());
                                            obs.setObsTypeId(VdtObservationTypes.wde_precip_type.getWdeObsTypeId());
                                            obs.setValue(result.getPrecipitationIntensity().getCode());
                                            obs.setConfValue(result.getConfidence());
                                            obs.setElevation(0);

                                            addPseudoObservation(obs);
                                        }
                                    }
                            ),
                            new InferencerProcessorMapping<>(
                                    RwxInferencerFactory.newPavementConditionInferencer(),
                                    new AbstractInferenceResultProcessor<PavementConditionInferenceResult>() {
                                        @Override
                                        public void process(PavementConditionInferenceResult result) {
                                            Observation obs = createObsCopy(getCurrentObs());
                                            obs.setObsTypeId(VdtObservationTypes.wde_precip_type.getWdeObsTypeId());
                                            obs.setValue(result.getPavementCondition().getCode());
                                            obs.setConfValue(result.getConfidence());
                                            obs.setElevation(0);

                                            addPseudoObservation(obs);
                                        }
                                    }
                            ),
                            new InferencerProcessorMapping<>(
                                    RwxInferencerFactory.newPavementSlicknessInferencer(),
                                    new AbstractInferenceResultProcessor<PavementSlicknessInferenceResult>() {
                                        @Override
                                        public void process(PavementSlicknessInferenceResult result) {
                                            Observation obs = createObsCopy(getCurrentObs());
                                            obs.setObsTypeId(VdtObservationTypes.wde_precip_type.getWdeObsTypeId());
                                            obs.setValue(result.getPavementSlickness().getCode());
                                            obs.setConfValue(result.getConfidence());
                                            obs.setElevation(0);

                                            addPseudoObservation(obs);

                                        }
                                    }
                            ),
                            new InferencerProcessorMapping<>(
                                    RwxInferencerFactory.newVisibilityInferencer(),
                                    new AbstractInferenceResultProcessor<VisibilityInferenceResult>() {
                                        @Override
                                        public void process(VisibilityInferenceResult result) {
                                            Observation obs = createObsCopy(getCurrentObs());
                                            obs.setObsTypeId(VdtObservationTypes.wde_precip_type.getWdeObsTypeId());
                                            obs.setValue(result.getVisibility().getCode());
                                            obs.setConfValue(result.getConfidence());
                                            obs.setElevation(0);

                                            addPseudoObservation(obs);
                                        }
                                    }
                            )
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Observation createObsCopy(IObs origObs) {
        Observation newObs = new Observation(
                origObs.getObsTypeId(),
                origObs.getSourceId(),
                origObs.getSensorId(),
                origObs.getObsTimeLong(),
                origObs.getRecvTimeLong(),
                origObs.getLatitude(),
                origObs.getLongitude(),
                (short) origObs.getElevation(),
                origObs.getValue(),
                origObs.getQchCharFlag(),
                origObs.getConfValue()
        );

        return newObs;
    }

    private void addPseudoObservation(Observation obs) {
        synchronized (this.pseudoObservations) {
            this.pseudoObservations.add(obs);

            PlatformMonitor.getInstance().updatePlatform(obs);
        }
    }

    private void setCurrentObs(IObs obs) {
        synchronized (this.currentObs) {
            this.currentObs = obs;
        }
    }

    public IObs getCurrentObs() {
        synchronized (this.currentObs) {
            return this.currentObs;
        }
    }

    @Override
    protected void process(IObs obs) {

        setCurrentObs(obs);

        final VdtObservationProcessor processor = this;
        VdtObservationResolver resolver = new VdtObservationResolver(this, obs) {
            @Override
            public float getRadarRef() {
                IObs currentObs = getCurrentObs();
                int lat = currentObs.getLatitude();
                int lon = currentObs.getLongitude();

                float radarValue = Radar.getInstance().getReflectivity(lon, lat);

                return radarValue;
            }

            @Override
            public IObs doResolve(String obsTypeName) {

                final int obsTypeId = ObsTypeDao.getInstance().getObsTypeId(obsTypeName);

                IObs selectedObs = null;
                final IObs currentObs = getCurrentObs();

                //
                // If the observation currently being processed requests its own obsType, then it most likely
                // does not need to be processed further, however, return it to the caller and let them decide.
                //
                if (currentObs.getObsTypeId() == obsTypeId) {
                    return currentObs;
                }

                //
                // Acquire a copy of the current set of observations being processed so that any operations
                // against the set do not affect the behavior of other inferencers. This initial copy keeps
                // the set operations from creating a copy each time they are called.
                //
                final Set<IObs> currentObsSet = copyObsSet(getObservationProcessor().getCurrentObsSet());
                filterObsSetByObsTypeId(currentObsSet, obsTypeId); // ensure the obs is the right type
                filterObsSetByObsLink(currentObsSet, currentObs);  // ensure the obs is the on the same link

                if (currentObsSet != null && currentObsSet.size() > 0) {
                    //
                    // Attempt to resolve the requested observation using the current set of observations being
                    // processed. It is possible that the observation will be in the current set if the observation
                    // was collected by the same platform as the current observation.
                    //
                    for (IObs currentObsSetElement : currentObsSet) {
                        if (currentObsSetElement.getObsTypeId() == obsTypeId &&
                                compareObsTimeRange(currentObs, currentObsSetElement)) {
                            //
                            // The observation is approximately in the same area and type as that being requested
                            // as compared to the current observation being processed, so it must be the observation
                            // of interest. Observations that coexist within the same approximate timeframe and location
                            // should be the most relevant in any calculations.
                            //
                            selectedObs = currentObsSetElement;
                            break;
                        }
                    }
                }

                //
                // An observation was not resolved that exists within the current set of observations being processed,
                // so it might have been processed previously. This will require fetching a set of observations that are
                // approximate to the current observation in both time and location.
                //
                if (selectedObs == null) {
                    ObsSet resultObsSet = ObsMgr.getInstance().getObsSet(obsTypeId);

                    //
                    // Filter the observation set on obstype and link.
                    //
                    filterObsSetByObsTypeId(resultObsSet, obsTypeId);
                    filterObsSetByObsLink(resultObsSet, currentObs);

                    //
                    // Iterate through the ObsSet to find an observation that meets the current criteria.
                    //
                    for (int i = 0; i < resultObsSet.size(); ++i) {
                        IObs resultObs = resultObsSet.get(i);
                        if (resultObs == null) {
                            throw new NullPointerException("resultObs");
                        }

                        if (compareObsTimeRange(currentObs, resultObs)) {
                            selectedObs = resultObs;
                            break;
                        }
                    }
                }

                return selectedObs;
            }
        };

        for (InferencerProcessorMapping inferencerProcessorMapping : getInferenceProcessorMaps()) {

            Inferencer inferencer = inferencerProcessorMapping.getInferencer();
            InferenceResultProcessor resultProcessor = inferencerProcessorMapping.getResultProcessor();

            if (inferencer == null || resultProcessor == null) {
                throw new NullPointerException("inferencerProcessMap");
            }

            InferenceResult result = inferencer.doInference(resolver);
            resultProcessor.process(result);
        }

        synchronized (pseudoObservations) {
            ObservationDao.getInstance().insertObservations(pseudoObservations);
            pseudoObservations.clear();
        }
    }

    private boolean compareObsTimeRange(IObs obs1, IObs obs2) {
        if (obs1 == null || obs2 == null)
            throw new NullPointerException("compareObsLatLon");

        long obs1Timestamp = obs1.getObsTimeLong();
        long obs2Timestamp = obs2.getObsTimeLong();

        long obsTimestampDiff = Math.abs(obs1Timestamp - obs2Timestamp);

        //
        // If the difference between the two observation times is greater than 5 minutes then they aren't comparable. It
        // might also need to be part of a larger scoped comparison to determine whether a comparable observation exists
        // within these parameters, otherwise, find the closest observation and use that as long as its not too out of
        // the observation timerange.
        //
        // This might need to be configurable.
        //
        if (obs1Timestamp > 1000 * 60 * 5) {
            return false;
        }

        return true;
    }

    public Set<IObs> copyObsSet(IObsSet obsSet) {
        if (obsSet == null)
            throw new NullPointerException("obsSet");

        Set<IObs> resultSet = new HashSet<>();

        for (int i = 0; i < obsSet.size(); ++i) {
            resultSet.add(obsSet.get(i));
        }

        return resultSet;
    }

    public void filterObsSetByObsTypeId(Set<IObs> obsSet, int obsTypeId) {
        if (obsSet == null)
            throw new NullPointerException("obsSet");

        for (IObs obs : obsSet) {
            if (obs.getObsTypeId() != obsTypeId) {
                obsSet.remove(obs);
            }
        }
    }

    public void filterObsSetByObsTypeId(ObsSet obsSet, int obsTypeId) {
        if (obsSet == null)
            throw new NullPointerException("obsSet");

        for (IObs obs : obsSet) {
            if (obs.getObsTypeId() != obsTypeId) {
                obsSet.remove(obs);
            }
        }
    }

    public void filterObsSetByObsLink(Set<IObs> obsSet, IObs obs) {
        if (obsSet == null)
            throw new NullPointerException("obsSet");

        Roads roads = Roads.getInstance();
        Polyline obsLink = roads.getLink(100, obs.getLongitude(), obs.getLatitude());
        if (obsLink == null) {
            getLogger().debug("No link found for observation: {" + obs + "}");
            obsSet.clear();
        } else {

            for (IObs obsCurrentIter : obsSet) {
                Polyline obsCurrentIterLink = roads.getLink(100, obsCurrentIter.getLongitude(), obs.getLatitude());
                if (obsCurrentIterLink != null) {
                    obsSet.remove(obsCurrentIterLink);
                }
            }
        }
    }

    public void filterObsSetByObsLink(ObsSet obsSet, IObs obs) {
        if (obsSet == null)
            throw new NullPointerException("obsSet");

        Roads roads = Roads.getInstance();
        Polyline obsLink = roads.getLink(100, obs.getLongitude(), obs.getLatitude());
        if (obsLink == null) {
            getLogger().debug("No link found for observation: {" + obs + "}");
            obsSet.clear();
        } else {

            for (IObs obsCurrentIter : obsSet) {
                Polyline obsCurrentIterLink = roads.getLink(100, obsCurrentIter.getLongitude(), obs.getLatitude());
                if (obsCurrentIterLink != null) {
                    obsSet.remove(obsCurrentIterLink);
                }
            }
        }
    }
}
