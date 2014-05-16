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

$.get('/dialang-content/tls/' + dialang.session.al + '.html', function (data) {

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

            $.ajax({
                url: '/settls',
                type: 'POST',
                data: {'tl':tl,'skill':skill},
                dataType: 'text',
                timeout: dialang.uploadTimeout,
                success: function (response, textStatus, jqXHR) {

                    dialang.pass = { baskets: [], itemToBasketMap: {}, items: [], subskills: {} };
                    dialang.session.tl = tl;
                    dialang.session.skill = skill;
                    $('#confirm-dialog').dialog('destroy');

                    // If the vspt hasn't been done yet for this test language, switch
                    // to the vsptintro screen.
                    if (!dialang.session.vsptDone.hasOwnProperty(tl)) {
                        dialang.navigation.nextRules.tls();
                    } else {
                        // Pretend we are already on the vspt feedback screen
                        dialang.navigation.nextRules.vsptfeedback();
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {

                    alert('Failed to set test language and skill. Reason: ' + textStatus);
                    $('#confirm-dialog').dialog('destroy');
                }
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
