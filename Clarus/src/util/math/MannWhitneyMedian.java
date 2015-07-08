package util.math;

//import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import util.Introsort;

/**
 *
 */
public class MannWhitneyMedian
{
	private SortByValue m_oSortValue = new SortByValue();
	private SortByDelta m_oSortDelta = new SortByDelta();
	private ArrayList<Rank> m_oRankCache = new ArrayList<Rank>();
	private ArrayList<Rank> m_oRankEval = new ArrayList<Rank>();


	public MannWhitneyMedian()
	{
	}


	public MannWhitneyMedian(int nSize)
	{
		while (nSize-- > 0)
			m_oRankCache.add(new Rank());
	}


	public synchronized double compare(double[] dSet1, double[] dSet2)
	{
		// copy object references to local stack
		ArrayList<Rank> oRankCache = m_oRankCache;
		ArrayList<Rank> oRankEval = m_oRankEval;

		// copy double values to rank arrays
		internRank(1, dSet1, oRankEval, oRankCache);
		internRank(2, dSet2, oRankEval, oRankCache);

		// sort by value to determine median
		double dMedian = 0.0;
		int nIndex = oRankEval.size();
		// a logic 0 in the LSB indicates an even number
		if ((nIndex & 1) == 0)
		{
			// substitute mean for the median when there is no middle element
			while (nIndex-- > 0)
				dMedian += oRankEval.get(nIndex).m_dValue;

			nIndex = oRankEval.size();
			dMedian /= nIndex;
		}
		else
		{
			// sorting is only necessary when there is an odd number of values
			Introsort.usort(oRankEval, m_oSortValue);
			dMedian = oRankEval.get(oRankEval.size() / 2).m_dValue;
		}

		// calculate each delta value from the median value
		while (nIndex-- > 0)
		{
			Rank oRank = oRankEval.get(nIndex);
			double dValue = oRank.m_dValue;
			if (dValue < dMedian)
				oRank.m_dDelta = dMedian - dValue;
			else
				oRank.m_dDelta = dValue - dMedian;
		}

		// sort by the delta values
		Introsort.usort(oRankEval, m_oSortDelta);

		// find duplicate values and adjust their ranking
		adjustRank(oRankEval);

		// sum the rankings by originating set
		double dR1 = sumRanks(1, oRankEval);
		double dR2 = sumRanks(2, oRankEval);

		// calculate the U statistic for each set
		double dN1 = dSet1.length;
		double dN2 = dSet2.length;
		double dN1N2 = dN1 * dN2; // save one calculation
		double dU1 = dN1N2 + dN2 * (dN2 + 1) / 2.0 - dR2;
		double dU2 = dN1N2 + dN1 * (dN1 + 1) / 2.0 - dR1;

		// calculate the mean and variance of U
		double dUu = dN1N2 / 2.0;
		double dSu2 = dN1N2 * (dN1 + dN2 + 1) / 12.0;

		// calculate the squared z statistic
		double dUd = dU1;
		if (dU1 < dU2)
			dUd = dU2;
		dUd -= dUu;

		// print rank output for debugging
		/*DecimalFormat oFormat = new DecimalFormat("#0.0000");
		try
		{
			nIndex = 0;
			for (nIndex = 0; nIndex < oRankEval.size(); nIndex++)
			{
				System.out.print(oFormat.format(oRankEval.get(nIndex).m_nId));
				System.out.print(',');
			}
			System.out.println();

			for (nIndex = 0; nIndex < oRankEval.size(); nIndex++)
			{
				System.out.print(oFormat.format(oRankEval.get(nIndex).m_dValue));
				System.out.print(',');
			}
			System.out.println();

			for (nIndex = 0; nIndex < oRankEval.size(); nIndex++)
			{
				System.out.print(oFormat.format(oRankEval.get(nIndex).m_dDelta));
				System.out.print(',');
			}
			System.out.println();

			for (nIndex = 0; nIndex < oRankEval.size(); nIndex++)
			{
				System.out.print(oFormat.format(oRankEval.get(nIndex).m_dRank));
				System.out.print(',');
			}
			System.out.println();

			System.out.println(oFormat.format(dUd * dUd / dSu2));
			System.out.println();
		}
		catch (Exception oException)
		{
		}*/

		// save created rank objects to the cache for reuse
		nIndex = oRankEval.size();
		while (nIndex-- > 0)
			oRankCache.add(oRankEval.remove(nIndex));

		return (dUd * dUd / dSu2);
	}


