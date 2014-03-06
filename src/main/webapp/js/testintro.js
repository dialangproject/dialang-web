$('#next').prop('disabled', false).click(function (e) {
    $.ajax({
        url:'/starttest',
        dataType:'json',
        cache:false,
        success: function (testData, textStatus, jqXHR) {

            dialang.session.totalItems = testData.totalItems;
            dialang.session.currentBasketId = testData.startBasket;
            dialang.session.currentBasketNumber = 0;
            dialang.session.itemsCompleted = 0;
            dialang.switchState('test');
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert('Failed to get test data');
        }
    });
    return false;
});

$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/testintro/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);
    if(!dialang.instantFeedbackDisabled) {
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

    $('#confirm-skip-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});
    $('#confirm-skip-yes').click(function (e) {

        $('#confirm-skip-dialog').dialog('destroy');
        dialang.switchState('endoftest');
        return false;
    });

    $('#confirm-skip-no').click(function (e) {

        $('#confirm-skip-dialog').dialog('close');
        return false;
    });
});
