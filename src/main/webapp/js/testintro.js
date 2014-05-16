$('#next').prop('disabled', false).click(function (e) {

    $.ajax({
        url: '/starttest',
        dataType: 'json',
        timeout: dialang.uploadTimeout,
        success: function (testData, textStatus, jqXHR) {

            dialang.session.totalItems = testData.totalItems;
            dialang.session.currentBasketId = testData.startBasket;
            dialang.session.currentBasketNumber = 0;
            dialang.navigation.nextRules.testintro();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert('Failed to get test data. Reason: ' + textStatus);
        }
    });
    return false;
});

$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/keyboards/' + dialang.session.tl + '.html', function (data) {

    $('#keyboard').html(data);

    $(document).ready(function () {

        $('#keyboard-dialog').dialog({
            dialogClass: "no-close",
            resizable: false,
            width: 'auto',
            autoOpen: false
        });

        $('#keyboard button').click(function (e) {

            var v = dialang.lastFocused.value;
            var pre = v.substring(0, dialang.lastSelectionStart);
            var post = v.substring(dialang.lastSelectionEnd);
            var c = this.getAttribute('data-char');
            dialang.lastFocused.value = pre + c + post;
            dialang.lastSelectionStart = dialang.lastFocused.selectionStart;
            dialang.lastSelectionEnd = dialang.lastFocused.selectionEnd;
            $(dialang.lastFocused).focus().keyup();
            return false;
        });

        $('#keyboard-button').click(function (e) {

            if (dialang.session.keyboardDisplayed) {
                $('#keyboard-dialog').dialog('close');
                dialang.session.keyboardDisplayed = false;
            } else {
                $('#keyboard-dialog').dialog('open');
                dialang.session.keyboardDisplayed = true;
            }
        });
    });
});

$.get('/dialang-content/testintro/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    if(!dialang.flags.disallowInstantFeedback) {
        $('#feedback-button').click(function (e) {

            if(dialang.session.instantFeedbackOn) {
                dialang.session.instantFeedbackOn = false;
                $(this).attr('title', instantFeedbackOnTooltip)
                    .find('img').attr('src',"/images/instantFeedbackOff.gif");
            } else {
                dialang.session.instantFeedbackOn = true;
                $(this).attr('title', instantFeedbackOffTooltip)
                    .find('img').attr('src',"/images/instantFeedbackOn.gif");
            }
            return false;
        });
    } else {
        $('#feedback-button').hide();
        $('#feedback-label').hide();
    }

    $('#confirm-skip-dialog').dialog({
        modal: true,
        width: 'auto',
        autoOpen: false,
        resizable: false
    });

    $('#confirm-skip-yes').click(function (e) {

        $('#confirm-skip-dialog').dialog('destroy');
        return dialang.switchState('endoftest');
    });

    $('#confirm-skip-no').click(function (e) {

        $('#confirm-skip-dialog').dialog('close');
        return false;
    });
});
