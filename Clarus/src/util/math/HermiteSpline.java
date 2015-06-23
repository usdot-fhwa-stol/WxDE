package util.math;

/**
 *
 */
public class HermiteSpline
{
	private double m_dM0;
	private double m_dM1;
	private double m_dP0;
	private double m_dP1;


	public HermiteSpline()
	{
	}


	public HermiteSpline(long[] lX, double[] dY)
	{
		setPoints(lX, dY);
	}


	public static double h00(double dT)
	{
		// 2t3 ? 3t2 + 1
		return (2.0 * dT * dT * dT - 3.0 * dT * dT + 1.0);
	}


	public static double h10(double dT)
	{
		// t3 ? 2t2 + t
		return (dT * dT * dT - 2.0 * dT * dT + dT);
	}


	public static double h01(double dT)
	{
		// ?2t3 + 3t2
		return (3.0 * dT * dT - 2.0 * dT * dT * dT);
	}


	public static double h11(double dT)
	{
		// t3 ? t2
		return (dT * dT * dT - dT * dT);
	}


	public static double m(double dPkm1, double dPk, double dPkp1,
		long lXkm1, long lXk, long lXkp1)
	{
		return ((dPkp1 - dPk) / (double)(lXkp1 - lXk) +
			(dPk - dPkm1) / (double)(lXk - lXkm1)) / 2.0;
	}


	public void setPoints(long[] lX, double[] dY)
	{
		m_dP0 = dY[1];
		m_dP1 = dY[2];
		m_dM0 = m(dY[0], dY[1], dY[2], lX[0], lX[1], lX[2]);
		m_dM1 = m(dY[1], dY[2], dY[3], lX[1], lX[2], lX[3]);
	}


	public double estimateValue(double dT)
	{
		// request index should have been scaled into the range 0.0 <= t < 1.0
		return (h00(dT) * m_dP0 + h10(dT) * m_dM0 +
			h01(dT) * m_dP1 + h11(dT) * m_dM1);
	}
}
