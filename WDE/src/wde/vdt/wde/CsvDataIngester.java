package wde.vdt.wde;

import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import wde.util.io.CharTokenizer;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;


public class CsvDataIngester {
    static CsvDataIngester instance = null;
    ArrayList<PlatformObs> m_oPlatforms = new ArrayList<>();

    private CsvDataIngester() {
    }

    public static CsvDataIngester getInstance() {
        if (instance == null)
            instance = new CsvDataIngester();

        return instance;
    }

    public static void main(String[] sArgs)
            throws Exception {
        if (sArgs == null || sArgs.length < 2)
            return;

        CsvDataIngester oCsv = new CsvDataIngester();
        oCsv.readCsvFile(sArgs[0]);
        oCsv.writeNcFile(sArgs[1]);
    }

    public boolean readCsvFile(String sFilePath)
            throws Exception {
        boolean recordsFound = false;

        StringBuilder sCol = new StringBuilder();
        CharTokenizer oTokenizer = new CharTokenizer(",", "\n");

        FileInputStream oFileInput = new FileInputStream(sFilePath);
        oTokenizer.setInput(oFileInput);
        while (oTokenizer.nextSet()) // skip header rows
        {
            oTokenizer.nextToken(sCol);
            if (sCol.indexOf("---BEGIN OF RECORDS") >= 0)
                break;
        }

        PlatformObs oSearch = new PlatformObs();
        while (oTokenizer.nextSet()) {
            if (!oSearch.readRecord(oTokenizer, sCol))
                break;

            int nIndex = Collections.binarySearch(m_oPlatforms, oSearch);
            if (nIndex < 0) {
                nIndex = ~nIndex;
                m_oPlatforms.add(nIndex, new PlatformObs(oSearch));
                recordsFound = true;
            }
            m_oPlatforms.get(nIndex).add(new ObsLabel(oSearch.m_sObsType,
                    oSearch.m_fValues[PlatformObs.OBS_VALUE]));
        }
        oFileInput.close();

        return recordsFound;
    }

