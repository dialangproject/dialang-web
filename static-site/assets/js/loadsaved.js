$('#back').prop('disabled', false).click(function (e) {
    return dialang.navigation.backRules.loadsaved();
});

$('#next').prop('disabled', false).click(function (e) {
    dialang.navigation.nextRules.loadsaved();
});

$.get('/dialang-content/loadsaved/' + dialang.session.al + '.html', function (shell) {

    $('#content').html(shell);

    $(document).ready(function () {

        $('#dialang-token-button').click(function (e) {

            var token = $('#dialang-token-field').val();

            $.ajax({
                url: '/load?token=' + token,
                success: function (data, textStatus, jqXHR) {
                    if (data.error) {
                        console.log('error:' + data.error);
                    } else {
                        dialang.session.tl = data.tl;
                        dialang.session.skill = data.skill;
                        dialang.session.totalItems = data.bookletLength
                        dialang.session.vsptDone[dialang.session.tl] = data.vsptSubmitted;
                        dialang.session.saDone = data.saSubmitted;

                        dialang.pass.loading = true;

                        if (data.vsptSubmitted) {
                            dialang.session.vsptMearaScore = data.vsptMearaScore;
                            dialang.session.vsptLevel = data.vsptLevel;
                        }

                        if (data.saSubmitted) {
                            dialang.session.saLevel = data.saLevel;
                        }

                        if (data.scoredBasketList.length > 0) {
                            dialang.utils.setupKeyboard();

                            data.scoredBasketList.forEach(function (basket) {
                                dialang.utils.configureScoredBasket(basket);
                            });

                            dialang.pass.currentBasketId = data.nextBasketId;

                            if (!dialang.session.instantFeedbackOn) {
                                dialang.switchState('test');
                            }
                        } else if (!data.vsptSubmitted && !data.vsptSkipped && !dialang.flags.hideVSPT) {
                            dialang.switchState('vsptintro');
                        } else {
                            if (!data.saSubmitted && !data.saSkipped && !dialang.flags.hideSA) {
                                dialang.switchState('saintro');
                            } else {
                                //console.log('load could not determine saved state. Defaulting to tls ...');
                                dialang.switchState('testintro');
                                //dialang.switchState('tls');
                            }
                        }
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    alert('textStatus: ' + textStatus);
                    alert('Error: ' + errorThrown);
                }
            });
        });

        $('#dialang-token-field').keyup(function (e) {
            $('#dialang-token-button').prop('disabled', this.value.length != 36)
        });
    });
});
