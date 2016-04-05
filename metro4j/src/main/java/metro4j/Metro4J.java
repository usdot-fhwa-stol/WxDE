package metro4j;

public class Metro4J {
    
    public static double dDT = 30.0;
    public static int nNRECMAX = 73;
    public static int nNCOLMAX = 73;
    public static int nNGRILLEMAX = 200;
    public static double dPI = 3.141592653590e0;
    public static double dOMEGA = 0.7292e-4;
    
    static {
        System.loadLibrary("metro");
    }
    
    public static native MetroResult Do_Metro(
            boolean bFlat,
            double dMLat,
            double dMLon,
            double dpZones[],
            long nNbrOfZone,
            long npMateriau[],
            double dpTA[],
            double dpQP[],
            double dpFF[],
            double dpPS[],
            double dpFsPy[],
            double dpFI[],
            double dpFT[],
            double dpTYP[],
            double dpRc[],
            double dpTAO[],
            double dpRTO[],
            double dpDTO[],
            double dpAH[],
            double dpTimeO[],
            long npSWO[],
            boolean bpNoObs[],
            double dDeltaT,
            long nLenObservation,
            long nNbrTimeSteps,
            boolean bSilent,
            long tmtTyp);

//    public native void init_structure(
//            long nTimeStepMax,
//            long nGrilleLevelMax,
//            long[] stRC,
//            double[] stRA,
//            double[] stRT,
//            double[] stIR,
//            double[] stSF,
//            double[] stSN,
//            double[] stFV,
//            double[] stFC,
//            double[] stFA,
//            double[] stG,
//            double[] stBB,
//            double[] stFP,
//            long[] stEc,
//            double[] stSST,
//            double[] stDepth,
//            double[] stLT);

//    public native void free_structure();

    //void print_input(BOOL bFlat, double dMLat, double dMLon, double* dpZones, long nNbrOfZone,  long* npMateriau, double* dpTA, double* dpQP, double* dpFF,  double* dpPS, double* dpFS, double* dpFI, double* dpFT, double* dpTYP, double* dpRC, double* dpTAO,  double* dpRTO, double* dpDTO, double* dpAH, double* dpTimeO, long* npSwo, BOOL* bpNoObs, double dDeltaT, long nLenObservation, long nNbrTimeSteps);
    //public static native void print_input(boolean bFlat, double dMLat, double dMLon, double dpZones[], long nNbrOfZone, long npMateriau[], double dpTA[], double dpQP[], double dpFF[], double dpPS[], double dpFS[], double dpFI[], double dpFT[], double dpTYP[], double dpRC[], double dpTAO[], double dpRTO[], double dpDTO[], double dpAH[], double dpTimeO[], long npSwo[], boolean bpNoObs[], double dDeltaT, long nLenObservation, long nNbrTimeSteps);
 
//    public static native double[] get_ra();
//    public static native double[] get_sn();
//    public static native long[] get_rc();
//    public static native double[] get_rt();
//    public static native double[] get_ir();
//    public static native double[] get_sf();
//    public static native double[] get_fv();
//    public static native double[] get_fc();
//    public static native double[] get_fa();
//    public static native double[] get_g();
//    public static native double[] get_bb();
//    public static native double[] get_fp();
//    public static native long[] get_echec();
//    public static native double[] get_sst();
//    public static native double[] get_depth();
//    public static native long get_nbr_levels();
//    public static native double[] get_lt();
}
