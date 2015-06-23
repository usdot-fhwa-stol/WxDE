package wde.data;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Rde {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}