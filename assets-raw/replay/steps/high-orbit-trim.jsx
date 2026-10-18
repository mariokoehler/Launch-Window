// Entry corridor trim (2026-10-18; the lunar Gemini programs' Earth-orbit tests, user): on from where the coast
// out ends, just past the long ellipse's high point, time slowed there (0.3) as the craft barely moves; a short
// burn at 1.2 (a puff) lowers the far low point from the orbit's 390 into the entry corridor (the band, gold,
// at the low point), and the craft starts down the new path, drawn dashed as it's set, the old one fading
// (high-orbit-scene.jsx).
// Glitch: the first burn is short and moves the path only halfway; a second at 2.6 finishes it.
// Failure: the same short first burn, and the second misfires: the path still passes above the band.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var TRIM = 1.2, AGAIN = 2.6, RATE = 0.3;

hoScene(false, ["o-ellipse-down", "l-corridor", "m-puff"]);
var BIG = kepler(HO_RP, HO_RA), DOWN = kepler(HO_ENTRY, HO_RA), PERI = hoPeri();
turn("o-ellipse", function () { return PERI - 90; });
turn("o-ellipse-down", function () { return PERI - 90; });
hoCorridor(PERI);
// Seconds past the high point: on from the coast's end (3.4 + 6.0 after the low point), slowed.
var S0 = 6.0 - HO_END + 6.0 - BIG.period / 2;
var past = function (t) { return S0 + RATE * t; };
// How far the burns have moved it onto the lowered path: the success at once, the others halfway, then the
// glitch's second burn the rest.
var ease = function (t0, t) { return EASE.inout(clamp((t - t0) / 0.6, 0, 1)); };
var onto = glitch ? function (t) { return 0.5 * ease(TRIM, t) + 0.5 * ease(AGAIN, t); }
         : failure ? function (t) { return 0.5 * ease(TRIM, t); }
         : function (t) { return ease(TRIM, t); };
var where = function (t) {
    var a = pastHigh(BIG, PERI, past(t)), b = pastHigh(DOWN, PERI, past(t)), w = onto(t);
    return [mix(a[0], b[0], w), mix(a[1], b[1], w)];
};
flyAlong(where);
// The lowered path shows once it's set (the success's at once, the glitch's at its second burn); the failure's
// never is.
var shown = glitch ? function (t) { return ease(AGAIN, t); } : failure ? function () { return 0; } : function (t) { return ease(TRIM, t); };
fade("o-ellipse-down", shown);
fade("o-ellipse", function (t) { return 1 - shown(t); });
// The puffs at the craft: one at TRIM (smaller where it's short), the glitch's and the failure's second at AGAIN
// (the failure's a sputter).
var puffs = [[TRIM, glitch || failure ? 1.4 : 2.2]].concat(glitch ? [[AGAIN, 1.8]] : failure ? [[AGAIN, 0.7]] : []);
var puffAt = function (t) { var p = null; for (var i = 0; i < puffs.length; i++) if (t >= puffs[i][0]) p = puffs[i]; return p; };
move("m-puff", function (t) { var p = puffAt(t), q = where(p ? p[0] : 0); return [q[0] - 960, q[1] - 540]; });
grow("m-puff", function (t) { var p = puffAt(t); return p ? p[1] * (0.4 + 0.6 * clamp((t - p[0]) / 0.5, 0, 1)) : 0.4; });
fade("m-puff", function (t) { var p = puffAt(t); return p ? 1 - clamp((t - p[0] - 0.3) / 0.6, 0, 1) : 0; });
