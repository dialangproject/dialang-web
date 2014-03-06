$('#skipback').prop('disabled', true);

$('#back').prop('disabled', false).click(function (e) {

    dialang.switchState('legend');
    return false;
});

$('#next').prop('disabled', false).click(function (e) {

    dialang.switchState('tls');
    return false;
});
$('#skipforward').prop('disabled', true);

$.get('/dialang-content/flowchart/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    $('.dialog').dialog({autoOpen: false, width: 500, height: 450});
});
