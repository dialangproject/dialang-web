$('#help').prop('disabled', true);
$('#save-button').prop('disabled', true);
$.get('/dialang-content/instructormenu/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
    $(document).ready(function () {

        $('#dialang-all-users-report-button').click(function (e) {
            dialang.switchState('allusersreportmenu');
        });

        $('#dialang-single-user-report-button').click(function (e) {
            dialang.switchState('singleuserreportmenu');
        });

        $('#dialang-take-a-test-button').click(function (e) {
            document.location = '/getals';
        });
    });
});
