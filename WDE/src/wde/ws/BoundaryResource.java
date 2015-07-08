package wde.ws;

import wde.dao.orm.BoundaryDao;
import wde.dao.orm.BoundaryDaoImpl;
import wde.dao.orm.DataMapDao;
import wde.dao.orm.DataMapDaoImpl;
import wde.data.Boundary;
import wde.data.DataMapState;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;


@Path("boundary")
public class BoundaryResource {

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest req;
    @Context
    protected HttpServletResponse resp;

    private BoundaryDao boundaryDao;
    private DataMapDao dataMapDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Boundary> getBoundaries() {
        boundaryDao = new BoundaryDaoImpl();
        List<Boundary> boundaries = boundaryDao.getBoundaries();

        return boundaries;
    }

    @GET
    @Path("statuses")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Boundary> getBoundariesAndStatuses() {
        boundaryDao = new BoundaryDaoImpl();
        List<Boundary> boundaries = boundaryDao.getBoundariesAndStatuses();

        return boundaries;
    }

    @GET
    @Path("states")
    @Produces({MediaType.APPLICATION_JSON})
    public List<DataMapState> getDataMapStates() {
        dataMapDao = new DataMapDaoImpl();

        List<DataMapState> statesData = dataMapDao.getStates();

        return statesData;
    }

}