    public void writeNcFile(String sFilepath)
            throws Exception {
        NetcdfFileWriteable oFile = NetcdfFileWriteable.createNew(sFilepath, false);

        Dimension oMaxNameLength = oFile.addDimension("maxNameLength", 51);
        Dimension oRecNum = oFile.addUnlimitedDimension("recNum");

        ArrayList<Dimension> oDims = new ArrayList<>();
        addVar(oFile, DataType.CHAR, "stationName", oDims, oRecNum, oMaxNameLength);
        addVar(oFile, DataType.FLOAT, "latitude", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "longitude", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "elevation", oDims, oRecNum);
        addVar(oFile, DataType.DOUBLE, "observationTime", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "temperature", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "dewpoint", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "relHumidity", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "stationPressure", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "windDir", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "windSpeed", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "windGust", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "visibility", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precipAccum", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precipRate", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "solarRadiation", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "roadTemperature1", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "roadLiquidFreezeTemp1", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "roadState1", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "roadSubsurfaceTemp1", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "waterLevel", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "iceThickness", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "minTemp24Hour", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "maxTemp24Hour", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precip3hr", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precip6hr", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precip12hr", oDims, oRecNum);
        addVar(oFile, DataType.FLOAT, "precip10min", oDims, oRecNum);

        oFile.addGlobalAttribute("_CoordSysBuilder", "ucar.nc2.dataset.conv.DefaultConvention");
        oFile.create(); // create the file

        ArrayChar sValues = new ArrayChar.D2(m_oPlatforms.size(), oMaxNameLength.getLength());
        for (int nIndex = 0; nIndex < m_oPlatforms.size(); nIndex++)
            sValues.setString(nIndex, Integer.toString(m_oPlatforms.get(nIndex).m_nId));
        oFile.write("stationName", sValues); // write station names
        sValues = null; // mark for gc

        ArrayFloat fValues = new ArrayFloat.D1(m_oPlatforms.size());
        writeFloatArray("latitude", "latitude", fValues, oFile);
        writeFloatArray("longitude", "longitude", fValues, oFile);
        writeFloatArray("elevation", "elevation", fValues, oFile);

        ArrayDouble dValues = new ArrayDouble.D1(m_oPlatforms.size());
        for (int nIndex = 0; nIndex < m_oPlatforms.size(); nIndex++)
            dValues.setDouble(nIndex, m_oPlatforms.get(nIndex).m_lTimestamp);
        oFile.write("observationTime", dValues); // write timestamps
        dValues = null; // mark for gc

        ArrayShort tValues = new ArrayShort.D1(m_oPlatforms.size());

        writeFloatArray("temperature", "essAirTemperature", fValues, oFile);
        writeFloatArray("dewpoint", "essDewpointTemp", fValues, oFile);
        writeFloatArray("relHumidity", "essRelativeHumidity", fValues, oFile);
        writeFloatArray("stationPressure", "essAtmosphericPressure", fValues, oFile);
        writeFloatArray("windDir", "windSensorAvgDirection", fValues, oFile);
        writeFloatArray("windSpeed", "windSensorAvgSpeed", fValues, oFile);
        writeFloatArray("windGust", "windSensorGustSpeed", fValues, oFile);
        writeFloatArray("visibility", "essVisibility", fValues, oFile);
        writeFloatArray("precipAccum", "", fValues, oFile);
        writeFloatArray("precipRate", "essPrecipRate", fValues, oFile);
        writeFloatArray("solarRadiation", "essInstantaneousSolarRadiation", fValues, oFile);
        writeFloatArray("roadTemperature1", "essSurfaceTemperature", fValues, oFile);
        writeFloatArray("roadLiquidFreezeTemp1", "essSurfaceFreezePoint", fValues, oFile);
        writeFloatArray("roadState1", "essSurfaceStatus", fValues, oFile);
        writeFloatArray("roadSubsurfaceTemp1", "essSubSurfaceTemperature", fValues, oFile);
        writeFloatArray("waterLevel", "essSurfaceIceOrWaterDepth", fValues, oFile);
        writeFloatArray("iceThickness", "essIceThickness", fValues, oFile);
        writeFloatArray("minTemp24Hour", "essMinTemp", fValues, oFile);
        writeFloatArray("maxTemp24Hour", "essMaxTemp", fValues, oFile);
        writeFloatArray("precip3hr", "essPrecipitationThreeHours", fValues, oFile);
        writeFloatArray("precip6hr", "essPrecipitationSixHours", fValues, oFile);
        writeFloatArray("precip12hr", "essPrecipitationTwelveHours", fValues, oFile);
        writeFloatArray("precip10min", "precip10min", fValues, oFile);

        oFile.close();
    }

    void addVar(NetcdfFileWriteable oFile, DataType oType, String sVar,
                ArrayList<Dimension> oDims, Dimension... oDim) {
        oDims.clear(); // copy variable list of dimensions to list
        for (int nIndex = 0; nIndex < oDim.length; nIndex++)
            oDims.add(oDim[nIndex]);

        oFile.addVariable(sVar, oType, oDims);

        if (oType == DataType.SHORT) {
            short tMissing = -9999;
            oFile.addVariableAttribute(sVar, "_FillValue", Short.MIN_VALUE);
            oFile.addVariableAttribute(sVar, "missing_value", tMissing);
        } else if (oType == DataType.INT) {
            oFile.addVariableAttribute(sVar, "_FillValue", Integer.MIN_VALUE);
            oFile.addVariableAttribute(sVar, "missing_value", -9999);
        } else if (oType == DataType.FLOAT) {
            oFile.addVariableAttribute(sVar, "_FillValue", Float.MAX_VALUE);
            oFile.addVariableAttribute(sVar, "missing_value", -9999.0F);
        } else if (oType == DataType.DOUBLE) {
            oFile.addVariableAttribute(sVar, "_FillValue", Double.MAX_VALUE);
            oFile.addVariableAttribute(sVar, "missing_value", -9999.0);
        }
    }

    void writeFloatArray(String sVar, String sLabel, ArrayFloat oValues,
                         NetcdfFileWriteable oFile) throws Exception {
        for (int nIndex = 0; nIndex < m_oPlatforms.size(); nIndex++)
            oValues.setFloat(nIndex, m_oPlatforms.get(nIndex).getFloat(sLabel));

        oFile.write(sVar, oValues);
    }

    void writeShortArray(String sVar, String sLabel, ArrayShort oValues,
                         NetcdfFileWriteable oFile) throws Exception {
        for (int nIndex = 0; nIndex < m_oPlatforms.size(); nIndex++)
            oValues.setShort(nIndex, m_oPlatforms.get(nIndex).getShort(sLabel));

        oFile.write(sVar, oValues);
    }
}
