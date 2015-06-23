package wde.data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement()
public class GeoCodeList {

    private List<GeoCode> geoCodeList;

    public GeoCodeList() {
    }

    public GeoCodeList(List<GeoCode> _geoCodeList) {
        this.setGeoCodeList(_geoCodeList);
    }

    @XmlElement(name = "geoCode")
    public List<GeoCode> getGeoCodeList() {
        return geoCodeList;
    }

    public void setGeoCodeList(List<GeoCode> _geoCodeList) {
        this.geoCodeList = _geoCodeList;
    }
}
