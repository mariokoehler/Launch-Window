// Closing in: in the Earth-orbit scene, the other craft is on the planned orbit (dashed) and the chaser
// on a lower one, behind it and catching up (lower is faster). At 1.6 it fires and rises onto the
// orbit by 2.6, just behind the other; the two go round together.
// Glitch: the burn cuts out at 2.0, halfway up, and relights at 2.6; it's there by 3.2, a little
// further on.
// Failure: the same cut, and no relight: left on the lower orbit, it runs on under the other and
// ahead of it, out of reach.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.6, CUT = 2.0, RELIGHT = 2.6;

orbitScene();
put("o-sat-b");
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
turn("o-earth", function (t) { return -0.25 * t; });

// The other craft goes round on the orbit; the chaser is rel(t) degrees behind it at radius(t).
var other = orbitAngles(OSTART + 10, function () { return 55; });
// The success's climb and catch-up; the glitch and the failure follow it exactly until the cut.
var upR = track([[FIRE, 330], [2.6, 390, "inout"]]);
var upRel = track([[0, 40], [FIRE, 18, "lin"], [2.6, 14, "out"]]);
var cutR = upR(CUT), cutRel = upRel(CUT);
var radius, rel;
if (failure) {
    // Left low, it runs on under the other, and once the relight hasn't come, ahead of it and away.
    radius = function (t) { return t <= CUT ? upR(t) : cutR; };
    rel = function (t) { var u = Math.max(0, t - RELIGHT); return t <= CUT ? upRel(t) : cutRel - 6 * (t - CUT) - 3 * u * u; };
} else if (glitch) {
    var on = track([[RELIGHT, cutR], [3.2, 390, "out"]]);
    var relOn = track([[CUT, cutRel], [RELIGHT, cutRel - 3.6, "lin"], [3.2, cutRel - 4.5, "out"]]);
    radius = function (t) { return t <= CUT ? upR(t) : on(t); };
    rel = function (t) { return t <= CUT ? upRel(t) : relOn(t); };
} else {
    radius = upR;
    rel = upRel;
}
var phi = function (t) { return other(t) + rel(t); };
orbitFly(phi, radius);
move("o-sat-b", function (t) { var p = orbitWhere(other, null, t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat-b", function (t) { return other(t); });
function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < 2.6;
    if (t < CUT) return true;
    return glitch && t >= RELIGHT && t < 3.2;
}
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
