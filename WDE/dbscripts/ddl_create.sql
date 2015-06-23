CREATE SCHEMA meta;
GRANT ALL PRIVILEGES on SCHEMA meta to wxde;

CREATE SCHEMA obs;
GRANT ALL PRIVILEGES on SCHEMA obs to wxde;

CREATE SEQUENCE meta.organization_id_seq;

GRANT ALL PRIVILEGES on meta.organization_id_seq to wxde;

CREATE TABLE meta.organization
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.organization_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  name varchar(200),
  location varchar(50)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.organization
  OWNER TO wxde;

CREATE SEQUENCE meta.contact_id_seq;
GRANT ALL PRIVILEGES on meta.contact_id_seq to wxde;

CREATE TABLE meta.contact
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.contact_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  name varchar(50) NOT NULL,
  title varchar(50),
  orgId integer REFERENCES meta.organization,
  phonePrimary char(10),
  phoneAlt char(10),
  phoneMobile char(10),
  fax char(10),
  email varchar(50),
  address1 varchar(50),
  address2 varchar(50),
  city varchar(50),
  state char(2),
  zip varchar(10),
  country char(3) 
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.contact
  OWNER TO wxde;

CREATE SEQUENCE meta.contrib_id_seq;
GRANT ALL PRIVILEGES on meta.contrib_id_seq to wxde;

CREATE TABLE meta.contrib
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.contrib_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  orgId integer,
  name varchar(50),
  agency varchar(200), 
  monitorHours integer,
  contactId integer REFERENCES meta.contact,
  altContactId integer REFERENCES meta.contact,
  metadataContactId integer REFERENCES meta.contact,
  display boolean NOT NULL DEFAULT TRUE,
  disclaimerLink varchar(255)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.contrib
  OWNER TO wxde;

CREATE SEQUENCE meta.site_id_seq;
GRANT ALL PRIVILEGES on meta.site_id_seq to wxde;

CREATE TABLE meta.site
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.site_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  stateSiteId varchar(50) NOT NULL,
  contribId integer REFERENCES meta.contrib(id),
  description varchar(50), 
  roadwayDesc varchar(255), 
  roadwayMilepost integer, 
  roadwayOffset real, 
  roadwayHeight real, 
  county varchar(100),
  state char(2),
  country char(3),
  accessDirections varchar(50),
  obstructions varchar(100),
  landscape varchar(100),
  stateSystemId varchar(45)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.site
  OWNER TO wxde;

CREATE SEQUENCE meta.platform_id_seq;
GRANT ALL PRIVILEGES on meta.platform_id_seq to wxde;

CREATE TABLE meta.platform
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.platform_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  platformCode varchar(50) NOT NULL,
  category char(1),
  description varchar(255), 
  type integer,
  contribId integer REFERENCES meta.contrib,
  siteId integer REFERENCES meta.site,
  locBaseLat decimal(18,9),
  locBaseLong decimal(18,9),
  locBaseElev decimal(18,9),
  locBaseDatum char(10),
  powerType char(1),
  doorOpen boolean,
  batteryStatus integer,
  lineVolts integer,
  maintContactId integer REFERENCES meta.contact
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.platform
  OWNER TO wxde;

CREATE SEQUENCE meta.image_id_seq;
GRANT ALL PRIVILEGES on meta.image_id_seq to wxde;

CREATE TABLE meta.image
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.image_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  siteId integer,
  description varchar(50),
  linkURL varchar(255)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.image
  OWNER TO wxde;

CREATE SEQUENCE meta.sensorType_id_seq;
GRANT ALL PRIVILEGES on meta.sensorType_id_seq to wxde;

CREATE TABLE meta.sensorType
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.sensorType_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  mfr varchar(50) NOT NULL,
  model varchar(50) NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.sensorType
  OWNER TO wxde;

CREATE SEQUENCE meta.source_id_seq;
GRANT ALL PRIVILEGES on meta.source_id_seq to wxde;

CREATE TABLE meta.source
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.source_id_seq'),
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  name varchar(20) NOT NULL,
  description varchar(50)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.source
  OWNER TO wxde;

CREATE SEQUENCE meta.qualityFlag_id_seq;
GRANT ALL PRIVILEGES on meta.qualityFlag_id_seq to wxde;

CREATE TABLE meta.qualityFlag
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.qualityFlag_id_seq'),
  sourceId int REFERENCES meta.source,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  qchCharFlagLen smallint NOT NULL,
  qchIntFlagLen smallint NOT NULL,
  qchFlagLabel varchar(30)[]
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.qualityFlag
  OWNER TO wxde;

