#!/usr/bin/env python

import os
import sys
import time
from optparse import OptionParser
import log_msg
from netCDF4 import Dataset
from pytz import timezone
import datetime
import calendar

MPH_TO_MPS = 0.44704

def main():
    usage_str = "%prog in_dir out_dir cdl spn_file"
    parser = OptionParser(usage = usage_str)
    parser.add_option("-d", "--date", dest="date", help="date")
    parser.add_option("-l", "--log", dest="log", help="log")

    (options, args) = parser.parse_args()

    if len(args) < 4:
        parser.print_help()
        sys.exit(2)

    in_dir = args[0]
    out_dir = args[1]
    cdl = args[2]
    spn_file = args[3]
    
    # ED need to change 300 to a parameter fed in on the command line
    ptime = time.mktime(time.gmtime()) - 300
    if options.date:
        ptime = time.mktime(time.strptime(options.date,"%Y%m%d.%H%M"))

    ptime = ptime - (ptime % 300)
    if options.log:
        logg = log_msg.LogMessage(options.log,"pyl")
    else:
        logg = log_msg.LogMessage("")

    logg.write_starting()
    logg.write_info("Executing: %s" % " ".join(sys.argv))
    logg.write_info("reading spn list from %s" % spn_file)
    f = open(spn_file,"r")
    spns = {}
    for l in f.readlines():
        if not ">Spn:" in l:
            continue
        ls = l.strip().split(",")
        spn = ls[2].replace(" ","")
        desc = ls[10]
        spns[spn] = desc
    f.close()
    time_tup = time.gmtime(ptime)
    day = time.strftime("%Y%m%d", time_tup)
    hhmm = time.strftime("%H%M", time_tup)
    in_day_dir = os.path.join(in_dir, day)
    fname = "ameritrak.%s.%s.txt" % (day,hhmm)
    in_file = os.path.join(in_day_dir, fname)
    if not os.path.exists(in_file):
        logg.write_info("%s not found" % in_file)
        logg.write_ending(0)
        sys.exit(0)
    logg.write_info("opening %s" % in_file)
    f = open(in_file,"r")

    data_dict = {}
    for l in f.readlines():
        if "GABE" in l:
            continue
        if l[-2] == ";":
            l = l[:-2]
        ls = l.strip().split(",")
        if ">Dj3X:" in ls[0]:
            add_dj(ls, logg, data_dict)
        elif ">DjTempX:" in ls[0]:
            add_djtemp(ls, logg, data_dict)
        elif ">VaiX:" in ls[0]:
            add_vaisala(ls, logg, data_dict)
        elif ">CanX:" in ls[0]:
            add_can(ls, logg, data_dict)
    f.close()

    if len(data_dict.keys()) == 0:
        logg.write_info("no valid data found")
        logg.write_ending(0)
        return 0
    write_nc(data_dict, logg, day, hhmm, out_dir, cdl, ptime)
    logg.write_ending(0)
    return 0

