dialang.session.feedbackMode = true;

// This is used to indicate whether explfb has been reached from
// the feedbackmenu or from the sa feedback page. It gets used in
// aboutsa.js
dialang.session.saFeedbackMode = false;

if (!dialang.flags.hideTLS) {
  $('#skipforward').prop('disabled', false).click(function () {

      $('#confirm-restart-dialog').dialog('open');
      return false;
  });
}

$.get('/dialang-content/feedbackmenu/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);

    if (!dialang.flags.hideTLS) {
        $('#confirm-restart-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});
        $('#confirm-restart-yes').click(function (e) {

            $('#confirm-restart-dialog').dialog('destroy');
            return dialang.switchState('tls');
        });

        $('#confirm-restart-no').click(function (e) {

            $('#confirm-restart-dialog').dialog('close');
            return false;
        });

        $('#confirm-restart-quit').click(function (e) {

            window.close();
            return false;
        });
    } else {
        $('#confirm-restart-dialog').remove();
    }

    $('#about-sa-button').prop('disabled', false).click(function (e) {

        return dialang.switchState('aboutsa');
    });

    if (dialang.session.itemsCompleted) {
        $('#check-answers-button').click(function (e) {
            return dialang.switchState('itemreview');
        });
    } else {
        $('#check-answers-button').attr('disabled',true);
    }

    if (dialang.session.testDone) {
        $('#your-level-button').click(function (e) {
            return dialang.switchState('testresults');
        });
        if (dialang.session.skill === 'structures' || dialang.session.skill === 'vocabulary') {
            $('#advice-button').attr('disabled', true);
        } else {
            $('#advice-button').click(function (e) {
                return dialang.switchState('advfb');
            });
        }
    } else {
        $('#your-level-button').attr('disabled',true);
        $('#sa-feedback-button').attr('disabled',true);
        $('#advice-button').attr('disabled',true);
    }

    if (dialang.session.vsptDone.hasOwnProperty(dialang.session.tl)) {
        $('#placement-test-button').prop('disabled', false).click(function (e) {
            return dialang.switchState('vsptfeedback');
        });
    } else {
        $('#placement-test-button').attr('disabled',true);
    }

    if (dialang.session.saDone) {
        $('#sa-feedback-button').click(function (e) {
            return dialang.switchState('safeedback');
        });
    } else {
        $('#sa-feedback-button').prop('disabled', true);
    }
});
