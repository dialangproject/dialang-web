$('#back').prop('disabled', false).click(function (e) {

    dialang.switchState('feedbackmenu');
    return false;
});


$.get('/dialang-content/advfb/' + dialang.session.al + '/index.html', function(shell) {

    $('#content').html(shell);

    $('#advfb-next-level-button').off('click').click(function (e) {

        // This toggles the behaviour of the level buttons. When true, advice on how to
        // get to the next level is displayed rather than a description of the attained level.
        if (dialang.session.adviceMode) {
            dialang.session.adviceMode = false;
        } else {
            dialang.session.adviceMode = true;
            $(this).html("<<");
        }
        $('#advfb-' + dialang.session.currentAdvfbLevel + '-button').trigger('click');
        return false;
    });

    $('#advfb-a1-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'a1';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/A1.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('A1 > A2');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/A1.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-a2-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'a2';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/A2.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('A2 > B1');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/A2.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-b1-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'b1';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/B1.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('B1 > B2');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/B1.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-b2-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'b2';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/B2.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('B2 > C1');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/B2.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-c1-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'c1';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/C1.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('C1 > C2');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/C1.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-c2-button').click(function (e) {

        dialang.session.currentAdvfbLevel = 'c2';
        $('#advfb-buttonbar > button').removeClass('selected');
        $(this).addClass('selected');
        if (dialang.session.adviceMode) {
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/' + dialang.session.tl + '/C1.html', function(advice) {
                $('#advfb-content').html(advice);
            });
        } else {
            $('#advfb-next-level-button').html('C1 > C2');
            $.get('/dialang-content/advfb/' + dialang.session.al + '/' + dialang.session.skill + '/C2.html', function(description) {
                $('#advfb-content').html(description);
            });
        }
        return false;
    });

    $('#advfb-' + dialang.session.itemLevel.toLowerCase() + '-button').addClass('achieved').trigger('click');
});
