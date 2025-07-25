"use strict";

$.ajaxSetup({ cache: false });

dialang.uploadTimeout = 30000; // 30 seconds

// Patch console. Not all browsers support it.
window.console = window.console || { log: function (m) { alert(m); } };

if (typeof dialang === 'undefined') {
  console.log("No dialang object defined. Returning prematurely from dialang.js ...");
}

dialang.keyboardMappings = {
    "dan_dk": "danish-qwerty",
    "deu_de": "german-qwertz-1",
    "spa_es": "spanish-qwerty",
    "fra_fr": "french-azerty-1",
    "por_pt": "portuguese-qwerty",
    "swe_se": "swedish-qwerty"
};

$('#help').click(function (e) {
    $('#help-dialog').dialog('open');
});

$('#save-button').click(function (e) {

    $.ajax({
        url: '/save',
        success: function (data, textStatus, jqXHR) {

            $('#save-dialog').dialog('open');
            $('#dialang-token').html(data.token);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.log('Failed to save. errorThrown: ' + errorThrown);
        }
    });
});

$.get('/dialang-content/iso_lang_mappings.json', function (mappings) {
    dialang.isoLangMappings = mappings;
});

dialang.setupKeyboardButton = function () {

    $('#keyboard-button').click(function (e) {

        if (dialang.keyboardOn) {
            $('input:text').each(function (index) {
                $(this).keyboard().getkeyboard().destroy();
            });
            dialang.keyboardOn = false;
        } else {
            var twoLetterCode = dialang.isoLangMappings[dialang.session.al] || 'en';
            var kbOptions = {
                autoAccept: true,
                language: twoLetterCode
            };

            var layout = dialang.keyboardMappings[dialang.session.tl];
            if (layout) {
                kbOptions.layout = layout;
                $('input:text').keyboard(kbOptions);
                $('.ui-keyboard-input').bind('change', function (e, keyboard, el){
                    if ('gaptext' === dialang.pass.currentBasketType) {
                        dialang.gapCompletionTest($('#radios > span > input'));
                    } else if ('shortanswer' === dialang.pass.currentBasketType) {
                        dialang.gapCompletionTest($('#radios > input'));
                    }
                });

            } else {
                $('input:text').keyboard(kbOptions);
            }
            dialang.keyboardOn = true;
        }
        return true;
    });
};

dialang.skipVSPT = function () {

    $.get('/skipvspt');

    $('#confirm-skip-dialog').dialog('destroy');
    if (!dialang.flags.hideSA) {
        if (dialang.session.skill === 'structures' || dialang.session.skill === 'vocabulary') {
            dialang.switchState('testintro');
        } else {
            dialang.switchState('saintro');
        }
    } else {
        if (!dialang.flags.hideTest) {
            dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            dialang.switchState('feedbackmenu');
        }
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
            } else {
                $('#reviewtab-' + item.id + ' > div > img').attr('src','/images/smiley.gif');
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

dialang.gapCompletionTest = function (inputs) {

    var complete = true;
    inputs.each(function (index, el) {

        if (el.value.length <= 0) {
            complete = false;
        }
    });

    dialang.responseComplete(complete);

    return false;
};

dialang.attachGapCompletionTest = function () {

    var inputs = null;

    if ('gaptext' === dialang.pass.currentBasketType) {
        inputs = $('#radios > span > input');
    } else if ('shortanswer' === dialang.pass.currentBasketType) {
        inputs = $('#radios > input');
    } else if ('gapdrop' === dialang.pass.currentBasketType) {
        inputs = $('#radios > span > select');
        inputs.change(function (e) {
            return dialang.gapCompletionTest(inputs);
        });
    }

    if ('shortanswer' === dialang.pass.currentBasketType
        || 'gaptext' === dialang.pass.currentBasketType) {

        inputs.keyup(function (e) {
            return dialang.gapCompletionTest(inputs);
        });
    }
};

dialang.switchState = function (state) {

  console.debug(`Switching to state ${state} ...`);

  if ('als' == state && dialang.flags.hideALS) {
    return false;
  }

  if ('tls' == state && dialang.flags.hideTLS) {
    return false;
  }

  if ('test' !== state) {
    $.get('/prod/content/' + state + '/' + dialang.session.al + '-toolbarTooltips.json', function (tips) {

      dialang.currentToolbarTooltips = tips;

      $('#skipback').attr('title', tips.skipback);
      $('#back').attr('title', tips.back);
      $('#next').attr('title', tips.next);
      $('#skipforward').attr('title', tips.skipforward);
      $('#save-button').attr('title', tips.save);
    });
  }

  $('#skipback,#back,#next,#skipforward').off('click').prop('disabled', true);
  $('#confirm-skip-dialog').remove();

  dialang.getScript('/prod/assets/js/' + state + '.js');

  // Google Analytics
  ga('set', 'page', '/' + state + '.html');
  ga('send', 'pageview');

  return false;
};

dialang.getScript = path => {

  const scriptTag = document.createElement("script"); // create a script tag
  const firstScriptTag = document.getElementsByTagName("script")[0]; // find the first script tag in the document
  scriptTag.src = path;
  firstScriptTag.parentNode.insertBefore(scriptTag, firstScriptTag); // append the script to the DOM
};
/*
// TEST MODE ONLY !!!!!!!
dialang.state = 'questionnaire';
dialang.session.al = 'eng_gb';
dialang.session.tl = 'dan_da';
//dialang.session.itemLevel = 'A2';
//dialang.session.testDone = 'true';
dialang.session.skill = 'writing';
// TEST MODE ONLY !!!!!!!
*/

if (dialang.state !== "als") {
  dialang.switchState(dialang.state);
}

$.get('/dialang-content/help/' + dialang.session.al + '.html', function (helpDialogMarkup) {

    $('#help-dialog').html(helpDialogMarkup);
    $('#help-tabs').tabs();
    $('#help-dialog').dialog({
        modal: true,
        width: 'auto',
        height: 600,
        autoOpen: false,
        resizable: false
    });
});

$.get('/dialang-content/save/' + dialang.session.al + '.html', function (saveDialogMarkup) {

    $('#save-dialog').html(saveDialogMarkup);
    $('#save-dialog').dialog({
        modal: true,
        width: 'auto',
        height: 300,
        autoOpen: false,
        resizable: false
    });
});

/*
$(document).ready(function () {  
          
    //this one line will disable the right mouse click menu  
     $(document)[0].oncontextmenu = function () {return false;}  
});
*/
