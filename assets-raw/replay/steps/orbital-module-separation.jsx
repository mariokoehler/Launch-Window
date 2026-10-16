// Orbital module separation (2026-10-16): the modules separation of a Soyuz-type craft, which drops its
// retro pack first (retrofire-jettison-om) and then the orbital module on its nose. On from that clip's
// last frame: the capsule base first, sinking towards the air. The latches fire at 0.8 and the module
// drifts off behind, tumbling.
// Glitch: latch a lets go, latch b holds: the module swings out on it, until a second try at 2.8 frees it.
// Failure: the second try frees nothing: the module hangs on, and as the capsule reaches the air it
// rocks (from 3.2) and a glow builds at its base.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CUT = 0.8, CUT2 = 2.8;

put("paper", "screen");
// The motes stream past at the speed the retrofire left (it took 260 px/s off).
motes(function () { return MOTE_SPEED - 260; });
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-glow", "r-capsule", "r-om", "r-om-cut-a", "r-om-cut-b"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

// Sinking on from where the jettison left it (231 px), and the Earth turned on from it (-4.476).
var sink = function (t) { return 231 + 18.5 * t + 0.4 * t * t; };
move("d-earth", function (t) { return [0, -sink(t)]; });
move("r-orbit", function (t) { return [0, -sink(t)]; });
motesBelow(sink);
turn("d-earth", function (t) { return -4.476 - 0.25 * t; });

var rock = failure ? function (t) { return t < 3.2 ? 0 : 14 * Math.min(1, (t - 3.2) / 1.5) * Math.sin(2 * Math.PI * 0.9 * (t - 3.2)); }
                   : function () { return 0; };
turn("r-carrier", function (t) { return 180 + rock(t); });

// The flashes: both latches fire at 0.8, and latch b again at 2.8 in the glitch and the failure.
function flash(times) {
    return function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.25) return 1;
        return 0;
    };
}
fade("r-om-cut-a", flash([CUT]));
fade("r-om-cut-b", flash(glitch || failure ? [CUT, CUT2] : [CUT]));

// The module's way off, in the carrier's frame (the nose is its +x): out past the nose, behind the
// capsule and a little up, tumbling. In the glitch and the failure it swings out on latch b first.
var FREE = glitch ? CUT2 : failure ? 999 : CUT;
var away = function (t) { var u = Math.max(0, t - FREE); return [40 * u + 26 * u * u, -12 * u]; };
var swing = glitch || failure ? function (t) { return t < CUT ? 0 : 40 * Math.min(1, (t - CUT) / 0.6) + 8 * Math.sin(2 * Math.PI * 1.2 * (t - CUT)) * Math.max(0, 1 - (t - CUT) / 2.5); }
                              : function () { return 0; };
// A hanging module swings a little more as the capsule rocks.
move("r-om", away);
turn("r-om", function (t) { return swing(t) + 40 * Math.max(0, t - FREE) + (failure ? 0.6 * rock(t) : 0); });

// The failure meets the air with it on: a glow at the base from 4.2.
fade("r-glow", failure ? track([[4.2, 0], [5.6, 0.7, "lin"]]) : function () { return 0; });
