if (!dialang.flags.hideFeedbackMenu) {
    $('#next').prop('disabled', false).click(function () {
        return dialang.navigation.nextRules.endoftest();
    });
}

$('#save-button').prop('disabled', true).hide();

$('#instantfeedback').hide();
dialang.session.instantFeedback = false;

$('#keyboard-button').off('click').hide();
dialang.session.keyboardDisplayed = false;

$('#progressbar').hide();

$('.review-dialog').remove();

$.get('/dialang-content/endoftest/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
});

dialang.session.testsDone = dialang.session.testsDone || [];
dialang.session.testsDone.push(dialang.session.tl + '-' + dialang.session.skill);
