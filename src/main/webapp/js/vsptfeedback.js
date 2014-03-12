if (dialang.session.feedbackMode) {
    // You can only navigate back from vspt feedback when you
    // came from the feedback menu
    $('#back').prop('disabled', false).click(function (e) {
        return dialang.navigation.backRules.vsptfeedback();
    });
} else {
    $('#next').prop('disabled', false).click(function (e) {
        return dialang.navigation.nextRules.vsptfeedback();
    });
}

$.get('/dialang-content/vsptfeedback/' + dialang.session.al + '/' + dialang.session.vsptLevel + '.html', function (data) {

    $('#content').html(data);
    $('#score').html(dialang.session.vsptMearaScore);
    $("#vsptfeedback-tabs").tabs({ active: activeTab }).addClass( "ui-tabs-vertical ui-helper-clearfix" );
    $("#vsptfeedback-tabs li").removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
});
