package wde.data;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import wde.util.Text;


@XmlRootElement
public class User {

    private String user;
    private String password;
    private String firstName;
    private String lastName;
    private String organization;
    private String organizationType;
    private String country;
    private String email;
    private String guid;
    private Boolean isVerified;
    private String passwordGuid;
    private Date datePassword;
    private Date dateCreated;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = Text.truncate(firstName, 100);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = Text.truncate(lastName, 100);
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = Text.truncate(organization, 100);
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = Text.truncate(organizationType, 32);
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = Text.truncate(country, 2);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Text.truncate(email, 100);
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getPasswordGuid() {
        return passwordGuid;
    }

    public void setPasswordGuid(String passwordGuid) {
        this.passwordGuid = passwordGuid;
    }

    public Date getDatePassword() {
        return datePassword;
    }

    public void setDatePassword(Date datePassword) {
        this.datePassword = datePassword;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

}
