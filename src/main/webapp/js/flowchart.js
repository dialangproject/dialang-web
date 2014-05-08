$('#skipback').prop('disabled', true);
$('#back').prop('disabled', false).click(function (e) {
    return dialang.navigation.backRules.flowchart();
});

if (!dialang.flags.hideTLS) {
    $('#next').prop('disabled', false).click(function (e) {
        return dialang.navigation.nextRules.flowchart();
    });
}

$('#skipforward').prop('disabled', true);

$.get('/dialang-content/flowchart/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    $('.dialog').dialog({
        width: 'auto',
        autoOpen: false,
        resizable: false
    });
});
