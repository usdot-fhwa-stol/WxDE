package wde.cs.imo;


import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.HashMap;


public class MiDot {
    public static String[][] LOOKUP =
            {
                    {"0495", "7346450495"},
                    {"0908", "7346450908"},
                    {"0949", "7346450949"},
                    {"1579", "7346451579"},
                    {"2768", "7346452768"},
                    {"3548", "7346453548"},
                    {"3616", "7346453616"},
                    {"3689", "7345483689"},
                    {"3772", "7345483772"},
                    {"3978", "7346453978"},
                    {"3996", "7346453996"},
                    {"4458", "7345484458"},
                    {"4544", "7345484544"},
                    {"4685", "7346454685"},
                    {"4746", "7345484746"},
                    {"4753", "7345484753"},
                    {"4765", "7345484765"},
                    {"4791", "7346454791"},
                    {"5044", "7345485044"},
                    {"5076", "7345485076"},
                    {"5104", "7345485104"},
                    {"5150", "7345485150"},
                    {"5212", "7345485212"},
                    {"5299", "7346455299"},
                    {"5356", "7346455356"},
                    {"5480", "7345485480"},
                    {"5483", "7345485483"},
                    {"5607", "7345485607"},
                    {"5702", "7345485702"},
                    {"5708", "7346455708"},
                    {"5732", "7345485732"},
                    {"5827", "7345485827"},
                    {"5852", "7345485852"},
                    {"5864", "7345485864"},
                    {"5872", "7345485872"},
                    {"5887", "7345485887"},
                    {"5889", "7345485889"},
                    {"5940", "7345485940"},
                    {"5973", "7346455973"},
                    {"5978", "7345485978"},
                    {"5982", "7345485982"},
                    {"5996", "7345485996"},
                    {"6014", "7346456014"},
                    {"6448", "7346456448"},
                    {"6722", "7346456722"},
                    {"7587", "7346457587"},
                    {"8073", "7346458073"},
                    {"8451", "7346458451"},
                    {"8474", "7346458474"},
                    {"9475", "7346459475"},
                    {"9604", "7346459604"},
                    {"9667", "7346459667"},
                    {"9753", "7346459753"},
                    {"9776", "7346459776"}
            };


    public static String[] COLS = {"HEAD_G", "SPD-G", "SPD-C", "BRK", "ABS",
            "ESP", "TCSE", "TCSB", "STMP", "DPNT", "TMP-SP", "HUMD"};


