// Reentry preparations with the craft already base first (2026-10-17): after a suborbital flight's turn
// (Gemini, Voskhod, Vostok) or after the retrofire (BURN: Mercury's hop, a Soyuz's). It doesn't turn
// again: it holds its attitude while the crew gets ready, the attitude jets trimming it, a puff at 1.0
// nudging it a few degrees and one from the other side at 2.4 bringing it back. Mercury's periscope
// (r-periscope, drawn only in Mercury's parts) slides in from 1.4, as it did about half a minute after
// the retrofire, before the pack went.
// Glitch: the puff at 2.4 overshoots: it swings past, rocks, and the jets settle it by 4.4.
// Failure: a jet sticks open at 2.4: it spins on, faster and faster, and can't be held.
// BURN (reentry-trim-burn.jsx): on from the retrofire's last frame: the motes as slow as it left them,
// the Earth where it left it, held there for the retrofire jettison or the modules' separation to pick
// up. Otherwise on from the orbit scene as the reentry preparations' turn starts it, ending where the
// retrofire begins.
var BURN = typeof BURN !== "undefined" && BURN;
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var NUDGE = 1.0, STOP = 2.4, PERI = 1.4;

put("paper", "screen");
// The motes: as slow as the retrofire left them (BURN), or a shade under the orbit's speed, so that over
// the clip they go round exactly twice and end where the retrofire's begin, as they began where the
// suborbital flight's ended.
motes(function () { return BURN ? MOTE_SPEED - 260 : 2 * 2220 / DUR; });
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-strap-a", "r-strap-b", "r-pack", "r-periscope", "r-capsule", "r-jet-l", "r-jet-r", "r-jet-stuck"];
// OM (project-soyuz.jsx; 2026-10-18): a Soyuz-type craft, its orbital module on the nose.
if (typeof OM !== "undefined" && OM) parts.splice(5, 0, "r-om");
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

if (BURN) {
    // Where the retrofire left it: sunk 140 px below its orbit, the Earth turned to -2.776.
    move("d-earth", function () { return [0, -140]; });
    move("r-orbit", function () { return [0, -140]; });
    motesBelow(function () { return 140; });
    turn("d-earth", function () { return -2.776; });
} else {
    // As the reentry preparations' turn: the Earth turns on below, to -1.5 at the end.
    turn("d-earth", function (t) { return -0.25 * t; });
}

// The trim: a few degrees off and back (success); back too hard, past, rocked and settled (glitch);
// pushed on by the stuck jet, faster and faster (failure). The same in all three until STOP.
var off = track([[NUDGE, 0], [STOP, 5, "out"]]);
var trim;
if (failure) {
    trim = function (t) { if (t <= STOP) return off(t); var u = t - STOP; return 5 - 60 * u - 70 * u * u; };
} else if (glitch) {
    var past = smooth([[STOP, 5], [2.9, -16], [3.5, 9], [4.0, -4], [4.4, 0], [6.0, 0]]);
    trim = function (t) { return t <= STOP ? off(t) : past(t); };
} else {
    trim = track([[NUDGE, 0], [STOP, 5, "out"], [3.2, 0, "inout"]]);
}
turn("r-carrier", function (t) { return 180 + trim(t); });

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
puffAt("r-jet-l", [NUDGE]);
puffAt("r-jet-r", failure ? [] : glitch ? [STOP, 3.5, 4.0] : [STOP]);
if (failure) {
    fade("r-jet-stuck", function (t) { return t >= STOP ? 1 : 0; });
    grow("r-jet-stuck", function (t) { return 1 + 0.08 * jitter(t, 5); });
} else {
    hide("r-jet-stuck");
}

// The periscope slides in along its own length (drawn out; its pivot at its root, its length along
// its own +y in the carrier's frame), over 0.8 s, in every variant.
grow("r-periscope", function (t) { return [1, 1 - 0.95 * clamp((t - PERI) / 0.8, 0, 1)]; });
fade("r-periscope", function (t) { return t < PERI + 0.8 ? 1 : 0; });
