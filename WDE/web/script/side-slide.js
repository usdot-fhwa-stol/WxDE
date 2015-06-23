$( function() {
    var flag = true;
    $(".tab-trigger").click( function() {
        if(flag == false) {
            $("#side_menu").animate({left: '-410px'});
            $(this).html("OPEN SETTINGS");
            flag = true;
        }
        else {
            $("#side_menu").animate({left: '0'});
            $(this).html("CLOSE SETTINGS");
            flag = false;
        }
    });
});