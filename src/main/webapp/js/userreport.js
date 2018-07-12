$('#help').prop('disabled', true);
$('#back').prop('disabled', false).click(function (e) {
    return dialang.navigation.backRules.userreport();
});

$.get('/dialang-content/userreport/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    $(document).ready(function () {

        var fromDateField = $('#dialang-from-date');
        var altFromDateField = $('#dialang-numeric-from-date');
        fromDateField.datepicker( { altField: altFromDateField, altFormat: '@' } );

        var toDateField = $('#dialang-to-date');
        var altToDateField = $('#dialang-numeric-to-date');
        toDateField.datepicker({ altField: altToDateField, altFormat: '@' } );

        $('#dialang-downloadreport-button').click(function (e) {

            var fromDate = altFromDateField.val();
            var toDate = altToDateField.val();
            var entered = $('#dialang-user-id').val();
            var matches = entered.match(/\(User ID:(\w*)\)/);
            var userId = (matches && matches.length == 2) ? matches[1] : entered;
            document.location = '/getltistudentreport?fromDate=' + fromDate + '&toDate=' + toDate + '&userId=' + userId;
        });

        $('#dialang-clear-button').click(function (e) {

            fromDateField.val('');
            altFromDateField.val('');
            toDateField.val('');
            altToDateField.val('');
            $('#dialang-user-id').val('');
        });
    });
});
