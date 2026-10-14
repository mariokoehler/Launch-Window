// Lunar ascent: in the orbit scene round the Moon, the craft lifts off its surface at 0.4, nose up,
// pitching over onto its way as it climbs, and the burn puts it on the orbit (dashed) by 2.4, where the
// craft that waits for it goes round.
// Glitch: the engine cuts out at 1.4, short of the orbit, and relights at 2.2; it's there by 3.0.
// Failure: the same cut, and the relight dies at 2.4: it falls back and hits the Moon.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 0.4, CUT = 1.4, RELIGHT = 2.2, DIES = 2.4, SURFACE = 256;

orbitScene("p-moon");
put("l-earth-far", "screen");
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
put("burst");
hide("burst");
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");

function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < 2.4;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    return glitch ? t < 3.0 : t < DIES;
}
// Burn seconds so far lift it from the surface to the orbit (390) at 1.8 s of burning, and speed it up
// to the orbit's pace.
var spent = [], b = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { spent.push(b); if (burning(f / FPS)) b += 1 / FPS; }
function burnt(t) { return spent[clamp(Math.round(t * FPS), 0, spent.length - 1)]; }
function progress(t) { return clamp(burnt(t) / 1.8, 0, 1); }
var radius = function (t) {
    var r = SURFACE + (390 - SURFACE) * EASE.out(progress(t));
    if (failure && t > DIES) r -= 60 * Math.pow(t - DIES, 2);
    return Math.max(r, SURFACE);
};
var HIT = failure ? (function () { for (var f = 0; f <= NFRAMES; f++) if (f / FPS > DIES && radius(f / FPS) <= SURFACE) return f / FPS; return 99; })() : 99;
var phi = orbitAngles(-100, function (t) { return t > HIT ? 0 : 55 * Math.pow(390 / radius(t), 1.5) * Math.sqrt(progress(t)); });
orbitFly(phi, radius);
// Nose out from the Moon at liftoff, along its way once it's up.
turn("o-sat", function (t) { return phi(t) + 90 * (1 - EASE.inout(progress(t))); });
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
turn("p-moon", function (t) { return -0.6 * t; });
// The craft waiting on the orbit, ahead.
put("o-sat-b");
var waits = orbitAngles(-175, function () { return 55; });
move("o-sat-b", function (t) { var p = orbitWhere(waits, null, t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat-b", function (t) { return waits(t); });
if (failure) {
    fade("o-sat", function (t) { return t < HIT ? 1 : 0; });
    var end = orbitWhere(phi, radius, Math.min(HIT, DUR));
    move("burst", function () { return [end[0] - 960, end[1] - 600]; });
    grow("burst", track([[HIT, 0.2], [HIT + 0.35, 0.9, "out"], [HIT + 1.0, 1.1, "lin"]]));
    fade("burst", track([[HIT, 0, "snap"], [HIT + 0.02, 1, "snap"], [HIT + 0.4, 1], [HIT + 1.0, 0, "lin"]]));
}
