// Modules separation: after the retrofire, the capsule (base first) lets go of its equipment
// module, the thing behind it that held its retro engine (the retro pack, drawn the same). The
// bolts fire at 0.8, the module drifts off ahead, and the cable between them parts at 1.25.
// Glitch: the cable holds (Vostok 1): the module hangs on it and the pair starts to tumble from 1.6,
// until the cable burns through at 3.4; the module flies off and the capsule rights itself by 5.0.
// Failure: the same, but the cable never parts: the tumble grows wild and the pair meets the air
// joined, a glow at the base from 4.4.
// OM (project-soyuz.jsx; 2026-10-18): a Soyuz-type craft, its orbital module on the nose; it goes at the
// bolts too, off past the nose, tumbling, whatever the module behind does.
// BACKUP (project-voskhod.jsx; 2026-10-19): Voskhod's backup retro on the nose goes at the bolts too, the same way.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var BOLTS = 0.8, SNAP = 1.25, TUMBLE = 1.6, BURN = 3.4;
var OM = typeof OM !== "undefined" && OM;
var BACKUP = typeof BACKUP !== "undefined" && BACKUP;

put("paper", "screen");
// The motes stream past at the speed the retrofire left (it took 260 px/s off).
motes(function () { return MOTE_SPEED - 260; });
put("d-earth");
put("r-orbit");
put("r-cable");
put("r-carrier");
var parts = ["r-glow", "r-module", "r-capsule", "r-pyro"];
if (OM) parts.push("r-om");
if (BACKUP) parts.push("r-backup");
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");
put("burst");
hide("burst");
if (OM) {
    // In the carrier's frame the nose is +x: off ahead of it and a little aside, tumbling.
    move("r-om", function (t) { var u = Math.max(0, t - BOLTS); return [80 * u + 60 * u * u, 14 * u]; });
    turn("r-om", function (t) { return 35 * Math.max(0, t - BOLTS); });
}
if (BACKUP) {
    move("r-backup", function (t) { var u = Math.max(0, t - BOLTS); return [70 * u + 50 * u * u, -12 * u]; });
    turn("r-backup", function (t) { return -40 * Math.max(0, t - BOLTS); });
}

// Sinking on as after the jettison, so the reentry starts where either leaves off. SEP_FROM (2026-10-19, the LK-700's,
// whose jettison comes first): where it starts sinking from, how fast, and the Earth's turn.
var SEP_FROM = typeof SEP_FROM !== "undefined" ? SEP_FROM : [140, 14, -2.776];
var sink = function (t) { return SEP_FROM[0] + SEP_FROM[1] * t + 0.4 * t * t; };
move("d-earth", function (t) { return [0, -sink(t)]; });
move("r-orbit", function (t) { return [0, -sink(t)]; });
motesBelow(sink);
turn("d-earth", function (t) { return SEP_FROM[2] - 0.25 * t; });

// The tumble of the joined pair: the same in glitch and failure until the cable burns at 3.4; the
// glitch's dies away by 5.0, the failure's grows wild.
var F = 0.55;
function amp(t) {
    if (!(glitch || failure) || t < TUMBLE) return 0;
    var a = 35 * Math.min(1, (t - TUMBLE) / 1.0);
    if (glitch && t > BURN) a *= clamp((5.0 - t) / 1.6, 0, 1);
    if (failure && t > BURN) a += 110 * (t - BURN);
    return a;
}
function tumble(t) { return amp(t) * Math.sin(2 * Math.PI * F * (t - TUMBLE) * (failure && t > BURN ? 1 + 0.4 * (t - BURN) : 1)); }
turn("r-carrier", function (t) { return 180 + tumble(t); });

// The module, in the carrier's frame (nose right at 0, so -x is behind the base): out to the
// cable's length, then held (glitch, failure); free from the snap (success) or the burn (glitch).
var held = function (t) { return -56 * clamp((t - BOLTS) / 0.45, 0, 1) * (2 - clamp((t - BOLTS) / 0.45, 0, 1)); };
var FREE = glitch ? BURN : failure ? 999 : SNAP;
function off(t) {
    var u = Math.max(0, t - FREE);
    var x = t < FREE ? held(t) : held(FREE) - (60 * u + 40 * u * u);
    return [x, -12 * u];
}
// Held, it swings behind the capsule's tumble; free, it turns away.
function swing(t) { return t < FREE ? -0.7 * tumble(t - 0.15) : -0.7 * tumble(FREE - 0.15) - 50 * (t - FREE); }
move("r-module", off);
turn("r-module", swing);

// The bolts: a flash at 0.8.
fade("r-pyro", function (t) { return t >= BOLTS && t < BOLTS + 0.25 ? 1 : 0; });

// The cable, from the capsule's base to the module's near end, in the world (aimed and stretched
// each frame). The capsule's base and the module's end, at the carrier's 0, from parts.py.
// A craft's own module (project-vostok.jsx) has its own pivot and near end.
var RC = [960, 470], BASE = [812, 470];
var MOD = typeof MODULE_AT !== "undefined" ? MODULE_AT : [788, 470], END = typeof MODULE_END !== "undefined" ? MODULE_END : 24;
function rot(p, c, deg) {
    var a = deg * RAD, dx = p[0] - c[0], dy = p[1] - c[1];
    return [c[0] + dx * Math.cos(a) - dy * Math.sin(a), c[1] + dx * Math.sin(a) + dy * Math.cos(a)];
}
function ends(t) {
    var th = 180 + tumble(t), o = off(t), phi = swing(t);
    var m = [MOD[0] + o[0], MOD[1] + o[1]];
    var b = rot([m[0] + END, m[1]], m, phi);
    return [rot(BASE, RC, th), rot(b, RC, th)];
}
var CUT = glitch ? BURN : failure ? 999 : SNAP;
move("r-cable", function (t) { var e = ends(Math.min(t, CUT)); return [e[0][0] - 960, e[0][1] - 470]; });
turn("r-cable", function (t) { var e = ends(Math.min(t, CUT)); return Math.atan2(e[1][1] - e[0][1], e[1][0] - e[0][0]) / RAD; });
grow("r-cable", function (t) {
    var e = ends(Math.min(t, CUT));
    return [Math.max(0.05, Math.sqrt(Math.pow(e[1][0] - e[0][0], 2) + Math.pow(e[1][1] - e[0][1], 2)) / 100), 1];
});
fade("r-cable", function (t) { return t >= BOLTS - 0.01 && t < CUT ? 1 : 0; });

// Where the cable parts: a spark (the glitch's burns through, so a bigger one).
if (!failure) {
    var spark = ends(CUT);
    var mid = [(spark[0][0] + spark[1][0]) / 2, (spark[0][1] + spark[1][1]) / 2];
    var k = glitch ? 0.8 : 0.4;
    move("burst", function () { return [mid[0] - 960, mid[1] - 600]; });
    grow("burst", track([[CUT, 0.2 * k], [CUT + 0.3, k, "out"], [CUT + 0.7, 1.1 * k, "lin"]]));
    fade("burst", track([[CUT, 0, "snap"], [CUT + 0.02, 1, "snap"], [CUT + 0.3, 1], [CUT + 0.7, 0, "lin"]]));
}

// The failure meets the air joined: a glow at the base from 4.4.
fade("r-glow", failure ? track([[4.4, 0], [5.6, 0.8, "lin"]]) : function () { return 0; });
