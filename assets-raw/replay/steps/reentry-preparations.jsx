// Reentry preparations: in orbit over the Earth's limb, the capsule flies nose first with its
// retro pack behind. The attitude jets at its nose turn it round, base first, for the retrofire:
// a puff at 0.8 starts the turn, one from the other side at 2.6 stops it at 180.
// Glitch: the stopping puff comes late; it swings past to 215, rocks, and the jets settle it at 180
// by 4.4.
// Failure: a jet sticks open at 2.6: the capsule spins on, faster and faster, and can't be held.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var START = 0.8, STOP = 2.6;

put("paper", "screen");
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-strap-a", "r-strap-b", "r-pack", "r-capsule", "r-jet-l", "r-jet-r", "r-jet-stuck"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

turn("d-earth", function (t) { return -0.25 * t; });

// The turn: steady from START, stopped at STOP (success); late and past, then settled (glitch);
// never stopped, and speeding up (failure).
var spin;
if (failure) {
    spin = function (t) {
        if (t <= STOP) return track([[START, 0], [STOP, 165, "in"]])(t);
        var u = t - STOP;
        return 165 + 150 * u + 70 * u * u;
    };
} else if (glitch) {
    // The same turn as the success until the stop that doesn't come.
    var past = smooth([[STOP, 165], [3.1, 215], [3.7, 168], [4.1, 186], [4.4, 180], [6.0, 180]]);
    var before = track([[START, 0], [STOP, 165, "in"]]);
    spin = function (t) { return t <= STOP ? before(t) : past(t); };
} else {
    spin = track([[START, 0], [STOP, 165, "in"], [3.0, 180, "out"]]);
}
turn("r-carrier", spin);

// The jets: a puff at the start of the turn (one side), and at its stop (the other).
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
puffAt("r-jet-r", failure ? [] : glitch ? [3.0, 3.7, 4.2] : [STOP]);
// The stuck jet: a plume from 2.6 on, flickering.
if (failure) {
    fade("r-jet-stuck", function (t) { return t >= STOP ? 1 : 0; });
    grow("r-jet-stuck", function (t) { return 1 + 0.08 * jitter(t, 5); });
} else {
    hide("r-jet-stuck");
}
