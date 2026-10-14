// In orbit round another planet (PLANET, set by the step's own file first: "venus" or "mars"), in the
// Earth orbit's scene with that planet instead: the probe goes round mapping it, a gold beam on the
// surface below it, sending home a ring every 0.75 s.
// Glitch: at 2.0 it drops into safe mode, beam off, quiet, tumbling a little; it's back at 3.2.
// Failure: the same, but it never comes back: from 3.0 it goes dark, lost.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SAFE = 2.0, BACK = 3.2, LOST = 3.0;

orbitScene("p-" + PLANET);
put("p-beam");
layers["p-beam"].moveAfter(layers["o-sat"]);

var phi = orbitAngles(OSTART, function () { return 55; });
var wobble = function (t) {
    if (!(glitch || failure) || t < SAFE) return 0;
    var a = 22 * Math.sin(2 * Math.PI * 1.1 * (t - SAFE));
    return glitch && t > BACK ? a * clamp(1 - (t - BACK) / 0.5, 0, 1) : a;
};
move("o-sat", function (t) { var p = orbitWhere(phi, null, t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function (t) { return phi(t) + wobble(t); });
// The planet turns slowly under the probe.
turn("p-" + PLANET, function (t) { return -0.6 * t; });

var working = function (t) { return !(glitch || failure) || t < SAFE || (glitch && t >= BACK); };
move("p-beam", function (t) { var p = orbitWhere(phi, null, t); return [p[0] - OCX, p[1] - OCY]; });
turn("p-beam", function (t) { return phi(t); });
fade("p-beam", function (t) { return working(t) ? 0.85 + 0.15 * Math.sin(2 * Math.PI * 3 * t) : 0; });

radioRings(sendTimes(function (t) { return !working(t); }), phi);
fade("o-sat", failure ? track([[LOST + 0.4, 1], [LOST + 1.0, 0.35, "lin"]]) : function () { return 1; });
