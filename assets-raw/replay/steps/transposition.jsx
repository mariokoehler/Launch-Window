// Transposition and docking: the capsule lets go of the adapter at 0.3 and backs off, the adapter's
// panels swing open and fly away, showing the lander; the capsule turns round, closes in and docks with
// the lander's hatch at 2.8 (a flash).
// Glitch: at 2.8 it bounces off, and again at 3.6; the third try at 4.4 holds (Apollo 14 needed six).
// Failure: three bounces, and it backs away: the lander stays in its adapter.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SEP = 0.3, TURN = 1.2, IN = 2.2, TOUCH = [2.8, 3.6, 4.4];

apolloScene();
lmAt(function () { return 0; });

// Backing off with its nose to the adapter, turning round, coming in.
var off = track([[SEP, 44], [1.2, -200, "out"]]);
var spin = track([[TURN, 180], [IN, 0, "inout"]]);
var first = track([[IN, -200], [2.4, -186, "in"], [TOUCH[0], DOCKED, "lin"]]);
var x;
if (glitch || failure) {
    // A bounce back to 30 px off after each touch, and in again.
    var tries = function (t) {
        for (var k = 0; k < TOUCH.length - 1; k++) {
            if (t > TOUCH[k] && t <= TOUCH[k + 1]) {
                var u = (t - TOUCH[k]) / (TOUCH[k + 1] - TOUCH[k]);
                return DOCKED - 34 * Math.sin(Math.PI * u);
            }
        }
        if (t > TOUCH[2]) return failure ? track([[TOUCH[2], DOCKED], [5.0, -40, "out"], [6.4, -110, "lin"]])(t) : DOCKED;
        return null;
    };
    x = function (t) { return t <= SEP ? 44 : t <= IN ? off(t) : t <= TOUCH[0] ? first(t) : tries(t); };
} else {
    x = function (t) { return t <= SEP ? 44 : t <= IN ? off(t) : t <= TOUCH[0] ? first(t) : DOCKED; };
}
move("k-carrier", function (t) { return [x(t), 0]; });
turn("k-carrier", function (t) { return t < TURN ? 180 : spin(t); });
// The panels: open from 0.4, then off and away, turning.
turn("t-panel-top", track([[0.4, 0], [1.0, 50, "out"], [2.6, 120, "lin"]]));
turn("t-panel-bottom", track([[0.4, 0], [1.0, -50, "out"], [2.6, -120, "lin"]]));
move("t-panel-top", track([[1.0, [0, 0]], [2.6, [40, -260], "in"]]));
move("t-panel-bottom", track([[1.0, [0, 0]], [2.6, [40, 260], "in"]]));
fade("t-panel-top", track([[2.0, 1], [2.6, 0, "lin"]]));
fade("t-panel-bottom", track([[2.0, 1], [2.6, 0, "lin"]]));
flashes("t-flash-sep", glitch ? [SEP, TOUCH[2]] : failure ? [SEP] : [SEP, TOUCH[0]]);
puffs("k-brake", [SEP]);
puffs("k-side", [TURN]);
puffs("k-push", glitch || failure ? [IN, 3.2, 4.0] : [IN]);
