// Deployment: in orbit over the Earth's limb, springs push the payload off the upper stage at 1.0,
// and the stage backs away with a puff of its thrusters at 1.6. The payload comes alive: a radio
// ring from 2.6.
// Glitch: the springs fire at 1.0 but the payload stays on (a jolt); a second try at 2.0 frees it,
// tumbling, and it steadies by 3.6.
// Failure: the stage's thrusters fire the wrong way at 1.6; it runs into the payload at 2.2, which
// spins away, a piece knocked off it, and never calls home.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";

put("paper", "screen");
put("d-earth");
put("d-springs");
put("d-stage");
put("d-payload");
put("d-puff");
put("d-chip");
for (var i = 1; i <= 4; i++) put("ring-" + i);
hide("d-chip");

// The Earth turns below, slowly.
turn("d-earth", function (t) { return -0.25 * t; });

// The craft is drawn 1.5 times the launch's size (parts.py), and so are its moves.
var K = 1.5;
var FREE = glitch ? 2.0 : 1.0;
// The payload's drift away from the stage (px): from rest at the push to 300 at 6.0.
var drift = smooth([[FREE, 0], [FREE + 0.4, 55 * K], [6.0, 300 * K]]);
var HIT = 2.2;
var payload = failure ? function (t) { return t < HIT ? [drift(t), 0] : [drift(HIT) + 190 * K * (t - HIT), -70 * K * (t - HIT)]; }
                      : function (t) { return [drift(t), 0]; };
// The glitch: a jolt at the first push, a tumble after the second.
var jolt = glitch ? function (t) { return t >= 1.0 && t < 1.35 ? 5 * K * jitter(t, 3) : 0; } : function () { return 0; };
move("d-payload", function (t) { var p = payload(t); return [p[0] + jolt(t), p[1]]; });
turn("d-payload", failure ? function (t) { return t < HIT ? 0 : 160 * (t - HIT); }
                : glitch ? track([[2.0, 0], [2.5, 26, "out"], [3.0, -12, "inout"], [3.6, 0, "inout"]])
                : function () { return 0; });

// The stage: a recoil from the push, then the puff that backs it away (or, failing, pushes it on).
var stage = failure ? smooth([[1.0, 0], [1.5, -30 * K], [HIT, drift(HIT) - 2], [HIT + 0.5, drift(HIT) + 6 * K], [6.5, drift(HIT) + 30 * K]])
          : smooth([[FREE, 0], [FREE + 0.6, -40 * K], [FREE + 1.2, -70 * K], [6.0, -130 * K]]);
move("d-stage", function (t) { return [stage(t) + jolt(t), 0]; });
turn("d-stage", failure ? track([[HIT, 0], [HIT + 0.6, -9, "out"]]) : function () { return 0; });

// The springs flash at each push.
var pushes = glitch ? [1.0, 2.0] : [1.0];
fade("d-springs", function (t) {
    for (var k = 0; k < pushes.length; k++) if (t >= pushes[k] && t < pushes[k] + 0.35) return 1;
    return 0;
});
move("d-springs", function (t) { return [Math.max(0, Math.min(stage(t), 0)), 0]; });
grow("d-springs", function (t) {
    for (var k = 0; k < pushes.length; k++) if (t >= pushes[k] && t < pushes[k] + 0.35) return [1 + 1.2 * (t - pushes[k]) / 0.35, 1];
    return 1;
});

// The thrusters' puff: at the stage's front, or behind it on a failure, where it pushes the wrong way.
var PUFF = FREE + 0.6;
move("d-puff", function (t) { return [stage(t) + (failure ? -150 * K : 0), 0]; });
grow("d-puff", track([[PUFF, 0.4], [PUFF + 0.5, 1.4, "out"], [PUFF + 1.2, 1.7, "lin"]]));
fade("d-puff", track([[PUFF, 0, "snap"], [PUFF + 0.02, 1, "snap"], [PUFF + 0.5, 1], [PUFF + 1.2, 0, "lin"]]));

if (failure) {
    // A piece knocked off at the hit, flying up and back.
    fade("d-chip", function (t) { return t >= HIT ? 1 : 0; });
    move("d-chip", function (t) { var u = Math.max(0, t - HIT); return [drift(HIT) - 60 * K * u, -150 * K * u]; });
    turn("d-chip", function (t) { return -300 * Math.max(0, t - HIT); });
    camera(function () { return 0; }, function (t) { return t >= HIT ? 6 * Math.max(0, 1 - (t - HIT) / 0.4) : 0; });
}

// The payload's radio rings, from its nose, once it's alive.
var sends = [];
if (!failure) for (var s = glitch ? 3.4 : 2.6; s < DUR; s += 0.8) sends.push(s);
for (i = 1; i <= 4; i++) {
    (function (n, k) {
        var mine = function (t) {
            var last = -1;
            for (var j = k; j < sends.length; j += 4) if (sends[j] <= t) last = sends[j];
            return last;
        };
        // The rings rest at (960, 560); the payload's nose at (912, 560).
        move(n, function (t) { var te = mine(t); var p = payload(te < 0 ? 0 : te); return [912 + p[0] - 960, p[1]]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 1.0 : 1.0 + 2.4 * clamp((t - te) / 1.0, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : 1 - clamp((t - te) / 1.0, 0, 1); });
    })("ring-" + i, i - 1);
}
