/**
 * Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
 * 
 * Author: 	n/a
 * Date: 	n/a
 * 
 * Modification History:
 *		dd-Mmm-yyyy		iii		[Bug #]
 *			Change description.
 *
 * 29-Jun-2012			das		
 * 		Added support for configuration tokens so that configuration of
 * 		the application can be done in a single file.
 */
package util;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a means of parsing configuration files, gathering the entries and
 * their associated values, storing them until the next call to
 * {@link Config#refresh() } for later retrieval.
 */
public class Config
{
	/**
	 * Name of the configuration file.
	 */
	private String m_sFilename;
	/**
	 * Array of key-mapped config entry lists.
	 */
	private ArrayList<ConfigEntry> m_oEntries = new ArrayList<ConfigEntry>();
	
	/**
	 * Map of configuration tokens to replace tokenized configuration properties.
	 */
	private Map<String,String> m_tokens = new HashMap<String,String>();

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Config}
	 * </p>
	 */
	protected Config()
	{
	}
	

	/**
	 * <b> Constructor </b>
	 * <p>
	 * Sets the filename attribute to the provided value, and calls
	 * {@link Config#refresh() }.
	 * </p>
	 * @param sFilename file for this instance of {@code Config}
	 * @param oTokens tokens used for configuration replacement
	 */
	Config(String sFilename, Map<String,String> oTokens)
	{
		m_sFilename = sFilename;
		m_tokens = oTokens;
		refresh();
	}


	/**
	 * Clears the configuration values, and reads the configuration file,
	 * ignoring commented lines (lines beginning with #). Adds the configuration
	 * entries, and their corresponding values as they're encountered to the
	 * list of configuration entries.
	 */
	public void refresh()
	{
		// clear any existing configuration values
		int nIndex = m_oEntries.size();
		while (nIndex-- > 0)
			m_oEntries.get(nIndex).clear();

		try
		{
			BufferedReader oBufferedReader =
				new BufferedReader(new FileReader(m_sFilename));

			// ignore blank and comment # lines
			String sLine = null;
			ConfigEntry oConfigEntry = null;
			while ((sLine = oBufferedReader.readLine()) != null)
			{
				if (sLine.length() > 0 && !sLine.startsWith(ConfigSvc.COMMENT))
				{
					// skip reading the line when no key-value separator found
					int nEqualIndex = sLine.indexOf("=");
					if (nEqualIndex < 0)
						continue;
					
					String sKey = sLine.substring(0, nEqualIndex);
					nIndex = Collections.binarySearch(m_oEntries, sKey);
					if (nIndex < 0)
					{
						oConfigEntry = new ConfigEntry(sKey);
						m_oEntries.add(~nIndex, oConfigEntry);
					}
					else
						oConfigEntry = m_oEntries.get(nIndex);

					String confVal = sLine.substring(++nEqualIndex);
					
					// check if the config value has a tokens which should be surrounded
					// if so replace it with the token value.
					// e.g. @@tokenValue@@
					for (String token : m_tokens.keySet()) {
						confVal = confVal.replace(ConfigSvc.TOKEN_CHAR + token + ConfigSvc.TOKEN_CHAR, m_tokens.get(token));
					}
					
					
					oConfigEntry.add(confVal);
				}
			}
		}
		catch (Exception oException)
		{
			Log.getInstance().write(oException);
		}
	}


	/**
	 * Retrieves the first value contained in the configuration entry list
	 * corresponding to the provided key.
	 *
	 * @param sKey key corresponding to the configuration entry list of
	 * interest.
	 * @param nDefault default configuraion value
	 * @return either the first value of the configuration entry list, or the
	 * default value  if the configuration list couldn't be found, or the
	 * retrieved value is empty.
	 */
	public int getInt(String sKey, int nDefault)
	{
		try
		{
			return Integer.parseInt(getString(sKey));
		}
		catch (Exception oException)
		{
		}

		return nDefault;
	}


	/**
	 * Retrieves the first value contained in the configuration entry list
	 * corresponding to the provided key.
	 *
	 * @param sKey key corresponding to the configuration entry list of
	 * interest.
	 * @param lDefault default configuraion value
	 * @return either the first value of the configuration entry list, or the
	 * default value  if the configuration list couldn't be found, or the
	 * retrieved value is empty. 
	 */
	public long getLong(String sKey, long lDefault)
	{
		try
		{
			return Long.parseLong(getString(sKey));
		}
		catch (Exception oException)
		{
		}

		return lDefault;
	}


	/**
	 * Retrieves the first value contained in the configuration entry list
	 * corresponding to the provided key.
	 *
	 * @param sKey key corresponding to the configuration entry list of
	 * interest.
	 * @param sDefault default configuraion value
	 * @return either the first value of the configuration entry list, or the
	 * default value  if the configuration list couldn't be found, or the
	 * retrieved value is empty.
	 */
	public String getString(String sKey, String sDefault)
	{
		String sValue = getString(sKey);
		if (sValue == null || sValue.length() == 0)
			return sDefault;

		return sValue;
	}


	/**
	 * Retrieves the configuration entry array corresponding to the provided
	 * key.
	 * @param sKey key corresponding to the configuration entry list of
	 * interest.
	 * @return the configuration entry array if found, null if either the list
	 * is not found, or it contains no elements.
	 */
	public String[] getStringArray(String sKey)
	{
		ConfigEntry oConfigEntry = getEntry(sKey);
		if (oConfigEntry == null)
			return null;
		
		if (oConfigEntry.size() == 0)
			return null;
		
		String[] sValues = new String[oConfigEntry.size()];
		for (int nIndex = 0; nIndex < oConfigEntry.size(); nIndex++)
			sValues[nIndex] = oConfigEntry.get(nIndex);
			
		return sValues;
	}


	/**
	 * Returns the configuration entry list corresponding to the provided key.
	 * @param sKey key corresponding to the configuration entry list of
	 * interest.
	 * @return the configuration entry if found, null otherwise.
	 */
	private ConfigEntry getEntry(String sKey)
	{
		int nIndex = Collections.binarySearch(m_oEntries, sKey);
		if (nIndex < 0)
			return null;

		return m_oEntries.get(nIndex);
	}


	/**
	 * Retrieves the first entry in the configuration-entry list corresponding
	 * to the provided key.
	 * @param sKey corresponds to the configuration entry list of interest.
	 * @return the first entry in the list if the list is found, othewise null.
	 */
	private String getString(String sKey)
	{
		ConfigEntry oConfigEntry = getEntry(sKey);
		if (oConfigEntry == null)
			return null;

		return oConfigEntry.get(0);
	}
	
	
	/**
	 * Maps a key to an array of values.
	 * <p>
	 * Extends {@code ArrayList} to allow list of values to mapped the
	 * associated key.
	 * </p>
	 * <p>
	 * Implements {@code Comparable} to compare config entry arrays by key.
	 * </p>
	 */
	private class ConfigEntry extends ArrayList<String> 
		implements Comparable<String>
	{
		/**
		 * Key to map to the value array.
		 */
		private String m_sKey;
		

        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code ConfigEntry}
		 * </p>
         */
		private ConfigEntry()
		{
		}
		

		/**
		 * <b> Constructor </b>
		 * <p>
		 * Sets the key to map to the value array.
		 * </p>
		 * @param sKey key to map.
		 */
		private ConfigEntry(String sKey)
		{
			m_sKey = sKey;
		}
		

		/**
		 * Compares <i> this </i> {@code ConfigEntry} by key.
		 * @param sKey key to compare <i> this </i> with.
		 * @return 0 if the keys are equivalent.
		 */
		public int compareTo(String sKey)
		{
			return m_sKey.compareTo(sKey);
		}
	}
}
