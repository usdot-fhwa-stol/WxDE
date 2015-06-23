package wde.data;

import java.util.List;


public class CoordinatesList {

    private List<Coordinates> coordinates;

    public CoordinatesList() {
    }

    public CoordinatesList(List<Coordinates> _coordinates) {
        this.setCoordinates(_coordinates);
    }

    public List<Coordinates> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Coordinates> coordinates) {
        this.coordinates = coordinates;
    }
}