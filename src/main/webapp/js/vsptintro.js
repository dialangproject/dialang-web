$('#back').prop('disabled', false).click(function (e) {

    dialang.switchState('tls');
    return false;
});

$('#next').prop('disabled', false).click(function (e) {

    dialang.switchState('vspt');
    return false;
});

$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/vsptintro/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    $('#confirm-skip-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});
    $('#confirm-skip-yes').on('click', dialang.skipVSPT);
    $('#confirm-skip-no').click(function (e) {
        $('#confirm-skip-dialog').dialog('close');
    });
});
