$('#help').prop('disabled', true);
$('#back').prop('disabled', false).click(function (e) {
    return dialang.navigation.backRules.allusersreportmenu();
});

$.get('/dialang-content/allusersreportmenu/' + dialang.session.al + '.html', function (data) {
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
            document.location = '/getltistudentreport?fromDate=' + fromDate + '&toDate=' + toDate;
        });
    });
});
