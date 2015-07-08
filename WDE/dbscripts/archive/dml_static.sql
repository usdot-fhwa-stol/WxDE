insert INTO meta.distGroup (description) values ('Do not send to anyone');
insert INTO meta.distGroup (description) values ('Available for everyone');

insert INTO meta.source (staticId, updateTime, name) 
values (1, current_timestamp, 'Clarus Collector');

insert INTO meta.qualityFlag (sourceId, updateTime, toTime, qchCharFlagLen, qchIntFlagLen, qchFlagLabel)
values (1, '2008-12-31', '2009-06-12 18:00', 9, 0, '{
"QchsSequenceComplete","QchsManualFlag","QchsServiceSensorRange",
"QchsServiceClimateRange","QchsServiceStep","QchsServiceLike",
"QchsServicePersist","QchsServiceBarnes","QchsServicePressure"}');
insert INTO meta.qualityFlag (sourceId, updateTime, toTime, qchCharFlagLen, qchIntFlagLen, qchFlagLabel)
values (1, '2009-06-12 18:00', '2011-03-03 12:00', 10, 0, '{
"QchsSequenceComplete","QchsManualFlag","QchsServiceSensorRange",
"QchsServiceClimateRange","QchsServiceStep","QchsServiceLike",
"QchsServicePersist","QchsServiceBarnes","QchServiceDewpoint","QchsServicePressure"}');
insert INTO meta.qualityFlag (sourceId, updateTime, qchCharFlagLen, qchIntFlagLen, qchFlagLabel)
values (1, '2011-03-03 12:00', 12, 0, '{
"Complete","Manual","Sensor_Range","Climate_Range","Step","Like_Instrument",
"Persistence","IQR_Spatial","Barnes_Spatial","Dew_Point","Sea_Level_Pressure","Precip_Accum"}');
