dialang.setExplfbBackHandler = function () {

    $('#back').prop('disabled', false).click(function (e) {

        if (dialang.session.saFeedbackMode) {
            dialang.switchState('safeedback');
        } else {
            dialang.switchState('feedbackmenu');
        }
        return false;
    });
};
dialang.setExplfbBackHandler();

$.get('/dialang-content/aboutsa/' + dialang.session.al + '/index.html', function (shell) {

    $('#content').html(shell);
    $.get('/dialang-content/aboutsa/' + dialang.session.al + '/main.html', function (main) {
        $('#explfb-content').html(main);
    });

    $('#howoften').click(function (e) {

        dialang.setExplfbBackHandler();
        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/howoften.html', function (howoften) {

            $('#explfb-content').html(howoften);
            $('#infrequently').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#howoften').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/infrequently.html', function (infrequently) {
                    $('#explfb-content').html(infrequently);
                });
                return false;
            });
            $('#longtime').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#howoften').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/longtime.html', function (longtime) {
                    $('#explfb-content').html(longtime);
                });
                return false;
            });
        });
        return false;
    });

    $('#how').click(function (e) {

        dialang.setExplfbBackHandler();
        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/how.html', function (how) {

            $('#explfb-content').html(how);

            $('#overestimate').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#how').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/overestimate.html', function (overestimate) {
                    $('#explfb-content').html(overestimate);
                });
                return false;
            });

            $('#underestimate').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#how').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/underestimate.html', function (underestimate) {
                    $('#explfb-content').html(underestimate);
                });
                return false;
            });
        });
    });

    $('#situations').click(function (e) {

        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/situations.html', function (situations) {
            $('#explfb-content').html(situations);
        });
        return false;
    });

    $('#otherlearners').click(function (e) {
        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/otherlearners.html', function (otherlearners) {
            $('#explfb-content').html(otherlearners);
        });
        return false;
    });

    $('#othertests').click(function (e) {

        dialang.setExplfbBackHandler();
        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/othertests.html', function (othertests) {

            $('#explfb-content').html(othertests);
            $('#differenttests').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#othertests').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/differenttests.html', function (differenttests) {
                    $('#explfb-content').html(differenttests);
                });
                return false;
            });
            $('#schooltests').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#othertests').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/schooltests.html', function (schooltests) {
                    $('#explfb-content').html(schooltests);
                });
                return false;
            });
            $('#worktests').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#othertests').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/worktests.html', function (worktests) {
                    $('#explfb-content').html(worktests);
                });
                return false;
            });
            $('#internationaltests').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#othertests').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/internationaltests.html', function (internationaltests) {
                    $('#explfb-content').html(internationaltests);
                });
                return false;
            });
        });
        return false;
    });

    $('#yourtargets').click(function (e) {

        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/yourtargets.html', function (yourtargets) {
            $('#explfb-content').html(yourtargets);
        });
        return false;
    });

    $('#reallife').click(function (e) {

        dialang.setExplfbBackHandler();
        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/reallife.html', function (reallife) {

            $('#explfb-content').html(reallife);
            $('#anxiety').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/anxiety.html', function (anxiety) {
                    $('#explfb-content').html(anxiety);
                });
                return false;
            });
            $('#timeallowed').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/timeallowed.html', function (timeallowed) {
                    $('#explfb-content').html(timeallowed);
                });
                return false;
            });
            $('#support').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/support.html', function (support) {
                    $('#explfb-content').html(support);
                });
                return false;
            });
            $('#number').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/number.html', function (number) {
                    $('#explfb-content').html(number);
                });
                return false;
            });
            $('#familiarity').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/familiarity.html', function (familiarity) {
                    $('#explfb-content').html(familiarity);
                });
                return false;
            });
            $('#medium').click(function (e) {

                $('#back').off('click').click(function (e) {

                    $('#reallife').trigger('click');
                    return false;
                });
                $.get('/dialang-content/aboutsa/' + dialang.session.al + '/medium.html', function (medium) {
                    $('#explfb-content').html(medium);
                });
                return false;
            });
        });
        return false;
    });

    $('#otherreasons').click(function (e) {

        $.get('/dialang-content/aboutsa/' + dialang.session.al + '/otherreasons.html', function (otherreasons) {
            $('#explfb-content').html(otherreasons);
        });
        return false;
    });
});
