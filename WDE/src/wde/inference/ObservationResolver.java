package wde.inference;

import wde.obs.IObs;

public interface ObservationResolver {

    IObs resolve(String obsTypeName);

}
