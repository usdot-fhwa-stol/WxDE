#!/usr/bin/env python
#
import csv
import pprint
import urllib

def download_csv(filename):
    testfile = urllib.URLopener()
    return testfile.retrieve("%s/%s" % (base_url, filename), filename)

base_url="https://rwis.indot.in.gov/export/"

stations = [
    ["Amity", None],
    ["Angola", None],
    ["Argos", None],
    ["Birdseye", None],
    ["Black_River_Rest_Area_(Griffin)", None],
    ["Bloomington", None],
    ["Cloverdale", None],
    ["Columbus", None],
    ["Corydon", None],
    ["Covington", None],
    ["Crawfordsville", None],
    ["ElkhartEast", None],
    ["Evansville", None],
    ["Fort_Wayne", None],
    ["Fowler", None],
    ["Frankfort", None],
    ["Gas_City", None],
    ["Greensburg", None],
    ["HoweLaGrange", None],
    ["I65@I865", None],
    ["I-74_@_I-465", None],
    ["I-94_@_I-65_", None],
    ["I-94_@_US-421", None],
    ["Jeffersonville", None],
    ["Kokomo", None],
    ["Loogootee", None],
    ["MarketSt", None],
    ["New_Castle", None],
    ["Penntown", None],
    ["Rensselaer", None],
    ["Scottsburg", None],
    ["Seymour", None],
    ["SouthBend", None],
    ["Sullivan", None],
    ["US-31_@_SR-38", None],
    ["Versailles", None],
    ["Vincennes", None],
    ["Westpoint", None],
]

known_fields = [
    'date_time', 
    'Spot Wind Direction \xc2\xb0', 
    'Wind Speed mph', 
    'Avg. Wind Speed mph', 
    'Avg. Wind Direction \xc2\xb0', 
    'Gust Wind Speed mph', 
    'Gust Wind Direction \xc2\xb0', 
    'Air Temperature \xc2\xb0F', 
    'Wet Bulb Temperature \xc2\xb0F', 
    'Dew Point \xc2\xb0F', 
    'Max Air Temperature \xc2\xb0F', 
    'Min Air Temperature \xc2\xb0F',
    'Precipitation Yes/No [logic]',
    'Precipitation Start Time timestamp',
    'Precipitation End Time timestamp',
    'Rel. Humidity %',
    'Road Condition Approach [logic]',
    'Road Temperature Approach \xc2\xb0F',
    'Subsurface Temperature 1 \xc2\xb0F',
    'Freezing Temperature Approach \xc2\xb0F',
    'Water Film Approach mil',
    'Saline Concentration Approach %',
    'Friction Approach N/A',
    'Road Condition Bridge [logic]',
    'Road Temperature Bridge \xc2\xb0F',
    'WaterFilm Height Bridge mil',
    'Freeze Point Bridge \xc2\xb0F',
    'Saline Concentration Bridge %',
    'Friction Bridge N/A',
    '',
    'Road Temp. Approach \xc2\xb0F',
    'Freeze Point \xc2\xb0F',
    'Sub Surface Temperature \xc2\xb0F',
    'Surface Salinity Approach %',
    'Road Temp. Bridge \xc2\xb0F',
    'SubSurface Temperature \xc2\xb0F',
    'Surface Salinity Bridge %',
    'Precipitation Situation [logic]',
    'Subsurface Temperature  \xc2\xb0F',
    'Friction N/A',
    'Visibility m',
    'Road Temp Bridge \xc2\xb0F',
    'Freeze Point Approach \xc2\xb0F',
    'Surface Temperature Approach \xc2\xb0F',
    'Subsurface Temperature Approach \xc2\xb0F',
    'Water Film  Approach mil',
    'Surface Salinity #3 %',
    'Road Condition # 3 [logic]',
    'Road Temp #3 \xc2\xb0F',
    'Freeze Point #3 \xc2\xb0F',
    'Road Condition 1 [logic]',
    'Road Condition 2 [logic]',
    'Road Temp. 1 \xc2\xb0F',
    'Road Temp. 2 \xc2\xb0F',
    'Freeze Point 1 \xc2\xb0F',
    'Freeze Point 2 \xc2\xb0F',
    'Surface Salinity 1 %',
    'Surface Salinity 2 %',
    'Freezing Temp. Approach \xc2\xb0F',
    'Saline Concentration Bridge  %',
    'Road Condition [logic]',
    'Surface Temperature \xc2\xb0F',
    'Freezing Temperature \xc2\xb0F',
    'Water Film  mil',
    'Saline Concentration %',
    'Freeze Point Bridge [index]',
    'Saline Concentration Bridge [rate]'
]

fields = []
for station in stations:
    filename = "%s%s" % (station[0], ".csv")
    try:
       receipt = download_csv(filename)       
       station[1] = receipt[1]
       print "Downloaded file %s..." % filename
    except IOError:
        print "An issue was encountered attempting to download %s from the server." % filename
        pass
    with open(filename, "r") as f_in:
        reader = csv.DictReader(f_in)
        headers = next(reader)
        for h in headers:                
            if h not in fields:
                fields.append(h)
fields.sort()

with open("header", "w") as h_out:
    pprint.pprint(fields, h_out, 4)
    h_out.close()

with open("output.csv", "w") as f_out:
    writer = csv.DictWriter(f_out, fieldnames=fields)
    f_out.write(",".join(fields) + '\n')
    for station in stations:
        filename = "%s%s" % (station[0], ".csv")
        with open(filename) as f_in:
            reader = csv.DictReader(f_in)
            for line in reader:
                writer.writerow(line)

