package wde.data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;


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
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getOrganizationType() {
        return organizationType;
    }

    public void setOrganizationType(String organizationType) {
        this.organizationType = organizationType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
