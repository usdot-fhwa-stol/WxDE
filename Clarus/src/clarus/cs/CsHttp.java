// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsHttp.java
 */

package clarus.cs;

import java.io.DataInputStream;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import clarus.ClarusMgr;
import clarus.qedc.IObsSet;
import clarus.qedc.ObsMgr;

/**
 * Provides an interface to collect observation data over Http connections.
 *
 * @see CsHttp#doPost
 */
public class CsHttp extends HttpServlet
{
    /**
     * Catches the main instance of {@code ClarusMgr} in the constructor.
     */
	private ClarusMgr m_oClarusMgr;
    /**
     * Catches the main instance of {@code ObsMgr} in the constructor.
     */
	private ObsMgr m_oObsMgr;
	
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code CsHttp}
	 * </p>
	 */
	public CsHttp()
	{
	}
	
    /**
     * Inserts a delay to allow remote debugging connection. Initializes the
     * two manager members to their respective singleton instances.
     * @param oConfig Pointer to the servlet configuration.
     */
    @Override
	public void init(ServletConfig oConfig)
	{
		try
		{
			// insert a delay to allow remote debugging connection
			Thread.sleep(Long.parseLong(oConfig.getInitParameter("delay")));
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
		
		m_oClarusMgr = ClarusMgr.getInstance();
		m_oClarusMgr.startup();
		m_oObsMgr = ObsMgr.getInstance();
	}
	
	/**
     * Shuts the {@code ClarusMgr} down.
     *
     * @see ClarusMgr#shutdown()
     */
	@Override
	public void destroy()
	{
		m_oClarusMgr.shutdown();
	}


    /**
     * Calls {@see CsHttp#doPost}.
     *
     * @param oRequest Incoming data from the HttpServlet.
     * @param oResponse Outgoing data to the HttpServlet.
     */
	@Override
    public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
		doPost(oRequest, oResponse);
    }
  
    /**
     * Reads from oRequest the Observation Type Id, and retrieves the
     * Observation set from the {@code ObsMgr}. It then enqueues the Observation
     * set into the {@code ClarusMgr} instance.
     * 
     * @param oRequest Incoming data from the HttpServlet.
     * @param oResponse Outgoing data to the HttpServlet.
     */
	@Override
	public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
        try
        {
			DataInputStream iDataIn = 
				new DataInputStream(oRequest.getInputStream());
			
			int nObsTypeId = iDataIn.readInt();
			IObsSet iObsSet = m_oObsMgr.getObsSet(nObsTypeId);
			
			boolean bHasMore = true;
			while (bHasMore)
			{
				try
				{
					iObsSet.addObs
					(
						iDataIn.readInt(), 
						iDataIn.readLong(), 
						iDataIn.readInt(), 
						iDataIn.readInt(), 
						iDataIn.readShort(),
						iDataIn.readDouble(), 
						iDataIn.readInt(), 
						iDataIn.readInt(), 
						iDataIn.readFloat()
					);
				}
				catch (Exception oException)
				{
					bHasMore = false;
				}
			}
			
			iDataIn.close();
			m_oClarusMgr.queue(iObsSet);
        }
        catch (Exception oException)
        {
            oException.printStackTrace();
        }
    }
}
