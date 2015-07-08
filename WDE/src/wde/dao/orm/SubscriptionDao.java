package wde.dao.orm;

import wde.data.Subscription;

import java.util.List;

public interface SubscriptionDao {

    List<Subscription> getSubscriptionsByOwner(String user);

    List<Subscription> getSubscriptionsByMember(String user);

    List<Subscription> getPublicSubscriptions(String user);

    List<Subscription> getSubscriptions(String user);

    Subscription getSubscription(Integer subscriptionId);

}
