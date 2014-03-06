// We need to do this in case this isn't the first run
dialang.session.reviewBasket = null;
dialang.session.reviewItemId = null;
dialang.session.feedbackMode = false;
dialang.session.itemsCompleted = 0;
dialang.session.testDone = false;

$('#skipback').prop('disabled', false).click(function (e) {

    window.location.href = '/dialang-content/als.html';
    return false;
});

$('#back').prop('disabled', false).click(function (e) {

    dialang.switchState('flowchart');
    return false;
});

$.get('/dialang-content/tls/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);

    $('#disclaimer-dialog').dialog({modal: true, width: 500, height: 450});
    $('#disclaimer-button').click(function (e) {

        $('#disclaimer-dialog').dialog('destroy');
        $(this).off('click');
        return false;
    });

    $('#confirmation-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});

    $('#confirmation_no').click(function (e) {

        $('#confirmation-dialog').dialog('close');
        return false;
    });

    $('.tls-link').click(function () {

        var langskill = $(this).attr('title');
        var tl = $(this).attr('tl');
        var skill = $(this).attr('skill');

        $('#confirmation_langskill').html(langskill);
        $('#confirmation_yes').off('click').click(function (e) {

            $.ajax({
                url: '/settls',
                type: 'POST',
                data: {'tl':tl,'skill':skill},
                dataType: 'text',
                success: function (response, textStatus, jqXHR) {

                    dialang.pass = {'baskets':[],'itemToBasketMap':{},'items':[],'subskills':{}};
                    dialang.session.tl = tl;
                    dialang.session.skill = skill;
                    $('#confirmation-dialog').dialog('destroy');

                    // If the vspt hasn't been done yet for this test language, switch
                    // to the vspt.
                    if(!dialang.session.vsptDone.hasOwnProperty(tl)) {
                        dialang.switchState('vsptintro');
                    } else {
                        // There is no SA for structures or vocabulary tests
                        if(dialang.session.skill === 'vocabulary' || dialang.session.skill === 'structures') {
                            dialang.switchState('testintro');
                        } else {
                            dialang.switchState('saintro');
                        }
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {

                    alert('Failed to set test language and skill');
                    $('#confirmation-dialog').dialog('destroy');
                }
            });
            return false;
        });
        $('#confirmation-dialog').dialog('open');
        return false;
    }); // tls-link click

    // Disable the completed tests
    var testsDone = dialang.session.testsDone;

    if(testsDone) {
        testsDone.forEach(function (test) {

            $('#' + test)
                .off('click')
                .attr('href','')
                .children('img')
                .attr('src','/images/done.gif');
        });
    }
});
