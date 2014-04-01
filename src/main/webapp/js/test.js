dialang.initialiseReviewDialog = function (modal) {

    $('.review-dialog').dialog({modal: modal || false,
        width: 500,
        height: 450,
        autoOpen: false,
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
        $('.ui-dialog-titlebar-close span').removeClass('ui-icon-closethick').addClass('ui-icon-prevButton');
    } else {
        $('.ui-dialog-titlebar-close span').removeClass('ui-icon-closethick').addClass('ui-icon-nextButton');
    }

    return false;
};

dialang.responseComplete = function () {

    var audio = $('#audio');
    if (audio.length) {
        if (audio[0].ended) {
            $('#next').prop('disabled', false);
        }
    } else {
        $('#next').prop('disabled', false);
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

    if (!dialang.nextBasketTooltip || !dialang.quitTestTooltip) {
        $.get('/dialang-content/baskets/' + dialang.session.al + '-toolbarTooltips.json', function (tips) {
            dialang.nextBasketTooltip = tips.next;
            dialang.quitTestTooltip = tips.skipforward;
            $('#next').attr('title', dialang.nextBasketTooltip);
            $('#skipforward').attr('title', dialang.quitTestTooltip);
        });
    }

    if (!dialang.flags.disallowInstantFeedback) {
        if (dialang.session.instantFeedbackOn) {
            $('#instantfeedback').show().attr('title', instantFeedbackOffTooltip).find('img').attr('src', '/images/instantFeedbackOn.gif');
        } else {
            $('#instantfeedback').show().attr('title', instantFeedbackOnTooltip).find('img').attr('src', '/images/instantFeedbackOff.gif');
        }
    } else {
        $('#instantfeedback').hide();
    }

    $('#instantfeedback').off('click').click(function (e) {
        if (dialang.session.instantFeedbackOn) {
            dialang.session.instantFeedbackOn = false;
            $(this).attr('title', instantFeedbackOnTooltip)
                .find('img').attr('src', "/images/instantFeedbackOff.gif");
        } else {
            dialang.session.instantFeedbackOn = true;
            dialang.initialiseReviewDialog(true);
            $(this).attr('title', instantFeedbackOffTooltip)
                .find('img').attr('src', "/images/instantFeedbackOn.gif");
        }
        return false;
    });

    // We're not in review mode, so show the progress bar
    if (dialang.session.itemsCompleted === 0) {
        $('#progressbar').css('display', 'inline-block').progressbar({max: parseInt(dialang.session.totalItems, 10), value: 0});
    } else {
        $('#progressbar').progressbar('option', 'value', parseInt(dialang.session.itemsCompleted, 10));
    }

    $.get('/dialang-content/baskets/' + dialang.session.al + "/" + dialang.session.currentBasketId + '.html', function (data) {

        $('#content').html(data);

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
            timeout: 5000,
            success: function (nextBasketData, textStatus, jqXHR, jqFormElement) {

                var basket = nextBasketData.scoredBasket;

                // Map the basket onto the basket id for lookup later.
                dialang.pass.baskets[basket.id] = basket;

                basket.items.forEach(function (item) {

                    // Map the item id onto the basket id for lookup later.
                    dialang.pass.itemToBasketMap[item.id] = basket.id;

                    dialang.pass.items.push(item);

                    if (item.itemType === 'mcq' || item.itemType === 'gapdrop') {

                        // Set the response text on this item
                        item.answers.forEach(function (answer) {

                            if (answer.correct) {
                                item.correctAnswer = answer.text;
                            }
                            if (answer.id === item.responseId) {
                                item.responseText = answer.text;
                            }
                        });
                    } else if (item.itemType === 'gaptext' || item.itemType === 'shortanswer') {

                        var answersMarkup = '';
                        item.answers.forEach(function (answer) {
                            answersMarkup += answer.text + '<br />';
                        });
                        item.correctAnswer = answersMarkup;
                    }
        
                    var subskill = item.subskill;

                    if (!dialang.pass.subskills[subskill]) {
                        // No subskill keyed yet, ensure that one is.
                        dialang.pass.subskills[subskill] = {'correct':[],'incorrect':[]};
                    }

                    if (item.correct) {
                        dialang.pass.subskills[subskill].correct.push(item);
                    } else {
                        dialang.pass.subskills[subskill].incorrect.push(item);
                    }
                }); // end items loop

                if (!dialang.scoredBaskets) {
                    dialang.scoredBaskets = [];
                }

                dialang.scoredBaskets.push(basket);

                if (nextBasketData.testDone) {
                    dialang.session.testDone = true;
                    dialang.session.itemLevel = nextBasketData.itemLevel;
                    if (!dialang.session.instantFeedbackOn) {
                        dialang.switchState('endoftest');
                    }
                } else {

                    dialang.session.currentBasketId = nextBasketData.nextBasketId;
                    dialang.session.itemsCompleted = nextBasketData.itemsCompleted;
                    if (!dialang.session.instantFeedbackOn) {
                        dialang.switchState('test');
                    }
                }

                if (dialang.session.instantFeedbackOn) {
                    if (basket.basketType === 'mcq' ) {
                        // MCQ baskets only ever have one item.
                        var mcqItem = basket.items[0];
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
                    } else if (basket.basketType === 'tabbedpane' ) {

                        dialang.launchMultiItemReviewDialog(basket,1,function (clickedItemId) {

                            // Get the index of the clicked review tab
                            var index = $('#tabbedpane-tabs a[href="#tabs-' + clickedItemId + '"]').parent().index();
                            $("#tabbedpane-tabs").tabs('option','active',index);
                        });
                    } else if (basket.basketType === 'gapdrop') {

                        dialang.launchMultiItemReviewDialog(basket, 1, function (clickedItemId) {

                            $('select').removeClass("outlined");
                            $('select[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
                        });
                    } else if (basket.basketType === 'shortanswer' || basket.basketType === 'gaptext') {

                        dialang.launchMultiItemReviewDialog(basket, 1, function (clickedItemId) {

                            $('input[type="text"]').removeClass("outlined");
                            $('input[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
                        });
                    }
                } // if (dialang.session.instantFeedbackOn)
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert('Failed to submit basket. Reason: ' + textStatus);
            }
        }); // ajaxForm

        if ('mcq' === basketType) {
            $.getScript('/js/mcqresponse.js');
        } else if ('gapdrop' === basketType) {
            $.getScript('/js/gapdropresponse.js');
        } else if ('gaptext' === basketType) {
            $.getScript('/js/gaptextresponse.js');
        } else if ('shortanswer' === basketType) {
            $.getScript('/js/shortanswerresponse.js');
        } else if ('tabbedpane' === basketType) {
            $.getScript('/js/tabbedpaneresponse.js');
        }

        $('#confirm-skip-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});

        $('#confirm-skip-yes').off('click').click(function (e) {

            dialang.switchState("endoftest");
            return false;
        });

        $('#confirm-skip-no').off('click').click(function (e) {

            $('#confirm-skip-dialog').dialog('close');
            return false;
        });
    });
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

    var basket = dialang.session.reviewBasket;

    var initialItemId = dialang.session.reviewItemId;

    var positionInBasket = dialang.session.reviewItemPosition;

    $.get('/dialang-content/baskets/' + dialang.session.al + "/" + basket.id + '.html', function (data) {

        $('#content').html(data);

        dialang.initialiseReviewDialog();

        $('#confirm-skip-dialog').hide();

        if (basket.basketType === 'mcq' ) {
            // MCQ baskets only ever have one item.
            var item = basket.items[0];
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
        } else if (basket.basketType === 'tabbedpane' ) {

            // This is a tabbebpane basket as it has multiple mcq items

            $("#tabbedpane-tabs").tabs();

            // Launch the dialog
            dialang.launchMultiItemReviewDialog(basket,positionInBasket, function (clickedItemId) {

                // Get the index of the clicked review tab
                var index = $('#tabbedpane-tabs a[href="#tabs-' + clickedItemId + '"]').parent().index();
                $("#tabbedpane-tabs").tabs('option','active',index);
            });

            // Select the response radio buttons
            basket.items.forEach(function (item) {
                $("input[value=\"" + item.responseId + "\"]").prop("checked", true);
            });
        } else if (basket.basketType === 'gapdrop') {

            // Launch the dialog
            dialang.launchMultiItemReviewDialog(basket,positionInBasket, function (clickedItemId) {

                $('select').removeClass("outlined");
                $('select[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
            });

            basket.items.forEach(function (item) {
                $("option[value=\"" + item.responseId + "\"]").prop("selected", true);
            });
        } else if (basket.basketType === 'shortanswer' || basket.basketType === 'gaptext') {
            // Launch the dialog
            dialang.launchMultiItemReviewDialog(basket,positionInBasket, function (clickedItemId) {

                $('input[type="text"]').removeClass("outlined");
                $('input[name="'+ clickedItemId + '-response"]').addClass("outlined"); 
            });
            basket.items.forEach(function (item) {
                $("input[name=\"" + item.id + "-response\"]").val(item.responseText);
            });
        }
    });
}

