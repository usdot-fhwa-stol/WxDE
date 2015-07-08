package wde.ws;

import wde.security.Encryption;
import wde.util.Config;
import wde.util.ConfigSvc;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;


@Path("rde")
public class RdeResource {

    @Context
    HttpServletRequest req;
    @Context
    HttpServletResponse res;
    @Context
    ServletContext context;

    private String[] hosts;

    public RdeResource() {
        loadProperties();
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public void getAuth() {
        req.getSession();

        String header = req.getHeader("referer");
        String referringHost = null;

        String key = req.getParameter("key");
        String lat = req.getParameter("lat");
        String lng = req.getParameter("long");
        String radius = req.getParameter("radius");

        if (header != null) {
            try {
                referringHost = new URI(req.getHeader("referer")).getHost();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            boolean invalidUser = true;

            if (Arrays.asList(hosts).contains(referringHost)) {
                String reqUser = req.getRemoteUser();
                String userId = "RDEGuest";

                if (reqUser == null) {
                    String password = null;

                    try {
                        password = Encryption.getOriginalMessage(key);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (password != null) {
                        req.login(userId, password);
                        invalidUser = false;
                    }
                } else if (reqUser.equals(userId))
                    invalidUser = false;
            }

            if (invalidUser)
                res.sendRedirect("http://" + req.getServerName() + ":" + req.getServerPort());
            else
                res.sendRedirect(
                        "http://" + req.getServerName() + ":" + req.getServerPort() +
                                "/auth/wizardGeospatial.jsp?lat=" + lat + "&long=" + lng + "&radius=" + radius);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @OPTIONS
    public Response myResource() {
        return Response.ok().build();
    }

    private void loadProperties() {
        ConfigSvc configSvc = ConfigSvc.getInstance();
        Config config = configSvc.getConfig(this);

        String hostStr = config.getString("hosts", "");
        hosts = hostStr.split(",");
    }
}