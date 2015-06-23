package wde.ws;

import wde.dao.orm.CountriesDao;
import wde.dao.orm.CountriesDaoImpl;
import wde.data.Country;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;


@Path("countries")
public class CountriesResource {

    private CountriesDao countriesDao;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Country> getCountries() {
        countriesDao = new CountriesDaoImpl();
        return countriesDao.getCountries();
    }
}