CREATE TABLE meta.obsType
(
  id integer PRIMARY KEY,
  updatetime timestamp with time zone NOT NULL,
  obsType varchar(50),
  obs1204Units varchar(50),
  obsDesc varchar(255),
  obsInternalUnits varchar(45),
  active boolean NOT NULL DEFAULT FALSE, 
  obsEnglishUnits varchar(45)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.obsType
  OWNER TO wxde;

CREATE TABLE meta.qchparm
(
  id integer PRIMARY KEY,
  sensorTypeId integer REFERENCES meta.sensorType,
  obsTypeId integer REFERENCES meta.obsType,
  isDefault boolean NOT NULL DEFAULT TRUE,
  minRange real,
  maxRange real,
  resolution real,
  accuracy real,
  ratePos decimal(18,9),
  rateNeg decimal(18,9),
  rateInterval decimal(18,9),
  persistInterval decimal(18,9),
  persistThreshold decimal(18,9),
  likeThreshold decimal(18,9)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.qchparm
  OWNER TO wxde;

CREATE SEQUENCE meta.distGroup_id_seq;
GRANT ALL PRIVILEGES on meta.distGroup_id_seq to wxde;

CREATE TABLE meta.distGroup
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.distGroup_id_seq'),
  description varchar(50)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.distGroup
  OWNER TO wxde;

CREATE SEQUENCE meta.sensor_id_seq;
GRANT ALL PRIVILEGES on meta.sensor_id_seq to wxde;

CREATE TABLE meta.sensor
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.sensor_id_seq'),
  sourceId integer REFERENCES meta.source, 
  staticId integer NOT NULL,
  updateTime timestamp with time zone NOT NULL,
  toTime timestamp with time zone,
  platformId integer REFERENCES meta.platform,
  contribId integer REFERENCES meta.contrib,
  sensorIndex integer NOT NULL DEFAULT '0',
  obsTypeId integer REFERENCES meta.obsType,
  qchparmId integer REFERENCES meta.qchparm,
  distGroup integer REFERENCES meta.distGroup,
  nsOffset real,
  ewOffset real,
  elevOffset real,
  surfaceOffset real,
  installDate timestamp,
  calibDate timestamp,
  maintDate timestamp,
  maintBegin timestamp,
  maintEnd timestamp,
  embeddedMaterial varchar(100),
  sensorLocation char(10)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.sensor
  OWNER TO wxde;

# Executed on the dev, test, demo, archive, prod

CREATE SEQUENCE meta.segment_id_seq;
GRANT ALL PRIVILEGES on meta.segment_id_seq to wxde;

CREATE TABLE meta.segment
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.segment_id_seq'),
  contribId integer REFERENCES meta.contrib,
  segmentId integer NOT NULL,
  segmentName varchar(50)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.segment
  OWNER TO wxde;

CREATE SEQUENCE meta.lastUpdate_id_seq;
GRANT ALL PRIVILEGES on meta.lastUpdate_id_seq to wxde;

CREATE TABLE meta.lastUpdate
(
  id integer PRIMARY KEY DEFAULT NEXTVAL('meta.lastUpdate_id_seq'),
  name varchar(30),
  updateTime timestamp with time zone
)
WITH (
  OIDS=FALSE
);
ALTER TABLE meta.lastUpdate
  OWNER TO wxde;

CREATE SEQUENCE obs.obs_id_seq;

GRANT ALL PRIVILEGES on obs.obs_id_seq to wxde;

CREATE TABLE obs.obs
(
  obsTypeId integer REFERENCES meta.obsType,
  sourceId integer REFERENCES meta.source,
  sensorId integer REFERENCES meta.sensor,
  obsTime timestamp NOT NULL,
  recvTime timestamp with time zone NOT NULL,
  latitude integer NOT NULL,
  longitude integer NOT NULL,
  elevation integer NOT NULL,
  value double precision NOT NULL,
  confValue real NOT NULL,
  qchCharFlag char[],
  qchIntFlag smallint[]
)
WITH (
  OIDS=FALSE
);
ALTER TABLE obs.obs
  OWNER TO wxde;

-- Unfortunately Postgresql 9.2 does not support constraints on composite types
-- In addition, the two flags are defined as varchar as array of array is not
-- yet supported
CREATE TYPE obs.obs_value AS (
  sourceId integer,
  sensorId integer,	
  obsTime timestamp,
  recvTime timestamp,
  latitude integer,
  longitude integer,
  elevation integer,
  value double precision,
  confValue real,
  qchCharFlags varchar(50),
  qchIntFlags varchar(250)
);

CREATE TABLE obs.archiveObs
(
  duration daterange NOT NULL,
  gridId char(3) NOT NULL,
  obsTypeId integer REFERENCES meta.obsType,
  value obs.obs_value[],
  PRIMARY KEY (duration, gridId, obsTypeId)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE obs.archiveObs
  OWNER TO wxde;
  
CREATE TABLE obs.invalidObs
(
  contribId integer,
  platformCode varchar(50) NOT NULL,
  obsTypeId integer,
  sensorIndex integer NOT NULL DEFAULT '0',
  obsTime timestamp,
  recvTime timestamp,
  value double precision NOT NULL,
  sourceid integer,
  latitude integer,
  longitude integer,
  qchcharflag character(1)[]
)
WITH (
  OIDS=FALSE
);
ALTER TABLE obs.invalidObs
  OWNER TO wxde;


-- auth schema
CREATE SCHEMA auth
  AUTHORIZATION wxde;

GRANT ALL ON SCHEMA auth TO wxde;

-- auth.country
CREATE TABLE auth.country
(
  country_code character(2) NOT NULL,
  country_name character varying(50) NOT NULL,
  CONSTRAINT pk_country_code PRIMARY KEY (country_code)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.country
  OWNER TO wxde;

-- auth.organizaiton_type
CREATE TABLE auth.organization_type
(
  organization_type character varying(32) NOT NULL,
  organization_type_name character varying(32) NOT NULL,
  CONSTRAINT pk_organization_type PRIMARY KEY (organization_type)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.organization_type
  OWNER TO wxde;

  
-- auth.user_role
CREATE TABLE auth.user_role
(
  user_name character varying(15) NOT NULL,
  user_role character varying(15) NOT NULL,
  CONSTRAINT "PK_USER_ROLE" PRIMARY KEY (user_name, user_role),
  CONSTRAINT "FK_USER_ROLE" FOREIGN KEY (user_name)
      REFERENCES auth.user_table (user_name) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.user_role
  OWNER TO wxde;

-- Index: auth.fki_country

-- DROP INDEX auth.fki_country;

CREATE INDEX fki_country
  ON auth.user_table
  USING btree
  (country COLLATE pg_catalog."default");

-- Index: auth.fki_organization_type

-- DROP INDEX auth.fki_organization_type;

CREATE INDEX fki_organization_type
  ON auth.user_table
  USING btree
  (organization_type COLLATE pg_catalog."default");
  
-- Table: auth.feedback_type

-- DROP TABLE auth.feedback_type;

CREATE TABLE auth.feedback_type
(
  id serial NOT NULL,
  name character varying NOT NULL,
  CONSTRAINT pk_id PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.feedback_type
  OWNER TO wxde;
  
-- Table: auth.feedback

-- DROP TABLE auth.feedback;

CREATE TABLE auth.feedback
(
  id serial NOT NULL,
  name character varying(100),
  email character varying(100) NOT NULL,
  section character varying(100) NOT NULL,
  description character varying(500) NOT NULL,
  feedback_type_id integer NOT NULL,
  user_name character varying(15),
  date_created date,
  timestamp_created timestamp without time zone,
  CONSTRAINT pk_feedback_id PRIMARY KEY (id),
  CONSTRAINT fk_feedback_type_id FOREIGN KEY (feedback_type_id)
      REFERENCES auth.feedback_type (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_user_name FOREIGN KEY (user_name)
      REFERENCES auth.user_table (user_name) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.feedback
  OWNER TO wxde;
  
-- Table: auth.user_table

-- DROP TABLE auth.user_table;

CREATE TABLE auth.user_table
(
  user_name character varying(15) NOT NULL,
  user_password character varying(100) NOT NULL,
  first_name character varying(32) NOT NULL,
  last_name character varying(32) NOT NULL,
  organization character varying(32) NOT NULL,
  organization_type character varying(32) NOT NULL,
  country character varying(2) NOT NULL,
  email character varying(50) NOT NULL,
  guid character varying(100),
  verified boolean,
  date_created date,
  password_guid character varying(100),
  date_password_reset date,
  CONSTRAINT pk_username PRIMARY KEY (user_name),
  CONSTRAINT fk_country FOREIGN KEY (country)
      REFERENCES auth.country (country_code) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT fk_organization_type FOREIGN KEY (organization_type)
      REFERENCES auth.organization_type (organization_type) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
ALTER TABLE auth.user_table
  OWNER TO wxde;

-- Index: auth.fki_country

-- DROP INDEX auth.fki_country;

CREATE INDEX fki_country
  ON auth.user_table
  USING btree
  (country COLLATE pg_catalog."default");

-- Index: auth.fki_organization_type

-- DROP INDEX auth.fki_organization_type;

CREATE INDEX fki_organization_type
  ON auth.user_table
  USING btree
  (organization_type COLLATE pg_catalog."default");

-- Table: conf.collectionstatistics

-- DROP TABLE conf.dailycollectionstatistics;

CREATE TABLE conf.dailycollectionstatistics
(
  contribid integer REFERENCES meta.contrib,
  contribname varchar(50),
  collectiondate timestamp without time zone NOT NULL,  
  category char(1),
  numobservations integer NOT NULL
)
WITH (
  OIDS=FALSE
);
ALTER TABLE conf.dailycollectionstatistics
  OWNER TO wxde;
  
-- Table: conf.collectionstatus

-- DROP TABLE conf.collectionstatus;

CREATE TABLE conf.collectionstatus
(
  contribid integer REFERENCES meta.contrib,
  updatetime timestamp without time zone NOT NULL, 
  status smallint NOT NULL DEFAULT (0)::smallint,
  statistics character varying(100)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE conf.collectionstatus
  OWNER TO wxde;
