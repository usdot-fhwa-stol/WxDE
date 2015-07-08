package wde.cs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * @author bryan.krueger
 */
public class MergeKY implements Runnable {
    private String m_sDirectory;
    private String m_sOutputFilename;


    /**
     * Creates a new instance of MergeKY
     */
    private MergeKY() {
    }


    private MergeKY(String sDirectory, String sOutputFilename) {
        m_sDirectory = sDirectory;
        m_sOutputFilename = sOutputFilename;
    }

    /**
     * @param sArgs the command line arguments
     */
    public static void main(String[] sArgs) {
        new MergeKY(sArgs[0], sArgs[1]).run();
    }

    @Override
    public void run() {
        // save all the file references from the directory
        File[] oFiles = new File(m_sDirectory).listFiles();
        if (oFiles.length == 0)
            return;

        ArrayList<File> oAllFiles = new ArrayList<>();
        int nFileIndex = oFiles.length;
        while (nFileIndex-- > 0) // narrow the list to only .dat files
        {
            if (oFiles[nFileIndex].getName().endsWith(".dat"))
                oAllFiles.add(oFiles[nFileIndex]);
        }

        try {
            // take the last line from each .dat file, remove all double
            // quotes, and insert filename as platform id
            FileWriter oWriter = new FileWriter(m_sOutputFilename);

            nFileIndex = oAllFiles.size();
            while (nFileIndex-- > 0) // parse each filename in the list
            {
                // save filename and exclude extension
                File oFile = oAllFiles.get(nFileIndex);
                String sPrepend = oFile.getName();
                sPrepend = sPrepend.substring(0, sPrepend.lastIndexOf("."));

                String sLine = "";
                String sSaveLine = "";
                BufferedReader oReader = new BufferedReader(new FileReader(oFile));
                while ((sLine = oReader.readLine()) != null)
                    sSaveLine = sLine;

                oReader.close();

                oWriter.write(sPrepend);
                oWriter.write(',');
                for (int nIndex = 0; nIndex < sSaveLine.length(); nIndex++) {
                    char cValue = sSaveLine.charAt(nIndex);
                    if (cValue != '"')
                        oWriter.write(cValue);
                }
                oWriter.write('\n');
            }

            oWriter.close();
        } catch (Exception oException) {
            oException.printStackTrace(System.out);
        }
    }
}