    public static void main(String[] sArgs)
            throws Exception {
        String sPlatformId;
        String sDate = null;
        String sAirTemp = null;
        String sPressure = null;
        SimpleDateFormat oDateFormat = new SimpleDateFormat("ddHHmm'.csv'");
        GregorianCalendar oCalendar = new GregorianCalendar();
        HashMap<String, Integer> oCols = new HashMap();
        HashMap<String, String> oVals = new HashMap();

        HashMap<String, String> oCodes = new HashMap(); // init code lookup
        for (int nIndex = 0; nIndex < LOOKUP.length; nIndex++)
            oCodes.put(LOOKUP[nIndex][0], LOOKUP[nIndex][1]);

        int nChar = sArgs.length; // fix missing path terminators
        while (nChar-- > 0) {
            if (!sArgs[nChar].endsWith("/"))
                sArgs[nChar] += "/";
        }


        URL oUrl = new URL(sArgs[0]); // get list of remote files
        InputStream iInputStream = oUrl.openConnection().getInputStream();

        StringBuilder sBuffer = new StringBuilder();
        while ((nChar = iInputStream.read()) >= 0)
            sBuffer.append((char) nChar);

        iInputStream.close();


        int nStart = 0;
        while ((nStart = sBuffer.indexOf("href=\"", nStart)) >= 0) {
            nStart += 6;
            int nEnd = sBuffer.indexOf("\"", nStart);
            String sFilename = sBuffer.substring(nStart, nEnd);
            nStart = nEnd;

            if (!sFilename.endsWith(".csv"))
                continue; // skip non-csv files, i.e. parent directory/jpeg

            String[] sFileParts = sFilename.split("_");
            sPlatformId = sFileParts[0].trim(); // set default id
            String sTempCode = oCodes.get(sPlatformId);
            if (sTempCode != null)
                sPlatformId = sTempCode;

            String sDirMid = sFileParts[1]; // ddmmyyyy
            if (sFileParts[3].compareTo("v3.7.1") >= 0) // mmddyyyy
                sDirMid = sDirMid.substring(2, 4) + sDirMid.substring(0, 2) + sDirMid.substring(4, 8);
            String sDirTop = sDirMid.substring(4, 8) + sDirMid.substring(2, 4); // year-month directory

            // generate filename from clock so late files are collected
            String sObsFile = sArgs[1] + sDirTop + "/" + sDirTop +
                    oDateFormat.format(oCalendar.getTime());

            try {
                File oDir = new File(sArgs[1] + sDirTop + "/" + sDirMid);
                oDir.mkdirs(); // verify storage location exists

                File oFile = new File(oDir.getPath() + "/" + sFilename);
                if (oFile.exists())
                    continue; // ignore previously downloaded files

                oUrl = new URL(sArgs[0] + sFilename);
                BufferedReader oReader = new BufferedReader(
                        new InputStreamReader(oUrl.openConnection().getInputStream()));

                // collect observations into file copy and daily accumulator
                FileWriter oDstFile = new FileWriter(oFile);
                FileWriter oObsFile = new FileWriter(sObsFile, true);

                for (int nIndex = 0; nIndex < COLS.length; nIndex++)
                    oVals.put(COLS[nIndex], ""); // clear previous values

                boolean bHasObs = false; // default unknown observations
                int nRow = 0; // accumulate the number of observation records
                String[] sValues; // placeholder for columns
                String sLine;

                while ((sLine = oReader.readLine()) != null) {
                    oDstFile.write(sLine); // copy remote file content
                    oDstFile.write("\n"); // replace newline

                    if (!bHasObs) // skip header processing when complete
                    {
                        if (sLine.startsWith("VIN:")) // update platform id
                        {
                            sValues = sLine.split(","); // VIN in last column
                            sTempCode = sValues[sValues.length - 1].trim();
                            if (sTempCode.length() > 0 && !sTempCode.contains("10001"))
                                sPlatformId = sTempCode;
                        }

                        if (sLine.startsWith("Date:")) // save date information
                        {
                            sValues = sLine.split(","); // date in last column
                            sDate = sValues[sValues.length - 1].trim();
                        }

                        if (sLine.startsWith("Air Temp:")) // save air temp
                        {
                            sValues = sLine.split(","); // use last column
                            sAirTemp = sValues[sValues.length - 1].trim();
                        }

                        if (sLine.startsWith("Barometer:")) // save pressure
                        {
                            sValues = sLine.split(","); // use last column
                            sPressure = sValues[sValues.length - 1].trim();
                        }

                        if (sLine.startsWith("TIME")) // init column lookup
                        {
                            oCols.clear(); // remove old col definition
                            String[] sCols = sLine.split(",");
                            for (int nIndex = 0; nIndex < sCols.length; nIndex++)
                                oCols.put(sCols[nIndex], new Integer(nIndex));

                            bHasObs = true;
                        }
                    } else {
                        if (sPlatformId.length() > 10 && sPlatformId.length() < 17) // make cell phone numbers 10 digits
                            sPlatformId = sPlatformId.substring(sPlatformId.length() - 10, sPlatformId.length());

                        if (sPlatformId.length() == 0)
                            sPlatformId = "10001"; // map to anonymous vehicle when unknown

                        sValues = sLine.split(",");
                        int nSat = Integer.parseInt(sValues[oCols.get("SAT").intValue()].trim());
                        if (nSat > 3 && nSat < 10001) {
                            oObsFile.write(sPlatformId);
                            oObsFile.write(",");
                            oObsFile.write(sDate);
                            oObsFile.write(" ");
                            oObsFile.write(sValues[oCols.get("TIME").intValue()].trim());
                            oObsFile.write(",");
                            oObsFile.write(sValues[oCols.get("LAT").intValue()].trim());
                            oObsFile.write(",");
                            oObsFile.write(sValues[oCols.get("LONG").intValue()].trim());
                            oObsFile.write(",");
                            oObsFile.write(sValues[oCols.get("ALT").intValue()].trim());
                            oObsFile.write(",");
                            oObsFile.write(sAirTemp);
                            oObsFile.write(",");
                            oObsFile.write(sPressure);

                            for (int nIndex = 0; nIndex < COLS.length; nIndex++) {
                                oObsFile.write(",");
                                String sKey = COLS[nIndex];
                                Integer oIndex = oCols.get(sKey);
                                String sPrev = oVals.get(sKey);

                                if (oIndex != null) {
                                    String sValue = sValues[oIndex.intValue()].trim();
                                    if (sValue.compareTo(sPrev) != 0) {
                                        oObsFile.write(sValue);
                                        oVals.put(sKey, sValue);
                                    }
                                }
                            }
                            oObsFile.write("\n");

                            ++nRow;
                            if (nRow > 0) // reset air temp and pressure
                                sAirTemp = sPressure = "";
                        }
                    }
                }

                oDstFile.close();
                oObsFile.close();
                oReader.close();
            } catch (Exception oException) {
                oException.printStackTrace();
            }
        }
    }
}
