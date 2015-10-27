#!/bin/bash
#
exec 3>&2 # logging stream (file descriptor 3) defaults to STDERR
verbosity=3 # default to show warnings
silent_lvl=0
crt_lvl=1
err_lvl=2
wrn_lvl=3
dbg_lvl=4
inf_lvl=5

notify() { log $silent_lvl "NOTE: $1"; } # Always prints
critical() { log $crt_lvl "CRITICAL: $2"; }
error() { log $err_lvl "ERROR: $1"; }
warn() { log $wrn_lvl "WARNING: $1"; }
debug() { log $dbg_lvl "DEBUG: $1"; }
inf() { log $inf_lvl "INFO: $1"; } # "info" is already a command
log() {
    if [ $verbosity -ge $1 ]; then
        datestring=`date +'%Y-%m-%d %H:%M:%S'`
        # Expand escaped characters, wrap at 70 chars, indent wrapped lines
        echo -e "$datestring $2" | fold -w70 -s | sed '2~1s/^/  /' >&3
    fi
}

base=$(dirname "$0")
if [ "$base" = "." ]; then
	base=$(pwd)
fi

data="${base}/data"

wget -o wget.log -r -l1 --no-parent -nd -nc -P ${data} --user=clarus --password=MNcdk755 -A ssi.atmos.*,ssi.Sub.*,ssi.surface.* ftp://rwis.dot.state.mn.us/external_images/
[ -f wget.log ] || {
	die "Could not find the wget.log file." | tee -a mn.log
}

echo Processing wget log to retrieve list of files downloaded...
for file in $(egrep  "surface.*txt.*saved" wget.log | cut -d ' ' -f 6 | sed 's/^.\(.*\).$/\1/'); do
(
    echo -n Applying awk script to $file...
    $base/fix-mn-csv-firstcolumn.awk $file > $file.tmp 2>&1
    mv $file.tmp $file 2>&1
    echo Done.
) | tee -a mn.log 
done
echo Complete.
