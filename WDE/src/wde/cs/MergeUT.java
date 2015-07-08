package wde.cs;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * @author bryan.krueger
 */
public class MergeUT implements Runnable {
    private int m_nTzOffset;
    private int m_nTsColumn;
    private String m_sDirectory;
    private String m_sFileExtension;
    private String m_sOutputFilename;
    private ArrayList<StringBuilder> m_oColumns = new ArrayList<StringBuilder>();


    /**
     * Creates a new instance of MergeUT
     */
    private MergeUT() {
    }


    private MergeUT(String sDirectory, String sFileExtension,
                    String sOutputFilename, String sTsColumn, String sTzOffset) {
        try {
            m_sDirectory = sDirectory;
            m_sFileExtension = sFileExtension;
            m_sOutputFilename = sOutputFilename;
            m_nTsColumn = Integer.parseInt(sTsColumn);
            m_nTzOffset = Integer.parseInt(sTzOffset);
        } catch (Exception oException) {
            oException.printStackTrace(System.out);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] sArgs) throws Exception {
        // command line arguments:
        // source directory
        // source file extension filter
        // full destination filename including path
        // column where timestamp appears
        // UTC absolute timezone offset in milliseconds
        new MergeUT(sArgs[0], sArgs[1], sArgs[2], sArgs[3], sArgs[4]).run();
    }

    private StringBuilder getColumn(int nIndex) {
        StringBuilder oColumn = null;

        if (nIndex >= m_oColumns.size()) {
            oColumn = new StringBuilder();
            m_oColumns.add(oColumn);
        } else
            oColumn = m_oColumns.get(nIndex);

        return oColumn;
    }

    private void clearColumns() {
        int nIndex = m_oColumns.size();
        while (nIndex-- > 0) {
            StringBuilder oColumn = m_oColumns.get(nIndex);
            if (oColumn != null)
                oColumn.setLength(0);
        }
    }

    public void run() {
        // set up the timestamp calculator and formatting
        SimpleTimeZone oTzUT = new SimpleTimeZone(m_nTzOffset, "USA/UT");
        SimpleTimeZone oTzUTC = new SimpleTimeZone(0, "UTC");
        SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // save all the file references from the directory
        File[] oFiles = new File(m_sDirectory).listFiles();
        if (oFiles.length == 0)
            return;

        // narrow the list to only .dat files
        ArrayList<File> oAllFiles = new ArrayList<File>();
        int nFileIndex = oFiles.length;
        while (nFileIndex-- > 0) {
            if (oFiles[nFileIndex].getName().endsWith(m_sFileExtension))
                oAllFiles.add(oFiles[nFileIndex]);
        }

        try {
            // take the last line from each .dat file, replace the constant
            // station identifier 102 with the filename, reformat the
            // timestamp, and save the column values to the output file

            FileWriter oFileWriter = new FileWriter(m_sOutputFilename);

            // parse each filename in the list
            int nChar = 0;
            boolean bNewLine = false;
            String sPrepend = null;
            StringBuilder oColumn = null;
            nFileIndex = oAllFiles.size();
            while (nFileIndex-- > 0) {
                File oSrcFile = oAllFiles.get(nFileIndex);
                // prepend with the filename excluding the filename extension
                sPrepend = oSrcFile.getName();
                sPrepend = sPrepend.substring(0, sPrepend.lastIndexOf(m_sFileExtension));

                try {
                    int nColumnIndex = 0;
                    clearColumns();
                    FileReader oFileReader = new FileReader(oSrcFile);
                    oColumn = null;

                    while ((nChar = oFileReader.read()) >= 0) {
                        // ignore carriage returns and double quotes
                        if (nChar != '\"' && nChar != '\r') {
                            // clear the columns for each line except the last
                            if (nChar == '\n')
                                bNewLine = true;
                            else {
                                // this indicates a new line was started
                                if (bNewLine) {
                                    nColumnIndex = 0;
                                    bNewLine = false;
                                    clearColumns();
                                }

                                if (nChar == ',')
                                    ++nColumnIndex;
                                else {
                                    // save all other characters to the buffer
                                    oColumn = getColumn(nColumnIndex);
                                    oColumn.append((char) nChar);
                                }
                            }
                        }
                    }

                    oFileReader.close();

                    // modify the timestamp
                    oColumn = getColumn(m_nTsColumn);
                    oDateFormat.setTimeZone(oTzUT);
                    Date oDate = oDateFormat.parse(oColumn.toString());
                    oDateFormat.setTimeZone(oTzUTC);
                    oColumn.setLength(0);
                    oColumn.append(oDateFormat.format(oDate));

                    // first output the station name
                    oFileWriter.write(sPrepend);

                    // write all column contents to the output file
                    for (int nCharIndex = 0; nCharIndex < m_oColumns.size(); nCharIndex++) {
                        oFileWriter.write(',');

                        oColumn = getColumn(nCharIndex);
                        if (oColumn.length() > 0) {
                            for (nColumnIndex = 0; nColumnIndex < oColumn.length(); nColumnIndex++)
                                oFileWriter.write(oColumn.charAt(nColumnIndex));
                        }
                    }

                    oFileWriter.write('\n');
                } catch (Exception oException) {
                    // trap when a file has a problem
                    oException.printStackTrace(System.out);
                    System.out.println(oSrcFile.getAbsoluteFile());
                }
            }

            // finish up the output file
            oFileWriter.flush();
            oFileWriter.close();
        } catch (Exception oException) {
            oException.printStackTrace(System.out);
        }
    }
}
