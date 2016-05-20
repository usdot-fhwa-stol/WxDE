/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;


public class NDFDTemp extends NDFDFile
{	
	NDFDTemp()
	{
		m_sObsTypes = new String[]{"Air Temperature"};
		m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/NDFD/temp/";
		m_sSrcFile = "ds.temp.bin";
		init();
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDTemp oNDFDTemp = new NDFDTemp();
		System.out.println(oNDFDTemp.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}