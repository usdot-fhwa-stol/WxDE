/*
                                             Table "meta.obstype"
      Column      |           Type           |       Modifiers        | Storage  | Stats target | Description
------------------+--------------------------+------------------------+----------+--------------+-------------
 id               | integer                  | not null               | plain    |              |
 updatetime       | timestamp with time zone | not null               | plain    |              |
 obstype          | character varying(50)    |                        | extended |              |
 obs1204units     | character varying(50)    |                        | extended |              |
 obsdesc          | character varying(255)   |                        | extended |              |
 obsinternalunits | character varying(45)    |                        | extended |              |
 active           | boolean                  | not null default false | plain    |              |
 obsenglishunits  | character varying(45)    |                        | extended |              |
Indexes:
    "obstype_pkey" PRIMARY KEY, btree (id)
Referenced by:
    TABLE "obs.archiveobs" CONSTRAINT "archiveobs_obstypeid_fkey" FOREIGN KEY (obstypeid) REFERENCES meta.obstype(id)
    TABLE "obs.obs" CONSTRAINT "obs_obstypeid_fkey" FOREIGN KEY (obstypeid) REFERENCES meta.obstype(id)
    TABLE "meta.qchparm" CONSTRAINT "qchparm_obstypeid_fkey" FOREIGN KEY (obstypeid) REFERENCES meta.obstype(id)
    TABLE "meta.sensor" CONSTRAINT "sensor_obstypeid_fkey" FOREIGN KEY (obstypeid) REFERENCES meta.obstype(id)
Has OIDs: no
 */
 /*
    id    |         updatetime         |                  obstype                  |               obs1204units               |                                                                                                   obsdesc                                                                                                    | obsinternalunits | active | obsenglishunits
---------+----------------------------+-------------------------------------------+------------------------------------------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+------------------+--------+-----------------
     541 | 2013-03-06 19:51:33.012+00 | essLatitude                               | 10^-6 degrees                            | Latitude of the ESS station [observation] per WGS-84 datum                                                                                                                                                   | deg              | f      | deg
     542 | 2013-03-06 19:51:33.013+00 | essLongitude                              | 10^-6 degrees                            | East longitude from the Prime Meridian of the ESS station [observation]                                                                                                                                      | deg              | f      | deg
     544 | 2013-03-06 19:51:33.014+00 | essVehicleBearing                         | degrees clockwise from true North        | Current bearing of the vehicle                                                                                                                                                                               | deg              | f      | deg
     545 | 2013-03-06 19:51:33.015+00 | essVehicleOdometer                        | meters                                   | Current odometer reading of the vehicle                                                                                                                                                                      | m                | f      | ft
     551 | 2013-03-06 19:51:33.015+00 | essReferenceHeight                        | meters above mean sea level              | Reference elevation of the ESS; height to base of station for permanent ESS height to the ground surface upon which the ESS resides for transportable ESS, or height to surface under vehicle for mobile ESS | m                | f      | ft
     554 | 2013-03-06 19:51:33.016+00 | essAtmosphericPressure                    | 10^-1 millibars, or 10^-1 hectopascals   | Force per unit area exerted by the atmosphere                                                                                                                                                                | mbar             | t      | inHg
   56104 | 2013-03-06 19:51:33.016+00 | windSensorAvgSpeed                        | 10^-1 meters per second                  | Two-minute average of the wind speed                                                                                                                                                                         | m/s              | t      | mph
   56105 | 2013-03-06 19:51:33.017+00 | windSensorAvgDirection                    | degrees clockwise from true North        | Two-min. average of wind direction (CW from North)                                                                                                                                                           | deg              | t      | deg
  */

insert into meta.obstype(id, updatetime, obstype, obs1204units, obsdesc, obsinternalunits, active, obsenglishunits)
values (
  1000000,
    now(),
    'wdePrecipitationType',
    'integer',
    'Derived value that describes the type of precipitation in the observation.',
    NULL,
    't',
    NULL
);

insert into meta.obstype(id, updatetime, obstype, obs1204units, obsdesc, obsinternalunits, active, obsenglishunits)
values (
  1000001,
  now(),
  'wdePrecipitationIntesity',
  'integer',
  'Derived value that describes the intensity of the precipitation in the observation.',
  NULL,
  't',
  NULL
);

insert into meta.obstype(id, updatetime, obstype, obs1204units, obsdesc, obsinternalunits, active, obsenglishunits)
values (
  1000002,
  now(),
  'wdePavementCondition',
  'integer',
  'Derived value that describes the pavement conditions in the observation.',
  NULL,
  't',
  NULL
);

insert into meta.obstype(id, updatetime, obstype, obs1204units, obsdesc, obsinternalunits, active, obsenglishunits)
values (
  1000003,
  now(),
  'wdePavementSlickness',
  'integer',
  'Derived value that describes the pavement slickness in the observation.',
  NULL,
  't',
  NULL
);

insert into meta.obstype(id, updatetime, obstype, obs1204units, obsdesc, obsinternalunits, active, obsenglishunits)
values (
  1000004,
  now(),
  'wdeVisibility',
  'integer',
  'Derived value that describes the visibility conditions in the observation.',
  NULL,
  't',
  NULL
);
