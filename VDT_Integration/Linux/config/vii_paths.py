#!/usr/bin/env python

"""This module defines directory and file names used by GTGN."""


#/*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*
# * Copyright (c) 1995-2007 UCAR
# * University Corporation for Atmospheric Research(UCAR)
# * National Center for Atmospheric Research(NCAR)
# * Research Applications Program(RAP)
# * P.O.Box 3000, Boulder, Colorado, 80307-3000, USA
# * All rights reserved. Licenced use only.
# * $Date: 2013-04-19 21:41:44 $
# *=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*/

import os


if os.environ.has_key('VII_DATA'):
    DATA = os.environ['VII_DATA']
else:
    print "Environment variable VII_DATA must be defined."
    raise ValueError
 

# Name schema information

weatherbug_base = "weatherbug"
weatherbug_format = "%B.%D.i%I%F.%S"
weatherbug_patterns = [weatherbug_base, "YYYYMMDD", "HHMM", "", "nc"]

probe_message_qc_base = "vdt"
probe_message_qc_format = "%B.%D.%I%F.%S"
probe_message_qc_patterns = [probe_message_qc_base, "YYYYMMDD", "HHMM", "", "nc"]

probe_message_qc_history_base = "history_probe_msg_qc"

#
# VII directories
#

PROCESSED_DIR = os.path.join(DATA, "processed")
STATIC_DATA_DIR = os.path.join(DATA, "static")
CONFIG_DIR = os.path.join(STATIC_DATA_DIR, "config")
DERIVED_DATA_DIR = os.path.join(DATA, "derived")
LOCK_DIR = os.path.join(DATA, "lock")
LOG_DIR = os.path.join(DATA, "log")
RAW_DATA_DIR = os.path.join(DATA, "raw")


RADAR_DIR = os.path.join(RAW_DATA_DIR, "radar", "netcdf", "mosaic2d")
RTMA_DIR = os.path.join(RAW_DATA_DIR, "rtma")
CLOUD_MASK_DIR = os.path.join(PROCESSED_DIR, "satellite", "cloud_mask", "netcdf", "blend")
METAR_DIR = os.path.join(RAW_DATA_DIR, "metar", "netcdf")
RWIS_DIR = "/d2/ldm/data/dec_data/obs/madis"

#
# NOTE:
#
# TMP_DIR must be located on the same partition as the the derived
# data directories or the fileds in the derived data directories will
# not be written atomically.  This means that VII could erroneously
# attempt to process an incomplete derived data file.
#
TMP_DIR = os.path.join(DATA, "tmp")

#
# cdl files
#
CDL_DIR = os.path.join(STATIC_DATA_DIR, "cdl")

# used for netcdf conversions
WEATHERBUG_CDL = os.path.join(CDL_DIR, "weatherbug.cdl")

LOG_PY_SUFFIX = "pyl"

HISTORY_PROCESSED_BASE = "processed_history"

# log file names
Run_probe_message_dataset_manager_log_file = os.path.join(LOG_DIR, "run_probe_message_dataset_manager")
Run_ameritrak_ingest_log_file = os.path.join(LOG_DIR, "run_ameritrak_ingest")

# make directory tree

VII_TOP_DIR_LIST = [PROCESSED_DIR, STATIC_DATA_DIR, DERIVED_DATA_DIR, LOCK_DIR, LOG_DIR, RAW_DATA_DIR, CDL_DIR]

VII_SUB_DIR_LIST = [TMP_DIR]

VII_DIR_LISTING = VII_TOP_DIR_LIST + VII_SUB_DIR_LIST

for directory in VII_DIR_LISTING:
    if not os.path.exists(directory):
        print "mkdir %s" % directory
        os.makedirs(directory)
                            
