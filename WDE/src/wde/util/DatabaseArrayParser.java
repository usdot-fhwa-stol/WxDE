/************************************************************************
 * Source filename: DatabaseArrayParser.java
 * <p/>
 * Creation date: Mar 22, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import java.util.ArrayList;

public class DatabaseArrayParser {

    private static final String DIVIDER_PATTERN = "\"\\((|\\))|[\\))][\"][,]";

    /**
     * Method parses a postgres Row into a List of Strings.
     * <p> 
     * The postgres row is represented by a String and consists of one or more columns, that are separated by a comma. 
     * The row must begin with an open bracket and must end with a closing bracket. 
     * Each column must begin with a letter or a quote. If a column begins with a quote, the column must end with a quote. 
     * Inside quotation a quote is represented by a double quote or by backslash and quote, a backslash is represented by double backslash. 
     *
     * @param value
     * @return List of Strings 
     * @throws Exception
     */
    public ArrayList<String> postgresROW2StringList(String value) throws Exception {
        if (!(value.startsWith("{") && value.endsWith("}")))
            throw new Exception("postgresROW2StringList() ROW must begin with '{' and end with '}': " + value);

        value = value.substring(1, value.length() - 1);
        String[] strs = value.split(DIVIDER_PATTERN);
        int size = strs.length;
        int len = strs[size - 1].length();
        if (strs[size - 1].endsWith(")\""))
            strs[size - 1] = strs[size - 1].substring(0, len - 2);
        ArrayList<String> strArray = new ArrayList<String>();
        for (String s : strs) {
            if (s.trim().length() != 0)
                strArray.add(s);
        }

        return strArray;
    }
}