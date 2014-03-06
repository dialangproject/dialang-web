$(document).ready(function () {

    var audio = document.getElementById('audio');
    audio.paused = true;
    var playButton = $('#playaudio');

    audio.addEventListener('canplaythrough', function () {
        playButton.prop('disabled', false);
    }, false);

    audio.addEventListener('playing', function () {
        playButton.find('img').attr('src','/images/SpkPressedAnimated.gif');
    }, false);

    audio.addEventListener('ended', function () {
        playButton.prop('disabled', true).find('img').attr('src','/images/SpkDisabled.gif');
    }, false);

    playButton.click(function (e) {

        audio.play();
        return false;
    });
});
