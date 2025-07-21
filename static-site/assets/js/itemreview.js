dialang.session.reviewMode = true;

$('#back').prop('disabled', false).click(function () {

    dialang.session.reviewMode = false;
    return dialang.switchState('feedbackmenu');
});

$.get('/dialang-content/itemreview/' + dialang.session.al + '.html', function (data) {

    $('#content').html(data);

    var rows = [];
    for (var subskillKey in dialang.pass.subskills) {
        var fullSubskill = dialang.session.skill + '.' + subskillKey;
        var subskill = dialang.pass.subskills[subskillKey];
        rows.push({
                'description': subskillLookup[fullSubskill], // The translated subskill name
                'correct': subskill.correct, // The list of correct items for this subskill
                'incorrect': subskill.incorrect // The list of incorrect items for this subskil
                });
    }

    // Build the table using mustache
    $.get('/templates/itemreviewtable.mustache', function (template) {

        var output = Mustache.render(template, {'rows':rows});
        $('#item-table').html(output);
        $('.itemreview-button').click(function (e) {

            var itemButtonId =  this.id;
            var clickedItem = null;
            dialang.pass.items.forEach(function (item) {

                // The button id is the item id
                if (item.id == itemButtonId) {
                    clickedItem = item;
                }
            });

            if(clickedItem !== null) {
                dialang.session.reviewBasket = dialang.pass.baskets[dialang.pass.itemToBasketMap[clickedItem.id]];
                dialang.session.reviewItemId = clickedItem.id;
                dialang.session.reviewItemPosition = clickedItem.positionInBasket;
                dialang.switchState('test');
            } else {
                console.error('The clicked item was not in dialang.pass.items.');
            }
            return false;
        });
    },'text');
}); // get content
