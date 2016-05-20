/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;


public class NDFDSky extends NDFDFile
{
	NDFDSky()
	{
		m_sObsTypes = new String[]{"Sky"};
		m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/NDFD/sky/";
		m_sSrcFile = "ds.sky.bin";
		init();
	}
	
	
	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDSky oNDFDSky = new NDFDSky();
		System.out.println(oNDFDSky.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}