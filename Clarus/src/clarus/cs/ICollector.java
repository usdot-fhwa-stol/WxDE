// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ICollector.java
 */

package clarus.cs;

import util.net.NetConn;

/**
 * Interface implemented by any class that performs the collection
 * and processing of observations from contributing agencies.
 */
public interface ICollector
{
	/**
	 * The <tt>CollectorSvc</tt> calls this method to initiate regular
	 * collection of observation data from a contributing agency. The
	 * collection time is based on a configurable schedule.
	 * 
	 * @param nContribId configuration database reference used to
	 *        identify a contributor data source
	 * @param oNetConn wrapped network connection to the observation data source
	 * @param lTimestamp the scheduled time when this method was called
	 */
	public void collect(int nContribId, NetConn oNetConn, long lTimestamp);
}
