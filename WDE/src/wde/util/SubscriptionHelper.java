package wde.util;

import wde.dao.orm.SubscriptionDao;
import wde.dao.orm.SubscriptionDaoImpl;
import wde.data.Subscription;

import java.util.List;

public class SubscriptionHelper {

    public static boolean isAuthorized(String user, String subId) {

        String tempSubId;
        SubscriptionDao subscriptionDao = new SubscriptionDaoImpl();
        List<Subscription> subs = subscriptionDao.getPublicSubscriptions(user);
        for (Subscription sub : subs) {
            tempSubId = sub.getSubscriptionId().toString();
            if (subId.equals(tempSubId)) {
                return true;
            }
        }

        subs = subscriptionDao.getSubscriptionsByOwner(user);
        for (Subscription sub : subs) {
            tempSubId = sub.getSubscriptionId().toString();
            if (subId.equals(tempSubId)) {
                return true;
            }
        }

        subs = subscriptionDao.getSubscriptionsByMember(user);
        for (Subscription sub : subs) {
            tempSubId = sub.getSubscriptionId().toString();
            if (subId.equals(tempSubId)) {
                return true;
            }
        }

        return false;
    }
}
