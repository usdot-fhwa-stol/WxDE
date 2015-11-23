/************************************************************************
 * Source filename: QualityCheckFlags.java
 * <p/>
 * Creation date: Apr 30, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

public class QualityCheckFlagUtil {

    private static final int[] qcLength = {15, 16};
    private static String[] padding = {"000000000000000", "0000000000000000"};
    private static char[] qcChars = {'/', '-', 'N', 'P'};

    public static int getQcLength(int sourceId) {
        int len = 0;
        if (sourceId == 1 || sourceId == 2)
            len = qcLength[sourceId - 1];

        return len;
    }

    public static char[] getQcCharFlags(int sourceId, int runFlags, int passFlags) {
        char[] qcCharFlags = null;

        if (sourceId != 1 && sourceId != 2)
            return qcCharFlags;

        qcCharFlags = new char[qcLength[sourceId - 1]];

        String runFlagStr = Integer.toBinaryString(runFlags);
        runFlagStr = padding[sourceId - 1].substring(runFlagStr.length()) + runFlagStr;

        String passFlagStr = Integer.toBinaryString(passFlags);
        passFlagStr = padding[sourceId - 1].substring(passFlagStr.length()) + passFlagStr;

        for (int i = 0; i < qcLength[sourceId - 1]; i++) {
            String key = "" + runFlagStr.charAt(i) + passFlagStr.charAt(i);
            qcCharFlags[qcLength[sourceId - 1] - 1 - i] = qcChars[Integer.parseInt(key, 2)];
        }

        return qcCharFlags;
    }

    public static QualityCheckFlags getFlags(int sourceId, char[] flagStr) {
        QualityCheckFlags qcf = null;

        if (sourceId != 1 && sourceId != 2)
            return qcf;

        int rf = 0;
        int pf = 0;
        for (int i = qcLength[sourceId - 1] - 1; i >= 0; i--) {
            switch (flagStr[i]) {
                case '/':
                    break;
                case '-':
                    pf += 1;
                    break;
                case 'N':
                    rf += 1;
                    break;
                case 'P':
                    rf += 1;
                    pf += 1;
                    break;
            }
            rf = rf << 1;
            pf = pf << 1;
        }
        rf = rf >> 1;
        pf = pf >> 1;

        qcf = new QualityCheckFlags(rf, pf);

        return qcf;
    }

    public static void main(String[] args) {
        char[] qcCharFlags = getQcCharFlags(1, 5, 7);
        for (int i = 0; i < 12; i++)
            System.out.println(qcCharFlags[i]);

        System.out.println(String.valueOf(qcCharFlags));

        QualityCheckFlags qcf = getFlags(1, qcCharFlags);
        System.out.println(qcf.getRunFlags());
        System.out.println(qcf.getPassFlags());
    }
}
