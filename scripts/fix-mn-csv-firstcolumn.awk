#! /usr/bin/awk -f

BEGIN {

	FS = OFS = ",";

}

NR > 1 {

	part1_begin = 1;
	part1_end = length($1) - 3;
	part2_begin = part1_end + 1;
	part2_end = length($1);
	
	new_first_column = substr($1, 1, part1_end);
	
	new_second_column = substr($1, part2_begin, part2_end);
	gsub(/^0*/, "", new_second_column);
	if (new_second_column ~ /^ *$/) new_second_column = 0;
	
	$1 = new_first_column FS new_second_column;
	
}

1
