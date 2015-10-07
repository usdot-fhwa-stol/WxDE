using database wxde;

truncate table meta.qualityflag;

insert into meta.qualityflag(sourceid, updatetime, totime, qchcharflaglen, qchintflaglen, qchflaglabel) values (1, '2015-10-05 00:00:00+00', '2016-10-05 00:00:00+00', 24, 0, '{QchsSequenceComplete,QchsManualFlag,QchsServiceSensorRange,QchsServiceClimateRange,QchsServiceStep,QchsServiceLike,QchsServicePersist,QchsServiceBarnes,QchsServicePressure,Complete,Manual,Sensor_Range,Climate_Range,Step,Like_Instrument,Persistence,IQR_Spatial,Barnes_Spatial,Dew_Point,Sea_Level_Pressure,Precip_Accum,Model_Analysis,Neighboring_Vehicle,Standard_Deviation}');