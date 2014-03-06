$('#skipback').prop('disabled', true);
$('#back').prop('disabled', false).click(function (e) {
    document.location.href = '/dialang-content/als.html';
    return false;
});
$('#next').prop('disabled', false).click(function (e) {
    dialang.switchState('flowchart');
    return false;
});
$('#skipforward').prop('disabled', true);

$.get('/dialang-content/legend/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
});
