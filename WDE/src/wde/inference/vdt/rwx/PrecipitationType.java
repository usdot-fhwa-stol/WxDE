package wde.inference.vdt.rwx;

public class PrecipitationType {

    public static final PrecipitationType NO_PRECIP = new PrecipitationType(0, "No Precipitation", "NO_PRECIP");
    public static final PrecipitationType RAIN = new PrecipitationType(1, "Rain", "RAIN");
    public static final PrecipitationType MIX = new PrecipitationType(2, "Mix", "MIX");
    public static final PrecipitationType SNOW = new PrecipitationType(3, "Snow", "SNOW");

    private int code;
    private String displayName;
    private String constantName;

    PrecipitationType(int code, String displayName, String constantName) {
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

    public static PrecipitationType fromCode(int code) {

        PrecipitationType precipitationType = NO_PRECIP;
        switch (code) {
            case 1:
                precipitationType = RAIN;
            case 2:
                precipitationType = MIX;
            case 3:
                precipitationType = SNOW;
        }

        return precipitationType;
    }
}