def write_nc(data_dict, logg, day, hhmm, out_dir, cdl, ptime):
    out_day_dir = os.path.join(out_dir, day)
    if not os.path.exists(out_day_dir):
        cmd = "mkdir -p %s" % out_day_dir
        logg.write_info(cmd)
        os.system(cmd)
    spds = []
    lats = []
    lons = []
    headings = []
    air_temps2 = []
    sur_temps = []
    nc_obstimes = []
    bar_press = []
    air_temps = []
    nc_vids = []
    source_ids = []
    ofname = "probe_message.%s.%s.nc" % (day, hhmm)
    ofpath = os.path.join(out_day_dir, ofname)
    cmd = "ncgen -o %s %s" % (ofpath, cdl)
    logg.write_info(cmd)
    os.system(cmd)
    logg.write_info("opening %s for writing" % ofpath)
    nc = Dataset(ofpath,"a")
    obstimes = data_dict.keys()
    obstimes.sort()
    csv_file = "/home/zhengg/vdt/data/derived/mn_time_range/mn_time_range_%s.csv" % day
    fd = open(csv_file,'a')
    for obstime in obstimes:
        if obstime < (ptime-60) or obstime > (ptime + 300 + 60):
            logg.write_info("%s out of time range" % (time.strftime("%Y%m%d %H:%M:%S", time.gmtime(obstime))))
            begin_time_range = "%s.%s00" % (day, hhmm)
            end_time_range = get_end_time_range(day, hhmm) + "00"
            time_strings = str(begin_time_range) + "," + str(end_time_range) + "," + str(time.strftime("%Y%m%d.%H%M%S", time.gmtime(obstime))) + "\n"
            fd.write(time_strings)
            continue
        vids = data_dict[obstime].keys()
        vids.sort()
        for vid in vids:
            nc_obstimes.append(obstime)
            lats.append(data_dict[obstime][vid]["lat"])
            lons.append(data_dict[obstime][vid]["lon"])
            headings.append(data_dict[obstime][vid]["heading"])
            air_temps2.append(data_dict[obstime][vid]["air_temp2"])
            sur_temps.append(data_dict[obstime][vid]["surface_temp"])
            spds.append(data_dict[obstime][vid]["speed"])
            bar_press.append(data_dict[obstime][vid]["bar_pressure"])
            air_temps.append(data_dict[obstime][vid]["air_temp"])
            nc_vids.append(list(vid.ljust(32,'\0')))
            source_ids.append(data_dict[obstime][vid]["source_id"])
            
    fd.close()
    current_time_value = time.time()
    nc_rectimes = [current_time_value] * len(nc_obstimes)

    nc.variables["latitude"][:] = lats
    nc.variables["longitude"][:] = lons
    nc.variables["air_temp2"][:] = air_temps2
    nc.variables["surface_temp"][:] = sur_temps
    nc.variables["obs_time"][:] = nc_obstimes
    nc.variables["rec_time"][:] = nc_rectimes
    nc.variables["speed"][:] = spds
    nc.variables["heading"][:] = headings
    nc.variables["bar_pressure"][:] = bar_press
    nc.variables["air_temp"][:] = air_temps
    nc.variables["vehicle_id"][:] = nc_vids
    nc.variables["source_id"][:] = source_ids
    nc.close()

def add_can(ls,logg,data_dict):
    spn_list = ls[10].split("|")
    vid = ls[2]
    if "null" in ls[4] or "null" in ls[5]:
        return
    lat = float(ls[4])
    lon = float(ls[5])
    heading = float(ls[6])
    speed = float(ls[8]) * MPH_TO_MPS
    obs_time_str = ls[3]
    obstime = get_obs_time(obs_time_str,logg)
    if obstime == None:
        return
    if not data_dict.has_key(obstime):
        data_dict[obstime] = {}
    if not data_dict[obstime].has_key(vid):
        data_dict[obstime][vid] = get_fill_val()
    #nc_vids.append(list(vid.ljust(32,'\0')))
    data_dict[obstime][vid]["source_id"] = list("ameritrak_can".ljust(32, '\0'))
    data_dict[obstime][vid]["lat"] = lat
    data_dict[obstime][vid]["lon"] = lon
    data_dict[obstime][vid]["heading"] = heading
    data_dict[obstime][vid]["speed"] = speed
    for spn in spn_list:
        ls = spn.split(" ")
        if len(ls) < 3:
            continue
        (num,val,unit) = ls
        num = float(num)
        val = float(val)
        if num == 108:
            data_dict[obstime][vid]["bar_pressure"] = val*10.0
        if num == 171:
            data_dict[obstime][vid]["air_temp"] = val - 273.0
    return 0

