$(function() {
	$('#loading').fadeIn('slow');

	var strSection, strName, dataName, strDescription, dateCreated, timeCreated;

	//for functionality/effect for the loader.gif
	setTimeout( function() {
		
		$.ajax({
            url:'/resources/admin/feedback', 
            dataType: 'json',
            success: populateData,
            cache: false
        });
		
		function populateData(data) {
			var tempArray = new Array();
			if (data == null) {
				$("#noFeedbacks").show();
				$('#loading').fadeOut();
			}

			if (data != null) {
				if ($.isArray(data.feedback)) {
					tempArray = data.feedback;
				} else {
					tempArray.push(data.feedback);
				}
			}
				
			$.each(
				tempArray,
				function(index, item) {
	
					strName = item.name;
					dataName = strName;
					strSection = item.section
							.split('/');
					strSection = strSection[strSection.length - 1]
							.split('#')[0]
							.split('.')[0];
					// check if section is
					// empty then assign
					// index.jsp to it
					if (strSection == "")
						strSection = 'index';
						// check if name is
						// empty(undefined)
						// then assign Not
						// Given to it
					if (strName == "" || strName == undefined) {
						strName = '<em style="color: #aaa;">Not Provided</em>';
						dataName = 'Not Provided';
					}
					dateCreated = item.dateCreated2;
					if (dateCreated == "" || dateCreated == undefined) {
						dateCreated = '<em style="color: #aaa;">Not Provided</em>';
					}
					timeCreated = item.timeCreated;
					if (timeCreated == "" || timeCreated == undefined) {
						timeCreated = '<em style="color: #aaa;">Not Provided</em>';
					}
	
					strDescription = item.description.replace(/[\/\\'"<>]/g,'');
					
					$('.reports-table')
						.append(
							'<tr>'
								+ '<td id="feedbackType" data-type="'
								+ item.feedbackType.name
								+ '">'
								+ item.feedbackType.name
								+ '</td>'
								+ '<td id="feedbackID" class="align-center">'
								+ item.feedbackId
								+ '</td>'
								+ '<td id="feedbackUser" data-name="'
								+ dataName
								+ '" data-email="'
								+ item.email
								+ '">'
								+ 'Name: <b>'
								+ strName
								+ '</b> <br>'
								+ 'Email: <b>'
								+ item.email
								+ '</b></td>'
								+ '<td class="align-center" id="feedbackSection" data-section="'
								+ strSection
								+ '"><a class="page-link link" href="'
								+ item.section
								+ '" title="Go to Section" target="_blank">'
								+ strSection
								+ '</a></td>'
								+ '<td><a href="#" class="btn-dark align-center descriptionTrigger" data-msg="'
								+ strDescription
								+ '"><img src="/image/icons/light/fa-file.png" style="margin-bottom: -1px;" alt="File Icon" /> View</a>'
								+ '<td id="feedbackDate" data-name="'
								+ dataName
								+ '" data-date="'
								+ item.tsCreated
								+ '">'
								+ 'Date: <b>'
								+ dateCreated
								+ '</b> <br>'
								+ 'Time: <b>'
								+ timeCreated
								+ '</b></td>'
							+ '</tr>');

				$('.descriptionTrigger')
					.on(
						'click',
						function(e) {
							e.defaultPrevented;
							$('#descriptionWriter')
								.html(
									'<p><span class="label">Type: </span>'
										+ $(this)
											.parent('td')
											.siblings('#feedbackType')
											.attr('data-type')
									+ '</p>'
									+ '<p><span class="label">Name: </span>'
										+ $(this)
											.parent('td')
											.siblings('#feedbackUser')
											.attr('data-name')
									+ '</p>'
									+ '<p><span class="label">Email: </span>'
										+ $(this)
											.parent('td')
											.siblings('#feedbackUser')
											.attr('data-email')
									+ '</p>'
									+ '<p><span class="label">Section: </span>'
										+ $(this)
											.parent('td')
											.siblings('#feedbackSection')
											.attr('data-section')
										+ '.jsp'
									+ '</p>'
									+ '<p><span class="label">Description:</span></p>'
									+ '<p>'
										+ $(this).attr('data-msg')
									+ '</p>'
								);
							$("#descriptionModal").dialog("open");
						});

				$('#loading').fadeOut();
			});
				
		}
	}, 1000);
});

var table = $('#sortable-table');

$('#sort-type, #sort-date').css("cursor", "pointer").wrapInner(
		'<span class="explode" title="Click to sort this column"/>').each(
		function() {

			var th = $(this), thIndex = th.index(), inverse = true, caret;
			th.bind("click", function() {

				table.find('td').filter(function() {

					return $(this).index() === thIndex;

				}).sortElements(
						function(a, b) {

							return parseInt($.text([ a ])) > parseInt($
									.text([ b ])) ? inverse ? -1 : 1
									: inverse ? 1 : -1;

						}, function() {

							// parentNode is the element we want to move
							return this.parentNode;

						});

				if (inverse == true)
					caret = "icon-sort-up";
				else
					caret = "icon-sort-down";

				$(this).find(".icon-stack").html(
						'<i class="icon-sort" style="color: #AAA;"></i>'
								+ '<i class="' + caret + '"></i>');

				inverse = !inverse;

			});

	});