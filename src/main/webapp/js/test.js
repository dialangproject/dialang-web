dialang.initialiseReviewDialog = function (modal) {

    $('.review-dialog').dialog({modal: modal || false,
        width: 370,
        autoOpen: false,
        resizable: false,
        close: function (event, ui) {

            if (dialang.session.instantFeedbackOn) {
                if (dialang.session.testDone) {
                    dialang.switchState("endoftest");
                } else {
                    dialang.switchState("test");
                }
            } else {
                dialang.switchState("itemreview");
            }
        }
    });

    if (dialang.session.reviewMode) {
        $('.ui-dialog-titlebar-close span')
            .removeClass('ui-icon-closethick').addClass('ui-icon-prevButton');
    } else {
        $('.ui-dialog-titlebar-close span')
            .removeClass('ui-icon-closethick').addClass('ui-icon-nextButton');
    }

    return false;
};

dialang.responseComplete = function (complete) {

    if (complete) {
        var audio = $('#audio');
        if (audio.length) {
            if (audio[0].ended) {
                $('#next').prop('disabled', false);
            }
        } else {
            $('#next').prop('disabled', false);
        }
    } else {
        $('#next').prop('disabled', true);
    }
};

if (!dialang.session.reviewMode) {

    $('#skipforward').prop('disabled', false).click(function (e) {

        $('#confirm-skip-dialog').dialog('open');
        return false;
    });

    $('#next').click(function (e) {

        $('#basketform').submit();
        return false;
    });

    if (dialang.flags.disallowInstantFeedback) {
        $('#instantfeedback').hide();
    }

    if (!dialang.nextBasketTooltip || !dialang.quitTestTooltip || !dialang.instantFeedbackOnTooltip || !dialang.instantFeedbackOffTooltip) {
        $.get('/dialang-content/baskets/' + dialang.session.al + '-toolbarTooltips.json', function (tips) {

            dialang.nextBasketTooltip = tips.next;
            dialang.quitTestTooltip = tips.skipforward;
            dialang.instantFeedbackOnTooltip = tips.instantfeedbackontooltip;
            dialang.instantFeedbackOffTooltip = tips.instantfeedbackofftooltip;
            $('#next').attr('title', dialang.nextBasketTooltip);
            $('#skipforward').attr('title', dialang.quitTestTooltip);

            if (!dialang.flags.disallowInstantFeedback) {
                if (dialang.session.instantFeedbackOn) {
                    $('#instantfeedback').show().attr('title', dialang.instantFeedbackOffTooltip).find('img').attr('src', '/images/instantFeedbackOn.gif');
                } else {
                    $('#instantfeedback').show().attr('title', dialang.instantFeedbackOnTooltip).find('img').attr('src', '/images/instantFeedbackOff.gif');
                }
            }
        });
    }

    $('#instantfeedback').off('click').click(function (e) {

        if (dialang.session.instantFeedbackOn) {
            dialang.session.instantFeedbackOn = false;
            $(this).attr('title', dialang.instantFeedbackOnTooltip)
                .find('img').attr('src', "/images/instantFeedbackOff.gif");
        } else {
            dialang.session.instantFeedbackOn = true;
            dialang.initialiseReviewDialog(true);
            $(this).attr('title', dialang.instantFeedbackOffTooltip)
                .find('img').attr('src', "/images/instantFeedbackOn.gif");
        }
        return false;
    });

    // We're not in review mode, so show the progress bar
    if (dialang.pass.items.length == 0 || dialang.pass.loading) {
        $('#progressbar').css('display', 'inline-block').progressbar({max: parseInt(dialang.session.totalItems, 10), value: 0}).height(26);
        $('#keyboard-button').show();

        if (dialang.pass.loading) {
            $('#progressbar').progressbar('option', 'value', parseInt(dialang.pass.items.length, 10));
        }
    } else {
        $('#progressbar').progressbar('option', 'value', parseInt(dialang.pass.items.length, 10));
    }

    $.get('/dialang-content/baskets/' + dialang.session.al + "/" + dialang.pass.currentBasketId + '.html', function (data) {

        $('#content').html(data);

        $('#confirm-skip-dialog').dialog({
            modal: true,
            width: 'auto',
            autoOpen: false,
            resizable: false
        });

        $('#confirm-skip-yes').click(function (e) {

            dialang.switchState("endoftest");
            $('#confirm-skip-dialog').dialog('destroy');
            return false;
        });

        $('#confirm-skip-no').click(function (e) {

            $('#confirm-skip-dialog').dialog('close');
            return false;
        });

        $('input[type=text]').focusout(function (e) {
            dialang.lastFocused = this;
            dialang.lastSelectionStart  = this.selectionStart;
            dialang.lastSelectionEnd = this.selectionEnd;
        });

        if (dialang.pass.currentBasketType === 'gaptext'
                || dialang.pass.currentBasketType === 'shortanswer') {
            $('#keyboard-button').prop('disabled', false);
        } else {
            $('#keyboard-button').prop('disabled', true);
            //$('#keyboard-dialog').dialog('close');
            //dialang.session.keyboardDisplayed = false;
        }

        var numItemsInThisBasket = parseInt($('#number-of-items').val(), 10);
        if (numItemsInThisBasket === 1) {
            $('#progressbar-label').html(dialang.pass.items.length + 1);
        } else {
            $('#progressbar-label').html((dialang.pass.items.length + 1) + "-" + (dialang.pass.items.length + numItemsInThisBasket));
        }

        if (dialang.session.instantFeedbackOn) {
            dialang.initialiseReviewDialog(true);
        } else {
            // Hide all the review type dialogs
            $('.review-dialog').hide();
        }

        $('#basketform').ajaxForm({
            dataType: 'json',
            timeout: dialang.uploadTimeout,
            success: function (nextBasketData, textStatus, jqXHR, jqFormElement) {

                if (nextBasketData.redirect) {
                    window.location = nextBasketData.redirect;
                } else {
                    $('#save-button').show().prop('disabled', false);

                    var scoredBasket = nextBasketData.scoredBasket;

                    dialang.utils.configureScoredBasket(scoredBasket);

                    if (nextBasketData.testDone) {
                        dialang.session.testDone = true;
                        dialang.session.itemLevel = nextBasketData.itemLevel;
                        if (!dialang.session.instantFeedbackOn) {
                            dialang.switchState('endoftest');
                        }
                    } else {
                        dialang.pass.currentBasketId = nextBasketData.nextBasketId;
                        if (!dialang.session.instantFeedbackOn) {
                            dialang.switchState('test');
                        }
                    }

                    if (dialang.session.instantFeedbackOn) {
                        if (scoredBasket.basketType === 'mcq') {
                            // MCQ baskets only ever have one item.
                            var mcqItem = scoredBasket.items[0];
                            $('#mcq-review-dialog').dialog('open');
                            $('.ui-dialog-titlebar-close span').removeClass('ui-icon-closethick').addClass('ui-icon-nextButton');
                            if (!mcqItem.correct) {
                                $('.review-smiley > img').attr('src','/images/frowney.gif');
                            } else {
                                $('.review-smiley > img').attr('src','/images/smiley.gif');
                            }
                            $('.review-given-answer p').html(mcqItem.responseText);
                            mcqItem.answers.forEach(function (answer) {

                                if (answer.correct) {
                                    $('.review-correct-answer p').html(answer.text);
                                }
                            });
                        } else if (scoredBasket.basketType === 'tabbedpane' ) {

                            dialang.launchMultiItemReviewDialog(scoredBasket,1,function (clickedItemId) {

                                // Get the index of the clicked review tab
                                var index = $('#tabbedpane-tabs a[href="#tabs-' + clickedItemId + '"]').parent().index();
                                $("#tabbedpane-tabs").tabs('option','active',index);
                            });
                        } else if (scoredBasket.basketType === 'gapdrop') {

                            dialang.launchMultiItemReviewDialog(scoredBasket, 1, function (clickedItemId) {

                                $('select').removeClass("outlined");
                                $('select[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
                            });
                        } else if (scoredBasket.basketType === 'shortanswer' || scoredBasket.basketType === 'gaptext') {

                            dialang.launchMultiItemReviewDialog(scoredBasket, 1, function (clickedItemId) {

                                $('input[type="text"]').removeClass("outlined");
                                $('input[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
                            });
                        }
                    } // if (dialang.session.instantFeedbackOn)
                } // if (nextBasketData.redirect)
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert('Failed to submit basket. Reason: ' + textStatus);
            }
        }); // ajaxForm

        if ('mcq' === dialang.pass.currentBasketType) {
            $.getScript('/js/mcqresponse.js');
        } else if ('gapdrop' === dialang.pass.currentBasketType) {
            $.getScript('/js/gapdropresponse.js');
        } else if ('gaptext' === dialang.pass.currentBasketType) {
            $.getScript('/js/gaptextresponse.js');
        } else if ('shortanswer' === dialang.pass.currentBasketType) {
            $.getScript('/js/shortanswerresponse.js');
        } else if ('tabbedpane' === dialang.pass.currentBasketType) {
            $.getScript('/js/tabbedpaneresponse.js');
        }
    }); // get basket html
} else {

    // We're in item review mode

    // Setup the toolbar appropriately
    $('#back').prop('disabled', false).click(function () {

        $('.review-dialog').dialog('destroy');
        dialang.switchState("itemreview");
        return false;
    });

    $('#next').off('click').prop('disabled', true);
    $('#skipforward').prop('disabled', true);

    var reviewBasket = dialang.session.reviewBasket;

    var initialItemId = dialang.session.reviewItemId;

    var positionInBasket = dialang.session.reviewItemPosition;

    $.get('/dialang-content/baskets/' + dialang.session.al + "/" + reviewBasket.id + '.html', function (data) {

        $('#content').html(data);

        // We don't need the confirm dialog when reviewing
        $('#confirm-skip-dialog').remove();

        dialang.initialiseReviewDialog();

        if (reviewBasket.basketType === 'mcq' ) {
            // MCQ baskets only ever have one item.
            var item = reviewBasket.items[0];
            $('#mcq-review-dialog').dialog('open');
            if (!item.correct) {
                $('.review-smiley > img').attr('src', '/images/frowney.gif');
            } else {
                $('.review-smiley > img').attr('src', '/images/smiley.gif');
            }
            $('.review-given-answer p').html(item.responseText);

            item.answers.forEach(function (answer) {

                if (answer.correct) {
                    $('.review-correct-answer p').html(answer.text);
                }
            });
            $("input[value=\"" + item.responseId + "\"]").attr("checked",true);
        } else if (reviewBasket.basketType === 'tabbedpane' ) {

            // This is a tabbebpane basket as it has multiple mcq items

            $("#tabbedpane-tabs").tabs();

            // Launch the dialog
            dialang.launchMultiItemReviewDialog(reviewBasket,positionInBasket, function (clickedItemId) {

                // Get the index of the clicked review tab
                var index = $('#tabbedpane-tabs a[href="#tabs-' + clickedItemId + '"]').parent().index();
                $("#tabbedpane-tabs").tabs('option','active',index);
            });

            // Select the response radio buttons
            reviewBasket.items.forEach(function (item) {
                $("input[value=\"" + item.responseId + "\"]").prop("checked", true);
            });
        } else if (reviewBasket.basketType === 'gapdrop') {

            // Launch the dialog
            dialang.launchMultiItemReviewDialog(reviewBasket,positionInBasket, function (clickedItemId) {

                $('select').removeClass("outlined");
                $('select[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
            });

            reviewBasket.items.forEach(function (item) {
                $("option[value=\"" + item.responseId + "\"]").prop("selected", true);
            });
        } else if (reviewBasket.basketType === 'shortanswer' || reviewBasket.basketType === 'gaptext') {
            // Launch the dialog
            dialang.launchMultiItemReviewDialog(reviewBasket,positionInBasket, function (clickedItemId) {

                $('input[type="text"]').removeClass("outlined");
                $('input[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
            });
            reviewBasket.items.forEach(function (item) {
                $("input[name=\"" + item.id + "-response\"]").val(item.responseText);
            });
        }

        $(document).ready(function () {
            $('#basketform *').filter(':input').prop('disabled', true);
        });
    });
}

