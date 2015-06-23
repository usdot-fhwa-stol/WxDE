DROP TABLE obs.archiveObs;

DROP TYPE obs.obs_value;

DROP TABLE obs.obs_template;
DROP TABLE obs.obs;
DROP SEQUENCE obs.obs_id_seq;

DROP TABLE meta.lastUpdate;
DROP SEQUENCE meta.lastUpdate_id_seq;

DROP TABLE meta.sensor;
DROP SEQUENCE meta.sensor_id_seq;

DROP TABLE meta.distGroup;
DROP SEQUENCE meta.distGroup_id_seq;

DROP TABLE meta.qchparm;

DROP TABLE meta.obsType;

DROP TABLE meta.qualityFlag;
DROP SEQUENCE meta.qualityFlag_id_seq;

DROP TABLE meta.source;
DROP SEQUENCE meta.source_id_seq;

DROP TABLE meta.sensorType;
DROP SEQUENCE meta.sensorType_id_seq;

DROP TABLE meta.image;
DROP SEQUENCE meta.image_id_seq;

DROP TABLE meta.platform;
DROP SEQUENCE meta.platform_id_seq;

DROP TABLE meta.site;
DROP SEQUENCE meta.site_id_seq;

DROP TABLE meta.contrib;
DROP SEQUENCE meta.contrib_id_seq;

DROP TABLE meta.contact;
DROP SEQUENCE meta.contact_id_seq;

DROP TABLE meta.organization;
DROP SEQUENCE meta.organization_id_seq;

DROP SCHEMA obs;
DROP SCHEMA meta;
