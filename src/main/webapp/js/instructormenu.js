$('#navbar').hide();
$.get('/dialang-content/instructormenu/' + dialang.session.al + '.html', function (data) {
    $('#content').html(data);
    $(document).ready(function () {
    });
});
