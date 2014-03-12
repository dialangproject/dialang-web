"use strict";

dialang.navigation = {};

dialang.navigation.nextRules = {

    legend: function () {
        return dialang.switchState('flowchart');
    },
    flowchart: function () {

        if (!dialang.flags.hideTLS) {
            return dialang.switchState('tls');
        } else if (!dialang.flags.hideVSPT) {
            return dialang.switchState('vsptintro');
        } else if (!dialang.flags.hideSA && dialang.session.skill !== 'vocabulary' && dialang.session.skill !== 'structures') {
            return dialang.switchState('saintro');
        } else if (!dialang.flags.hideTest) {
            return dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    },
    tls: function () {

        if (!dialang.flags.hideVSPT) {
            return dialang.switchState('vsptintro');
        } else if (!dialang.flags.hideSA && dialang.session.skill !== 'vocabulary' && dialang.session.skill !== 'structures') {
            return dialang.switchState('saintro');
        } else if (!dialang.flags.hideTest) {
            return dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    },
    vsptintro: function () {
        return dialang.switchState('vspt');
    },
    vspt: function () {

        if (!dialang.flags.hideVSPTResults) {
            return dialang.switchState('vsptfeedback');
        } else if (!dialang.flags.hideSA && dialang.session.skill !== 'vocabulary' && dialang.session.skill !== 'structures') {
            return dialang.switchState('saintro');
        } else if (!dialang.flags.hideTest) {
            return dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    },
    vsptfeedback: function () {

        if (!dialang.flags.hideSA && dialang.session.skill !== 'vocabulary' && dialang.session.skill !== 'structures') {
            return dialang.switchState('saintro');
        } else if (!dialang.flags.hideTest) {
            return dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    },
    saintro: function () {
        return dialang.switchState('sa');
    },
    sa: function () {

        if (!dialang.flags.hideTest) {
            return dialang.switchState('testintro');
        } else if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    },
    testintro: function () {
        return dialang.switchState('test');
    },
    endoftest: function () {

        if (!dialang.flags.hideFeedbackMenu) {
            return dialang.switchState('feedbackmenu');
        }
    }
};

dialang.navigation.backRules = {

    legend: function () {
        return dialang.switchState('als');
    },
    flowchart: function () {
        return dialang.switchState('legend');
    },
    tls: function () {
        return dialang.switchState('flowchart');
    },
    vsptintro: function () {
        return dialang.switchState('tls');
    },
    vsptfeedback: function () {

        if (dialang.session.feedbackMode) {
            return dialang.switchState('feedbackmenu');
        }
    }
};
