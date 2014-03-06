"use strict";

if (typeof dialang === 'undefined') {
    alert("No dialang object defined. Returning prematurely from dialang.js ...");
}

if(typeof console === "undefined") {

    console = {
        debug: function () {},
        error: function (message) {
            alert(message);
        }
    };
}

$('#help').click(function (e) {
    $('#help-dialog').dialog('open');
});

dialang.skipVSPT = function () {

    $('#confirm-skip-dialog').dialog('destroy');
    if (dialang.session.skill === 'structures' || dialang.session.skill === 'vocabulary') {
        dialang.switchState('testintro');
    } else {
        dialang.switchState('saintro');
    }
};

dialang.launchFlowchartDialog = function (number) {

    $('.dialog').dialog('close');
    $('#dialog' + number).dialog('open');
};

dialang.launchMultiItemReviewDialog = function (basket, initialIndex, selectCallback) {

    // Get the yourAnswer and correctAnswer texts from the hidden form elements.
    var yourAnswerTitle, correctAnswerTitle;

    yourAnswerTitle = $('#review-dialog-youranswer-title').val();
    correctAnswerTitle = $('#review-dialog-correctanswer-title').val();

    $.get('/templates/multiitemreview.mustache', function (template) {

        // Render the dialog markup
        var output = Mustache.render(template, {'items': basket.items, 'yourAnswerTitle': yourAnswerTitle, 'correctAnswerTitle': correctAnswerTitle});
        $('#tp-review-dialog').html(output);

        // Set up the image for each item
        basket.items.forEach(function (item) {

            if (!item.correct) {
                $('#reviewtab-' + item.id + ' > div > img').attr('src','/images/frowney.gif');
            }
        });

        $(document).ready(function () {

            $("#review-tabs").tabs({
                select:function (event, ui) {

                    var clickedItemId = $(ui.panel).attr('item-id');
                    selectCallback(clickedItemId);
                }
            });

            $("#review-tabs").tabs('option', 'active', initialIndex - 1);
        });

        $('#tp-review-dialog').dialog('open');
        $('.ui-dialog-titlebar-close span').removeClass('ui-icon-closethick').addClass('ui-icon-prevButton');
    },'text');
};

dialang.switchState = function (state) {

    if ('test' !== state) {
        $.get('/dialang-content/' + state + '/' + dialang.session.al + '-toolbarTooltips.json', function (tips) {

            $('#skipback').attr('title', tips.skipback);
            $('#back').attr('title', tips.back);
            $('#next').attr('title', tips.next);
            $('#skipforward').attr('title', tips.skipforward);
        });
    }

    $('#skipback,#back,#next,#skipforward').off('click').prop('disabled', true);
    $('#confirm-skip-dialog').remove();
    $.getScript('/js/' + state + '.js');
};

// TEST MODE ONLY !!!!!!!
/*
dialang.state = 'test';
dialang.session.al = 'eng_gb';
dialang.session.tl = 'spa_es';
dialang.session.itemLevel = 'A2';
dialang.session.testDone = 'true';
dialang.session.skill = 'reading';
*/
// TEST MODE ONLY !!!!!!!

dialang.switchState(dialang.state);

$.get('/dialang-content/help/' + dialang.session.al + '.html', function (helpDialogMarkup) {

    $('#help-dialog').html(helpDialogMarkup);
    $('#help-tabs').tabs();
    $('#help-dialog').dialog({modal: true, width: 600, height: 535, autoOpen: false});
});

/*
$(document).ready(function () {  
          
    //this one line will disable the right mouse click menu  
     $(document)[0].oncontextmenu = function () {return false;}  
});
*/
