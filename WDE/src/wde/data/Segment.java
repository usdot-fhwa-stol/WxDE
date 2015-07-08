package wde.data;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


@XmlRootElement
@XmlType(name = "Segment",
        propOrder = {
                "id",
                "name",
                "geoCodes"
        })
public class Segment {

    private Integer id;
    private String name;
    private GeoCodeList geoCodes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoCodeList getGeoCodes() {
        return geoCodes;
    }

    public void setGeoCodes(GeoCodeList geoCodes) {
        this.geoCodes = geoCodes;
    }
}
