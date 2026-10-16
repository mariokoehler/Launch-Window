// Retro shutdown (2026-10-16): Vostok's "retrofire jettison". Vostok had no retro pack to drop: its one
// retro engine was in the instrument module (the pack, standing for it), which the modules separation
// after this drops. So this is the burn's end: on from the retrofire, base first, the flames tail off
// by 0.5, and the attitude jets null what's left of the motion at 1.0; then it coasts on, steady.
// Glitch: the shutdown valve sticks: the engine sputters on, the capsule yawing, until it dies at 2.6
// and the jets steady it.
// Failure: it won't shut down: the engine burns on off its axis, pitching the capsule away until the
// propellant runs out (sputtering from 3.2, out at 3.8), the capsule left turning.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var OUT = failure ? 3.8 : glitch ? 2.6 : 0.5;

put("paper", "screen");
// The motes stream past at the speed the retrofire left (it took 260 px/s off).
motes(function () { return MOTE_SPEED - 260; });
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-flame-1", "r-flame-2", "r-flame-3", "r-strap-a", "r-strap-b", "r-pack", "r-capsule", "r-jet-l", "r-jet-r"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

// Sinking on from where the retrofire left it (140 px), slowly, so the modules separation (which
// starts there) cuts on near enough; the failure's long burn drops it faster.
var sink = function (t) { return 140 + 2.5 * t + (failure ? 6 * Math.max(0, Math.min(t, OUT) - 0.5) : 0); };
move("d-earth", function (t) { return [0, -sink(t)]; });
move("r-orbit", function (t) { return [0, -sink(t)]; });
motesBelow(sink);
turn("d-earth", function (t) { return -2.776 - 0.02 * t; });

// The flames: full at the start (the burn's end), then out by OUT; the glitch's and the failure's
// sputter on the way.
function lit(n) {
    return function (t) {
        if (t >= OUT) return 0;
        if (!(glitch || failure)) return 1 - t / OUT;
        if (t < 0.5) return 1;
        var late = failure ? t > 3.2 : true;
        return late ? (jitter(t, 5 + n) > -0.2 ? 0.8 : 0) : 1;
    };
}
for (i = 1; i <= 3; i++) {
    (function (n) {
        fade("r-flame-" + n, lit(n));
        grow("r-flame-" + n, function (t) { return [Math.max(0.05, lit(n)(t)) * (1 + 0.06 * Math.sin(2 * Math.PI * 12 * t + n)), 1]; });
    })(i);
}

// Its attitude: steady (success); a yaw while the glitch's engine sputters, nulled by 3.4; the
// failure's off-axis burn pitches it further and further, left turning once it's out.
var yaw;
if (failure) {
    yaw = function (t) {
        var u = Math.max(0, Math.min(t, OUT) - 0.5);
        return 9 * u * u + (t > OUT ? 22 * (t - OUT) : 0);
    };
} else if (glitch) {
    yaw = function (t) {
        if (t < 0.5) return 0;
        return 10 * Math.sin(2 * Math.PI * 0.9 * (t - 0.5)) * clamp((3.4 - t) / 0.8, 0, 1);
    };
} else {
    yaw = function () { return 0; };
}
turn("r-carrier", function (t) { return 180 + yaw(t); });

// The jets: a puff each to null the motion once the engine is out (none in the failure).
function puffAt(name, at) {
    grow(name, function (t) { return t >= at && t < at + 0.6 ? 0.5 + (t - at) / 0.6 : 0.5; });
    fade(name, function (t) { return !failure && t >= at && t < at + 0.6 ? 1 - (t - at) / 0.6 : 0; });
}
puffAt("r-jet-l", glitch ? 2.8 : 1.0);
puffAt("r-jet-r", glitch ? 3.3 : 1.3);
// A little shake while the engine burns.
camera(function () { return 0; }, function (t) { return lit(1)(t) > 0 ? 1.5 : 0; });
