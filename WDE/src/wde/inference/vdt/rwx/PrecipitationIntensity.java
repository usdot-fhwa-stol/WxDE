package wde.inference.vdt.rwx;

public class PrecipitationIntensity {

    public static final PrecipitationIntensity NO_PRECIP = new PrecipitationIntensity(0, "No Precipitation", "NO_PRECIP");
    public static final PrecipitationIntensity LIGHT_PRECIP = new PrecipitationIntensity(1, "Light Precipitation", "LIGHT_PRECIP");
    public static final PrecipitationIntensity MODERATE_PRECIP = new PrecipitationIntensity(2, "Moderate Precipitation", "MODERATE_PRECIP");
    public static final PrecipitationIntensity HEAVY_PRECIP = new PrecipitationIntensity(3, "Heavy Precipitation", "HEAVY_PRECIP");
    public static final PrecipitationIntensity ROAD_SPLASH = new PrecipitationIntensity(4, "Road Splash", "ROAD_SPLASH");

    private int code;
    private String displayName;
    private String constantName;

    PrecipitationIntensity(int code, String displayName, String constantName) {
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

    public static PrecipitationIntensity fromCode(int code) {

        PrecipitationIntensity result = NO_PRECIP;
        switch (code) {
            case 1:
                result = LIGHT_PRECIP;
            case 2:
                result = MODERATE_PRECIP;
            case 3:
                result = HEAVY_PRECIP;
            case 4:
                result = ROAD_SPLASH;
        }

        return result;
    }
}
