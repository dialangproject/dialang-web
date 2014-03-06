$('#radios > span > input').keyup(function (e) {

    var complete = true;
    $('#radios > span > input').each(function (index, el) {

        if(el.value.length <= 0) {
            complete = false;
        }
    });

    if(complete) {
        dialang.responseComplete();
    }

    return false;
});
