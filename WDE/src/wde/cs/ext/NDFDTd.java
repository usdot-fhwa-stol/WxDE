/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;


public class NDFDTd extends NDFDFile
{
	NDFDTd()
	{
		m_sObsTypes = new String[]{"Dew Point"};
		m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/NDFD/td/";
		m_sSrcFile = "ds.td.bin";
		init();
	}

	
	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDTd oNDFDTd = new NDFDTd();
		System.out.println(oNDFDTd.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}