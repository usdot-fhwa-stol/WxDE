package wde.ws;

import wde.dao.orm.OrganizationTypeDao;
import wde.dao.orm.OrganizationTypeDaoImpl;
import wde.data.OrganizationType;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("organizationTypes")
public class OrganizationTypeResource {

    private OrganizationTypeDao organizaitonTypeDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<OrganizationType> getOrganziationTypes() {
        organizaitonTypeDao = new OrganizationTypeDaoImpl();
        return organizaitonTypeDao.getOrganizationTypes();
    }
}
