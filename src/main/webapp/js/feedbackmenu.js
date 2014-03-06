dialang.session.feedbackMode = true;

// This is used to indicate whether explfb has been reached from
// the feedbackmenu or from the sa feedback page. It gets used in
// aboutsa.js
dialang.session.saFeedbackMode = false;

$('#skipforward').prop('disabled', false).click(function () {

    $('#confirm-restart-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/feedbackmenu/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);

    $('#confirm-restart-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});
    $('#confirm-restart-yes').click(function (e) {

        $('#confirm-restart-dialog').remove();
        dialang.switchState('tls');
        return false;
    });

    $('#confirm-restart-no').click(function (e) {

        $('#confirm-restart-dialog').dialog('close');
        return false;
    });

    $('#confirm-restart-quit').click(function (e) {

        window.close();
        return false;
    });

    $('#about-sa-button').prop('disabled', false).click(function (e) {

        dialang.switchState('aboutsa');
        return false;
    });

    if(dialang.session.itemsCompleted) {
        $('#check-answers-button').click(function (e) {

            dialang.switchState('itemreview');
            return false;
        });
    } else {
        $('#check-answers-button').attr('disabled',true);
    }

    if(dialang.session.testDone) {
        $('#your-level-button').click(function (e) {

            dialang.switchState('testresults');
            return false;
        });
        if(dialang.session.skill === 'structures' || dialang.session.skill === 'vocabulary') {
            $('#advice-button').attr('disabled', true);
        } else {
            $('#advice-button').click(function (e) {

                dialang.switchState('advfb');
                return false;
            });
        }
    } else {
        $('#your-level-button').attr('disabled',true);
        $('#sa-feedback-button').attr('disabled',true);
        $('#advice-button').attr('disabled',true);
    }

    if(dialang.session.vsptDone.hasOwnProperty(dialang.session.tl)) {
        $('#placement-test-button').prop('disabled', false).click(function (e) {

            dialang.switchState('vsptfeedback');
            return false;
        });
    } else {
        $('#placement-test-button').attr('disabled',true);
    }

    if(dialang.session.saDone) {
        $('#sa-feedback-button').click(function (e) {

            dialang.switchState('safeedback');
            return false;
        });
    } else {
        $('#sa-feedback-button').prop('disabled', true);
    }
});
