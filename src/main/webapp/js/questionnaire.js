$('#skipback').prop('disabled', true);
$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-restart-dialog').dialog('open');
    return false;
});
$('#back').prop('disabled', false).click(function (e) {
    dialang.navigation.backRules.questionnaire();
});

if (!dialang.flags.hideTLS) {
    $('#next').prop('disabled', false).click(function () {

        // Submit the form
        $.post('submitquestionnaire', $('#questionnaire-form').serialize(), function () {

            dialang.questionnaireShown = true;
            if (!dialang.flags.hideTLS) {
                $('#confirm-restart-dialog').dialog('open');
            }
        });
        return false;
    });
}

$.get('/dialang-content/questionnaire/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);

    $(document).ready(function () {

        var otherField = $('#questionnaire-othergender-field');
        $('.questionnaire-gender-button').click(function (e) { otherField.hide(); });
        $('#questionnaire-othergender-button').click(function (e) { otherField.show(); });
    });
});
