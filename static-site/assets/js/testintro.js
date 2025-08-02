$('#next').prop('disabled', false).click(function (e) {

  const url = "/prod/starttest";
  console.log(dialang.session);
  const body = { session: dialang.session };
  fetch(url, { "method": "POST", body: JSON.stringify(body) })
  .then(r => {

    if (r.ok) {
        return r.json();
    }

    throw new Error(`Failed to get test data at ${url}`);
  })
  .then(testData => {

    dialang.session.totalItems = testData.totalItems;
    dialang.pass.currentBasketId = testData.startBasket;
    dialang.session.currentBasketNumber = 0;
    dialang.navigation.nextRules.testintro();
  })
  .catch(error => alert(error));

  return false;
});

$('#skipforward').prop('disabled', false).click(function (e) {

  $('#confirm-skip-dialog').dialog('open');
  return false;
});

dialang.setupKeyboardButton();

$.get(`/prod/content/testintro/${dialang.session.al}.html`, function (data) {

  $('#content').html(data);
  if (!dialang.flags.disallowInstantFeedback) {
    $('#feedback-button').click(function (e) {

      if (dialang.session.instantFeedbackOn) {
        dialang.session.instantFeedbackOn = false;
        $(this).attr('title', dialang.currentToolbarTooltips.instantfeedbackontooltip)
          .find('img').attr('src',"/prod/assets/images/instantFeedbackOff.gif");
      } else {
        dialang.session.instantFeedbackOn = true;
        $(this).attr('title', dialang.currentToolbarTooltips.instantfeedbackofftooltip)
          .find('img').attr('src',"/prod/assets/images/instantFeedbackOn.gif");
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