def add_dj(ls,logg,data_dict):
    if "null" in ls[4]:
        return
    vid = ls[2]
    lat = float(ls[4])
    lon = float(ls[5])
    try:
        heading = float(ls[6])
    except:
        heading = -9999.0
            
    surface_temp = get_value(ls[13].replace("F",""),f_to_c)
    air_temp = get_value(ls[14].replace("F",""),f_to_c)
    speed = float(ls[8]) * MPH_TO_MPS
    
    obs_time_str = ls[3]
    obstime = get_obs_time(obs_time_str,logg)
    if obstime == None:
        return

    if not data_dict.has_key(obstime):
        data_dict[obstime] = {}
    if not data_dict[obstime].has_key(vid):
        data_dict[obstime][vid] = get_fill_val()
    data_dict[obstime][vid]["source_id"] = list("ameritrak_dj".ljust(32, '\0'))
    data_dict[obstime][vid]["speed"] = speed
    data_dict[obstime][vid]["lat"] = lat
    data_dict[obstime][vid]["lon"] = lon
    data_dict[obstime][vid]["heading"] = heading
    data_dict[obstime][vid]["air_temp2"] = air_temp
    data_dict[obstime][vid]["surface_temp"] = surface_temp

def add_djtemp(ls, logg, data_dict):
    if "null" in ls[4]:
        return
    vid = ls[2]

    obs_time_str = ls[3]
    obstime = get_obs_time(obs_time_str, logg)
    if obstime == None:
        return

    lat = get_value(ls[4])
    lon = get_value(ls[5])
    heading = get_value(ls[6])
    try:
        gps_quality = int(ls[7])
    except:
        gps_quality = 0
    if gps_quality == 0:
        return
    speed = get_value(ls[8], mph_to_mps)
    surface_temp = get_value(ls[10].replace("F",""), f_to_c)
    air_temp = get_value(ls[11].replace("F",""), f_to_c)
    
    if not data_dict.has_key(obstime):
        data_dict[obstime] = {}
    if not data_dict[obstime].has_key(vid):
        data_dict[obstime][vid] = get_fill_val()
    data_dict[obstime][vid]["source_id"] = list("ameritrak_djtemp".ljust(32, '\0'))
    data_dict[obstime][vid]["speed"] = speed
    data_dict[obstime][vid]["lat"] = lat
    data_dict[obstime][vid]["lon"] = lon
    data_dict[obstime][vid]["heading"] = heading
    data_dict[obstime][vid]["air_temp2"] = air_temp
    data_dict[obstime][vid]["surface_temp"] = surface_temp

def add_vaisala(ls, logg, data_dict):
    if "null" in ls[4]:
        return
    vid = ls[2]

    obs_time_str = ls[3]
    obstime = get_obs_time(obs_time_str, logg)
    if obstime == None:
        return

    lat = get_value(ls[4])
    lon = get_value(ls[5])
    heading = get_value(ls[6])
    try:
        gps_quality = int(ls[7])
    except:
        gps_quality = 0
    if gps_quality == 0:
        return
    speed = get_value(ls[8], mph_to_mps)
    surface_temp = get_value(ls[10].replace("F",""), f_to_c)
    air_temp = get_value(ls[11].replace("F",""), f_to_c)
    dew_temp = get_value(ls[12].replace("F",""), f_to_c)
    humidity = get_value(ls[13].replace("F",""))

    if not data_dict.has_key(obstime):
        data_dict[obstime] = {}
    if not data_dict[obstime].has_key(vid):
        data_dict[obstime][vid] = get_fill_val()
    data_dict[obstime][vid]["source_id"] = list("ameritrak_vaisala".ljust(32, '\0'))
    data_dict[obstime][vid]["speed"] = speed
    data_dict[obstime][vid]["lat"] = lat
    data_dict[obstime][vid]["lon"] = lon
    data_dict[obstime][vid]["heading"] = heading
    data_dict[obstime][vid]["air_temp2"] = air_temp
    data_dict[obstime][vid]["surface_temp"] = surface_temp
    data_dict[obstime][vid]["dew_temp"] = dew_temp
    data_dict[obstime][vid]["humidity"] = humidity

