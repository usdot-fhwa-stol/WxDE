package wde.inference;

public class ObservationTypeMapping {

    private final String vdtObsTypeName;
    private final String wdeObsTypeName;
    private Integer wdeObsTypeId = null;

    public ObservationTypeMapping(String vdtObsTypeName, String wdeObsTypeName) {
        this.vdtObsTypeName = vdtObsTypeName;
        this.wdeObsTypeName = wdeObsTypeName;
    }

    public ObservationTypeMapping(String vdtObsTypeName, String wdeObsTypeName, int wdeObsTypeId) {
        this.vdtObsTypeName = vdtObsTypeName;
        this.wdeObsTypeName = wdeObsTypeName;
        this.wdeObsTypeId = wdeObsTypeId;
    }

    public String getVdtObsTypeName() {
        return vdtObsTypeName;
    }

    public String getWdeObsTypeName() {
        return wdeObsTypeName;
    }

    public Integer getWdeObsTypeId() {
        return wdeObsTypeId;
    }

    public boolean hasWdeObsTypeId() {
        return wdeObsTypeId != null ? true : false;
    }
}
