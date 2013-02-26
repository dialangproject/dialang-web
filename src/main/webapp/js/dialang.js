$('.al-button').click( function(e) {
    window.location = "legend?al=" + this.id;
});

function cookieToObject() {

    var state = {};

    var nameEQ = "DIALANG=";
    var ca = document.cookie.split(';');
    for(var i=0,j=ca.length;i<j;i++) {
        var c = ca[i];
        // Trim leading whitespace
        while (c.charAt(0)==' ') {
            c = c.substring(1,c.length);
        }

        // If this is the DIALANG cookie, parse it.
        if (c.indexOf(nameEQ) == 0) {
            var dialangCookie = unescape(c.substring(nameEQ.length,c.length));
            var pairs = dialangCookie.split("|");
            for(var i=0,j=pairs.length;i<j;i++) {
                var pair = pairs[i].split("=");
                state[pair[0]] = pair[1];
            }
        }
    }
    return state;
}

function getTL() {
    return cookieToObject().tl;
}

function getSkill() {
    return cookieToObject().skill;
}

function getVSPTScore() {
    return cookieToObject().vsptScore;
}

function getVSPTLevel() {
    return cookieToObject().vsptLevel;
}
