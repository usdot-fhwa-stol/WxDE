package wde.ws;

import wde.dao.orm.ContributorDaoImpl;
import wde.data.Contributor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("auth/contributors")
public class ContributorResource {

    private ContributorDaoImpl contributorDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Contributor> getContributors() {
        contributorDao = new ContributorDaoImpl();
        return contributorDao.getContributors();
    }

    @Path("subset")
    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Contributor> getContributorsSubset() {
        contributorDao = new ContributorDaoImpl();
        return contributorDao.getContributorsSubset();
    }
}
