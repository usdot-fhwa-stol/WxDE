#!/bin/bash

vdt_root=/home/zhengg/vdt
metar_input=$vdt_root/data/raw/metar/ascii
metar2nc_script=$vdt_root/ingest/metar/metar2nc
interval=300

printf "Begin metar data collector - " 

now="$(date +'%Y%m%d %H%M%S')"
printf "Current time: $now, "
now="$(date +'%s')"
let remainder=($interval - $now % $interval)
printf "wait for $remainder seconds\n"
sleep $remainder

while :
do
	ofile="$(date +'%Y%m%d_%H').metar"
	ifile="$(date +'%H')Z.TXT"

	printf "downloading $ifile\n"
	curl -o $metar_input/$ofile ftp://tgftp.nws.noaa.gov/data/observations/metar/cycles/$ifile

	printf "converting metar data to NetCDF\n"
	cd $metar2nc_script 
	./run_metar2nc.py CONUS_config
	cd $vdt_root

	now="$(date +'%Y%m%d %H%M%S')"
	printf "Current time: $now, "
	now="$(date +'%s')"
	let remainder=($interval - $now % $interval)
	printf "wait for $remainder seconds\n"
	sleep $remainder
done
