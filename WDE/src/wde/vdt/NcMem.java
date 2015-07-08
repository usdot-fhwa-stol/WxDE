/************************************************************************
 * Source filename: NcMem.java
 * <p/>
 * Creation date: Nov 18, 2013
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

package wde.vdt;

import org.apache.log4j.Logger;
import ucar.ma2.Array;

import java.util.HashMap;

public abstract class NcMem {

    public static final double DOUBLE_FILL_VALUE = -9999.0;
    public static final float FLOAT_FILL_VALUE = -9999.0f;
    public static final short SHORT_FILL_VALUE = -9999;
    public static final short QC_FILL_VALUE = 255;
    public static final int INT_FILL_VALUE = -9999;
    private static final Logger logger = Logger.getLogger(NcMem.class);
    protected int recNum;

    protected HashMap<String, Class> attributeMap = null;

    protected HashMap<String, Class> variableMap = null;

    // The key is the variable name
    protected HashMap<String, Object> content = null;

    public NcMem() {
        recNum = 0;
        content = new HashMap<>();
        attributeMap = new HashMap<>();
        variableMap = new HashMap<>();
        loadVariableList();
    }

    /**
     * @return the attributeMap
     */
    public HashMap<String, Class> getAttributeMap() {
        return attributeMap;
    }

    /**
     * @return the variableMap
     */
    public HashMap<String, Class> getVariableMap() {
        return variableMap;
    }

    public void populateDoubleArrayVariable(String key, Array data) {
        logger.info("populateDoubleArrayVariable for " + key);
        NetCdfArrayList<Double> list = (NetCdfArrayList<Double>) content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        list.ensureCapacity(recNum);
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getDouble(i));
    }

    public void populateFloatArrayVariable(String key, Array data) {
        logger.info("populateFloatArrayVariable for " + key);
        NetCdfArrayList<Float> list = (NetCdfArrayList<Float>) content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        list.ensureCapacity(recNum);
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getFloat(i));
    }

    public void populateIntArrayVariable(String key, Array data) {
        logger.info("populatingIntArray for " + key);
        NetCdfArrayList<Integer> list = (NetCdfArrayList<Integer>) content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        list.ensureCapacity(recNum);
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getInt(i));
    }

    public void populateShortArrayVariable(String key, Array data) {
        logger.info("populateShortArrayVariable for " + key);
        NetCdfArrayList<Short> list = (NetCdfArrayList<Short>) content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        list.ensureCapacity(recNum);
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getShort(i));
    }

    public void populateStringArrayVariable(String key, Array data) {
        logger.info("populateStringArrayVariable for " + key);
        int strCount = data.getShape()[0];
        int strLen = data.getShape()[1];
        char[] elementChars = new char[strLen];

        NetCdfArrayList<String> list = (NetCdfArrayList<String>) content.get(key);
        if (recNum == 0)
            recNum = strCount;
        else if (recNum != strCount)
            logger.error("Different array size encountered");

        list.ensureCapacity(recNum);
        for (int i = 0; i < strCount; i++) {

            for (int j = 0; j < strLen; j++)
                elementChars[j] = data.getChar(i * strLen + j);

            String element = new String(elementChars);
            list.update(i, element.trim());
        }
    }

    public Object getList(String key) {
        return content.get(key);
    }

    /**
     * @return the recNum
     */
    public int getRecNum() {
        return recNum;
    }

    protected <T> void addVariable(String varName, T type) {
        NetCdfArrayList<T> list = new NetCdfArrayList<>();
        String typeName = type.toString();
        if (typeName.contains("Double"))
            ((NetCdfArrayList<Double>) list).init(DOUBLE_FILL_VALUE);
        else if (typeName.contains("Float"))
            ((NetCdfArrayList<Float>) list).init(FLOAT_FILL_VALUE);
        else if (typeName.contains("Short"))
            ((NetCdfArrayList<Short>) list).init(SHORT_FILL_VALUE);
        else if (typeName.contains("Integer"))
            ((NetCdfArrayList<Integer>) list).init(INT_FILL_VALUE);
        else if (typeName.contains("String"))
            ((NetCdfArrayList<String>) list).init("");

        content.put(varName, list);
    }

    abstract protected void loadVariableList();
}
