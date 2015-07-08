package wde.ws;

import wde.dao.orm.SubscriptionDao;
import wde.dao.orm.SubscriptionDaoImpl;
import wde.data.Subscription;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("auth/subscriptions")
public class SubscriptionResource {

    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpServletRequest req;

    private SubscriptionDao subscriptionDao;

    @GET
    @Path("owner")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Subscription> getSubscriptionsByOwner() {
        subscriptionDao = new SubscriptionDaoImpl();
        List<Subscription> subs = subscriptionDao.getSubscriptionsByOwner(req.getRemoteUser());

        return subs;
    }

    @GET
    @Path("member")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Subscription> getSubscriptionsByMember() {
        subscriptionDao = new SubscriptionDaoImpl();
        List<Subscription> subs = subscriptionDao.getSubscriptionsByMember(req.getRemoteUser());

        return subs;
    }

    @GET
    @Path("public")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Subscription> getPublicSubscriptions() {
        subscriptionDao = new SubscriptionDaoImpl();
        List<Subscription> subs = subscriptionDao.getPublicSubscriptions(req.getRemoteUser());

        return subs;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<Subscription> getSubscriptions() {
        subscriptionDao = new SubscriptionDaoImpl();
        List<Subscription> subs = subscriptionDao.getSubscriptions(req.getRemoteUser());

        return subs;
    }

    @GET
    @Path("{subscriptionId}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Subscription getSubscription(@PathParam("subscriptionId") Integer subscriptionId) {
        subscriptionDao = new SubscriptionDaoImpl();
        return subscriptionDao.getSubscription(subscriptionId);
    }
}
