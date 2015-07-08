-- Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
-- 
-- This DDL creates the databases and tables needed for Clarus.
--
-- Modification History:
--    dd-Mmm-yyyy	iii	[Bug #]
--       Change description.
--


CREATE DATABASE clarus_conf;
CREATE DATABASE clarus_meta;
CREATE DATABASE clarus_qedc;
CREATE DATABASE clarus_subs;

USE clarus_conf;

CREATE TABLE `cmmlmapping` (
  `id` int(11) NOT NULL DEFAULT '0',
  `obsTypeName` varchar(50) DEFAULT NULL,
  `category` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=561011 DEFAULT CHARSET=latin1;

CREATE TABLE `csvc` (
  `id` int(11) NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `contribId` int(11) NOT NULL,
  `midnightOffset` smallint(6) NOT NULL DEFAULT '0',
  `collectionInterval` smallint(6) NOT NULL,
  `instanceName` varchar(32) NOT NULL,
  `className` varchar(128) NOT NULL,
  `endpoint` varchar(255) NOT NULL,
  `username` varchar(32) DEFAULT NULL,
  `password` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

CREATE TABLE `csvcoldef` (
  `collectorId` int(11) NOT NULL,
  `columnId` int(11) NOT NULL,
  `obsTypeId` int(11) NOT NULL,
  `sensorIndex` int(11) DEFAULT NULL,
  `colWidth` int(11) DEFAULT NULL,
  `multiplier` double DEFAULT NULL,
  `className` varchar(128) NOT NULL,
  `unit` varchar(8) DEFAULT NULL,
  `ignoreValues` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`collectorId`,`columnId`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;


CREATE TABLE `csvcollector` (
  `id` int(11) NOT NULL,
  `csvcId` int(11) NOT NULL,
  `collectDelay` smallint(6) NOT NULL DEFAULT '0',
  `retryFlag` tinyint(1) NOT NULL DEFAULT '0',
  `collectTzId` int(11) NOT NULL,
  `contentTzId` int(11) NOT NULL,
  `timestampId` int(11) NOT NULL,
  `skipLines` tinyint(4) NOT NULL DEFAULT '1',
  `delimiter` varchar(2) DEFAULT NULL,
  `newline` varchar(2) DEFAULT NULL,
  `stationCode` varchar(50) DEFAULT NULL,
  `filepath` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


CREATE TABLE `obsvaluemap` (
  `obsTypeId` int(11) NOT NULL,
  `obsType` varchar(50) NOT NULL,
  `valueLabel` varchar(32) NOT NULL,
  `value` double DEFAULT NULL,
  PRIMARY KEY (`obsTypeId`,`valueLabel`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


CREATE TABLE `qchconfig` (
  `id` int(11) NOT NULL,
  `timerangeMin` int(11) DEFAULT NULL,
  `timerangeMax` int(11) DEFAULT NULL,
  `geoRadiusMin` int(11) DEFAULT NULL,
  `geoRadiusMax` int(11) DEFAULT NULL,
  `sdMin` double DEFAULT NULL,
  `sdMax` double DEFAULT NULL,
  `obsCountMin` int(11) DEFAULT NULL,
  `testName` varchar(45) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE `qchseq` (
  `qchseqmgrId` int(11) NOT NULL,
  `seq` int(11) NOT NULL,
  `bitPosition` int(11) NOT NULL,
  `runAlways` tinyint(1) NOT NULL DEFAULT '1',
  `weight` double NOT NULL DEFAULT '1',
  `qchconfigId` int(11) DEFAULT NULL,
  `className` varchar(128) NOT NULL,
  PRIMARY KEY (`qchseqmgrId`,`seq`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `qchseqmgr` (
  `id` int(11) NOT NULL,
  `obsTypeId` int(11) NOT NULL,
  `climateId` int(11) NOT NULL,
  `active` tinyint(4) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;


CREATE TABLE `timestampformat` (
  `id` int(11) NOT NULL,
  `formatstring` varchar(64) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;


CREATE TABLE `timezone` (
  `id` int(11) NOT NULL,
  `rawOffset` int(11) NOT NULL,
  `tzName` varchar(64) NOT NULL,
  `startMonth` varchar(10) DEFAULT NULL,
  `startDay` tinyint(4) DEFAULT NULL,
  `startDayOfWeek` varchar(10) DEFAULT NULL,
  `startTime` int(11) DEFAULT NULL,
  `endMonth` varchar(10) DEFAULT NULL,
  `endDay` tinyint(4) DEFAULT NULL,
  `endDayOfWeek` varchar(10) DEFAULT NULL,
  `endTime` int(11) DEFAULT NULL,
  `dstSavings` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;

CREATE TABLE `unit` (
  `id` int(11) NOT NULL,
  `srcUnit` varchar(8) NOT NULL,
  `dstUnit` varchar(8) NOT NULL,
  `multiplier` double NOT NULL DEFAULT '1',
  `divisor` double NOT NULL DEFAULT '1',
  `offset` double NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `Index_2` (`srcUnit`,`dstUnit`) USING BTREE
) ENGINE=MyISAM DEFAULT CHARSET=latin1 ROW_FORMAT=FIXED;


CREATE TABLE `xmlcollector` (
  `id` int(11) NOT NULL,
  `csvcId` int(11) NOT NULL,
  `defId` int(11) NOT NULL,
  `collectDelay` smallint(6) NOT NULL DEFAULT '0',
  `retryFlag` tinyint(1) NOT NULL DEFAULT '0',
  `collectTzId` int(11) NOT NULL,
  `contentTzId` int(11) NOT NULL,
  `timestampId` int(11) NOT NULL,
  `defaultSensorIndex` int(11) NOT NULL,
  `filepath` varchar(255) NOT NULL,
  PRIMARY KEY (`csvcId`,`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


CREATE TABLE `xmldef` (
  `collectorId` int(11) NOT NULL,
  `pathId` int(11) NOT NULL,
  `obsTypeId` int(11) NOT NULL,
  `multiplier` double DEFAULT NULL,
  `className` varchar(128) NOT NULL,
  `unit` varchar(8) DEFAULT NULL,
  `ignoreValues` varchar(128) DEFAULT NULL,
  `xmlPath` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`collectorId`,`pathId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;



USE clarus_meta;

CREATE TABLE `climate` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `regionDesc` varchar(50) DEFAULT NULL,
  `source` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=850000001 DEFAULT CHARSET=latin1;

CREATE TABLE `climaterecord` (
  `obsTypeId` int(11) NOT NULL,
  `month` tinyint(4) NOT NULL,
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `lowerValue` double NOT NULL,
  `upperValue` double NOT NULL,
  `avgValue` double DEFAULT NULL,
  PRIMARY KEY (`obsTypeId`,`month`,`lat`,`lon`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;


CREATE TABLE `contact` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) NOT NULL,
  `title` varchar(50) DEFAULT NULL,
  `orgId` int(11) NOT NULL,
  `phonePrimary` char(10) DEFAULT NULL,
  `phoneAlt` char(10) DEFAULT NULL,
  `phoneMobile` char(10) DEFAULT NULL,
  `fax` char(10) DEFAULT NULL,
  `pagerId` char(10) DEFAULT NULL,
  `pager` char(10) DEFAULT NULL,
  `email` varchar(50) DEFAULT NULL,
  `radioUnit` varchar(50) DEFAULT NULL,
  `address1` varchar(50) DEFAULT NULL,
  `address2` varchar(50) DEFAULT NULL,
  `city` varchar(50) DEFAULT NULL,
  `state` char(2) DEFAULT NULL,
  `zip` char(10) DEFAULT NULL,
  `country` char(3) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=73 DEFAULT CHARSET=latin1;


CREATE TABLE `contrib` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `orgId` int(11) DEFAULT NULL,
  `name` varchar(50) DEFAULT NULL,
  `agency` varchar(200) DEFAULT NULL,
  `monitorHours` int(11) DEFAULT '0',
  `contactId` int(11) DEFAULT NULL,
  `altContactId` int(11) DEFAULT NULL,
  `metadataContactId` int(11) DEFAULT NULL,
  `display` tinyint(1) NOT NULL DEFAULT '1',
  `disclaimerLink` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `orgId` (`orgId`),
  KEY `contactId` (`contactId`),
  KEY `altContactId` (`altContactId`),
  KEY `metadataContactId` (`metadataContactId`)
) ENGINE=MyISAM AUTO_INCREMENT=71 DEFAULT CHARSET=latin1;


CREATE TABLE `distgroup` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;


CREATE TABLE `image` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `siteId` int(11) DEFAULT NULL,
  `description` varchar(50) DEFAULT NULL,
  `linkURL` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `siteId` (`siteId`)
) ENGINE=MyISAM AUTO_INCREMENT=255 DEFAULT CHARSET=latin1;


CREATE TABLE `monitorcontact` (
  `contribId` int(11) NOT NULL,
  `email` varchar(100) NOT NULL,
  PRIMARY KEY (`contribId`,`email`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;


CREATE TABLE `obstype` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `obsType` varchar(50) DEFAULT NULL,
  `obs1204Units` varchar(50) DEFAULT NULL,
  `obsDesc` varchar(255) DEFAULT NULL,
  `obsInternalUnits` varchar(45) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `obsEnglishUnits` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=2000002 DEFAULT CHARSET=latin1;


CREATE TABLE `organization` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(200) DEFAULT NULL,
  `location` varchar(50) DEFAULT NULL,
  `purpose` varchar(50) DEFAULT NULL,
  `centerId` varchar(50) DEFAULT NULL,
  `centerName` varchar(50) DEFAULT NULL,
  `updateDate` datetime DEFAULT NULL,
  `contactId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=71 DEFAULT CHARSET=latin1;


CREATE TABLE `qchparm` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `sensorTypeId` int(11) DEFAULT NULL,
  `obsTypeId` int(11) DEFAULT NULL,
  `isDefault` int(11) NOT NULL DEFAULT '1',
  `minRange` float DEFAULT NULL,
  `maxRange` float DEFAULT NULL,
  `resolution` float DEFAULT NULL,
  `accuracy` float DEFAULT NULL,
  `ratePos` decimal(18,9) DEFAULT NULL,
  `rateNeg` decimal(18,9) DEFAULT NULL,
  `rateInterval` decimal(18,9) DEFAULT NULL,
  `persistInterval` decimal(18,9) DEFAULT NULL,
  `persistThreshold` decimal(18,9) DEFAULT NULL,
  `likeThreshold` decimal(18,9) DEFAULT NULL,
  `minDisplay` float DEFAULT NULL,
  `maxDisplay` float DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=2330 DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;


CREATE TABLE `sensor` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `stationId` int(11) NOT NULL DEFAULT '0',
  `sensorIndex` int(11) NOT NULL DEFAULT '0',
  `obsTypeId` int(11) NOT NULL DEFAULT '0',
  `qchparmId` int(11) NOT NULL DEFAULT '0',
  `distGroup` int(11) NOT NULL DEFAULT '0',
  `nsOffset` float DEFAULT NULL,
  `ewOffset` float DEFAULT NULL,
  `elevOffset` float DEFAULT NULL,
  `surfaceOffset` float DEFAULT NULL,
  `installDate` datetime DEFAULT NULL,
  `calibDate` datetime DEFAULT NULL,
  `maintDate` datetime DEFAULT NULL,
  `maintBegin` datetime DEFAULT NULL,
  `maintEnd` datetime DEFAULT NULL,
  `serial` varchar(50) DEFAULT NULL,
  `embeddedMaterial` varchar(100) DEFAULT NULL,
  `sensorLocation` char(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `stationId` (`stationId`),
  KEY `FK_sensor_distgroup` (`distGroup`),
  KEY `obsTypeId` (`qchparmId`)
) ENGINE=MyISAM AUTO_INCREMENT=95303 DEFAULT CHARSET=latin1;


CREATE TABLE `sensortype` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mfr` varchar(50) NOT NULL,
  `model` varchar(50) NOT NULL,
  `outputAvgInterval` int(11) DEFAULT NULL,
  `outputIntervalUnits` varchar(8) DEFAULT NULL,
  `samplingInterval` decimal(18,9) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=149 DEFAULT CHARSET=latin1;


CREATE TABLE `site` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `stateSiteId` varchar(50) NOT NULL,
  `contribId` int(11) DEFAULT NULL,
  `description` varchar(50) DEFAULT NULL,
  `roadwayDesc` varchar(255) DEFAULT NULL,
  `roadwayMilepost` int(11) DEFAULT NULL,
  `roadwayOffset` float DEFAULT NULL,
  `roadwayHeight` float DEFAULT NULL,
  `county` varchar(100) DEFAULT NULL,
  `state` char(2) DEFAULT NULL,
  `country` char(3) DEFAULT NULL,
  `accessDirections` varchar(50) DEFAULT NULL,
  `climateId` int(11) DEFAULT NULL,
  `representativeness` varchar(255) DEFAULT NULL,
  `obstructions` varchar(100) DEFAULT NULL,
  `landscape` varchar(100) DEFAULT NULL,
  `accessControlled` bit(1) DEFAULT NULL,
  `terrainSlope` int(11) DEFAULT NULL,
  `terrainSlopeDirection` int(11) DEFAULT NULL,
  `windRoughnessClass` int(11) DEFAULT NULL,
  `soilType` int(11) DEFAULT NULL,
  `stateSystemID` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `contribId` (`contribId`),
  KEY `climateId` (`climateId`)
) ENGINE=MyISAM AUTO_INCREMENT=5773 DEFAULT CHARSET=latin1;


CREATE TABLE `station` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `stationCode` varchar(50) NOT NULL,
  `category` char(1) NOT NULL DEFAULT '',
  `description` varchar(255) DEFAULT NULL,
  `type` int(11) DEFAULT NULL,
  `contribId` int(11) DEFAULT NULL,
  `siteId` int(11) DEFAULT NULL,
  `locBaseLat` decimal(18,9) DEFAULT NULL,
  `locBaseLong` decimal(18,9) DEFAULT NULL,
  `locBaseElev` decimal(18,9) DEFAULT NULL,
  `locBaseDatum` char(10) DEFAULT NULL,
  `powerType` char(1) DEFAULT NULL,
  `doorOpen` bit(1) DEFAULT NULL,
  `batteryStatus` int(11) DEFAULT NULL,
  `lineVolts` int(11) DEFAULT NULL,
  `maintContactId` int(11) DEFAULT NULL,
  `maintArea` varchar(50) DEFAULT NULL,
  `maintPrevFreq` varchar(50) DEFAULT NULL,
  `maintCalibFreq` varchar(50) DEFAULT NULL,
  `maintStatus` bit(1) DEFAULT NULL,
  `maintInstallDate` datetime DEFAULT NULL,
  `rpuNumCards` int(11) DEFAULT NULL,
  `rpuCommType` int(11) DEFAULT NULL,
  `rpuPhoneNum` char(10) DEFAULT NULL,
  `rpuIPAddress` char(15) DEFAULT NULL,
  `rpuMfr` varchar(50) DEFAULT NULL,
  `rpuUTCOffset` int(11) DEFAULT NULL,
  `rpuDST` bit(1) DEFAULT NULL,
  `obsCollFreq` int(11) DEFAULT NULL,
  `obsCollOffset` int(11) DEFAULT NULL,
  `obsTransFreq` int(11) DEFAULT NULL,
  `obsTransOffset` int(11) DEFAULT NULL,
  `obsTransFormat` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `contribId` (`contribId`),
  KEY `siteId` (`siteId`)
) ENGINE=MyISAM AUTO_INCREMENT=6697 DEFAULT CHARSET=latin1;

USE clarus_qedc;

CREATE TABLE `obs` (
  `obsType` int(11) NOT NULL,
  `sensorId` int(11) NOT NULL,
  `timestamp` datetime NOT NULL,
  `latitude` int(11) NOT NULL,
  `longitude` int(11) NOT NULL,
  `elevation` int(11) NOT NULL,
  `value` double NOT NULL,
  `confidence` float NOT NULL,
  `runFlags` int(11) NOT NULL,
  `passedFlags` int(11) NOT NULL,
  `created` bigint(20) NOT NULL,
  `updated` bigint(20) NOT NULL,
  PRIMARY KEY (`timestamp`,`obsType`,`sensorId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8
/*!50100 PARTITION BY HASH (DAYOFWEEK(timestamp) - 1)
PARTITIONS 7 */;


USE clarus_subs;

CREATE TABLE `subcontrib` (
  `subId` int(10) unsigned NOT NULL DEFAULT '0',
  `contribId` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`subId`,`contribId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;


CREATE TABLE `subradius` (
  `subId` int(10) unsigned NOT NULL DEFAULT '0',
  `lat` int(11) NOT NULL DEFAULT '0',
  `lng` int(11) NOT NULL DEFAULT '0',
  `radius` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`subId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;

CREATE TABLE `subscription` (
  `id` int(10) unsigned NOT NULL DEFAULT '0',
  `expires` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lat1` int(11) DEFAULT NULL,
  `lng1` int(11) DEFAULT NULL,
  `lat2` int(11) DEFAULT NULL,
  `lng2` int(11) DEFAULT NULL,
  `obsTypeId` int(10) unsigned DEFAULT NULL,
  `minValue` double DEFAULT NULL,
  `maxValue` double DEFAULT NULL,
  `qchRun` int(11) DEFAULT NULL,
  `qchFlags` int(11) DEFAULT NULL,
  `password` varchar(15) NOT NULL DEFAULT '',
  `format` varchar(8) NOT NULL DEFAULT 'CSV',
  `cycle` tinyint(4) NOT NULL DEFAULT '20',
  `contactName` varchar(64) DEFAULT NULL,
  `contactEmail` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;


CREATE TABLE `substation` (
  `subId` int(10) unsigned NOT NULL DEFAULT '0',
  `stationId` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`subId`,`stationId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 ROW_FORMAT=FIXED;


