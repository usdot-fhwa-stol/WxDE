package wde.data;

import javax.xml.bind.annotation.XmlRootElement;
import wde.util.Text;


@XmlRootElement
public class Subscription {

    private Integer subscriptionId;
    private Integer isPublic;
    private Boolean isEditable;
    private String name;
    private String description;
    private String uuid;
    private User user;

    public Integer getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Integer subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Integer getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Integer isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsEditable() {
        return isEditable;
    }

    public void setIsEditable(Boolean isEditable) {
        this.isEditable = isEditable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Text.truncate(name, 5);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
