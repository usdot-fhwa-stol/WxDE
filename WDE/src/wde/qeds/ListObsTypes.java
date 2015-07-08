// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ListObsTypes.java
 */
package wde.qeds;

import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Writes observation-type data in xml format across an http connection.
 * <p>
 * Extends {@code HttpServlet} to enforce a standard interface for requesting
 * data, and responding to these data requests.
 * </p>
 */
public class ListObsTypes extends HttpServlet {
    /**
     * Pointer to observation-type cache singleton instance.
     */
    private ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code ListObsTypes}
     * </p>
     */
    public ListObsTypes() {
    }


    /**
     * Wraps
     * {@link ListObsTypes#doPost(HttpServletRequest, HttpServletResponse)}.
     *
     * @param oRequest  Requested data to be sent to {@code oResponse}. Connected
     *                  prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     *                  data to.
     */
    @Override
    public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        doPost(oRequest, oResponse);
    }


    /**
     * Retrieves a copy of the cached list of observation types and sorts the
     * copied list by observation name. Prints the list of observation-types
     * in rows, in xml format. Each xml row represents an element in the
     * observation-type list, and is of the format:
     * <blockquote>
     * &lt obsType id= name= active=1 units= >
     * </blockquote>
     *
     * @param oRequest  ignored.
     * @param oResponse Connected response servlet, to write the observation
     *                  type data to.
     */
    @Override
    public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        ArrayList<ObsType> obsTypes = obsTypeDao.getObsTypeList();
//        Introsort.usort(oObsTypes, m_oSortByName);

        try {
            oResponse.setContentType("text/xml");
            PrintWriter oPrintWriter = oResponse.getWriter();

            oPrintWriter.println("<?xml version=\"1.0\" ?>");
            oPrintWriter.print("<obsTypes numRows=\"");
            oPrintWriter.print(obsTypes.size());
            oPrintWriter.println("\">");

            for (int nIndex = 0; nIndex < obsTypes.size(); nIndex++) {
                ObsType obsType = obsTypes.get(nIndex);

                oPrintWriter.print("\t<obsType id=\"");
                oPrintWriter.print(obsType.getId());
                oPrintWriter.print("\" name=\"");
                oPrintWriter.print(obsType.getObsType());
                oPrintWriter.print("\" active=\"1\" ");

                oPrintWriter.print("units=\"");
                if (obsType.getObsInternalUnit() != null)
                    oPrintWriter.print(obsType.getObsInternalUnit());

                oPrintWriter.print("\" englishUnits=\"");
                if (obsType.getObsEnglishUnit() != null)
                    oPrintWriter.print(obsType.getObsEnglishUnit());

                oPrintWriter.println("\"/>");
            }

            // finish writing the xml
            oPrintWriter.println("</obsTypes>");
            oPrintWriter.flush();
            oPrintWriter.close();
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }
}
