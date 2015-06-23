package wde.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SubscriptionUser {

    private Subscription subscription;
    private User user;

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
