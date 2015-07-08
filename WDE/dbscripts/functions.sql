CREATE OR REPLACE FUNCTION
  obs.obs_partition_function()
RETURNS TRIGGER AS 
$BODY$
DECLARE
  _new_time int;
  _tablename text;
  _startdate text;
  _enddate text;
  _result record;
BEGIN
  --Takes the current inbound "time" value and determines when midnight is for the given date
  _startdate := to_char(NEW.obsTime, 'YYYY-MM-DD');
  _tablename := 'obs_'||_startdate;
 
  -- Check if the partition needed for the current record exists
  PERFORM 1
  FROM   pg_catalog.pg_class c
  JOIN   pg_catalog.pg_namespace n ON n.oid = c.relnamespace
  WHERE  c.relkind = 'r'
  AND    c.relname = _tablename
  AND    n.nspname = 'obs';
 
  -- If the partition needed does not yet exist, then we create it:
  -- Note that || is string concatenation (joining two strings to make one)
  IF NOT FOUND THEN
    _enddate:=_startdate::timestamp + INTERVAL '1 day';
    EXECUTE 'CREATE TABLE obs.' || quote_ident(_tablename) || '() INHERITS (obs.obs)';
 
  -- Table permissions are not inherited from the parent.
  -- If permissions change on the master be sure to change them on the child also.
  EXECUTE 'ALTER TABLE obs.' || quote_ident(_tablename) || ' OWNER TO wxde';
--  EXECUTE 'GRANT ALL ON TABLE obs.' || quote_ident(_tablename) || ' TO wxde';
 
  -- Indexes are defined per child, so we assign a default index that uses the partition columns
  -- EXECUTE 'CREATE INDEX ' || quote_ident(_tablename||'_indx1') || ' ON obs.' || quote_ident(_tablename) || ' (time, id)';
END IF;
 
-- Insert the current record into the correct partition, which we are sure will now exist.
EXECUTE 'INSERT INTO obs.' || quote_ident(_tablename) || ' VALUES ($1.*)' USING NEW;
RETURN NULL;
END;
$BODY$
LANGUAGE plpgsql;

CREATE TRIGGER obs_master_trigger
BEFORE INSERT ON obs.obs
FOR EACH ROW EXECUTE PROCEDURE obs.obs_partition_function();


CREATE OR REPLACE FUNCTION
  obs.partition_maintenance(in_tablename_prefix text, in_master_tablename text, in_asof date)
RETURNS text AS
$BODY$
DECLARE
  _result record;
  _current_time_without_special_characters text;
  _out_filename text;
  _return_message text;
  return_message text;
BEGIN
   -- Get the current date in YYYYMMDD_HHMMSS.ssssss format
   _current_time_without_special_characters := 
        REPLACE(REPLACE(REPLACE(NOW()::TIMESTAMP WITHOUT TIME ZONE::TEXT, '-', ''), ':', ''), ' ', '_');
 
   -- Initialize the return_message to empty to indicate no errors hit
   _return_message := '';
 
   --Validate input to function
   IF in_tablename_prefix IS NULL THEN
     RETURN 'Child table name prefix must be provided'::text;
   ELSIF in_master_tablename IS NULL THEN
     RETURN 'Master table name must be provided'::text;
   ELSIF in_asof IS NULL THEN
     RETURN 'You must provide the as-of date, NOW() is the typical value';
   END IF;
 
   FOR _result IN SELECT * FROM pg_tables WHERE schemaname='obs' LOOP
 
   IF POSITION(in_tablename_prefix in _result.tablename) > 0 AND char_length(substring(_result.tablename from '[0-9-]*$')) <> 0 AND (in_asof - interval '15 days') > to_timestamp(substring(_result.tablename from '[0-9-]*$'),'YYYY-MM-DD') THEN
 
      _out_filename := '/db/partition_dump/' || _result.tablename || '_' || _current_time_without_special_characters || '.sql.gz';
      BEGIN
        -- Call function export_partition(child_table text) to export the file
        PERFORM obs.export_partition(_result.tablename::text, _out_filename::text);
        -- If the export was successful drop the child partition
        EXECUTE 'DROP TABLE obs.' || quote_ident(_result.tablename);
        _return_message := return_message || 'Dumped table: ' || _result.tablename::text || ', ';
        RAISE NOTICE 'Dumped table %', _result.tablename::text;
      EXCEPTION WHEN OTHERS THEN
        _return_message := return_message || 'ERROR dumping table: ' || _result.tablename::text || ', ';
        RAISE NOTICE 'ERROR DUMPING %', _result.tablename::text;
      END;
     END IF;
   END LOOP;
 
   RETURN _return_message || 'Done'::text;
 END;
$BODY$
LANGUAGE plpgsql VOLATILE COST 100;
 
ALTER FUNCTION obs.partition_maintenance(text, text, date) OWNER TO postgres;
 
GRANT EXECUTE ON FUNCTION myschema.partition_maintenance(text, text, date) TO wxde



-- Helper Function for partition maintenance
CREATE OR REPLACE FUNCTION obs.export_partition(text, text) RETURNS text AS
$BASH$
#!/bin/bash
tablename=${1}
filename=${2}
# NOTE: pg_dump must be available in the path.
pg_dump -U wxde -t obs."${tablename}" wxde| gzip -c > ${filename} ;
$BASH$
LANGUAGE plsh;
 
ALTER FUNCTION obs.export_partition(text, text) OWNER TO postgres;
 
GRANT EXECUTE ON FUNCTION myschema.export_partition(text, text) TO wxde;
