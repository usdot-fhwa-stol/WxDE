package wde.qchs.algo;

import java.util.ArrayList;

import wde.data.shp.Polyline;
import wde.obs.IObs;
import wde.qchs.Roads;

/**
 *
 * @author bryan.krueger
 */
public class VehNeighbor extends IQR
{
	public VehNeighbor()
	{
	}


	/**
	 * Filter observation set for vehicles on the same pavement as the target
	 *
	 * @param nLat    latitude of the target observation.
	 * @param nLon    longitude of the target observation.
	 * @param oObsSet thread-local observation set to be filtered.
	 */
	@Override
	protected void filter(int nLat, int nLon, ArrayList<IObs> oObsSet)
	{
		Polyline oLink = Roads.getInstance().getLink(0.0001, nLon, nLat);
		if (oLink == null) // no link found for target
			oObsSet.clear();
		else
		{
			int nIndex = oObsSet.size();
			while (nIndex-- > 0) // reduce unnecessary copy operations upon removal
			{
				IObs iObs = oObsSet.get(nIndex);
				if (!oLink.contextSearch(0.0001, iObs.getLongitude(), iObs.getLatitude()))
					oObsSet.remove(nIndex); // remove observtions from set not on link
			}
		}
	}
}
