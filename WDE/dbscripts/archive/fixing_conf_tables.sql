# csvc table was empty.  Enter one entry below so the collector can collect something
# both the csvcoldef and qchseq tables need to be fixed after content from these two tables were migrated from MySQL to PostgreSQL

insert into conf.csvc (id, active, contribid, midnightoffset, collectioninterval, instancename, classname, endpoint, username, password) 
values(33, 1, 43, 185, 300, 'USA/SC', 'wde.cs.ascii.CsvSvc', 'ftp://birice.vaisala.com', 'scdot', 'BoykinSpaniel');

update conf.csvc set classname=replace(classname, 'clarus.cs', 'wde.cs') where classname like 'clarus.cs%';

update conf.csvcoldef set classname=replace(classname, 'clarus.cs', 'wde.cs') where classname like 'clarus.cs%';

update conf.qchseq set classname=replace(classname, 'clarus.qchs', 'wde.qchs') where classname like 'clarus.qchs%';

update conf.qchseqnew set classname=replace(classname, 'clarus.qchs', 'wde.qchs') where classname like 'clarus.qchs%';

update conf.xmldef set classname=replace(classname, 'clarus.cs', 'wde.cs') where classname like 'clarus.cs%';

update conf.csvcoldef set classname=replace(classname, 'wde.cs.ascii.StationCode', 'wde.cs.ascii.PlatformCode') where classname like 'wde.cs.ascii.StationCode%';

update conf.xmldef set classname=replace(classname, 'wde.cs.xml.StationCode', 'wde.cs.xml.PlatformCode') where classname like 'wde.cs.xml.StationCode%';