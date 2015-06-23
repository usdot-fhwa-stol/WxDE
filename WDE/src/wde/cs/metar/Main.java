package wde.cs.metar;

import wde.util.io.CharTokenizer;

import java.io.FileInputStream;
import java.io.FileWriter;

public class Main {
    public static void main(String[] sArgs)
            throws Exception {
        StringBuilder oBuffer = new StringBuilder();
        AWOS m_oAWOS = new AWOS();

        FileInputStream oInputFile = new FileInputStream(sArgs[0]);
        FileWriter oOutputFile = new FileWriter(sArgs[1]);

        m_oAWOS.printCSVHeader(oOutputFile, true);
        oOutputFile.write("\n");

        // open the input file and move the information to the csv output
        CharTokenizer oCharTokenizer = new CharTokenizer("\n", "\n");
        oCharTokenizer.setInput(oInputFile);
        int nCurrentLine = 2;
        // always attempt to read the timestamp information
        while (oCharTokenizer.nextSet()) {
            if (!oCharTokenizer.hasTokens())
                continue;
            oCharTokenizer.nextToken(oBuffer);
            m_oAWOS.clearValues();

            m_oAWOS.setBuffer(oBuffer);
            // save the report timestamp
            m_oAWOS.m_oReports[0].parse(0, 0);

            // read the METAR report information
            --nCurrentLine;
            if (oCharTokenizer.nextSet() && oCharTokenizer.hasTokens())
                oCharTokenizer.nextToken(oBuffer);
            else
                break;

            m_oAWOS.setBuffer(oBuffer);
            m_oAWOS.parse(0, nCurrentLine);
            // skip the blank line
            --nCurrentLine;
            if (!(oCharTokenizer.nextSet() && oCharTokenizer.hasTokens()))
                oCharTokenizer.nextToken(oBuffer);
            nCurrentLine = 2;
            m_oAWOS.toCSV(oOutputFile, true);
            oOutputFile.write("\n");
        }

        oInputFile.close();
        oOutputFile.close();
    }
}
