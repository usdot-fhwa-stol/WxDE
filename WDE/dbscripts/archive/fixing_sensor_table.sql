# sensor table was loaded using MetadataUpdater while the distgroup table was empty.  As a result, the distgroup column was all null
# The following two queries were provided by Bryan on 4/26/2013

update meta.sensor set distgroup=2;
update meta.sensor set distgroup=1 where platformid in (select id from meta.platform where contribid=4);
