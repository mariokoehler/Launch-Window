// Variant 3, failure (storyboard row 3): 7.0 s, then a 0.6 s hold.
stack("trail-Failure");

wisps(track([[0.8, 1], [1.2, 0, "lin"]]));

var CUT = 3.2, BREAK = 4.5;
// Ignition 0.8, full 1.0; flickering full and ignite every 0.08 s from 3.0, out at the cut.
states(["flame-ignite", "flame-full"], function (t) {
    if (t < 0.8 || t >= CUT) return "";
    if (t < 1.0) return "flame-ignite";
    if (t >= 3.0) return Math.floor((t - 3.0) / 0.08) % 2 === 0 ? "flame-full" : "flame-ignite";
    return "flame-full";
});
flicker(["flame-ignite", "flame-full"]);

turn("armUpper", track([[1.1, 0], [1.33, 83, "out"], [1.4, 80, "inout"]]));
turn("armLower", track([[1.1, 0], [1.33, -83, "out"], [1.4, -80, "inout"]]));

// Up the pad's line until 3.4 (the bell at 520), pitching left from the cut.
var rise = smooth([[1.6, 0], [2.2, 80], [3.4, 320]]);
var pitch = track([[CUT, 0], [3.4, -6, "in"], [BREAK, -32, "in"]]);
var drift = track([[3.4, -25.1], [BREAK, -200, "in"]]);
var p34 = poseFor(960, 520, -6);
// After the cut it coasts to a stop a little higher, then starts to drop.
var coast = smooth([[3.0, poseFor(960, 840 - rise(3.0), 0)[1]], [3.4, p34[1]], [3.95, -338], [BREAK, -330]]);
function pose(t) {
    if (t <= 3.4) { var p = poseFor(960, 840 - rise(t), pitch(t)); return [p[0], p[1], pitch(t)]; }
    return [drift(t), coast(t), pitch(t)];
}
move("rocket", function (t) { var p = pose(t); return [p[0], p[1]]; });
turn("rocket", function (t) { return pose(t)[2]; });
fade("rocket", function (t) { return t < BREAK ? 1 : 0; });
// The trail ends where the engine quit; the planned path goes on without it, dashed.
trailTo("trail-Failure", function (t) { var p = pose(Math.min(t, 3.4)); return bellOf(p[0], p[1], p[2])[1]; });
fade("trailPlanned", track([[CUT, 0], [3.5, 1, "lin"]]));

move("smoke-7", function () { return [0, -334]; });
grow("smoke-7", track([[CUT, 0.3], [3.5, 1.5, "out"], [4.6, 1.7, "lin"]]));
fade("smoke-7", track([[CUT, 0, "snap"], [CUT + 0.02, 1, "snap"], [3.6, 1], [4.6, 0.4, "lin"], [6.0, 0, "lin"]]));

// Break-up: each piece starts where it sits in the rocket, is pushed apart in 0.1 s to the
// storyboard's pose, then falls along its dashed curve, spinning, and bounces 6 px on landing.
var AT = {"debris-1": [960, 420], "debris-2": [960, 560], "debris-3": [960, 700], "debris-4": [960, 800],
          "debris-5": [928, 630], "debris-6": [990, 630]};
var APART = {"debris-1": [-313.4, -331.5, -18], "debris-2": [-227.6, -334.1, -38], "debris-3": [-140.6, -335.0, -26],
             "debris-4": [-74.9, -329.9, -44], "debris-5": [-209.2, -327.6, -62], "debris-6": [-158.7, -360.5, -2]};
var SPIN_TO = {"debris-1": -150, "debris-2": -82, "debris-3": -98, "debris-4": -196, "debris-5": -400, "debris-6": -330};
var LANDS = {"debris-1": 6.8, "debris-2": 6.4, "debris-3": 6.2, "debris-4": 6.6, "debris-5": 6.7, "debris-6": 6.3};
var rb = pose(BREAK);
for (var name in AT) {
    (function (n) {
        var c = AT[n], q = FALL[n], land = LANDS[n], a = APART[n];
        // The piece's pose as part of the rocket at the break.
        var rr = rb[2] * RAD;
        var dx = c[0] - 960, dy = c[1] - 600;
        var inRocket = [dx * Math.cos(rr) - dy * Math.sin(rr) + 960 + rb[0] - c[0],
                        dx * Math.sin(rr) + dy * Math.cos(rr) + 600 + rb[1] - c[1]];
        var apart = track([[BREAK, inRocket], [BREAK + 0.1, [a[0], a[1]], "out"]]);
        var spin = track([[BREAK, rb[2]], [BREAK + 0.1, a[2], "out"], [land, SPIN_TO[n], "lin"]]);
        move(n, function (t) {
            if (t <= BREAK + 0.1) return apart(t);
            var s = Math.min(1, (t - BREAK - 0.1) / (land - BREAK - 0.1));
            var u = 0.3 * s + 0.7 * s * s; // already moving, then gravity
            var x = (1 - u) * (1 - u) * q[0] + 2 * (1 - u) * u * q[2] + u * u * q[4];
            var y = (1 - u) * (1 - u) * q[1] + 2 * (1 - u) * u * q[3] + u * u * q[5];
            var b = t > land && t < land + 0.2 ? -6 * Math.sin(Math.PI * (t - land) / 0.2) : 0;
            return [x - c[0], y - c[1] + b];
        });
        turn(n, spin);
        fade(n, function (t) { return t < BREAK ? 0 : 1; });
    })(name);
}

