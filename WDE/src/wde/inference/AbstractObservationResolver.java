package wde.inference;

import wde.dao.ObsTypeDao;
import wde.obs.IObs;

public abstract class AbstractObservationResolver implements ObservationResolver {

    private ObsTypeDao obsTypeDao = null;

    public AbstractObservationResolver() {
        init();
    }

    protected void init() {
        if (obsTypeDao == null) {
            try {
                this.obsTypeDao = ObsTypeDao.getInstance();
                throw new Exception("An instance of ObsTypeDao could not be acquired.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public IObs resolve(String obsTypeName) {
        return this.doResolve(obsTypeName);
    }

    public Float resolveFloatValue(String obsTypeName) {

        IObs obs = doResolve(obsTypeName);

        return (obs == null || obs.getValue() == -9999.0f) ? Float.NaN : (float) obs.getValue();
    }

    public Double resolveDoubleValue(String obsTypeName) {

        IObs obs = doResolve(obsTypeName);

        return (obs == null || obs.getValue() == -9999.0) ? Double.NaN : obs.getValue();
    }

    public Integer resolveIntegerValue(String obsTypeName) {
        IObs obs = doResolve(obsTypeName);

        return (obs == null || obs.getValue() == -9999) ? -9999 : ((Double) obs.getValue()).intValue();
    }

    public abstract IObs doResolve(String obsTypeName);
}
