$('#navbar').hide();
$.get('/dialang-content/als.html', function (data) {
    $('#content').html(data);
});
