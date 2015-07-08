package wde.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(name = "Contributor", propOrder = {
        "contributorId",
        "staticId",
        "updateTime",
        "toTime",
        "orgId",
        "name",
        "agency",
        "monitorHours",
        "contactId",
        "altContactId",
        "metatdataContactId",
        "display",
        "disclaimer",
        "postalCode",
        "collectionStatus"
})
public class Contributor {

    private Integer contributorId;
    private Integer staticId;
    private String updateTime;
    private String toTime;
    private Integer orgId;
    private String name;
    private String agency;
    private Integer monitorHours;
    private Integer contactId;
    private Integer altContactId;
    private Integer metatdataContactId;
    private Boolean display;
    private String disclaimer;
    private String postalCode;
    private CollectionStatus collectionStatus;

    //Works so far but only displays the ff:
    //contributorId, staticId, updateTime, orgId,
    //name, agency, monitorHours, display, postalCode
    //for the datasource.csv
    //further discussion should be done.

    public Integer getContactId() {
        return contactId;
    }

    public void setContactId(Integer contactId) {
        this.contactId = contactId;
    }

    public Integer getAltContactId() {
        return altContactId;
    }

    public void setAltContactId(Integer altContactId) {
        this.altContactId = altContactId;
    }

    public Integer getMetatdataContactId() {
        return metatdataContactId;
    }

    public void setMetatdataContactId(Integer metatdataContactId) {
        this.metatdataContactId = metatdataContactId;
    }

    @XmlElement(nillable = true)
    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public Integer getContributorId() {
        return contributorId;
    }

    public void setContributorId(Integer contributorId) {
        this.contributorId = contributorId;
    }

    public Integer getStaticId() {
        return staticId;
    }

    public void setStaticId(Integer staticId) {
        this.staticId = staticId;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public Integer getMonitorHours() {
        return monitorHours;
    }

    public void setMonitorHours(Integer monitorHours) {
        this.monitorHours = monitorHours;
    }

    public Boolean getDisplay() {
        return display;
    }

    public void setDisplay(Boolean display) {
        this.display = display;
    }

    public Integer getOrgId() {
        return orgId;
    }

    public void setOrgId(Integer orgId) {
        this.orgId = orgId;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public CollectionStatus getCollectionStatus() {
        return collectionStatus;
    }

    public void setCollectionStatus(CollectionStatus collectionStatus) {
        this.collectionStatus = collectionStatus;
    }

}
