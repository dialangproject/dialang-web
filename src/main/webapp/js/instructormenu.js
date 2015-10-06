$('#help').prop('disabled', true);
$('#save-button').prop('disabled', true);
$.get('/dialang-content/instructormenu/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
    $(document).ready(function () {

        $('#dialang-user-report-button').click(function (e) {
            dialang.switchState('userreport');
        });

        $('#dialang-take-a-test-button').click(function (e) {
            document.location = '/getals';
        });
    });
});
