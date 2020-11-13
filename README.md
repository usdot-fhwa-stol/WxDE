# Project Description

The Weather Data Environment (WxDE) is a research project that collects and shares transportation-related weather data with a particular focus on weather data related to connected vehicle applications. The WxDE collects data in real time from both fixed environmental sensor stations and mobile sources. The WxDE computes value-added enhancements to this data, such as by computing quality-check values for observed data and computing inferred weather parameters from vehicle data (e.g., inferring precipitation based on windshield wiper activation). The WxDE archives both collected and computed data. The WxDE supports subscriptions for access to real-time data in near real time generated by individual weather-related connected vehicle projects.

# Prerequisites

Requires:
- Linux Server
- Java 7
- Apache Tomcat Webserver
- A jdbc compatible database

# Usage

## Building
The Weather Data Environment can be built and deployed on a Linux server by following these steps:

* Download source code and open in an IDE (NetBeans or Eclipse) to compile the Java code
* Download and install a jdbc compatible database on Linux server
* Create wxde database user
* Download and install jdbc driver for the selected database
* Run SQL scripts included with source code to initalize database
* Download, install and configure Apache Tomcat Webserver. Click [here for documentation](https://tomcat.apache.org/download-90.cgi)
* Create tomcat user on operating system and update file permissions based on tomcat's [recommendations](https://tomcat.apache.org/tomcat-9.0-doc/security-howto.html)
* Update tomcat server.xml file to include Host, Realm (for user authencation), and Resource (for user database) entries.
* Update tomcat web.xml to include servlet definitions
* Copy web files included with source code into the desired tomcat webapps folder
* Move compiled .class files into the tomcat classes directory
* Create data directory and metadata, reports, and subscriptons subdirectories in the tomcat webapps folder
* Install collector-scripts.tar.gz
* Add entries to the crontab to schedule regular execution of external collector scripts


## Testing

There are no test cases.

## Execution

Once everything is configured for the deployment, the following command can be ran to start the application:
```
sudo -u tomcat <path to tomcat>/bin/catalina.sh start
```

# Additional Notes

Accounts may have to be setup with agencies and vendors to receive a username and password to collect data.

## Creating/updating metadata

1. Determine the contributor id of the agency that is being updated by referencing the meta.contrib table.

	Example query to determine Georgia's contributor id:  
	```
	SELECT name, id FROM meta.contrib WHERE name ilike '%ga%';
	```
2. Edit the meta.site table. Sites describe the general location of platforms and sensors. The following attributes need to be included:
	- id: unique system site id (integer)
	- staticid: another system id, usually the same as the id attribute (integer)
	- updatetime: timestamp of when the update was made (timestamp, can use form 'yyyy-MM-dd')
	- statesiteid: id used by the agency for the site (character varying(50))
	- contribid: contributor id of the deploying agency (integer)
	- description: a short description of the location of the site (character varying(50))
	- state: the abbreviation of the state the site is located in (character varying(2))
	- country: the abbreviation of the country the site is located in (character varying(3))
	
Example query adding a site located in Oregon:
```
INSERT INTO meta.site (id, staticid, updatetime, statesiteid, contribid, description, state, country) VALUES (400047, 400047, '2020-07-15', '24023', 40, 'OR 140 @ Nevada State Line (OR 140 MP 64.24)', 'OR', 'USA');
```

3. Edit the meta.platform table. Platforms describe the physical attributes of sensor locations and whether the sensors are stationary or mobile. The following attributes need to be included:
	- id: unique system platform id (integer)
	- staticid: another system id, usually the same as the id attribute (integer)
	- updatetime: timestamp of when the update was made (timestamp, can use form 'yyyy-MM-dd') 
	- platformcode: agency id contained in data files to associate data to this platform
	- category: category of the platform (character(1) P = permanent, M = mobile)
	- description: description of the platform (character varying(255))
	- contribid: contributor id of the deploying agency (integer)
	- siteid: system id of the site associated to this platform (integer)
	- locbaselat: latitude of the platform in decimal degrees, if not mobile (numeric(18,9))
	- locbaselong: longitude of the platform in decimal degrees, if not mobile (numeric(18,9))
	- locbaseelev: elevation of the platform in meters, if not mobile (numeric(18,9))

Example query adding a platform to the site added in step 2:
```
INSERT INTO meta.platform (id, staticid, updatetime, platformcode, category, description, contribid, siteid, locbaselat, locbaselong, locbaseelev) VALUES (400047, 400047, '2020-07-15', '24023', 'P', 'OR 140 @ Nevada State Line (OR 140 MP 64.24)', 40, 400047, 42.0040100, -119.3352100, 1887);
```

4. Determine the type of sensor(s) being used on a platform by referencing the meta.sensortype table. If a sensor type does not exist, create an entry using the following attributes:
	- id: unique system sensortype id (integer)
	- staticid: another system id, usually the same as the id attribute (integer)
	- updatetime: timestamp of when the update was made (timestamp, can use form 'yyyy-MM-dd')
	- mfr: manufacturer of the sensor (character varying(50))
	- model: model of the sensor (character varying(50))
	
Example query adding a sensor type manufactured by High Sierra Electronics:
```
INSERT INTO meta.sensortype (id, staticid, updatetime, mfr, model) VALUES (436, 436, '2017-11-14', 'High Sierra Electronics', 'Mobile Ice Sight 5435-00');
```

5. For each combination of sensortype and observation type being collected, determine the quality checking parameter id to be used for that sensor by referencing the meta.qchparm table.

Example query to determine the quality checking parameter id for the sensor added in step 4 collecting air temperature
```
SELECT id FROM meta.qchparm WHERE sensortypeid = 436 AND obstypeid = 5733;
```


6. Edit the meta.sensor table. Sensors describe a single observation type being collected at a platform. The following attributes need to be included:
	- id: unique system sensor id (integer)
	- sourceid: id of the data source (integer 1 = 'WxDE')
	- staticid: another system id, usually the same as the id attribute (integer)
	- updatetime: timestamp of when the update was made (timestamp, can use form 'yyyy-MM-dd')
	- platformid: system id of the platform associated to this sensor
	- contribid: contributor id of the deploying agency (integer)
	- sensorindex: index of the sensor at the associated platform. Can be 0 or 1 based, needs to match the indices found in the data files (integer)
	- obstypeid: id of the observation type being collected. These ids are stored in meta.obstype (integer)
	- qchparmid: quality checking parameter id associated with the sensor and observation type (integer)
	- distgroup: distribution group id. Should be 2 for most cases (integer)
	
Example query adding a sensor at the platform added in step 3:
```	
INSERT INTO meta.sensor (id, sourceid, staticid, updatetime, platformid, contribid, sensorindex, obstypeid, qchparmid, distgroup) VALUES (40004700, 1, 40004700, '2020-07-15', 400047, 40, 0, 5733, 9, 2);
```

## Creating/editing collectors

1. Determine the contributor id of the agency that is being updated by referencing the meta.contrib table.

Example query to determine Georgia's contributor id:
```
SELECT name, id FROM meta.contrib WHERE name ilike '%ga%';
```

2. Edit the conf.csvc table. Collector services keep track the necessary data to access an agency's data feed(s). The following attributes need to be included:
	- id: unique system collector service id (integer)
	- active: flag indicating if the collector is active (smallint 1 = active)
	- contribid: comma separated list of contributor ids associated with this collector service (character varying(12))
	- midnightoffset: the midnight offset, in seconds, used alongside the collection interval to schedule regular collection (smallint)
	- collectioninterval: the interval, in seconds, to wait until attempting to collect data from this source on a regular basis (smallint)
	- instancename: the name given to this collector service (character varying(32))
	- classname: Java class used to instantiate this collector service (character varying(255))
	- endpoint: the base URL or file path used to collect the data
	- username: user name for login credentials, if applicable (character varying)
	- password: password for login credentials, if applicable (character varying)
	
Example queries adding collector services for the state of Oregon:
```
INSERT INTO conf.csvc (id, active, contribid, midnightoffset, collectioninterval, instancename, classname, endpoint) VALUES (4001, 1, '40', 180, 1200, 'USA/OR/file', 'wde.cs.ascii.CsvSvc', 'file:///home/wxde/collectors//');
INSERT INTO conf.csvc (id, active, contribid, midnightoffset, collectioninterval, instancename, classname, endpoint) VALUES (4002, 1, '40', 180, 1200, 'USA/OR/url', 'wde.cs.ascii.CsvSvc', 'odot.gov/dataportal', 'wxde-user', 'pw1234');
```

3. Edit the conf.csvcollector table for CSV data sources or the conf.xmlcollector table for XML data sources. The following attributes need to be included for both tables:
	- id: unique system collector id (integer)
	- csvcid: the collector service id associated with this collector (integer)
	- collectdelay: the number of seconds to delay at the beginning of each collection interval (smallint)
	- retryflag: flag indicating if the collector should retry collection if an error occurs (smallint)
	- collecttzid: system timezone id (which can be found in conf.timezone) used in the file names of the collector (integer)
	- contenttzid: system timezone id (which can be found in conf.timezone) used by the content inside of the files of the collector (integer)
	- timestampid: system timestamp format id (which can be found in conf.timestampformat) used by the content inside of the files of the collector (integer)
	- filepath: this is appended to the endpoint of the associated collector service to create the full path of the file collected. This is a String used to create a [SimpleDateFormat](https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html) object in Java so proper syntax needs to be followed (character varying(255))

	The following attributes are specific to the conf.csvcollector table:
	- skiplines: the number of lines to skip at the beginning of a file, usually to skip header information (smallint)
	- delimiter: the delimiter used to separate columns in the data file, if no value is given a ',' will be used (character varying(2))
	- newline: the delimiter used for newlines in the data file, if no value is given '\n' will be used (character varying(2))
	- stationcode: the id used to associate the file to a single platform within the system, if applicable
	
	The following attributes are specific to the conf.xmlcollector table:
	- defid: system id to associate entries in the conf.xmldef table to this collector (integer)
	- defaultsensorindex: the default sensor index to use for this collector (integer)
	
	Example queries adding collectors associated with the collector services added in step 2:
	```
	INSERT INTO conf.csvcollector (id, csvcid, collectdelay, retryflag, collecttzid, contenttzid, timestampid, skiplines, filepath) VALUES (400101, 4001, 0, 0, 0, -8001, 17, 1, 'OR/or.csv');
	INSERT INTO conf.csvcollector (id, csvcid, defid, collectdelay, retryflag, collecttzid, contenttzid, timestampid, defaultsensorindex, filepath) VALUES (400201, 4002, 11, 0, 0, 0, -8001, 17, 1, 0, '/data'yyyyMMdd_HHmm'.xml');
	```

4. For CSV data sources, edit the conf.csvcoldef. CSV column definitions are used to associate observations with a sensor in the system. The following attributes need to be included:
	- collectorid: system csvcollector id associated with this column definition (integer)
	- columnid: the 0 based index of the column definition in a line of the data file (integer)
	- obstypeid: system observation type id associated to the data value in this column. Observation type ids can be found in the meta.obstype table. Use 0 if the column does not describe a data value, like a timestamp or platformid (integer)
	- multiplier: multiplier applied to the value in this column to get the correct value (double)
	- sensorindex: sensor index used to associate data to correct sensor in system, if applicable (integer)
	- classname: Java class used to instantiate this column (character varying(255))
	- unit: unit associated with the value in this column (character varying(8))
	- ignorevalues: semicolon separated list of values to ignore in this column (character varying(128))

Example queries adding CSV column definitions associated with the collector added in step 3:	
```
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 0, 0, NULL, wde.cs.ascii.PlatformCode, NULL, NULL);
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 1, 0, NULL, wde.cs.ascii.SensorId, NULL, NULL);
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 2, 0, NULL, wde.cs.ascii.Timestamp, NULL, NULL);
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 3, 0, NULL, wde.cs.ascii.Timestamp, NULL, NULL);
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 4, 5733, NULL, wde.cs.ascii.DataValue, 'C', '1001');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 5, 5101, NULL, wde.cs.ascii.DataValue, 'm', '1000001');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 6, 56105, NULL, wde.cs.ascii.MappedValue, 'deg', '361');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 7, 56104, NULL, wde.cs.ascii.DataValue, 'km/h, '65535');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 8, 56109, NULL, wde.cs.ascii.MappedValue, 'deg', '361');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 9, 56108, NULL, wde.cs.ascii.DataValue, 'km/h', '65535');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 10, 207, NULL, wde.cs.ascii.MappedValue, NULL, 'No Com;No Data');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 11, 581, NULL, wde.cs.ascii.DataValue, '%', '101');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 12, 511313, NULL, wde.cs.ascii.DataValue, 'C', '1001');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 13, 587, NULL, wde.cs.ascii.DataValue, 'cm/h', '65565');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 14, 589, NULL, wde.cs.ascii.MappedValue, NULL, NULL);
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 15, 511316, 0.01, wde.cs.ascii.DataValue, 'mm', '255;NaN');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 16, 51138, NULL, wde.cs.ascii.DataValue, 'C', '1001');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 17, 554, NULL, wde.cs.ascii.DataValue, 'mbar', '65535');
INSERT INTO conf.csvcoldef (collectorid, columnid, obstypeid, multiplier, classname, unit, ignorevalues) VALUES (400101, 18, 575, NULL, wde.cs.ascii.DataValue, 'C', '1001');
```

5. For XML data sources, edit the conf.xmldef. XML definition are used to associate observation with a sensor in the system. The following attributes need to be included:
	- collectorid: system defid from conf.xmlcollector table associated with this XML definition (integer)
	- pathid: sequential id for XML definitions in the same file (integer)
	- obstypeid: system observation type id associated to the data value in this definition. Observation type ids can be found in the meta.obstype table. Use 0 if the definition does not describe a data value, like a timestamp or platformid (integer)
	- multiplier: multiplier applied to the value in this definition to get the correct value (double)
	- classname: Java class used to instantiate this definition (character varying(255))
	- unit: unit associated with the value in this definition (character varying(8))
	- ignorevalues: semicolon separated list of values to ignore in this definition (character varying(128))
	- xmlpath: serialized xml path of tags and attributes for this definition (character varying(256))
	
Example XML described by the xmlpath '/data/station/value/code/temp':
```
<data>
 <station>
   <value code="temp">21.3</value>
 </station>
</data>
```

Exmaple query adding XML definition associated with the collector added in step 3:
```
INSERT INTO conf.xmldef (collectorid, pathid, obstypeid, multiplier, classname, unit, ignorevalues, xmlpath) VALUES (11, 1, 5733, NULL, 'wde.cs.xml.DataValue', 'C', NULL, '/data/station/value/code/temp');
```

# Version History and Retention

**Status:** This project is in the release phase

**Release Frequency:** This project currently has no plans for updates

**Release History: See [CHANGELOG.md](CHANGELOG.md)**

**Retention:** This project will remain publicly accessible for the foreseeable future

# License

This project is licensed under the Creative Commons 1.0 Universal (CC0 1.0) License - see the [LICENSE.md](LICENSE.md) for more details. 

# Contributions

Please read [CONTRIBUTING.md](CONTRIBUTING.MD) for details on our Code of Conduct, the process for submitting pull requests to us, and how contributions will be released.

# Contact Information

Contact Name: Federal Highway Administration

Contact Information: [Federal Highway Adminstration](http://www.fhwa.dot.gov/) | [Turner-Fairbank Highway Research Center](http://www.fhwa.dot.gov/research/tfhrc/)

# Acknowledgements

Please site the Federal Highway Administration instance of the Weather Data Environment if using this code: [https://wxde.fhwa.dot.gov/](https://wxde.fhwa.dot.gov/)
