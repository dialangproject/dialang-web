$('#help').prop('disabled', true);
$('#back').prop('disabled', false).click(function (e) {
    return dialang.navigation.backRules.singleuserreportmenu();
});

$.get('/dialang-content/singleuserreportmenu/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
});
