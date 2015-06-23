#!/usr/bin/env python

""""""

# ============================================================================== #
#                                                                                #
#   (c) Copyright, 2011 University Corporation for Atmospheric Research (UCAR).  #
#       All rights reserved.                                                     #
#                                                                                #
#       File: $RCSfile: run_probe_message_dataset_manager.py,v $                                           #
#       Version: $Revision: 1.4 $  Dated: $Date: 2013-04-19 21:58:14 $           #
#                                                                                #
# ============================================================================== #

import log_msg
import os
import sys
from optparse import OptionParser
import vii_paths
import time
import datetime
import string

def main():

    usage_str = "%prog"
    parser = OptionParser(usage = usage_str)
    parser.add_option("-d", "--date", dest="date", help="date")
    parser.add_option("-l", "--log", dest="log", help="write log messages to specified file")

    (options, args) = parser.parse_args()

    if len(args) < 0:
        parser.print_help()
        sys.exit(2)

    if options.log:
        logg = log_msg.LogMessage(options.log,"pyl")
    else:
        logg = log_msg.LogMessage(vii_paths.Run_probe_message_dataset_manager_log_file)

    # Get the begin time and end time
    begin_time = time.mktime(time.gmtime()) - 300
    if options.date:
        begin_time = time.mktime(time.strptime(options.date,"%Y%m%d.%H%M"))
    begin_time = begin_time - (begin_time % 300)
    end_time = begin_time + 300

    # Get the radar data times
    begin_time_radar = time.mktime(time.gmtime()) - 120
    if options.date:
        begin_time_radar = time.mktime(time.strptime(options.date,"%Y%m%d.%H%M"))
    begin_time_radar = begin_time_radar - (begin_time_radar % 120)
    end_time_radar = begin_time_radar + 120
    begin_time_tup_radar = time.gmtime(begin_time_radar)
    day_radar = time.strftime("%Y%m%d", begin_time_tup_radar)
    hhmm_radar = time.strftime("%H%M", begin_time_tup_radar)

    # Get previous hour and/or day for model data
    model_time = begin_time - 3600
    model_time_tup = time.gmtime(model_time)
    model_day = time.strftime("%Y%m%d", model_time_tup)
    model_hhmm = time.strftime("%H%M", model_time_tup)

    # Convert begin time and end time
    begin_time_tup = time.gmtime(begin_time)
    day = time.strftime("%Y%m%d", begin_time_tup)
    hhmm = time.strftime("%H%M", begin_time_tup)

    end_time_tup = time.gmtime(end_time)
    end_day = time.strftime("%Y%m%d", end_time_tup)
    end_hhmm = time.strftime("%H%M", end_time_tup)
    begin_time = day + hhmm
    end_time = end_day + end_hhmm

    logg.write_starting()
    logg.write_info("Executing: %s" % " ".join(sys.argv))

    # Prepare file names and paths
    radar_fname_path = "radar.2d.tile%%d.%s.%s.nc" % (day_radar, hhmm_radar)
    rtma_fname = "rtma.t%sz.2dvarges_ndfd.grb2" % (model_hhmm[:2])
    cloud_fname = "cloud.%s.%s.nc" % (day, hhmm)
    metar_fname = "metar.%s.%s.nc" % (day, hhmm)
    rwis_fname = "madis.%s.%s00.nc" % (day, hhmm[0:2])
    probe_message_fname = "probe_message.%s.%s.nc" % (day, hhmm)
    radar_fpath= os.path.join(vii_paths.RADAR_DIR, day, radar_fname_path)
    rtma_fpath = os.path.join(vii_paths.RTMA_DIR, model_day, rtma_fname)
    cloud_fpath = os.path.join(vii_paths.CLOUD_MASK_DIR, day, cloud_fname)
    metar_fpath = os.path.join(vii_paths.METAR_DIR, day, metar_fname)
    rwis_fpath = os.path.join(vii_paths.RWIS_DIR, day, rwis_fname)
    in_dir = os.path.join(vii_paths.PROCESSED_DIR, "mn_probe_message")
    probe_message_input_file = os.path.join(in_dir, day, probe_message_fname)
    out_dir = os.path.join(vii_paths.PROCESSED_DIR, "mn_vdt_output")
    out_day_dir = os.path.join(out_dir, day)
    qc_out_fname = "vdt.%s.%s.nc" % (day, hhmm)
    qc_out_fpath = os.path.join(out_day_dir, qc_out_fname)
    log_dir = vii_paths.LOG_DIR
    log_file = "%s/probe_message_dataset_manager" % log_dir
    seg_file = os.path.join(vii_paths.CDL_DIR, "mn_roads.nc")
    #seg_file = os.path.join(vii_paths.CDL_DIR, "mn_roads.20130807.nc")
    config_file = os.path.join(vii_paths.CONFIG_DIR, "vdt_config.cfg")
    segment_statistics_out_fname = "segment_statistics.%s.%s.nc" % (day, hhmm)
    segment_statistics_out_fpath = os.path.join(out_day_dir, segment_statistics_out_fname)
    segment_assessment_out_fname = "segment_assessment.%s.%s.nc" % (day, hhmm)
    segment_assessment_out_fpath = os.path.join(out_day_dir, segment_assessment_out_fname)
    grid_cell_statistics_out_fname = "grid_cell_statistics.%s.%s.nc" % (day, hhmm)
    grid_cell_statistics_out_fpath = os.path.join(out_day_dir, grid_cell_statistics_out_fname)

    if not os.path.exists(out_day_dir):
        cmd = "mkdir -p %s" % out_day_dir
        logg.write_info(cmd)
        os.system(cmd)
    opts = {"-m" : (metar_fpath,True),
            "-r" : (radar_fpath,False),
            "-w" : (rwis_fpath,True),
            "-c" : (cloud_fpath,True),
            "-a" : (rtma_fpath,True)}
    
    cmd = ["./probe_message_dataset_manager","-l %s" % log_file]
    for opt in opts.keys():
        (fpath,check) = opts[opt]
        if check and not os.path.exists(fpath):
            logg.write_info("skipping option %s, %s not found" % (opt, fpath))
        elif not check:
            # Added for radar data which passes in a pattern not the exact filename
            cmd.append("%s %s" % (opt, fpath))
        else:
            cmd.append("%s %s" % (opt, fpath))

    # Grid cell statistics file
    grid_cell_statistics_output = "-g %s" % grid_cell_statistics_out_fpath
    cmd.append(grid_cell_statistics_output)

    # Config file
    cmd.append(config_file)

    # Begin time 
    cmd.append(begin_time)

    # End time 
    cmd.append(end_time)

    # Road segment file
    cmd.append(seg_file)

    # Probe message file
    cmd.append(probe_message_input_file)

    # Probe message qc'd output file
    cmd.append(qc_out_fpath)

    # Prboe message qc statistics output file
    cmd.append(segment_statistics_out_fpath)

    # Road segment assessment output file
    cmd.append(segment_assessment_out_fpath)

    cmd = string.join(cmd," ")

    # Execute command
    print "cmd: %s\n" % cmd
    logg.write_info("Executing: %s" % cmd)
    ret = os.system(cmd)
    ret = 0
    logg.write_ending(ret)
    return ret

if __name__ == "__main__":

   sys.exit(main())
