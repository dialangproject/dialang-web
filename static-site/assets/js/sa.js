$('#skipforward').prop('disabled', false).click(function (e) {

  $('#confirm-skip-dialog').dialog('open');
  return false;
});

$.get(`/prod/content/sa/${dialang.session.al}/${dialang.session.skill}.html`, function (data) {

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

    const formData = new FormData(document.getElementById('saform'));
    formData.append("skill", dialang.session.skill);
    formData.append("sessionId", dialang.session.id);

    const url = "/prod/scoresa";
    fetch(url, {
      method: "POST",
      body: JSON.stringify(Object.fromEntries(formData)),
    })
    .then(r => {

      if (r.ok) {
        return r.json();
      }

      throw new Error(`Failed to score SA at ${url}`);
    })
    .then(scores => {

      console.log(scores);

      if (scores.redirect) {
          window.location = scores.redirect;
      } else {
        dialang.session.saSubmitted = 1;
        dialang.session.saPPE = scores.saPPE;
        dialang.session.saLevel = scores.saLevel;
        dialang.session.saDone = true;
        $('#save-button').prop('disabled', false);
        $('#confirm-send-dialog').dialog('destroy');
        dialang.navigation.nextRules.sa();
      }
    })
    .catch(error => {
      alert('Failed to submit sa. Reason: ' + error);
    });
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

    $.get('/skipsa');
    $('#confirm-skip-dialog').dialog('destroy');
    return dialang.navigation.nextRules.sa();
  });

  $('#confirm-skip-no').click(function (e) {

    $('#confirm-skip-dialog').dialog('close');
    return false;
  });
});
