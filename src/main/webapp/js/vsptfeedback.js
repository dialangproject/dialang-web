if (dialang.session.feedbackMode) {
    $('#back').prop('disabled', false).click(function () {

        dialang.switchState('feedbackmenu');
        return false;
    });
} else {
    // There is no SA for structures or vocabulary tests
    if (dialang.session.skill === 'vocabulary' || dialang.session.skill === 'structures') {
        $('#next').prop('disabled', false).click(function (e) {

            dialang.switchState('testintro');
            return false;
        });
    } else {
        $('#next').prop('disabled', false).click(function () {

            dialang.switchState('saintro');
            return false;
        });
    }
}

$.get('/dialang-content/vsptfeedback/' + dialang.session.al + '/' + dialang.session.vsptLevel + '.html', function (data) {

    $('#content').html(data);
    $('#score').html(dialang.session.vsptMearaScore);
    $("#vsptfeedback-tabs").tabs({ active: activeTab }).addClass( "ui-tabs-vertical ui-helper-clearfix" );
    $("#vsptfeedback-tabs li").removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
});
