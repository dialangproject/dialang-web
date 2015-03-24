$('#skipback').prop('disabled', true);
$('#save-button').prop('disabled', true);

if (!dialang.flags.hideALS) {
    $('#back').prop('disabled', false).click(function (e) {
        return dialang.navigation.backRules.legend();
    });
}

$('#next').prop('disabled', false).click(function (e) {
    return dialang.navigation.nextRules.legend();
});

$('#skipforward').prop('disabled', true);

$.get('/dialang-content/legend/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
});
