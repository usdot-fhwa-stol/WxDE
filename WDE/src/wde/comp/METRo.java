/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.comp;

import java.text.SimpleDateFormat;
import wde.cs.ext.Radar;

/**
 *
 * @author aaron.cherney
 */
public class METRo
{
	static int m_nGridHor = 3500;
	static int m_nGridVer = 7000;
	static int m_nNumOfRadarFiles = 1;
	static boolean[][] m_bPresenceOfPrecip = new boolean[3500][7000];
	static SimpleDateFormat oHour = new SimpleDateFormat("HH':'mm");
	
	METRo()
	{
	}
	
	private static void getPresenceOfPrecip(long lTimeStamp)
	{
		for (int i = 0;i < m_nGridHor;i++)
		{
			for(int j =0; j < m_nGridVer;j++)
			{
				for(int h = 0;h < m_nNumOfRadarFiles;h++)
				{
					if(Radar.getInstance().getReading(0, lTimeStamp - h * 120000, -10000 * i + 54995000, 10000 * j - 129995000) > 0)
					{
						m_bPresenceOfPrecip[i][j] = true;
						System.out.println(Radar.getInstance().getReading(0, lTimeStamp - h, -10000 * i + 54995000, 10000 * j - 129995000));
						break;
					}
				}
				System.out.println(Integer.toString(i+1) + " " + Integer.toString(j+1) + " " + m_bPresenceOfPrecip[i][j] + " " + Integer.toString(-10000 * i + 54995000) + " " + Integer.toString(10000 * j - 129995000));
			}
		}
	}
	
	public static void main(String[] sAgrs)
	{
		Radar.getInstance();
		getPresenceOfPrecip(System.currentTimeMillis());
	}
}
