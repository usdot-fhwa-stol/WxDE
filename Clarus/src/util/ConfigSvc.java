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
 * 
 */

/**
 * @file ConfigSvc.java
 */
package util;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Provides a means of retrieving the configurations from the configuration 
 * directory of various object, by object type or name.
 *
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link ConfigSvc#getInstance()}
 * </p>
 */
public class ConfigSvc
{
	/**
	 * Configuration root location.
	 */
	private static String CONFIG_ROOT = "./";
	/**
	 * Comment marker, ignores lines prefaced with this string.
	 */
	static String COMMENT = "#";
	/**
	 * Line delimiter.
	 */
	static String SEPARATOR = ",";
	
	/**
	 * Token character
	 */
	static String TOKEN_CHAR = "@@";
	
	/**
	 * Pointer to the singleton instance of {@code ConfigSvc}.
	 */
	private static ConfigSvc g_oInstance = new ConfigSvc();
	
	/**
	 * Default properties. This is used to store default properties that start with "conf".
	 */
	private Map<String,String> g_tokens = new HashMap<String,String>();
	
	/**
	 * <b> Accessor </b>
	 * @return the singleton instance of {@code ConfigSvc}.
	 */
	public static ConfigSvc getInstance()
	{
		return g_oInstance;
	}
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Updates the configuration root directory location, comment marker, and
	 * line delimiter.
	 * </p>
	 */
	private ConfigSvc()
	{
		try
		{
			// the class name periods need to be replaced with underscores 
			// so the resource bundle does not try to load a class instead
			String sRootConfig = getClass().getName().replace('.', '_');
			
			// update the configuration file root directory
			ResourceBundle oConfig = ResourceBundle.getBundle(sRootConfig);

			
			String sRoot = oConfig.getString("root");
			if (sRoot != null && sRoot.length() > 0)
				CONFIG_ROOT = sRoot;
			
			// If the comment was not found in the properties then an exception will
			// be thrown. Since this is optional configuration do not throw an exception.
			try { 
				String sComment = oConfig.getString("comment");
				if (sComment != null && sComment.length() > 0)
					COMMENT = sComment;
			} catch (Exception e) {
			}
			
			
			// If the separator was not found in the properties then an exception will
			// be thrown. Since this is optional configuration do not throw an exception.
			try { 
				String sSeparator = oConfig.getString("separator");
				if (sSeparator != null && sSeparator.length() > 0)
					SEPARATOR = sSeparator;
			} catch (Exception e) {
			}
			
			// Get the configuration tokens. These are properties that start with "conf".
			try { 
				g_tokens = new HashMap<String,String>();
				Enumeration configKeys = oConfig.getKeys();
				if ( configKeys != null ) {
				   while (configKeys.hasMoreElements()) {
					   String key = (String) configKeys.nextElement();
					   if ( key != null && key.startsWith("conf.") ) {
						   g_tokens.put(key.substring(5), oConfig.getString(key));
					   }
				   }
				}
			} catch (Exception e) {
			}
			
		}
		catch (Exception oException)
		{
		}
		
		// append additional slash if one is not present
		if (!CONFIG_ROOT.endsWith("/"))
			CONFIG_ROOT += "/";
	}


	/**
	 * Gets a {@code Config} object that's pointing to the configuration of the
	 * provided object.
	 * @param oObject object whose configuration to retrieve.
	 * @return a new {@code Config} instance pointing to the configuration of
	 * the supplied object type.
	 */
	public Config getConfig(Object oObject)
	{
		return getConfig(oObject.getClass().getName());
	}


	/**
	 * Gets the configuration with the provided name.
	 * @param sName name of the configuration of interest.
	 * @return new instance of {@link Config} pointing to the configuration
	 * file of the supplied name.
	 */
	public Config getConfig(String sName)
	{
		return new Config(CONFIG_ROOT + sName,g_tokens);
	}
}
