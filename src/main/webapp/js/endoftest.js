$('#next').prop('disabled', false).click(function () {

    dialang.switchState('feedbackmenu');
    return false;
});

$('#instantfeedback').hide();

dialang.session.instantFeedback = false;

$('#progressbar').hide();

$('.review-dialog').remove();

$.get('/dialang-content/endoftest/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
});

dialang.session.testsDone = dialang.session.testsDone || [];
dialang.session.testsDone.push(dialang.session.tl + '-' + dialang.session.skill);