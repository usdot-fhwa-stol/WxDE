package wde.ws;

import wde.dao.orm.PlatformDao;
import wde.dao.orm.PlatformDaoImpl;
import wde.data.Platform;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("auth/platforms")
public class PlatformResource {

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest req;

    private PlatformDao platformDao;

    @GET
    @Path("contributor/{contributorId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Platform> getPlatforms(@PathParam("contributorId") Integer contributorId) {
        platformDao = new PlatformDaoImpl();
        return platformDao.getPlatforms(contributorId);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Platform> getPlatforms() {
        platformDao = new PlatformDaoImpl();
        return platformDao.getPlatforms();
    }
}