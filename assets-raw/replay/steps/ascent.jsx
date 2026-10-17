// Ascent: the camera rides along as the rocket pitches over on its planned path (dashed), climbs
// past the clouds, and stages: the first engine cuts at 2.8, the lower stage falls away at 2.9, the
// upper engine lights at 3.2.
// Glitch: after staging the upper stage swings off the path from 3.3 and is steered back by 4.9.
// Failure: the stages don't part; the upper engine lights into the lower stage at 3.2 and the rocket
// breaks up at 3.3.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var MECO = 2.8, SEP = 2.9, IGN = 3.2, BREAK = 3.3;

put("paper", "screen");
put("a-ground");
put("planned");
for (var i = 1; i <= 6; i++) put("a-cloud-" + i);
put("carrier");
// Back to front: the flames and the upper bell behind the stages, the lower stage last.
var parts = ["flame-upper", "upper-bell", "debris-1", "debris-2", "flame-full", "debris-4", "debris-3", "debris-5", "debris-6", "burst"];
for (i = 0; i < parts.length; i++) put(parts[i], "carrier");
hide("debris-5"); hide("debris-6"); hide("burst");
// The escape tower on the capsule (2026-10-17; drawn only in Mercury's parts, a-les): jettisoned once the
// climb is safe, at LES, its motor carrying it off ahead and aside. The failure breaks up first, and it
// goes with the capsule.
var LES = 3.7;
put("a-les", "debris-1");
if (!failure) {
    move("a-les", function (t) { var u = Math.max(0, t - LES); return [40 * u + 60 * u * u, -(120 * u + 700 * u * u)]; });
    turn("a-les", function (t) { return 25 * Math.max(0, t - LES); });
}

// The planned path (parts.py draws the same arc): 8° off vertical, turning right on a 5200 px radius.
var P0 = [860, 200], TH0 = 8, R = 5200;
var CX = P0[0] + R * Math.cos(TH0 * RAD), CY = P0[1] + R * Math.sin(TH0 * RAD);
function path(s) { var th = TH0 * RAD + s / R; return [CX - R * Math.cos(th), CY - R * Math.sin(th), th / RAD]; }
// How far along it the rocket is: accelerating; after the break it coasts at the speed it had.
function dist(t) {
    if (failure && t > BREAK) return dist(BREAK) + (280 + 140 * BREAK) * (t - BREAK);
    return 280 * t + 70 * t * t;
}
// The glitch's swing: off the path to the side (px) and turned (degrees), back by 4.9.
var side = glitch ? smooth([[3.3, 0], [3.9, 120], [4.5, 50], [4.9, 0], [6.5, 0]]) : function () { return 0; };
var swing = glitch ? function (t) { return t < 3.3 || t > 4.9 ? 0 : 20 * Math.sin(2 * Math.PI * 1.25 * (t - 3.3)) * Math.min(1, (4.9 - t) / 0.6); }
                   : function () { return 0; };
function pose(t) {
    var p = path(dist(t)), n = [Math.cos(p[2] * RAD), Math.sin(p[2] * RAD)], o = side(t);
    return [p[0] + n[0] * o, p[1] + n[1] * o, p[2] + swing(t)];
}
move("carrier", function (t) { var p = pose(t); return [p[0] - 960, p[1] - 600]; });
turn("carrier", function (t) { return pose(t)[2]; });
// The camera keeps the rocket a little left of centre, shaking while an engine burns.
bake(tf(world, "Position"), function (t) {
    var p = pose(t), burn = t < MECO || (t >= IGN && !(failure && t >= BREAK)) ? 2 : 0;
    return [840 - p[0] + burn * jitter(t, 1), 560 - p[1] + burn * jitter(t, 2)];
});

// The engines: the first flame until the cut, the upper one from its light (until the break).
fade("flame-full", function (t) { return t < MECO ? 1 : 0; });
fade("flame-upper", function (t) { return t >= IGN && !(failure && t >= BREAK) ? 1 : 0; });
flicker(["flame-full", "flame-upper"]);

if (!failure) {
    // The lower stage drops back along the rocket's axis, turning slowly.
    var lower = function (t) { var u = Math.max(0, t - SEP); return [-20 * u, 40 * u + 260 * u * u]; };
    move("debris-3", lower);
    move("debris-4", lower);
    turn("debris-3", function (t) { return -16 * Math.max(0, t - SEP); });
    turn("debris-4", function (t) { return -16 * Math.max(0, t - SEP); });
} else {
    // The break: a burst at the interstage, and every piece thrown, falling behind.
    move("burst", function () { return [0, 30]; });
    grow("burst", track([[BREAK, 0.3], [BREAK + 0.35, 2.2, "out"], [BREAK + 1.0, 2.6, "lin"]]));
    fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
    var throws = { "debris-1": [-80, -260, -150], "debris-2": [120, -120, 200], "debris-3": [-140, 140, -120],
                   "debris-4": [90, 260, 90], "debris-5": [-300, -60, 400], "debris-6": [300, 40, -400], "upper-bell": [120, -120, 200] };
    for (var n in throws) {
        (function (name, v) {
            var u = function (t) { return Math.max(0, t - BREAK); };
            move(name, function (t) { var k = u(t); return [v[0] * k, v[1] * k + 160 * k * k]; });
            turn(name, function (t) { return v[2] * u(t); });
            if (name === "debris-5" || name === "debris-6") fade(name, function (t) { return t >= BREAK ? 1 : 0; });
        })(n, throws[n]);
    }
    // The upper engine lights into the stage below it: a sputter until the break.
    fade("flame-upper", function (t) { return t >= IGN && t < BREAK ? 1 : 0; });
}
