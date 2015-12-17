package wde.inference.vdt.rwx;

public class PavementSlickness {

    public static final PavementSlickness PAVEMENT_NOT_SLICK = new PavementSlickness(0, "Pavement Not Slick", "PAVEMENT_NOT_SLICK");
    public static final PavementSlickness PAVEMENT_SLICK = new PavementSlickness(1, "Pavement Slick", "PAVEMENT_SLICK");
    
    private static final PavementSlickness[] conditions = new PavementSlickness[] {
            PAVEMENT_NOT_SLICK, PAVEMENT_SLICK
    };

    private int code;
    private String displayName;
    private String constantName;

    PavementSlickness(int code, String displayName, String constantName) {
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

    public static PavementSlickness fromCode(int code) {

        PavementSlickness result = PAVEMENT_NOT_SLICK;
        if (code == 1)
            result = PAVEMENT_SLICK;

        return result;
    }

    public static PavementSlickness fromFlag(boolean flag) {

        PavementSlickness result = PAVEMENT_NOT_SLICK;
        if (flag)
            result = PAVEMENT_SLICK;

        return result;
    }
}
