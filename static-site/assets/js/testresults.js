$('#back').prop('disabled', false).click(function () {

    dialang.switchState('feedbackmenu');
    return false;
});

dialang.launchTestLevelDialog = function (number) {

    $('.testlevel-dialog').dialog('close');
    $('#testlevel-dialog' + number).dialog('open');
};

$.get('/dialang-content/testresults/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.itemLevel + '.html', function (data) {

    $('#content').html(data);

    $('.testlevel-dialog').dialog({
        width: 'auto',
        autoOpen: false,
        resizable: false
    });
});

