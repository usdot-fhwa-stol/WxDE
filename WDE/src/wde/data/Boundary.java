package wde.data;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

@XmlRootElement
@XmlType(name = "Boundary",
        propOrder = {
                "postalCode",
                "name",
                "dataType",
                "link",
                "geometry",
                "centerLat",
                "centerLon",
                "centerZoom",
                "contributor",
                "contributors"
        })
public class Boundary {

    private String name;
    private String postalCode;
    private Integer dataType;
    private String link;
    private String geometry;
    private Double centerLat;
    private Double centerLon;
    private Integer centerZoom;
    private Contributor contributor;
    private List<Contributor> contributors;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public Integer getDataType() {
        return dataType;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }

    public Double getCenterLat() {
        return centerLat;
    }

    public void setCenterLat(Double centerLat) {
        this.centerLat = centerLat;
    }

    public Double getCenterLon() {
        return centerLon;
    }

    public void setCenterLon(Double centerLon) {
        this.centerLon = centerLon;
    }

    public Integer getCenterZoom() {
        return centerZoom;
    }

    public void setCenterZoom(Integer centerZoom) {
        this.centerZoom = centerZoom;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

}