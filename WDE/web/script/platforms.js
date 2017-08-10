$( function() {
	
	//start/show loader.gif
	$('#loading').fadeIn();
	
	var csvStations = '';
	
	//get hash from url to set source of our webservice
	var locHash = location.hash.split("#")[1],
		hashSource = locHash.split('.')[0],
		dataSource = locHash.split('.')[1],
    //Is the hash value vulnerable to XSS?
		jsonSource = '/resources/auth/platforms/contributor/' + hashSource + '?' + csrf_nonce_param,
		fileName = dataSource + '-Stations';

	$('#sourceName').html(dataSource);
	
	//for functionality/effect for the loader.gif
	setTimeout( function() {
	
	$.ajax({url:jsonSource, dataType: 'JSON', success: populatePlatform, cache: false});
	
	function populatePlatform(data) {
		csvStations = JSON2CSV(data, "platform");
		$.each(data.platform, function(key, value){
			$('.reports-table').append(
				'<tr>'
					+ '<td>'
					+ value.stationCode
					+ '</td>'
					+ '<td class="align-center">'
					+ value.category
					+ '</td>'
					+ '<td>'
					+ value.description
					+ '</td>'
					+ '<td>'
					+ value.locBaseLat
					+ '</td>'
					+ '<td>'
					+ value.locBaseLong
					+ '</td>'
					+ '<td class="align-center">'
					+ value.locBaseElev
					+ '</td>'
//					+ '<td>'
//					+ value.updateTime
//					+ '</td>'
				+ '</tr>');
		});
		console.log(csvStations);
	}
		$('#loading').fadeOut();

	}, 1000); //end setTimeout()

	//select table headers to add click-ability to sort column
	$('#sort-code, #sort-category, #sort-description, #sort-latitude, #sort-longitude, #sort-elevation, #sort-update').css("cursor", "pointer")
		//add element title-like effect to disclose to users that this column is sortable
		.wrapInner('<span class="explode" title="Click to sort this column"/>')
		//start of sorting
		.each( function() {

				var th = $(this), thIndex = th.index(), inverse = true, caret;
				th.bind("click", function() {

					$('#sortable-table').find('td').filter(function() {

						return $(this).index() === thIndex;

					}).sortElements(
							function(a, b) {

								return $.text([ a ]) > $.text([ b ]) ? inverse ? -1
										: 1 : inverse ? 1 : -1;

							}, function() {

								// parentNode is the element we want to move
								return this.parentNode;

							});

					if (inverse == false)
						caret = "icon-sort-up";
					else
						caret = "icon-sort-down";

					$(this).find(".icon-stack").html(
							'<i class="icon-sort" style="color: #AAA;"></i>'
									+ '<i class="' + caret + '"></i>');

					//set initial sort if ascending or descending;
					//inverse = !inverse means initially column data are descending
					inverse = !inverse;

				});

			});
	
	//saving data as csv file.
	$('#saveStations').downloadify({
		filename: function(){
			return fileName + '.csv';
		},
		data: function(){ 
			return csvStations;
		},
		onComplete: function(){ alert('Your File Has Been Saved!'); },
		onCancel: function(){ alert('You have cancelled the saving of this file.'); },
		onError: function(){ alert('You must put something in the File Contents or there will be nothing to save!'); },
		swf: '/vendor/downloadify/media/downloadify.swf',
		downloadImage: '/vendor/downloadify/images/downloadcsv.png',
		width: 120,
		height: 28,
		transparent: true,
		append: false
	});
	
    function JSON2CSV(objArray, subObject) {
    	
    	var array = null, str = '', line = '';

    	if (subObject == null || subObject === '') {
    		array = objArray;
    	} else {
    		array = objArray[subObject];
    	}

        for (var index in array[0]) {
            var value = index + "";
            line += '"' + value.replace(/"/g, '""') + '",';
        }

        line = line.slice(0, -1);
        str += line + ',\r\n';

        for (var i = 0; i < array.length; i++) {
            var line = '';

            for (var index in array[i]) {
                if (typeof array[i][index] != 'object') {
                    var val = array[i][index] + "";
                    line += '"' + val.replace(/"/g, '""') + '",';
                } else {
                	line += ',';
                }
            }

            line = line.slice(0, -1);
            str += line + ',\r\n';
        }
        return str;
        
    }

}); //end of jQuery