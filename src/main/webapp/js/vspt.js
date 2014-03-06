$('#skipforward').prop('disabled', false).click(function (e) {

    $('#confirm-skip-dialog').dialog('open');
    return false;
});

$.get('/dialang-content/vspt/' + dialang.session.al + '/' + dialang.session.tl + '.html', function (data) {

    $('#content').html(data);

    $('.word').click(function (e) {
        
        var wordId = this.id.substring(0,this.id.indexOf('_'));

        // Indicate this word as answered.
        answered[wordId] = true;

        // Switch the button image appropriately.
        if ($(this).hasClass('valid-button')) {
            $("[for=" + this.id + "]").html("<img src=\"/images/trueselected.gif\"/>");
            $("[for=" + wordId + "_incorrect]").html("<img src=\"/images/false.gif\"/>");
        } else {
            $("[for=" + this.id + "]").html("<img src=\"/images/falseselected.gif\"/>");
            $("[for=" + wordId + "_correct]").html("<img src=\"/images/true.gif\"/>");
        }

        // Have we answered all the words?
        var allAnswered = true;
        for (var id in answered) {
            if (answered[id] != true) {
                allAnswered = false;
            }
        }

        if (allAnswered) {
            // All the words have been answered, enable the send and next buttons.
            $('#send-button').prop('disabled', false);
            $('#next').prop('disabled', false);
        }

        // This will change the colour of the word. The css class is called 'done'.
        $(this).parent().addClass('done');
    });

    $(document).keydown(function (e) {

        if(e.keyCode == '72' && e.ctrlKey) {
            $('#send-button,#next').prop('disabled', false);
            $('#vspt-table > tbody > tr > td > input.correct').attr('checked','checked').trigger('click');
            return false;
        } else if(e.keyCode == '76' && e.ctrlKey) {
            $('#send-button,#next').prop('disabled', false);
            $('#vspt-table > tbody > tr > td > input.incorrect').attr('checked','checked').trigger('click');
            return false;
        }
    });

    $('#confirm-send-dialog').dialog({modal: true, width: 400, height: 250, autoOpen: false});

    $('#confirm-send-yes').click(function (e) {

        $('#vsptform').ajaxSubmit({
            'dataType':'json',
            success: function (scores, textStatus, jqXHR, jqFormElement) {

                dialang.session.vsptMearaScore = scores.vsptMearaScore;
                dialang.session.vsptLevel = scores.vsptLevel;
                $('#confirm-send-dialog').dialog('destroy');

                dialang.session.vsptDone[dialang.session.tl] = true;

                dialang.switchState('vsptfeedback');
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert('Failed to submit vspt');
            }
        }); // ajaxSubmit
        return false;
    });

    $('#confirm-send-no').click(function (e) {

        $('#confirm-send-dialog').dialog('close');
        return false;
    });

    $('#send-button,#next').click(function (e) {

        $('#confirm-send-dialog').dialog('open');
        return false;
    });

    $('#confirm-skip-dialog').dialog({modal: true, width: 500, height: 450, autoOpen: false});
    $('#confirm-skip-yes').on('click',dialang.skipVSPT);
    $('#confirm-skip-no').click(function (e) {
        $('#confirm-skip-dialog').dialog('close');
        return false;
    });

    $('#skipforward').click(function (e) {

        $('#confirm-skip-dialog').dialog('open');
        return false;
    });
});