	private void adjustRank(ArrayList<Rank> oRankSet)
	{
		// first, apply the ranking--1-based--to each value object
		int nUpper = oRankSet.size();
		while (nUpper-- > 0)
			oRankSet.get(nUpper).m_dRank = nUpper + 1;

		nUpper = oRankSet.size() - 1;
		while (nUpper > 0)
		{
			Rank oRank = oRankSet.get(nUpper);
			double dRank = oRank.m_dRank;
			double dDelta = oRank.m_dDelta;

			// search backward for the final occurrence of a duplicate value
			int nLower = nUpper;
			while (nLower-- > 0 && (oRank = oRankSet.get(nLower)).m_dDelta == dDelta)
				dRank += oRank.m_dRank;

			// if there is more than one repeated value, calculate the mean
			// rank and apply the same rank to each value object in the range
			if (nUpper - nLower > 1)
			{
				dRank /= (nUpper - nLower);
				while (nUpper > nLower)
					oRankSet.get(nUpper--).m_dRank = dRank;
			}
			else
				--nUpper;
		}
	}


	private double sumRanks(int nId, ArrayList<Rank> oRankSet)
	{
		double dSum = 0.0;
		Rank oRank = null;

		int nIndex = oRankSet.size();
		while (nIndex-- > 0)
		{
			oRank = oRankSet.get(nIndex);
			if (oRank.m_nId == nId)
				dSum += oRank.m_dRank;
		}

		return dSum;
	}


	private void internRank(int nId, double[] dSrc,
		ArrayList<Rank> oDest, ArrayList<Rank> oCache)
	{
		Rank oRank = null;
		int nIndex = dSrc.length;
		while (nIndex-- > 0)
		{
			if (oCache.isEmpty())
				oRank = new Rank();
			else
				oRank = oCache.remove(oCache.size() - 1);

			oRank.init(nId, dSrc[nIndex]);
			m_oRankEval.add(oRank);
		}
	}


	private class Rank
	{
		int m_nId;
		double m_dValue;
		double m_dDelta;
		double m_dRank;


		Rank()
		{
		}


		void init(int nId, double dValue)
		{
			m_nId = nId;
			m_dValue = dValue;
			m_dDelta = Double.NaN;
			m_dRank = Double.NaN;
		}
	}


	private class SortByValue implements Comparator<Rank>
	{
		SortByValue()
		{
		}


		public int compare(Rank oLhs, Rank oRhs)
		{
			if (oLhs.m_dValue < oRhs.m_dValue)
				return -1;

			if (oLhs.m_dValue > oRhs.m_dValue)
				return 1;

			return 0;
		}
	}


	private class SortByDelta implements Comparator<Rank>
	{
		SortByDelta()
		{
		}


		public int compare(Rank oLhs, Rank oRhs)
		{
			if (oLhs.m_dDelta < oRhs.m_dDelta)
				return -1;

			if (oLhs.m_dDelta > oRhs.m_dDelta)
				return 1;

			return 0;
		}
	}


	public static void main(String[] sArgs)
	{
		MannWhitneyMedian oMW = new MannWhitneyMedian(40);

		java.util.Random oRandom = new java.util.Random();
		double[] dSet1 = new double[20];
		int nIndex = 20;
		while (nIndex-- > 0)
			dSet1[nIndex] = oRandom.nextDouble() * 100.0;

		double[] dSet2 = new double[20];
		nIndex = 20;
		while (nIndex-- > 0)
			dSet2[nIndex] = oRandom.nextDouble() * 100.0;

		System.out.println(oMW.compare(dSet1, dSet2));
	}
}
