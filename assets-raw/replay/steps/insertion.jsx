// Orbit insertion: the last burn of the climb. In the Earth-orbit scene, the craft comes up from a low
// path below its planned orbit (dashed), fires at 0.8 and rises onto the orbit by 2.0, then goes round
// on it as the Earth orbit does.
// Glitch: the burn cuts out at 1.4, short of the orbit, and relights at 2.2; it reaches it by 2.8.
// Failure: the same cut, and the relight dies at 2.4: it falls back towards the air and burns up.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 0.8, CUT = 1.4, RELIGHT = 2.2, DIES = 2.4;

orbitScene();
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
put("burst");
hide("burst");
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
turn("o-earth", function (t) { return -0.25 * t; });

function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < 2.0;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    return glitch ? t < 2.8 : t < DIES;
}
// Burn seconds so far raise it from 300 to the orbit (390) at 1.2 s of burning.
var spent = [], b = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { spent.push(b); if (burning(f / FPS)) b += 1 / FPS; }
function burnt(t) { return spent[clamp(Math.round(t * FPS), 0, spent.length - 1)]; }
var radius = function (t) {
    var r = 300 + 90 * clamp(burnt(t) / 1.2, 0, 1);
    if (failure && t > DIES) r -= 50 * Math.pow(t - DIES, 2);
    return Math.max(r, 262);
};
var BURN = failure ? (function () { for (var f = 0; f <= NFRAMES; f++) if (radius(f / FPS) <= 262) return f / FPS; return 99; })() : 99;
// Round at the orbit's pace for its height (faster lower).
var phi = orbitAngles(205, function (t) { return 55 * Math.pow(390 / radius(t), 1.5); });
orbitFly(phi, radius);
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
if (failure) {
    fade("o-sat", function (t) { return t < BURN ? 1 : 0; });
    var end = orbitWhere(phi, radius, Math.min(BURN, DUR));
    move("burst", function () { return [end[0] - 960, end[1] - 600]; });
    grow("burst", track([[BURN, 0.2], [BURN + 0.35, 0.9, "out"], [BURN + 1.0, 1.1, "lin"]]));
    fade("burst", track([[BURN, 0, "snap"], [BURN + 0.02, 1, "snap"], [BURN + 0.4, 1], [BURN + 1.0, 0, "lin"]]));
}
