#!/bin/bash
#
base=$(dirname "$0")
if [ $base = '.' ]; then
	base=$(pwd)
fi

data='/opt/collectors/MN/data/RWISexport'
#data="${base}/mn_files"

wget -o wget.log -r -l1 --no-parent -nd -nc -P ${data} --user=clarus --password=MNcdk755 -A ssi.atmos.*,ssi.Sub.*,ssi.surface.* ftp://rwis.dot.state.mn.us/external_images/

echo Processing wget log to retrieve list of files downloaded...
for file in $(egrep  "surface.*txt.*saved" wget.log | cut -d ' ' -f 6 | sed 's/^.\(.*\).$/\1/'); do
	echo Applying awk script to $file...
	$base/fix-mn-csv-firstcolumn.awk $file > $file.tmp
	mv $file.tmp $file
done

echo Done.

