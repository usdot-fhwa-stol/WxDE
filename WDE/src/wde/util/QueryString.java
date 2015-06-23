/************************************************************************
 * Source filename: QueryString.java
 * <p/>
 * Creation date: Feb 22, 2013
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

import java.sql.Timestamp;

public class QueryString {

    public static String convert(String str, boolean isEnd) {
        String padding = (isEnd) ? "" : ",";
        String result = ((str == null || str.length() == 0) ? (null + padding) : ("'" + str + "'" + padding));
        return result;
    }

    public static String convert(char aChar, boolean isEnd) {
        String padding = (isEnd) ? "" : ",";
        String result = (aChar == 0) ? (null + padding) : ("'" + aChar + "'" + padding);
        return result;
    }

    public static String convertId(int num, boolean isEnd) {
        String padding = (isEnd) ? "" : ",";
        String result = ((num == 0) ? (null + padding) : (num + padding));
        return result;
    }

    public static String convert(int num, boolean isEnd) {
        String result = num + ((isEnd) ? "" : ",");
        return result;
    }

    public static String convert(float num, boolean isEnd) {
        String result = num + ((isEnd) ? "" : ",");
        return result;
    }

    public static String convert(double num, boolean isEnd) {
        String result = num + ((isEnd) ? "" : ",");
        return result;
    }

    public static String convert(Timestamp timestamp, String quote, boolean isEnd) {
        String padding = (isEnd) ? "" : ",";
        String result = ((timestamp == null) ? (null + padding) : (quote + timestamp + quote + padding));
        return result;
    }

    public static String[] parseCSVLine(String aLine) {
        char DOUBLEQUOTE = '"';
        char SINGLEQUOTE = '\'';
        String strToRemove = "\\N";
        char COMMA = ',';
        char SUB = 0x001A; // or whatever character that will NEVER appear in the input String

//        System.out.println(aLine);

        // Replace commas inside quoted text with substitute character
        boolean quote = false;
        for (int index = 0; index < aLine.length(); index++) {
            char ch = aLine.charAt(index);
            if (ch == DOUBLEQUOTE) {
                quote = !quote;
            } else if (ch == COMMA && quote) {
                aLine = aLine.substring(0, index) + SUB + aLine.substring(index + 1);
            }
        }

        // Strip out all quotation marks and escape SINGLEQUOTE
        for (int index = 0; index < aLine.length(); index++) {
            if (aLine.charAt(index) == SINGLEQUOTE) {
                aLine = aLine.substring(0, index) + SINGLEQUOTE + aLine.substring(index);
                index++;
            }
            if (aLine.charAt(index) == DOUBLEQUOTE) {
                aLine = aLine.substring(0, index) + aLine.substring(index + 1);
            }
        }

        // Removes all instances of strToRemove
        aLine = aLine.replace(strToRemove, " ") + " ";

        // Parse input into tokens
        String[] tokens = aLine.split(",");

        // restore commas in place of SUB characters
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replace(SUB, COMMA).trim();
        }

        return tokens;
    }

    public static String escapeSingleQuote(String str) {
        char SINGLEQUOTE = '\'';

        for (int index = 0; index < str.length(); index++) {
            if (str.charAt(index) == SINGLEQUOTE) {
                str = str.substring(0, index) + SINGLEQUOTE + str.substring(index);
                index++;
            }
        }
        return str.trim();
    }
}
