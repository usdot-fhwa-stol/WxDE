// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ListObsTypes.java
 */
package clarus.qeds;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import clarus.emc.IObsType;
import clarus.emc.ObsTypes;
import util.Introsort;

/**
 * Writes observation-type data in xml format across an http connection.
 * <p>
 * Extends {@code HttpServlet} to enforce a standard interface for requesting
 * data, and responding to these data requests.
 * </p>
 */
public class ListObsTypes extends HttpServlet
{
    /**
     * Pointer to observation-type cache singleton instance.
     */
    private ObsTypes m_oObsTypes = ObsTypes.getInstance();
    /**
     * {@code IObsType} comparator that orders observation-types by name.
     */
    private SortByName m_oSortByName = new SortByName();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code ListObsTypes}
	 * </p>
	 */
    public ListObsTypes()
    {
    }


    /**
     * Wraps
     * {@link ListObsTypes#doPost(HttpServletRequest, HttpServletResponse)}.
     * @param oRequest Requested data to be sent to {@code oResponse}. Connected
     * prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     * data to.
     */
	@Override
	public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
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
     * @param oRequest ignored.
     * @param oResponse Connected response servlet, to write the observation
     * type data to.
     */
	@Override
    public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
		ArrayList<IObsType> oObsTypes = m_oObsTypes.getList();
        Introsort.usort(oObsTypes, m_oSortByName);
        
		try
		{
            oResponse.setContentType("text/xml");
            PrintWriter oPrintWriter = oResponse.getWriter();

            oPrintWriter.println("<?xml version=\"1.0\" ?>");
            oPrintWriter.print("<obsTypes numRows=\"");
            oPrintWriter.print(oObsTypes.size());
            oPrintWriter.println("\">");

            for (int nIndex = 0; nIndex < oObsTypes.size(); nIndex++)
            {
                IObsType iObsType = oObsTypes.get(nIndex);
                
                oPrintWriter.print("\t<obsType id=\"");
                oPrintWriter.print(iObsType.getId());
                oPrintWriter.print("\" name=\"");
                oPrintWriter.print(iObsType.getName());
                oPrintWriter.print("\" active=\"1\" ");

                oPrintWriter.print("units=\"");
                if (iObsType.getUnit() != null)
                    oPrintWriter.print(iObsType.getUnit());

                oPrintWriter.print("\" englishUnits=\"");
                if (iObsType.getEnglishUnit() != null)
                    oPrintWriter.print(iObsType.getEnglishUnit());

                oPrintWriter.println("\"/>");
            }

            // finish writing the xml
            oPrintWriter.println("</obsTypes>");
            oPrintWriter.flush();
            oPrintWriter.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
    }


    /**
     * Allows ordering of {@code IObsType} objects by observation-type name.
     * <p>
     * Extends {@code Comparator} to enforce a standard interface for ordering
     * of {@code IObsType} objects.
     * </p>
     */
	private class SortByName implements Comparator<IObsType>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortByName}
		 * </p>
         */
		private SortByName()
		{
		}


        /**
         * Compares the two supplied {@code IObsType} objects by name.
         * @param oLhs observation-type to compare to {@code oRhs}.
         * @param oRhs observation-type to compare to {@code oLhs}.
         * @return 0 if the observation-type names are equivalent.
         */
		public int compare(IObsType oLhs, IObsType oRhs)
		{
            return oLhs.getName().compareTo(oRhs.getName());
		}
	}
}
