/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;


public class NDFDWspd extends NDFDFile
{
	NDFDWspd()
	{
		m_sObsTypes = new String[]{"Wind Speed"};
		m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/NDFD/wspd/";
		m_sSrcFile = "ds.wspd.bin";
		init();
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDWspd oNDFDWspd = new NDFDWspd();
		System.out.println(oNDFDWspd.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}