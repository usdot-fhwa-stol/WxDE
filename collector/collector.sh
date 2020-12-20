#!/bin/bash

stations=$(cat << EOF
Amity
Angola
Argos
Birdseye
Black_River_Rest_Area_(Griffin)
Bloomington
Cloverdale
Columbus
Corydon
Covington
Crawfordsville
ElkhartEast
Evansville
Fort_Wayne
Fowler
Frankfort
Gas_City
Greensburg
HoweLaGrange
I65@I865
I-74_@_I-465
I-94_@_I-65
I-94_@_US-421
Jeffersonville
Kokomo
Loogootee
MarketSt
New_Castle
Penntown
Rensselaer
Scottsburg
Seymour
SouthBend
Sullivan
US-31_@_SR-38
Versailles
Vincennes
Westpoint
EOF
)

base_url="https://rwis.indot.in.gov/export/"
for station in $stations; do 
	echo $station
	url="$base_url$station.csv"
	$(wget "$url") && echo "$url...success" || (
		echo "$url...failed"
	)
	#cat $station.csv | head -n 1 >> headers
done
