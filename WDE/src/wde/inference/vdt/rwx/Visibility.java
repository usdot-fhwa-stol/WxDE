package wde.inference.vdt.rwx;

public class Visibility {

    public static final Visibility VIS_NORMAL = new Visibility(0, "Normal", "VIS_NORMAL");
    public static final Visibility VIS_LOW = new Visibility(1, "Low", "VIS_LOW");
    public static final Visibility VIS_HEAVY_RAIN = new Visibility(2, "Heavy Rain", "VIS_HEAVY_RAIN");
    public static final Visibility VIS_HEAVY_SNOW = new Visibility(3, "Heavy Snow", "VIS_HEAVY_SNOW");
    public static final Visibility VIS_BLOWING_SNOW = new Visibility(4, "Blowing Snow", "VIS_BLOWING_SNOW");
    public static final Visibility VIS_FOG = new Visibility(5, "Fog", "VIS_FOG");
    public static final Visibility VIS_HAZE = new Visibility(6, "Haze", "VIS_HAZE");
    public static final Visibility VIS_DUST = new Visibility(7, "Dust", "VIS_DUST");
    public static final Visibility VIS_SMOKE = new Visibility(8, "Smoke", "VIS_SMOKE");

    private static final Visibility[] conditions = new Visibility[] {
            VIS_NORMAL, VIS_LOW, VIS_HEAVY_RAIN, VIS_HEAVY_SNOW, VIS_BLOWING_SNOW, VIS_FOG, VIS_HAZE, VIS_DUST, VIS_SMOKE
    };

    private int code;
    private String displayName;
    private String constantName;

    Visibility(int code, String displayName, String constantName) {
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

    public static Visibility fromCode(int code) {
        Visibility result = VIS_NORMAL;
        for(Visibility condition : conditions) {
            if (condition.getCode() == code) {
                result = condition;
                break;
            }
        }

        return result;
    }
}
