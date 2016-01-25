package wde.inference.vdt.rwx;

public class PavementCondition {

    public static final PavementCondition DRY_PAVEMENT = new PavementCondition(0, "Dry Pavement", "DRY_PAVEMEMT");
    public static final PavementCondition WET_PAVEMENT = new PavementCondition(1, "Wet Pavement", "WET_PAVEMEMT");
    public static final PavementCondition SNOW_COVERED = new PavementCondition(2, "Snow Covered", "SNOW_COVERED");
    public static final PavementCondition ICE_COVERED = new PavementCondition(3, "Ice Covered", "ICE_COVERED");
    public static final PavementCondition HYDROPLANE = new PavementCondition(4, "Hydroplane", "HYDROPLANE");
    public static final PavementCondition BLACK_ICE = new PavementCondition(5, "Black Ice", "BLACK_ICE");
    public static final PavementCondition DRY_WET_PAVEMENT = new PavementCondition(6, "Dry Wet Pavement", "DRY_WET_PAVEMENT");
    public static final PavementCondition DRY_FROZEN_PAVEMENT = new PavementCondition(7, "Dry Frozen Pavement", "DRY_FROZEN_PAVEMEMT");

    private static final PavementCondition[] conditions = new PavementCondition[] {
        DRY_PAVEMENT, WET_PAVEMENT, SNOW_COVERED, ICE_COVERED, HYDROPLANE, BLACK_ICE, DRY_WET_PAVEMENT, DRY_FROZEN_PAVEMENT
    };

    private int code;
    private String displayName;
    private String constantName;

    PavementCondition(int code, String displayName, String constantName) {
        this.code = code;
        this.displayName = displayName;
        this.constantName = constantName;
    }

    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getConstantName() {
        return constantName;
    }

    public static PavementCondition fromCode(int code) {

        PavementCondition result = DRY_PAVEMENT;
        for(PavementCondition condition : conditions) {
            if (condition.getCode() == code) {
                result = condition;
                break;
            }
        }

        return result;
    }
}
