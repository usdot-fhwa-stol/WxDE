$(function() {
	var strURL = '';
	$("#dialog").dialog({
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
		},
		buttons : {
			"Continue" : function() {
				$(this).dialog("close");
				window.open(strURL);
			},
			Cancel : function() {
				$(this).dialog("close");
			}
		}
	});
	$(".exit-link").click(
			function(e) {
				e.preventDefault();
				strURL = $(this).attr("href");
				$("#link").html(
						'<em style="text-decoration: underline;">' + strURL
								+ "</em>");
				$("#dialog").dialog("open");
			});
});
