$( function() {
	
	//start/show loader.gif
	$('#loading').fadeIn('slow');
	
	var exitLinkURL = '',
	    strDisclaimer = "",
	    jsonContributors;
    
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
    
    //for functionality/effect for the loader.gif
	setTimeout( function() {

		$.ajax({
			url:'/resources/auth/contributors?' + csrf_nonce_param,
			dataType: 'json',
			success: populateData,
			cache: false
			});

		function populateData(data) {			
	        $.each( 
					data.contributor,
					function(key, value) {
						strDisclaimer = value.disclaimer;
						if (strDisclaimer == undefined || typeof strDisclaimer == 'object') {
							strDisclaimer = "<td class=\"align-center\"><span style=\"color: #999\">none</span></td>";
							saveDisclaimer = "none";
						}
						else {
							saveDisclaimer = strDisclaimer;
							strDisclaimer = '<td class=\"align-center\"><a href="'
								+ strDisclaimer
								+ '" class="btn-dark btn-access align-center exit-link"><img src="/image/icons/light/fa-file.png" alt="Sort Icon" style="margin-bottom: -1px;" /> View</a>';
						};
									
						$('.reports-table').append(
							'<tr>'
								+ '<td id="sourceName" data-type="'
								+ value.name
								+ '">'
								+ value.name
								+ '</td>'
								+ '<td>'
								+ value.agency
								+ '</td>'
								+ strDisclaimer
								+ '<td><a href="/auth2/platforms.jsp'
                + '?' + csrf_nonce_param
								+ '#' + value.contributorId + '.' + value.name
								+ '" class="btn-dark btn-access align-center"><img src="/image/icons/light/fa-file.png" alt="Sort Icon" style="margin-bottom: -1px;" /> View</a>'
							+ '</tr>');
					} //end function(key, value)
				); //end $.each
				
			$(".exit-link").click(
					function(e) {
						e.preventDefault();
						exitLinkURL = $(this).attr("href");
						$("#link").html(
								'<em style="text-decoration: underline; margin-right: 19px;">'
										+ exitLinkURL
										+ "</em>");
						$("#dialog").dialog("open");
					});

			$('#loading').fadeOut();
			
		}//end of function populateData()

		//Get data for building the downloadable CSV file
		//This required a separate ajax call because it just
		//needs fewer data to present.
		$.ajax({
			url:'/resources/auth/contributors/subset?' + csrf_nonce_param,
			dataType: 'json',
			success: function(data) {
				jsonContributors = JSON2CSV(data, "contributor");
				
				$('#saveDataSource').downloadify({
					filename: function(){
						return "datasource.csv";
					},
					data: function(){ 
						return jsonContributors;
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
			}
		});//end of ajax call to resources/auth/contributors/subset

	}, 1000);//end setTimeout()

	//dialog for opening disclaimer links
	$("#dialog").dialog({
		autoOpen : false,
		resizable : false,
		height : "auto",
		width: "auto",
		modal : true,
		show : {
			effect : "fade",
			duration : 1E3
		},
		hide : {
			effect : "fade",
			duration : 1E3
		},
		buttons : {
			"Continue" : function() {
				$(this).dialog("close");
				window.open(exitLinkURL);
			},
			Cancel : function() {
				$(this).dialog("close");
			}
		}
	});
	
	//select table headers to add click-ability to sort column
	$('#sort-name, #sort-agency').css("cursor", "pointer")
		//add element title-like effect to disclose to users that this column is sortable
		.wrapInner('<span class="explode" title="Click to sort this column"/>')
		//start of sorting
		.each( function() {

			var th = $(this), thIndex = th.index(), inverse = false, caret;
			
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

				if (inverse == true) {
					caret = "icon-sort-up";
				} else {
					caret = "icon-sort-down";
				}

				$(this).find(".icon-stack").html(
						'<i class="icon-sort" style="color: #AAA;"></i>'
								+ '<i class="' + caret + '"></i>');
				
				//set initial sort if ascending or descending;
				//inverse = !inverse means initially column data are descending
				inverse = !inverse;

			});

		});

});
