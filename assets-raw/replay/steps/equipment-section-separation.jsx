// Equipment section separation (2026-10-16): Gemini's way home starts by dropping the back of its
// adapter, the equipment section, which hides the retro section (the pack) behind the capsule. In
// orbit over the Earth's limb, nose first, the motes streaming past: the latches fire at 0.8 and the
// section drifts off behind, tumbling; then the attitude jets turn the capsule base first (2.2 to
// 3.8) for the retrofire that follows.
// Glitch: latch a lets go, latch b holds: the section swings out on it until a second try at 2.8
// frees it, and the turn comes later (3.4 to 5.0).
// Failure: the second try frees nothing: the section hangs on, the retros behind it, and the capsule
// stays nose first.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CUT = 0.8, CUT2 = 2.8;
var TURN0 = glitch ? 3.4 : 2.2, TURN1 = TURN0 + 1.6;

put("paper", "screen");
motes(function () { return MOTE_SPEED; });
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-strap-a", "r-strap-b", "r-pack", "r-capsule", "r-jet-l", "r-jet-r"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");
// In the world, not on the carrier: once free it mustn't turn with the capsule (which stays at 0 while
// it hangs on).
put("r-trunk");
put("r-trunk-cut-a");
put("r-trunk-cut-b");

turn("d-earth", function (t) { return -0.25 * t; });

// The turn: from TURN0, stopped at 180 (none in the failure).
var spin = failure ? function () { return 0; } : track([[TURN0, 0], [TURN1 - 0.4, 165, "in"], [TURN1, 180, "out"]]);
turn("r-carrier", spin);
// The jets: a puff at the start of the turn (one side), and at its stop (the other).
function puffAt(name, at) {
    grow(name, function (t) { return t >= at && t < at + 0.6 ? 0.5 + (t - at) / 0.6 : 0.5; });
    fade(name, function (t) { return !failure && t >= at && t < at + 0.6 ? 1 - (t - at) / 0.6 : 0; });
}
puffAt("r-jet-l", TURN0);
puffAt("r-jet-r", TURN1 - 0.4);

// The flashes: both latches fire at 0.8, and latch b again at 2.8 in the glitch and the failure.
function flash(times) {
    return function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.25) return 1;
        return 0;
    };
}
fade("r-trunk-cut-a", flash([CUT]));
fade("r-trunk-cut-b", flash(glitch || failure ? [CUT, CUT2] : [CUT]));

// The section's way off, in the world (the capsule at 0, nose right: behind it is -x): back and a
// little down, tumbling. In the glitch and the failure it swings out on latch b first.
var FREE = glitch ? CUT2 : failure ? 999 : CUT;
var away = function (t) { var u = Math.max(0, t - FREE); return [-(46 * u + 30 * u * u), 10 * u]; };
var swing = glitch || failure ? function (t) { return t < CUT ? 0 : -35 * Math.min(1, (t - CUT) / 0.6) + 8 * Math.sin(2 * Math.PI * 1.2 * (t - CUT)) * Math.max(0, 1 - (t - CUT) / 2.5); }
                              : function () { return 0; };
move("r-trunk", away);
turn("r-trunk", function (t) { return swing(t) - 35 * Math.max(0, t - FREE); });
