package wde.data.osm;


/**
 * Common SHP file reading utility functions.
 *
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Utility
{
	/**
	 * Creates a new instance of Utility
	 */
	private Utility()
	{
	}


  /**
	 * Reverses the two bytes in a short integer.  The first byte of the short becomes the
	 * second byte and the second byte of the short becomes the first.
	 * <br><br>
	 * Example: swap(1) returns 256
	 *         <ul style='list-style-type:none'>
	 *           <li><nobr>1 => 00000000 00000001</nobr></li>
	 *           <li><nobr>256 => 00000001 00000000</nobr></li>
	 *         </ul>
	 * <br> and swap(257) returns 257
	 *         <ul style='list-style-type:none'>
	 *           <li><nobr>257 => 00000001 00000001</nobr></li>
	 *           <li><nobr>257 => 00000001 00000001</nobr></li>
	 *         </ul>
	 * 
	 * @param rValue short integer value to have its bytes swapped
	 * @return short integer with bytes swapped
	 */
	public static short swap(short rValue)
	{
		int nByte1 = rValue & 0xff;
		int nByte2 = (rValue >> 8) & 0xff;

		return (short)(nByte1 << 8 | nByte2);
	}


  /**
	 * Reverses the four bytes in an integer.  The first byte becomes the fourth byte, the second
	 * byte becomes the third byte, the third byte becomes the second byte and the fourth byte
	 * becomes the first byte.
   * <br><br>
	 * Example: swap(134217728) returns 16909320
	 *         <ul style='list-style-type:none'>
	 *           <li><nobr>134217728 => 00001000 00000100 00000010 00000001</nobr></li>
	 *           <li><nobr>16777216 => 00000001 00000010 00000100 00001000</nobr></li>
	 *         </ul>
	 * 
	 * @param nValue integer value to have its bytes swapped
	 * @return integer with bytes swapped
	 */
	public static int swap(int nValue)
	{
		return
		(
			(nValue << 24) + 
			(nValue << 8 & 0x00FF0000) + 
			(nValue >>> 8 & 0x0000FF00) + 
			(nValue >>> 24)
		);
	}


	/**
	 * Reverses the eight bytes in a long integer.  The first byte becomes the eighth byte, the second
	 * byte becomes the seventh byte, etc...
   * <br><br>
	 * Example: swap(72057594037927936) returns 1
	 *         <ul style='list-style-type:none'>
	 *           <li><nobr>134217728 => 00000001 00000000 00000000 00000000 00000000 00000000 00000000 00000000</nobr></li>
	 *           <li><nobr>16777216 => 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001</nobr></li>
	 *         </ul>
	 * 
	 * @param lValue long integer value to have its bytes swapped
	 * @return long integer with bytes swapped
	 */
	public static long swap(long lValue)
	{
		long lByte1 = lValue & 0xff;
		long lByte2 = (lValue >>  8) & 0xff;
		long lByte3 = (lValue >> 16) & 0xff;
		long lByte4 = (lValue >> 24) & 0xff;
		long lByte5 = (lValue >> 32) & 0xff;
		long lByte6 = (lValue >> 40) & 0xff;
		long lByte7 = (lValue >> 48) & 0xff;
		long lByte8 = (lValue >> 56) & 0xff;

		return
		(
			lByte1 << 56 | lByte2 << 48 | lByte3 << 40 | lByte4 << 32 |
			lByte5 << 24 | lByte6 << 16 | lByte7 <<  8 | lByte8
		);
	}


  /**
	 * Reverses the four bytes in a floating point number.  The first byte becomes the fourth byte, the second
	 * byte becomes the third byte, the third byte becomes the second byte and the fourth byte
	 * becomes the first byte.
	 * 
	 * @param fValue floating point value to have its bytes swapped
	 * @return long integer with bytes swapped
	 */
	public static float swap(float fValue)
	{
		return Float.intBitsToFloat(swap(Float.floatToIntBits(fValue)));
	}


	/**
	 * Reverses the eight bytes in a double precision number.  The first byte becomes the eighth byte, the second
	 * byte becomes the seventh byte, etc...
	 * 
	 * @param dValue double precision value to have its bytes swapped
	 * @return long integer with bytes swapped
	 */
	public static double swap(double dValue)
	{
		return Double.longBitsToDouble(swap(Double.doubleToLongBits(dValue)));
	}


	public static double swapD(long lValue)
	{
		return Double.longBitsToDouble(swap(lValue));
	}


	/**
	 * Returns the unsigned integer value of a byte.
	 * 
	 * @param yValue the byte
	 * @return the unsigned integer value of the byte
	 */
	public static int unsignByte(byte yValue)
	{
		int nValue = yValue;
		if (nValue < 0)
			nValue += 256;
		
		return nValue;
	}

	/**
	 * Determines the next smallest integer value.
	 * 
	 * @param nValue integer value to floor
	 * @param nPrecision
	 * @return next smallest integer to the parameter integer
	 */
	public static int floor(int nValue, int nPrecision)
	{
		// this integer flooring method returns the next smallest integer
		int nFlooredValue = nValue / nPrecision * nPrecision;
		
		// correct for negative numbers 
		// ensure the value was not previously floored or this will return the wrong result
		if (nValue < 0 && nFlooredValue != nValue)
			nFlooredValue -= nPrecision;
		
		return nFlooredValue;
	}


	/**
	 * Determines if the specified point is within the specified boundary. 
	 * A specified tolerance adjusts the compared region as needed.
	 * 
	 * @param nX x coordinate of point
	 * @param nY y coordinate of point
	 * @param nT y value of the top of the region
	 * @param nR x value of the right side of the region
	 * @param nB y value of the bottom of the region
	 * @param nL x value of the left side of the region
	 * @param nTol the allowed margin for a point to be considered inside
	 * @return true if the point is inside or on the rectangular region
	 */
	public static boolean isInside(int nX, int nY, 
		int nT, int nR, int nB, int nL, int nTol)
	{
		if (nR < nL) // swap the left and right bounds as needed
		{
			nR ^= nL;
			nL ^= nR;
			nR ^= nL;
		}

		if (nT < nB) // swap the top and bottom bounds as needed
		{
			nT ^= nB;
			nB ^= nT;
			nT ^= nB;
		}
	
		 // expand the bounds by the tolerance
    return (nX >= nL - nTol && nX <= nR + nTol && 
			nY >= nB - nTol && nY <= nT + nTol);
	}


	/**
	 * Determines the squared perpendicular distance between a point and a line. 
	 * All values are scaled to six decimal places. The distance is returned or 
	 * a negative integer when the point does not intersect the line.
	 * 
	 * @param nX	longitude
	 * @param nY	latitude
	 * @param nX1	longitude for the first end point of the line
	 * @param nY1	latitude for the first end point of the line
	 * @param nX2	longitude for the second end point of the line
	 * @param nY2	latitude for the second end point of the line
	 * @return scaled degree distance between the point and line
	 */
	public static int getPerpDist(int nX, int nY, 
		int nX1, int nY1, int nX2, int nY2)
	{
		double dDist = getPerpDist((double)nX, (double)nY, 
			(double)nX1, (double)nY1, (double)nX2, (double)nY2);

		if (Double.isNaN(dDist) || dDist > Integer.MAX_VALUE)
			return Integer.MIN_VALUE;

		return (int)dDist;
	}


	/**
	 * Determines the squared perpendicular distance between a point and a line. 
	 * All values are scaled to six decimal places. The distance is returned or 
	 * NaN when the point does not intersect the line.
	 * 
	 * @param dX	longitude
	 * @param dY	latitude
	 * @param dX1	longitude for the first end point of the line
	 * @param dY1	latitude for the first end point of the line
	 * @param dX2	longitude for the second end point of the line
	 * @param dY2	latitude for the second end point of the line
	 * @return scaled degree distance between the point and line
	 */
	public static double getPerpDist(double dX, double dY, 
		double dX1, double dY1, double dX2, double dY2)
	{
		double dXd = dX2 - dX1;
		double dYd = dY2 - dY1;
		double dXp = dX - dX1;
		double dYp = dY - dY1;

		if (dXd == 0 && dYd == 0) // line segment is a point
			return dXp * dXp + dYp * dYp; // squared dist between the points
		
		double dU = dXp * dXd + dYp * dYd;
		double dV = dXd * dXd + dYd * dYd;
	
		if (dU < 0 || dU > dV) // nearest point is not on the line
			return Double.NaN;

		// find the perpendicular intersection of the point on the line
		dXp = dX1 + (dU * dXd / dV);
		dYp = dY1 + (dU * dYd / dV);

		dXd = dX - dXp; // calculate the squared distance
		dYd = dY - dYp; // between the point and the intersection
		return dXd * dXd + dYd * dYd;
	}
}
