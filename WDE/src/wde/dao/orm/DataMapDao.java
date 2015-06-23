package wde.dao.orm;

import wde.data.DataMapState;

import java.util.List;

public interface DataMapDao {

    public DataMapState getDataMapState(String code);

    public List<DataMapState> getStates();

}
