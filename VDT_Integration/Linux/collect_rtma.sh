#!/bin/bash

vdt_root=/home/zhengg/vdt
rtma_folder=$vdt_root/data/raw/rtma
interval=300

printf "Begin rtma data collector - " 

printf "Current time: $(date +'%Y%m%d %H%M%S'), "
now="$(date +'%s')"
let remainder=($interval - $now % $interval)
printf "wait for $remainder seconds\n"
sleep $remainder

while :
do
	now="$(date +'%Y%m%d')"
	datefolder=rtma.$now

	file="rtma.t$(date +'%H')z.2dvarges_ndfd.grb2"

	if [ ! -e $rtma_folder/$now ]
	then
		mkdir $rtma_folder/$now
	fi

	printf "downloading $datefolder/$file\n"
	curl -o $rtma_folder/$now/$file ftp://ftp.ncep.noaa.gov/pub/data/nccf/com/rtma/prod/$datefolder/$file

	now="$(date +'%Y%m%d %H%M%S')"
	printf "Current time: $now, "
	now="$(date +'%s')"
	let remainder=($interval - $now % $interval)
	printf "wait for $remainder seconds\n"
	sleep $remainder
done
