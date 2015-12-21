package wde.dao;

import wde.metadata.ObsType;
import wde.metadata.TimeInvariantMetadata;

import java.util.ArrayList;
import java.util.HashMap;

public interface IObsTypeDao {
    HashMap<String, TimeInvariantMetadata> getObsTypeMap();

    ArrayList<ObsType> getObsTypeList();

    ObsType getObsType(int id);

    int getObsTypeId(String obsTypeName);

    ObsType getObsType(String obsTypeName);

    void updateObsTypeMap();

    boolean insertObsType(ObsType obsType, boolean atomic);

    ObsType getObsTypeFromDB(String id);
}
