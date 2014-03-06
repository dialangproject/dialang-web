$('#next').prop('disabled', false).click(function () {

    dialang.switchState('sa');
    return false;
});
$('#skipforward').prop('disabled', false).click(function () {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/saintro/' + dialang.session.al + '/' + dialang.session.skill + '.html', function (data) {

    $('#content').html(data);
    $('#confirm-skip-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});

    $('#confirm-skip-yes').click(function (e) {

        $('#confirm-skip-dialog').dialog('destroy');
        dialang.switchState('testintro');
        return false;
    });

    $('#confirm-skip-no').click(function (e) {

        $('#confirm-skip-dialog').dialog('close');
        return false;
    });
});
