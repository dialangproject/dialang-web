$('#back').prop('disabled', false).click(function () {

    dialang.switchState('feedbackmenu');
    return false;
});

dialang.launchTestLevelDialog = function (number) {

    $('.testleveldialog').dialog('close');
    $('#testleveldialog' + number).dialog('open');
};

$.get('/dialang-content/testresults/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.itemLevel + '.html', function (data) {

    $('#content').html(data);
    $('.testleveldialog').dialog({autoOpen: false, width: 500, height: 450});
});

