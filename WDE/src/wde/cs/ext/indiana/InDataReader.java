package wde.cs.ext.indiana;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import static wde.cs.ext.indiana.Obstypes.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "°"
 *
 * @author scot.lange
 */
public class InDataReader
{

  private static final FilenameFilter m_iFileNameFilter = new FilenameFilter()
  {
    @Override
    public boolean accept(File oDir, String sName)
    {
      return sName.endsWith(".csv");
    }
  };

  Map<String, String> m_oColumnObstypes = new HashMap<>();

  /**
   * @param args
   *          the command line arguments
   */
  public static void main(String[] args) throws IOException
  {
    processDirectory(new File(args[ 0 ]), new File(args[ 1 ]));
    // processDirectory(new File("E:/IN-Data"), new File("E:/test-out.csv"));

  }

  public static void processDirectory(File oDirectory, File oOutputFile) throws FileNotFoundException, IOException
  {
    Map<String, String> oColumnObstypes = getColumnObstypes();
    Map<String, List<String>> oObstypeValues = new HashMap<>();
    String[] sOutputObstypes = getOutputObstypes();

    Map<String, String> oObstypeNames = getObstypeNames();

    try(PrintWriter oOut = new PrintWriter(oOutputFile))
    {

      oOut.print("station,date,sensor");
      for(String sObstype : sOutputObstypes)
        oOut.append(",").append(oObstypeNames.get(sObstype));

      for(File oFile : oDirectory.listFiles(m_iFileNameFilter))
      {
        for(List<String> oObsList : oObstypeValues.values())
          oObsList.clear();

        int nMaxSensorCount = 0;

        String sStation = oFile.getName().substring(0, oFile.getName().length() - 4);
        String sHeader;
        String sLine;
        String sLastLine = null;
        try(BufferedReader oReader = new BufferedReader(new FileReader(oFile)))
        {
          sHeader = oReader.readLine();
          while((sLine = oReader.readLine()) != null)
            sLastLine = sLine;
        }

        if(sLastLine == null)
          continue;

        String[] sColumns = sHeader.split(",");
        String[] sValues = sLastLine.split(",");
        String sDate = null;
        int nIndex = Math.min(sColumns.length, sValues.length);
        while(--nIndex >= 0)
        {
          String sColumn = sColumns[ nIndex ];
          String sValue = sValues[ nIndex ].trim();
          if(sColumn.equalsIgnoreCase("date_time"))
          {
            sDate = sValue;
            continue;
          }

          if(sValue.isEmpty())
            continue;

          sColumn = oColumnObstypes.get(sColumn);
          List<String> oCurrentValues = oObstypeValues.get(sColumn);
          if(oCurrentValues == null)
          {
            oCurrentValues = new ArrayList<>();
            oObstypeValues.put(sColumn, oCurrentValues);
          }

          oCurrentValues.add(sValue);
          nMaxSensorCount = Math.max(nMaxSensorCount, oCurrentValues.size());
        }

        int nSensorIndex = nMaxSensorCount;
        while(--nSensorIndex >= 0)
        {
          oOut.println();
          oOut.append(sStation).append(",").append(sDate).append(",");
          oOut.print(nSensorIndex);

          for(String sObstype : sOutputObstypes)
          {
            oOut.print(",");

            List<String> oValues = oObstypeValues.get(sObstype);
            if(oValues != null && oValues.size() > nSensorIndex)
              oOut.print(oValues.get(nSensorIndex));
          }
        }
      }
    }

  }

  public static void writeColumnInventory(File oDirectory, File oOutputFile)
      throws FileNotFoundException, IOException
  {
    Set<String> oColumns = new HashSet<>();
    List<FileSummary> oDataFiles = new ArrayList<>();

    for(File oFile : oDirectory.listFiles())
    {
      String sHeader;
      try(BufferedReader oReader = new BufferedReader(new FileReader(oFile)))
      {
        sHeader = oReader.readLine();
      }

      Map<String, Integer> oColumnCounts = new HashMap<>();
      for(String sColumn : sHeader.split(","))
      {
        Integer nCount = oColumnCounts.get(sColumn);
        if(nCount == null)
        {
          oColumns.add(sColumn);
          nCount = 0;
        }

        oColumnCounts.put(sColumn, nCount + 1);
      }
      oDataFiles.add(new FileSummary(oFile.getName(), oColumnCounts));
    }

    List<String> oSortedColumns = new ArrayList(oColumns);
    Collections.sort(oSortedColumns);

    try(PrintStream oOut = new PrintStream(oOutputFile))
    {
      oOut.append("Column");
      for(FileSummary file : oDataFiles)
        oOut.append(",").append(file.getFileName());

      for(String sColumn : oSortedColumns)
      {
        oOut.println();
        oOut.print(sColumn);
        for(FileSummary file : oDataFiles)
        {
          oOut.print(",");
          Integer nValue = file.getColumns().get(sColumn);
          oOut.print(nValue == null ? 0 : nValue);
        }
      }
    }
  }

}
