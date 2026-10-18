// On the high orbit (2026-10-18; circumlunar Gemini's heat-shield test, the Earth orbit after the high-orbit
// injection, user): on from where the injection's success ends, the capsule coasts out along the long ellipse,
// slowing to its high point and starting back, sending a radio ring every 0.75 s.
// Glitch: the rings stop at 2.0 (it's far out, the signal weak) and come back at 3.4.
// Failure: the rings stop at 2.0 for good: the ground has lost it.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var QUIET = 2.0, BACK = 3.4;

hoScene();
// Where the injection left it: its burn's end angle and the time it has coasted since (6.0 - 2.6).
var angle = orbitAngles(OSTART, function (t) {
    var b = Math.max(0, Math.min(t, HO_END) - 1.0);
    return 55 + 7.5 * Math.min(b, 1.6);
});
var BIG = kepler(HO_RP, HO_RA), PERI = angle(HO_END), SINCE = 6.0 - HO_END;
turn("o-ellipse", function () { return PERI - 90; });
var where = function (t) { return onEllipse(BIG, PERI, SINCE + t); };
flyAlong(where);
var sends = sendTimes(function (t) { return (glitch && t >= QUIET && t < BACK) || (failure && t >= QUIET); });
radioRingsAt(sends, where);
