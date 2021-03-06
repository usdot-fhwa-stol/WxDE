package wde.data;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import wde.data.xml.TimestampAdapter;
import wde.util.Text;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Feedback {

    private Integer feedbackId;
    private FeedbackType feedbackType;
    private String userName;
    private String name;
    private String email;
    private String section;
    private String description;
    private Date dateCreated;
    private String dateCreated2;
    private String timeCreated;
    private String kaptcha;

    @XmlJavaTypeAdapter(value = TimestampAdapter.class)
    private Timestamp tsCreated;

    public void setKaptcha(String kaptcha) { this.kaptcha = kaptcha; }

    public String getKaptcha() { return this.kaptcha; }

    public Integer getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(Integer feedbackId) {
        this.feedbackId = feedbackId;
    }

    public FeedbackType getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(FeedbackType feedbackType) {
        this.feedbackType = feedbackType;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = Text.truncate(userName, 15);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Text.truncate(name, 100);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Text.truncate(email, 100);
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = Text.truncate(section,100);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Text.truncate(description, 500);
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Timestamp getTsCreated() {
        return tsCreated;
    }

    public void setTsCreated(Timestamp tsCreated) {
        this.tsCreated = tsCreated;
        this.setDateCreated2(new SimpleDateFormat("MM/dd/yyyy").format(tsCreated));
        this.setTimeCreated(new SimpleDateFormat("h:mm a z").format(tsCreated));
    }

    public String getDateCreated2() {
        return dateCreated2;
    }

    public void setDateCreated2(String dateCreated2) {
        this.dateCreated2 = dateCreated2;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(String timeCreated) {
        this.timeCreated = timeCreated;
    }

}
