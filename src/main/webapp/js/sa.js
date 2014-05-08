$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/sa/' + dialang.session.al + '/' + dialang.session.skill + '.html', function (data) {

    $('#content').html(data);

    $('#sa-table > tbody > tr > td > input.statement').click(function (e) {

        var statementId = this.id.substring(0, this.id.indexOf('_'));
        answered[statementId] = true;
        var allAnswered = true;
        for (var id in answered) {
            if (!answered[id]) {
                allAnswered = false;
            }
        }

        if (allAnswered) {
            $('#sa-send-button,#next').prop('disabled', false);
        }

        $(this).parent().parent().addClass('done');
    });

    $(document).keydown(function (e) {

        if (e.keyCode == '72' && e.ctrlKey) {
            $('#sa-table > tbody > tr > td > input.valid-button').attr('checked','checked').trigger('click');
            $('#sa-send-button,#next').prop('disabled', false);
        } else if (e.keyCode == '76' && e.ctrlKey) {
            $('#sa-table > tbody > tr > td > input.invalid-button').attr('checked','checked').trigger('click');
            $('#sa-send-button,#next').prop('disabled', false);
        }
    });

    $('#confirm-send-dialog').dialog({
        modal: true,
        width: 'auto',
        autoOpen: false,
        resizable: false
    });

    $('#confirm-send-yes').click(function (e) {

        $('#saform').ajaxSubmit({
            dataType: 'json',
            timeout: 5000,
            success: function (scores, textStatus, jqXHR, jqFormElement) {

                dialang.session.saLevel = scores.saLevel;
                dialang.session.saDone = true;
                $('#confirm-send-dialog').dialog('destroy');
                dialang.navigation.nextRules.sa();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert('Failed to submit sa. Reason: ' + textStatus);
            }
        });

        return false;
    });
    $('#confirm-send-no').click(function (e) {

        $('#confirm-send-dialog').dialog('close');
        return false;
    });
    $('#sa-send-button,#next').click(function (e) {

        $('#confirm-send-dialog').dialog('open');
        return false;
    });

    $('#confirm-skip-dialog').dialog({
        modal: true,
        width: 'auto',
        autoOpen: false,
        resizable: false
    });

    $('#confirm-skip-yes').click(function (e) {

        $('#confirm-skip-dialog').dialog('destroy');
        return dialang.navigation.nextRules.sa();
    });

    $('#confirm-skip-no').click(function (e) {

        $('#confirm-skip-dialog').dialog('close');
        return false;
    });
});
