$(function() {
	var pageMenu = "#" + $("body").attr("id");
	pageMenu = pageMenu + ", " + pageMenu + " a";
	$(pageMenu).addClass('active');
});