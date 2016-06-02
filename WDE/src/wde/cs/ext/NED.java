package wde.cs.ext;

import java.awt.geom.Point2D;
import java.io.RandomAccessFile;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This National Elevation Database (NED) class is a HTTP servlet that receives 
 * a latitude and longitude query and looks up the altitude of that location 
 * from a local directory of GridFloat formatted files that change infrequently 
 * and must be manually refreshed, probably annually.
 */
public class NED extends HttpServlet
{
	private File[] m_oFiles;


	/**
	 * The default NED constructor
	 */
	public NED()
	{
	}


	/**
	 * The init method is used configure the instantiated NED object.
	 *
	 * @param oConfig configuration object that specifies the source file location
	 * 
	 * @throws javax.servlet.ServletException
	 */
	@Override
	public void init(ServletConfig oConfig)
		throws ServletException
	{
		String sSrcDir = oConfig.getInitParameter("dir");
		if (sSrcDir == null || sSrcDir.length() == 0)
			sSrcDir = "/opt/NED/"; // default source file location

		if (!sSrcDir.endsWith("/"))
			sSrcDir += "/"; // fix missing end directory separator

		m_oFiles = new File(sSrcDir).listFiles();
	}


	/**
	 * The doGet method receives a latitude/longitude pair in the request, 
	 * searches for a corresponding altitude for that location, and responds 
	 * with a text float number or NaN when nothing is found.
	 *
	 * @param oReq the HTTP request containing the lat/lon to be looked up
	 * @param oRep the HTTP response that will contain an altitude or NaN
	 * 
	 * @throws javax.servlet.ServletException
	 * @throws java.io.IOException
	 */
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oRep)
		throws ServletException, IOException
	{
		String sAlt = "NaN"; // default response when error occurs or not found
		try
		{
			String[] sParams = oReq.getPathInfo().split("/");
			if (sParams.length != 3) // there will appear to be three parameters 
				throw new Exception(); // path starts with a "/" so ignore index 0

			String sLat = sParams[1]; // validate lat string
			if (sLat.length() != 8 || sLat.startsWith("-") || sLat.contains("."))
				throw new Exception(); // test for correct formatting
			int nLat = Integer.parseInt(sLat); // tests for being a valid number
			
			String sLon = sParams[2]; // validate lon string
			if (sLon.length() < 9 || sLon.length() > 10 
					|| !sLon.startsWith("-") || sLat.contains("."))
				throw new Exception(); // test for correct formatting
			int nLon = Integer.parseInt(sLon); // tests for being a valid number

			sAlt = getAlt(nLat, nLon);			
		}
		catch (Exception oException)
		{
		}

		oRep.setContentType("text/plain"); // returns altitude as a text string
		oRep.setContentLength(sAlt.length());
		PrintWriter oWriter = oRep.getWriter();
		oWriter.print(sAlt); // send altitude to client
		oWriter.flush();
	}


	/**
	 * The getAlt method does the work of determining which file to read, and 
	 * then computing the position within the file to read an altitude value. 
	 * File names correspond to the top left (NW) corner of the grid. Each grid 
	 * is 10812 x 10812 float elements in size with an overlap border of six 
	 * grid cells on each side;
	 *
	 * @param nLat the requested latitude in micro-degrees
	 * @param nLon the requested longitude in micro-degrees

	 * @throws java.io.IOException
	*/
	private String getAlt(int nLat, int nLon)
		throws IOException
	{
		int nTop = nLat / 1000000; // floor lat to nearest coordinate
		if (nTop * 1000000 != nLat) // correct for boundary intersection
			++nTop;

		int nLeft = nLon / 1000000; // floor lon to nearest coordinate
		if (nLeft * 1000000 != nLon) // correct for boundary intersection
			--nLeft;

		StringBuilder sLoc = new StringBuilder("n"); // build name search string
		sLoc.append(nTop).append("w0").append(-nLeft);
		if (sLoc.length() > 7) // indicates three-digit lon coordinate
			sLoc.deleteCharAt(4); // remove unneeded zero

		boolean bFound = false;
		int nIndex = m_oFiles.length; // find file that contains requested altitude
		while (!bFound && nIndex-- > 0)
			bFound = m_oFiles[nIndex].getPath().contains(sLoc);

		if (bFound)
		{
			double dRow = (double)(nTop * 1000000 - nLat) * 10800.0 / 1000000.0;
			double dCol = (double)(nLon - nLeft * 1000000) * 10800.0 / 1000000.0;
			long lRow = (long)dRow; // use cast long value later to obtain 
			long lCol = (long)dCol; // decimal part of double value
			long lPos = ((lRow + 6) * 10812 + (lCol + 6)) * 4; // 10812 cells per row
			dRow -= lRow; // reduce to decimal part for 
			dCol -= lCol; // point distance weighting calcualtions

			double[] dAlts = new double[4]; // quad grid to store nearest altitudes
			ByteBuffer oBuf = ByteBuffer.allocate(8); // hold two consecutive floats
			oBuf.order(ByteOrder.LITTLE_ENDIAN); // match source file byte order

			RandomAccessFile oIn = new RandomAccessFile(m_oFiles[nIndex], "r");
			oIn.seek(lPos); // first file position
			oIn.readFully(oBuf.array()); // read first two float values
			dAlts[0] = oBuf.getFloat();
			dAlts[1] = oBuf.getFloat();
			oIn.seek(lPos + 43248); // last file postion incremented by 10812 * 4
			oBuf.clear(); // reuse buffer
			oIn.readFully(oBuf.array()); // read last two float values
			dAlts[2] = oBuf.getFloat();
			dAlts[3] = oBuf.getFloat();
			oIn.close();

			double[] dWeights = new double[4];
			dWeights[0] = weight(0.0, 0.0, dCol, dRow);
			dWeights[1] = weight(dCol, dRow, 1.0, 0.0);
			dWeights[2] = weight(0.0, 1.0, dCol, dRow);
			dWeights[3] = weight(dCol, dRow, 1.0, 1.0);

			double dNum = 0.0;
			double dDen = 0.0;
			nIndex = dAlts.length;
			while (nIndex-- > 0) // accumulate weighted average
			{
				dNum += dWeights[nIndex] * dAlts[nIndex];
				dDen += dWeights[nIndex];
			}

			return Double.toString(dNum / dDen);
		}

		return "NaN"; // default not found response
	}


	private static double weight(double dX1, double dY1, double dX2, double dY2)
	{
		double dWeight = 1.0 - Point2D.distance(dX1, dY1, dX2, dY2);
		if (dWeight < 0.0) // ignore weights less than zero
			dWeight = 0.0;

		return dWeight;
	}
}
