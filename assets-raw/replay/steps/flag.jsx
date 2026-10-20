// Apollo's first moonwalk (user, 2026-10-20): the lander on the Moon, close in. The suited figure steps out
// onto the porch with the flag furled on its pole over the shoulder, climbs down, hops out beside the ladder,
// stands the pole up and works it into the ground in two pushes, and the cloth runs out on its crossbar. It
// steps back to look up at it, then hops back and climbs in. A plain cloth: no country's.
// Glitch: the first push meets rock and the pole jumps back; the second gets it in. The
// crossbar sticks halfway (as Apollo 11's did), the cloth bunched along it; a tug or two and it runs out.
// Failure: the same jump and shallow push, then the figure slips and falls flat; the pole, barely in, tips
// over. At 3.1 the visor fogs; it gets up at last and goes back in, the flag left lying.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FLAG = [1180, 796], GRIP = [1180, 848.5], LEN = 886 - 848.5;
var PLANT = [FLAG[0] - 18, 880, 0];

put("paper", "screen");
put("n-ground");
put("n-rocks");
put("e-flag-pole");
put("e-flag-furl", "e-flag-pole");
put("e-flag-cloth", "e-flag-pole");
put("e-flag-folds", "e-flag-pole");
put("e-suit");
put("e-fog", "e-suit");
hide("e-fog");
lander();
put("n-ladder");
// The camera in close on the ladder and the ground to its right: twice, (1130 800) at (960 600).
tf(world, "Scale").setValue([200, 200]);
tf(world, "Position").setValue([960 - 2 * 1130, 600 - 2 * 800]);

var trouble = glitch || failure;
var out = [[0, INSIDE], [0.6, PORCH], [1.4, FOOT], [1.9, PLANT]];
// When the hand lets go of the pole (never, falling with it), and how far above its planted place the pole's
// grip is: held high, pushed down in two goes (or jumping back off rock, then in only a little).
var LET_GO = failure ? 99 : glitch ? 2.75 : 2.6;
var lift = trouble ? track([[2.1, 26], [2.25, 16, "in"], [2.4, 22, "out"], [2.7, 0, "in"]])
                   : track([[2.1, 26], [2.3, 10, "in"], [2.55, 0, "in"]]);
var walk;
if (failure) {
    var SLIP = 2.8, fallen = [PLANT[0] + 20, 880, 84], up = 3.6, home = 4.2;
    var path = keys3(out.concat([[SLIP, PLANT], [SLIP + 0.3, fallen, "in"], [up, fallen], [up + 0.4, [PLANT[0] + 20, 880, 0]],
                                 [home, [PLANT[0] + 20, 880, 0]], [home + 0.9, FOOT], [home + 1.7, PORCH], [home + 2.1, INSIDE]]));
    walk = hopping(path, [[1.4, 1.9, 2], [home, home + 0.9, 3]]);
} else {
    var BACK = [1128, 880, 0], LOOK = [1128, 880, -6];
    var t0 = glitch ? 3.9 : 3.0, look = t0 + 0.3, home = glitch ? 4.6 : 4.1;
    var path2 = keys3(out.concat([[t0, PLANT], [look, BACK], [look + 0.3, LOOK], [home - 0.2, LOOK], [home, BACK],
                                  [home + 0.3, FOOT], [home + 1.1, PORCH], [home + 1.5, INSIDE]]));
    walk = hopping(path2, [[1.4, 1.9, 2], [t0, look, 1], [home, home + 0.3, 1]]);
}
walker(walk);

// The pole: in the hand, tilted back over the shoulder, then upright and pushed in; let go, it stays. Falling,
// it tips over backwards about its foot.
var TILT = track([[1.9, -25], [2.1, 0]]);
var planted = [GRIP[0], GRIP[1] - lift(LET_GO)];
var TOPPLE = failure ? track([[2.8, 0], [3.25, -88, "in"]]) : function () { return 0; };
function pole(t) {
    if (failure && t >= 2.8) {
        var a = TOPPLE(t) * RAD, foot = [GRIP[0], GRIP[1] - lift(2.8) + LEN];
        return [foot[0] + LEN * Math.sin(a), foot[1] - LEN * Math.cos(a)];
    }
    if (t >= LET_GO) return planted;
    var h = at(walk(t), EH);
    return t < 1.9 ? h : [h[0], h[1] - lift(t)];
}
move("e-flag-pole", function (t) { var p = pole(t); return [p[0] - GRIP[0], p[1] - GRIP[1]]; });
turn("e-flag-pole", function (t) { return failure && t >= 2.8 ? TOPPLE(t) : TILT(t); });

// The cloth on its crossbar: furled until let go, then run out (or stuck halfway, tugged, then out).
var RUN = trouble ? track([[2.8, 0.08], [3.05, 0.55, "out"], [3.25, 0.62], [3.35, 0.55], [3.55, 0.55], [3.8, 1, "out"]])
                  : track([[2.65, 0.08], [2.95, 1, "out"]]);
var run = failure ? function () { return 0.08; } : RUN;
grow("e-flag-cloth", function (t) { return [run(t), 1]; });
fade("e-flag-cloth", function (t) { return run(t) > 0.1 ? 1 : 0; });
fade("e-flag-furl", function (t) { return clamp(1 - (run(t) - 0.08) / 0.2, 0, 1); });
grow("e-flag-folds", function (t) { return [run(t), 1]; });
fade("e-flag-folds", glitch ? track([[2.8, 0], [3.0, 1], [3.55, 1], [3.8, 0]]) : function () { return 0; });
if (failure) fade("e-fog", track([[3.1, 0], [3.4, 1, "out"]]));
