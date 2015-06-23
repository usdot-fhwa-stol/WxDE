$(function() {
	$("#descriptionModal").dialog({
		autoOpen : false,
		resizable : false,
		height : "auto",
		modal : true,
		show : {
			effect : "fade",
			duration : 1E3
		},
		hide : {
			effect : "fade",
			duration : 1E3
		}
	});
});
