dialang.session.saFeedbackMode = true;

$('#back').prop('disabled', false).click(function () {

    dialang.switchState('feedbackmenu');
    return false;
});

$.get(`/prod/content/safeedback/${dialang.session.al}/${dialang.session.itemLevel}/${dialang.session.saLevel}.html`, function (data) {

    $('#content').html(data);
    $('#about-sa-button').click(function (e) {

        dialang.switchState('aboutsa');
        return false;
    });
});
