package wde.data.shp;

import java.io.DataInputStream;
import java.util.Iterator;

import wde.data.AbstractIterator;
import wde.util.MathUtil;


/**
 * Holds information associated with a geo-coordinate polyline. A polyline
 * can represent roads, rivers, rail lines, or other linear map features.
 *
 * @author bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Polyline extends AbstractIterator<PolylinePart> {
    private int m_nPartIndex;
    private int[] m_oParts;
    private int[] m_oPolyline;
    private final PolylinePart m_oPart = new PolylinePart();


    /**
     * Creates a new "blank" instance of Polyline
     */
    public Polyline() {
    }


    public void set(int[] oPolyline) {
        m_oPolyline = oPolyline; // update polyline data reference
        m_oParts = new int[oPolyline[4] + 1]; // create array to hold point offsets
        System.arraycopy(oPolyline, 4, m_oParts, 0, m_oParts.length);
        m_oParts[0] = m_oParts.length + 4; // set first point offset
        m_nPartIndex = 0; // reset part index
    }


    @Override
    public boolean hasNext() {
        return (m_oPolyline != null && m_nPartIndex < m_oParts.length);
    }


    @Override
    public PolylinePart next() {
        int nStart = m_oParts[m_nPartIndex];
        int nEnd = m_oPolyline.length; // default to end of polyline date
        if (++m_nPartIndex < m_oParts.length)
            nEnd = m_oParts[m_nPartIndex];

        m_oPart.set(m_oPolyline, nStart, nEnd - 2); // lines span two points
        return m_oPart;
    }


    /**
     * Creates an integer array that represents polyline data.
     *
     * @param oDataInputStream data stream used to build the polyline
     * @return an integer array that represents polyline parts and segments
     * @throws Exception
     */
    public static int[] read(DataInputStream oDataInputStream)
            throws Exception {
        oDataInputStream.readInt(); // ignore record number
        int nLen = oDataInputStream.readInt(); // read content length
        Utility.swap(oDataInputStream.readInt()); // ignore shape type
        nLen -= 2; // length is based on two-byte short

        int nXmin = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
        int nYmin = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
        int nXmax = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
        int nYmax = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
        nLen -= 16; // minimum bounding rectangle should not need reordering

        int nParts = Utility.swap(oDataInputStream.readInt());
        int nPoints = Utility.swap(oDataInputStream.readInt());
        nLen -= 4;

        int nIndex = 0;
        int[] nData = new int[4 + nParts + nPoints * 2];
        nData[nIndex++] = nXmin; // MBR, num parts, parts, points
        nData[nIndex++] = nYmin;
        nData[nIndex++] = nXmax;
        nData[nIndex++] = nYmax;

        for (int nPart = 0; nPart < nParts; nPart++) {
            nData[nIndex++] = Utility.swap(oDataInputStream.readInt()) * 2 + 4 + nParts;
            nLen -= 2;
        }
        nData[4] = nParts - 1; // overwrite first part index with part count

        while (nPoints-- > 0) {
            nData[nIndex++] = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
            nData[nIndex++] = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
            nLen -= 8;
        }

        while (nLen-- > 0) // ignore any remaining non-point data
            oDataInputStream.readShort();

        return nData;
    }


    /**
     * Determines if a point is within the snap distance of the polyline. This
     * method presumes that the polyline point data are set.
     *
     * @param nTol maximum distance for the point associate with the polyline
     * @param nX   longitudinal coordinate
     * @param nY   latitudinal coordinate
     * @return squared distance from the point to the line
     */
    public int snap(int nTol, int nX, int nY) {
        if (!Utility.isInside(nX, nY, m_oPolyline[3], m_oPolyline[2],
                m_oPolyline[1], m_oPolyline[0], nTol))
            return Integer.MIN_VALUE; // point not inside minimum bounding rectangle

        int nDist = Integer.MAX_VALUE; // narrow to the minimum dist
        int nSqTol = nTol * nTol; // squared tolerance for comparison

        set(m_oPolyline); // reset iterator
        while (hasNext()) {
            PolylinePart oPart = next();
            while (oPart.hasNext()) {
                int[] oL = oPart.next(); // is point inside line bounding box
                if (Utility.isInside(nX, nY, oL[3], oL[2], oL[1], oL[0], nTol)) {
                    int nSqDist = Utility.getPerpDist(nX, nY, oL[0], oL[1], oL[2], oL[3]);
                    if (nSqDist >= 0 && nSqDist <= nSqTol && nSqDist < nDist)
                        nDist = nSqDist; // reduce to next smallest distance
                }
            }
        }

        if (nDist == Integer.MAX_VALUE) // point did not intersect with line
            nDist = Integer.MIN_VALUE;
        return nDist;
    }
}
