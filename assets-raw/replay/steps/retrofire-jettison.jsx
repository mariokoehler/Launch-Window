// Retrofire jettison: the burn done, the capsule sinks towards the air base first, and the spent
// retro pack has to go before the heat shield meets the air. The straps let go at 1.0 and the pack
// drifts off ahead, tumbling.
// Glitch: one strap lets go at 1.0, the other holds: the pack swings out on it, until a second try
// at 2.8 frees it.
// Failure: the same until the second try at 2.8, which frees nothing: the pack hangs on, and as
// the capsule reaches the air it starts to rock (from 3.2).
// OM (retrofire-jettison-om.jsx): a Soyuz-type craft, its orbital module still on the nose (it goes in
// orbital-module-separation), after deorbit-burn-om (the Earth turned on from EARTH0).
var OM = typeof OM !== "undefined" && OM;
var EARTH0 = typeof EARTH0 !== "undefined" ? EARTH0 : -2.776;
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CUT = 1.0, CUT2 = 2.8;

put("paper", "screen");
// The motes stream past at the speed the retrofire left (it took 260 px/s off).
motes(function () { return MOTE_SPEED - 260; });
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-strap-a", "r-strap-b", "r-pack", "r-capsule", "r-cut-a", "r-cut-b"];
if (OM) parts.push("r-om");
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

// Sinking on from where the retrofire left it (140 px), until the air is close below.
var sink = function (t) { return 140 + 14 * t + 0.4 * t * t; };
move("d-earth", function (t) { return [0, -sink(t)]; });
move("r-orbit", function (t) { return [0, -sink(t)]; });
motesBelow(sink);
// Turned on from where the retrofire left it.
turn("d-earth", function (t) { return EARTH0 - 0.25 * t; });

var rock = failure ? function (t) { return t < 3.2 ? 0 : 14 * Math.min(1, (t - 3.2) / 1.5) * Math.sin(2 * Math.PI * 0.9 * (t - 3.2)); }
                   : function () { return 0; };
turn("r-carrier", function (t) { return 180 + rock(t); });

// The flashes: both straps fire at 1.0, and strap b again at 2.8 in the glitch and the failure.
function flash(times) {
    return function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.25) return 1;
        return 0;
    };
}
fade("r-cut-a", flash([CUT]));
fade("r-cut-b", flash(glitch || failure ? [CUT, CUT2] : [CUT]));

// The pack's way off, in the carrier's frame (base first is its -x): out past the base, ahead and a
// little down, tumbling. In the glitch and the failure it swings out on strap b first.
var FREE = glitch ? CUT2 : failure ? 999 : CUT;
var away = function (t) { var u = Math.max(0, t - FREE); return [-(50 * u + 34 * u * u), -14 * u]; };
var swing = glitch || failure ? function (t) { return t < CUT ? 0 : -50 * Math.min(1, (t - CUT) / 0.6) + 10 * Math.sin(2 * Math.PI * 1.2 * (t - CUT)) * Math.max(0, 1 - (t - CUT) / 2.5); }
                              : function () { return 0; };
// A hanging pack swings a little more as the capsule rocks.
var tumble = function (t) { return swing(t) - 45 * Math.max(0, t - FREE) + (failure ? 0.6 * rock(t) : 0); };
move("r-pack", away);
turn("r-pack", tumble);
fade("r-strap-a", function (t) { return t < CUT ? 1 : 0; });
if (glitch || failure) {
    // Strap b holds the pack: it swings with it, until the glitch's second try frees it.
    turn("r-strap-b", function (t) { return swing(t) + (failure ? 0.6 * rock(t) : 0); });
    fade("r-strap-b", function (t) { return t < FREE ? 1 : 0; });
} else {
    fade("r-strap-b", function (t) { return t < CUT ? 1 : 0; });
}
