$(document).ready(function () {
    $("#tabbedpane-tabs").tabs();
});

$("#radios > input").click(function (e) {

    var basketId = $(this).attr('basketId');
    $("#" + basketId + "-tab").addClass("completed-basket");

    var complete = true;
    itemIds.forEach(function (id) {

        if($('input[name=' + id + '-response]:checked').length === 0) {
            complete = false;
        }
    });

    dialang.responseComplete(complete);
});