def get_obs_time(obs_time_str, logg):
    date_format = "%y-%m-%d %H:%M:%S"
    date_format2 = "%Y-%m-%d %H:%M:%S"
    date_format3 = "%y-%m-%d-%H-%M-%S"
    date_format4 = "%Y-%m-%d %H:%M:"
    date_format5 = "%Y-%m-%d-%H-%M-"
    date_format6 = "%Y-%m-%d %H:%M:%S"
    date_format7 = "%Y-%m-%d-%H-%M-%S"
    obs_time_str = obs_time_str.strip().replace(".0","")
    try:
        otime = datetime.datetime.strptime(obs_time_str, date_format)
    except:
        try:
            otime = datetime.datetime.strptime(obs_time_str, date_format2)
        except:
            try:
                otime = datetime.datetime.strptime(obs_time_str, date_format3)
            except:
                try:
                    otime = datetime.datetime.strptime(obs_time_str, date_format4)
                except:
                    try:
                        otime = datetime.datetime.strptime(obs_time_str, date_format5)
                    except:
                        try:
                            otime = datetime.datetime.strptime(obs_time_str, date_format6)
                        except:
                            try:
                                otime = datetime.datetime.strptime(obs_time_str, date_format7)                           
                            except:
                                logg.write_info("couldn't parse %s using format %s, format %s, format %s, format %s, format %s, format %s, or format %s" % (obs_time_str, date_format, date_format2, date_format3, date_format4, date_format5, date_format6, date_format7))
                                return None
    central = timezone("US/Central")
    utc = timezone("UTC")
    otime = central.localize(otime)
    utc_otime = otime.astimezone(utc)
    obstime = time.mktime(utc_otime.timetuple())
    return (obstime - (obstime % 5))

def get_fill_val():
    return {"lat":-9999,
            "lon":-9999,
            "obstime":-9999,
            "air_temp":-9999,
            "air_temp2":-9999,
            "surface_temp":-9999,
            "speed":-9999,
            "heading":-9999,
            "bar_pressure":-9999}

def f_to_c(v):
    return (v - 32.0) * 5.0/9.0

def mph_to_mps(v):
    return v  * MPH_TO_MPS

def get_value(v,cnv = None):

    try:
        v = float(v)
    except:
        return -9999

    if cnv != None:
        return cnv(v)
    else:
        return v

def get_end_time_range(day, hhmm):
    """Add five minutes to begin time period to get the end of the time range"""
    date_str = day + hhmm
    date_tuple = datetotuple(date_str)
    unix_time = calendar.timegm(date_tuple)
    end_unix_time = unix_time + 300 # 5min
    end_time_tuple = time.gmtime(end_unix_time)
    end_time_range = time.strftime("%Y%m%d.%H%M", end_time_tuple)
    return end_time_range
        
def datetotuple(str):
    """Convert year month day string to time tuple. Works with YYYYMMDDHHMMSS, YYMMDDHHMM, YYMMDDHH, YYMMDD"""
    length = len(str)
    if length == 14:
	return (int(str[:4]), int(str[4:6]), int(str[6:8]), int(str[8:10]), int(str[10:12]), int(str[12:14]), 0, 0, 0)
    elif length == 8:
	return (int(str[:4]), int(str[4:6]), int(str[6:8]), 0, 0, 0, 0, 0, 0)
    elif length == 10:
	return (int(str[:4]), int(str[4:6]), int(str[6:8]), int(str[8:10]), 0, 0, 0, 0, 0)
    elif length == 12:
	return (int(str[:4]), int(str[4:6]), int(str[6:8]), int(str[8:10]), int(str[10:12]), 0, 0, 0, 0)

if "__main__" == __name__:
    sys.exit(main())