// The burst: a ring of gold strokes, snapping in and fading out.
move("burst", function () { return [-200, -330]; });
grow("burst", track([[BREAK, 0.4], [BREAK + 0.15, 1.0, "out"], [4.9, 1.3, "lin"]]));
fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [4.65, 1], [4.9, 0, "lin"]]));
// Smoke where it came apart: pops, drifts up 40 px, fades by 6.0.
var pops = {"smoke-8": [-160, -320, 1.2], "smoke-9": [-246, -350, 1.0]};
for (name in pops) {
    (function (n, p) {
        move(n, track([[BREAK, [p[0], p[1]]], [6.0, [p[0], p[1] - 40], "out"]]));
        grow(n, track([[BREAK, 0.3], [BREAK + 0.25, p[2], "out"]]));
        fade(n, track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [4.8, 1], [6.0, 0, "lin"]]));
    })(name, pops[name]);
}
// A puff where each of the big pieces lands, rising 30 px, then 20 more in the hold.
var landPuffs = {"smoke-10": [-440, -64, 1.3, LANDS["debris-2"]], "smoke-11": [-260, -60, 1.2, LANDS["debris-3"]],
                 "smoke-12": [-710, -60, 1.3, LANDS["debris-1"]]};
for (name in landPuffs) {
    (function (n, p) {
        move(n, track([[p[3], [p[0], p[1] + 30]], [7.0, [p[0], p[1]], "out"], [7.6, [p[0], p[1] - 20], "lin"]]));
        grow(n, track([[p[3], 0.3], [p[3] + 0.3, p[2], "out"]]));
        fade(n, track([[p[3], 0, "snap"], [p[3] + 0.02, 1, "snap"], [7.0, 0.8, "lin"], [7.6, 0.4, "lin"]]));
    })(name, landPuffs[name]);
}
// The fall paths draw in as the last pieces come down: the record of the loss.
for (var i = 1; i <= 6; i++) fade("fallPath-" + i, track([[6.3, 0], [7.0, 1, "inout"]]));

var s12 = [[0.8, -12, 0.2, 0, "snap"], [0.82, -12, 0.2, 1, "snap"], [1.0, -8, 0.5, 1, "out"], [1.4, 60, 1.0, 1, "out"],
           [2.2, 140, 1.4, 1, "out"], [3.4, 180, 1.5, 0.6, "out"], [4.6, 200, 1.5, 0.35, "out"], [7.0, 210, 1.5, 0.2, "lin"]];
var s34 = [[1.0, 30, 0.4, 0, "lin"], [1.4, 70, 1.0, 1, "out"], [2.2, 150, 1.4, 1, "out"], [3.4, 190, 1.5, 0.6, "out"],
           [4.6, 210, 1.5, 0.35, "out"], [7.0, 220, 1.5, 0.2, "lin"]];
var s56 = [[1.6, 100, 0.4, 0, "lin"], [2.2, 184, 1.4, 1, "out"], [3.4, 236, 1.5, 0.6, "out"], [4.6, 262, 1.5, 0.35, "out"],
           [7.0, 275, 1.5, 0.2, "lin"]];
groundSmoke({"smoke-1": s12, "smoke-2": s12, "smoke-3": s34, "smoke-4": s34, "smoke-5": s56, "smoke-6": s56});

// The camera never follows: a jolt at the cut, a hard shake at the break-up.
camera(function () { return 0; }, function (t) {
    var a = t < 1.0 ? 0 : t < 1.6 ? 2 : 3 * Math.max(0, 1 - (t - 1.6) / 0.6);
    if (t >= CUT) a = Math.max(a, 4 * Math.max(0, 1 - (t - CUT) / 0.3));
    if (t >= BREAK) a = Math.max(a, 14 * Math.max(0, 1 - (t - BREAK) / 0.6));
    return a;
});
