// Coast to reentry (2026-10-18; the lunar Gemini programs' Earth-orbit tests, user): on from the trim's end, down
// the lowered path, faster and faster, to the entry corridor (the band) at the low point, where it reaches the
// air at the end (high-orbit-scene.jsx).
// Glitch: from 2.0 its aim drifts shallow, the low point rising out of the band; a correction at 3.0 (a puff)
// puts it back.
// Failure: the same drift, and the correction misfires: it's set to pass above the band and skip off the air.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DRIFT = 2.0, FIX = 3.0;

hoScene(false, ["o-ellipse-down", "l-corridor", "m-puff"]);
var BIG = kepler(HO_RP, HO_RA), DOWN = kepler(HO_ENTRY, HO_RA), HIGH = kepler(330, HO_RA), PERI = hoPeri();
hide("o-ellipse");
turn("o-ellipse-down", function () { return PERI - 90; });
hoCorridor(PERI);
// Seconds past the high point: on from the trim's end (7.0 s at 0.3 a second there), then on to just short of
// the low point by the end, quickening.
var S0 = 6.0 - HO_END + 6.0 - BIG.period / 2 + 0.3 * 7.0, S1 = DOWN.period / 2 - 0.25;
var past = function (t) { var u = clamp(t / 6.0, 0, 1); return S0 + (S1 - S0) * (0.55 * u + 0.45 * u * u); };
var off = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var w = EASE.inout(clamp((t - DRIFT) / 0.8, 0, 1));
    if (glitch) w *= 1 - EASE.inout(clamp((t - FIX) / 0.6, 0, 1));
    return w;
};
var where = function (t) {
    var a = pastHigh(DOWN, PERI, past(t)), b = pastHigh(HIGH, PERI, past(t)), w = off(t);
    return [mix(a[0], b[0], w), mix(a[1], b[1], w)];
};
flyAlong(where);
var PUFF = glitch || failure ? FIX : 99, SIZE = failure ? 0.7 : 2.0;
move("m-puff", function (t) { var q = where(Math.max(t, PUFF)); return [q[0] - 960, q[1] - 540]; });
grow("m-puff", function (t) { return SIZE * (0.4 + 0.6 * clamp((t - PUFF) / 0.5, 0, 1)); });
fade("m-puff", function (t) { return t < PUFF ? 0 : 1 - clamp((t - PUFF - 0.3) / 0.6, 0, 1); });
