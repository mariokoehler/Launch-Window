// Entry corridor trim (2026-10-18; the lunar Gemini programs' Earth-orbit tests, user): on from where the coast
// out ends, just past the long ellipse's high point, time slowed there (0.3) as the craft barely moves. The craft
// turns round, tail first along its way (0.6 to 1.8), burns (2.0 to 2.6, its flame ahead of it), which lowers the
// far low point from the orbit's 390 into the entry corridor (the band, gold, at the low point), and turns back
// nose first (2.8 to 4.0); the new path is drawn dashed as it's set, the old one fading (high-orbit-scene.jsx).
// Glitch: the burn cuts out at 2.2, the path moved only halfway; tail first still, a second burn at 3.0 finishes
// it, and it turns back from 3.6.
// Failure: the same short burn, and the second only sputters: the path still passes above the band; it turns
// back all the same.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var RATE = 0.3, TURN = 0.6, BURN = 2.0, CUT = 2.2, AGAIN = 3.0;
var BURNT = glitch ? AGAIN + 0.4 : failure ? AGAIN + 0.15 : 2.6;
var BACK = glitch || failure ? 3.6 : 2.8;

hoScene(false, ["o-ellipse-down", "l-corridor"]);
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
var BIG = kepler(HO_RP, HO_RA), DOWN = kepler(HO_ENTRY, HO_RA), PERI = hoPeri();
turn("o-ellipse", function () { return PERI - 90; });
turn("o-ellipse-down", function () { return PERI - 90; });
hoCorridor(PERI);
// Seconds past the high point: on from the coast's end (3.4 + 6.0 after the low point), slowed.
var S0 = 6.0 - HO_END + 6.0 - BIG.period / 2;
var past = function (t) { return S0 + RATE * t; };
// When the engine burns, and how far the burns have moved it onto the lowered path.
function burning(t) {
    if (t < BURN) return false;
    if (!(glitch || failure)) return t < BURNT;
    return t < CUT || (t >= AGAIN && t < BURNT);
}
var ease = function (t0, t1, t) { return EASE.inout(clamp((t - t0) / (t1 - t0), 0, 1)); };
// The glitch's and the failure's first burn goes as the success's until it cuts out, so nothing shows early.
var full = function (t) { return ease(BURN, 2.6, t); }, part = full(CUT);
var onto = glitch ? function (t) { return full(Math.min(t, CUT)) + (1 - part) * ease(AGAIN, BURNT, t); }
         : failure ? function (t) { return full(Math.min(t, CUT)); }
         : full;
var where = function (t) {
    var a = pastHigh(BIG, PERI, past(t)), b = pastHigh(DOWN, PERI, past(t)), w = onto(t);
    return [mix(a[0], b[0], w), mix(a[1], b[1], w)];
};
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
// Its way (nose first), and the turns: round to tail first, then back.
var way = function (t) {
    var a = where(Math.max(0, t - 0.02)), b = where(t + 0.02);
    return Math.atan2(b[1] - a[1], b[0] - a[0]) / RAD + 90;
};
turn("o-sat", function (t) { return way(t) + 180 * ease(TURN, TURN + 1.2, t) - 180 * ease(BACK, BACK + 1.2, t); });
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.7 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
// The lowered path shows once it's set (the success's with its burn, the glitch's with its second); the
// failure's never is.
var shown = glitch ? function (t) { return ease(AGAIN, BURNT + 0.2, t); } : failure ? function () { return 0; }
          : function (t) { return ease(BURN, BURNT + 0.2, t); };
fade("o-ellipse-down", shown);
fade("o-ellipse", function (t) { return 1 - shown(t); });
