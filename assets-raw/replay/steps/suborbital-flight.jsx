// Suborbital flight: too slow to stay up, the capsule coasts on a ballistic arc (dashed) above the
// atmosphere. It rides up to the arc's top, the Earth's limb sinking away, and on the way the
// attitude jets turn it round, base first for the retrofire: a puff at 1.2 starts the turn, one at
// 3.0 stops it. It ends at the top, level, as the retrofire begins.
// Glitch: the stop comes late; it swings past, rocks, and settles by 4.8.
// Failure: a jet sticks open at 3.0, and it spins on out of control.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var START = 1.2, STOP = 3.0, K = 0.00015;

put("paper", "screen");
put("d-earth");
put("x-arc");
put("r-carrier");
var parts = ["r-strap-a", "r-strap-b", "r-pack", "r-capsule", "r-jet-l", "r-jet-r", "r-jet-stuck"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

// Where on the arc the capsule is: s px from its top, at a steady 172 px a second, the top at the end.
var s = function (t) { return -1100 + 1100 * t / 6.4; };
// The world under it slides so that point stays at the capsule (the Earth and the arc together).
function slide(t) { var x = s(t); return [-x, -K * x * x]; }
move("d-earth", slide);
move("x-arc", slide);
// Climbing, nose along the arc (y down: the slope 2Ks is negative on the way up).
function path(t) { return Math.atan(2 * K * s(t)) / RAD; }

// The turn, as the reentry preparations' (0 at the start, 180 base first).
var spin;
var before = track([[START, 0], [STOP, 165, "in"]]);
if (failure) {
    spin = function (t) { if (t <= STOP) return before(t); var u = t - STOP; return 165 + 150 * u + 70 * u * u; };
} else if (glitch) {
    var past = smooth([[STOP, 165], [3.5, 215], [4.1, 168], [4.5, 186], [4.8, 180], [6.4, 180]]);
    spin = function (t) { return t <= STOP ? before(t) : past(t); };
} else {
    spin = track([[START, 0], [STOP, 165, "in"], [3.4, 180, "out"]]);
}
// Its attitude follows the path while it points along it, less so as it turns (the path is level at the end).
turn("r-carrier", function (t) { return spin(t) + path(t) * (1 - clamp(spin(t) / 180, 0, 1)); });

function puffAt(name, times) {
    grow(name, function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.6) return 0.5 + (t - times[k]) / 0.6;
        return 0.5;
    });
    fade(name, function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.6) return 1 - (t - times[k]) / 0.6;
        return 0;
    });
}
puffAt("r-jet-l", [START]);
puffAt("r-jet-r", failure ? [] : glitch ? [3.4, 4.1, 4.6] : [STOP]);
if (failure) {
    fade("r-jet-stuck", function (t) { return t >= STOP ? 1 : 0; });
    grow("r-jet-stuck", function (t) { return 1 + 0.08 * jitter(t, 5); });
} else {
    hide("r-jet-stuck");
}
