// We need to do this in case this isn't the first run
dialang.session.reviewBasket = null;
dialang.session.reviewItemId = null;
dialang.session.feedbackMode = false;
dialang.session.testDone = false;

if (!dialang.flags.hideALS) {
  $('#skipback').prop('disabled', false).click(function (e) {

    dialang.switchState('als');
    return false;
  });
}

$('#back').prop('disabled', false).click(function (e) {
  return dialang.navigation.backRules.tls();
});

$.get('/prod/content/tls/' + dialang.session.al + '.html', function (data) {

  $('#content').html(data);

  $('#disclaimer-dialog').dialog({
    modal: true,
    width: 'auto',
    resizable: false
  });

  $('#disclaimer-button').click(function (e) {

    $('#disclaimer-dialog').dialog('destroy');
    return false;
  });

  $('#confirm-dialog').dialog({
    modal: true,
    width: 'auto',
    autoOpen: false,
    resizable: false
  });

  $('#confirm-no').click(function (e) {

    $('#confirm-dialog').dialog('close');
    return false;
  });

  $('.tls-link').click(function () {

    var langskill = $(this).attr('title');
    var tl = $(this).attr('tl');
    var skill = $(this).attr('skill');

    $('#confirmation_langskill').html(langskill);
    $('#confirm-yes').off('click').click(function (e) {

      const formData = new FormData();
      formData.append('sessionId', dialang.session.id);
      formData.append('al', dialang.session.al);
      formData.append('tl', tl);
      formData.append('skill', skill);

      const url = "/prod/settl";
      fetch(url, {
        method: "POST",
        headers: { 'Content-Type': 'application/json' },
        //body: formData
        body: JSON.stringify({ sessionId: dialang.session.id, al: dialang.session.al, tl, skill }),
      })
        .then(r => {

          if (r.ok) {
            return r.json();
          }

          throw new Error(`Failed to set test language at ${url}`);
        })
        .then(data => {

          dialang.pass = { id: data.passId, sessionId: data.sessionId, baskets: [], itemToBasketMap: {}, items: [], subskills: {} };
          dialang.session = { ...dialang.session, id: data.sessionId, tl, skill };
          console.log(dialang);
          $('#confirm-dialog').dialog('destroy');

          // If the vspt hasn't been done yet for this test language, switch
          // to the vsptintro screen.
          if (!dialang.session.vsptDone.hasOwnProperty(tl)) {
            dialang.navigation.nextRules.tls();
          } else {
            // Pretend we are already on the vspt feedback screen
            dialang.navigation.nextRules.vsptfeedback();
          }
        })
        .catch(error => {

          alert(`Failed to set test language and skill. Reason: ${error}`);
          $('#confirm-dialog').dialog('destroy');
        });

      return false;
    });
    $('#confirm-dialog').dialog('open');
    return false;
  }); // tls-link click

  // Disable the completed tests
  var testsDone = dialang.session.testsDone;

  if (testsDone) {
    testsDone.forEach(function (test) {

      $('#' + test)
        .off('click')
        .attr('href','')
        .children('img')
        .attr('src','/images/done.gif');
    });
  }
});
