// Back in through the airlock (2026-10-18; Voskhod 2's last spacewalk step): from FLOAT the figure pulls
// itself in along the tether to stand over the hatch, its suit swelling in the vacuum as it comes (Leonov's
// ballooned until he couldn't bend); it bleeds the suit down (a puff from the valve) and slides in feet first;
// the hatch closes.
// Glitch: what happened: feet first it sticks in the hatch at 2.1; it bleeds the suit down to a dangerously low
// pressure, pulls out, turns over and goes in head first, as Leonov did. Failure: the same, and at 3.0 the visor
// fogs over (he was overheating, soaked in sweat): it gets in at last, much later.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FLOAT = [-60, -110, -345], OVER = [0, -30, -360], STUCK = 2.1, FOG = 3.0;

spacewalkScene(false, true);
put("e-vent", "e-suit");

var come = [[0, FLOAT], [0.3, FLOAT], [1.2, [-20, -70, -365]], [1.8, OVER]];
var pose, swell, vent, done;
if (glitch || failure) {
    var stuck = [0, -8, -360], tug = [0, -13, -360], over = [0, -36, -180];
    var keys = come.concat([[STUCK, stuck], [2.3, tug], [2.5, stuck], [3.0, OVER]]);
    if (glitch) {
        keys = keys.concat([[3.6, over], [4.4, [0, INSIDE, -180]]]);
        done = 4.5;
    } else {
        keys = keys.concat([[4.0, over], [4.4, [0, -12, -180]], [4.7, [0, -16, -180]], [5.6, [0, INSIDE, -180]]]);
        done = 5.7;
        fade("e-fog", track([[FOG, 0], [FOG + 0.4, 1, "out"]]));
    }
    pose = poses(keys);
    swell = track([[0.6, 1], [1.8, 1.2], [2.3, 1.3], [2.6, 1.3], [3.0, 1.0]]);
    vent = [2.5, 2.7, 2.9];
} else {
    pose = poses(come.concat([[2.2, OVER], [3.0, [0, INSIDE, -360]]]));
    swell = track([[0.6, 1], [1.8, 1.2], [2.0, 1.2], [2.3, 1.0]]);
    vent = [1.9];
    done = 3.1;
}
walk(pose);
carry(pose, 99);
grow("e-suit", swell);
fade("e-vent", function (t) {
    for (var i = 0; i < vent.length; i++) if (t >= vent[i] && t < vent[i] + 0.25) return 1 - (t - vent[i]) / 0.25;
    return 0;
});
hatch(-1, done);
