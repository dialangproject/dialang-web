$('#radios > span > select').change(function (e) {

    var complete = true;
    $('#radios > span > select').each(function (index, el) {

        if (el.value.length <= 0) {
            complete = false;
        }
    });

    if (complete) {
        dialang.responseComplete();
    }

    return false;
});
