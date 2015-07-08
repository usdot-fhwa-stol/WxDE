package wde.dao.orm;

import wde.data.Boundary;

import java.util.List;

public interface BoundaryDao {

    public Boundary getBoundary(String code);

    public List<Boundary> getBoundaries();

    public List<Boundary> getBoundariesAndStatuses();

}
