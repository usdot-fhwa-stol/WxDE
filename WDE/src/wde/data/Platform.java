package wde.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(name = "Platform", propOrder = {
        "id",
        "staticId",
        "updateTime",
        "toTime",
        "stationCode",
        "category",
        "description",
        "type",
        "contributorId",
        "siteId",
        "locBaseLat",
        "locBaseLong",
        "locBaseElev",
        "locBaseDatum",
        "powerType",
        "doorOpen",
        "batteryStatus",
        "lineVolts",
        "maintContactId"
})

public class Platform {

    private Integer id;
    private Integer staticId;
    private String updateTime;
    private String toTime;
    private String stationCode;
    private String category;
    private String description;
    private Integer type;
    private Integer contributorId;
    private Integer siteId;
    private Double locBaseLat;
    private Double locBaseLong;
    private Double locBaseElev;
    private String locBaseDatum;
    private Character powerType;
    private Boolean doorOpen;
    private Integer batteryStatus;
    private Integer lineVolts;
    private Integer maintContactId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @XmlElement(nillable = true)
    public Integer getContributorId() {
        return contributorId;
    }

    public void setContributorId(Integer contributorId) {
        this.contributorId = contributorId;
    }

    @XmlElement(nillable = true)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    @XmlElement(nillable = true)
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @XmlElement(nillable = true)
    public Double getLocBaseLat() {
        return locBaseLat;
    }

    public void setLocBaseLat(Double locBaseLat) {
        this.locBaseLat = locBaseLat;
    }

    @XmlElement(nillable = true)
    public Double getLocBaseLong() {
        return locBaseLong;
    }

    public void setLocBaseLong(Double locBaseLong) {
        this.locBaseLong = locBaseLong;
    }

    @XmlElement(nillable = true)
    public Double getLocBaseElev() {
        return locBaseElev;
    }

    public void setLocBaseElev(Double locBaseElev) {
        this.locBaseElev = locBaseElev;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @XmlElement(nillable = true)
    public String getToTime() {
        return toTime;
    }

    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public Integer getStaticId() {
        return staticId;
    }

    public void setStaticId(Integer staticId) {
        this.staticId = staticId;
    }

    @XmlElement(nillable = true)
    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @XmlElement(nillable = true)
    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    @XmlElement(nillable = true)
    public String getLocBaseDatum() {
        return locBaseDatum;
    }

    public void setLocBaseDatum(String locBaseDatum) {
        this.locBaseDatum = locBaseDatum;
    }

    @XmlElement(nillable = true)
    public Character getPowerType() {
        return powerType;
    }

    public void setPowerType(Character powerType) {
        this.powerType = powerType;
    }

    @XmlElement(nillable = true)
    public Boolean getDoorOpen() {
        return doorOpen;
    }

    public void setDoorOpen(Boolean doorOpen) {
        this.doorOpen = doorOpen;
    }

    @XmlElement(nillable = true)
    public Integer getBatteryStatus() {
        return batteryStatus;
    }

    public void setBatteryStatus(Integer batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    @XmlElement(nillable = true)
    public Integer getLineVolts() {
        return lineVolts;
    }

    public void setLineVolts(Integer lineVolts) {
        this.lineVolts = lineVolts;
    }

    @XmlElement(nillable = true)
    public Integer getMaintContactId() {
        return maintContactId;
    }

    public void setMaintContactId(Integer maintContactId) {
        this.maintContactId = maintContactId;
    }
}
