// A cruise between two planets (FROM, TO and ARC, set by the step's own file first, with the
// variants' story), on the map of the planets: the Sun in the middle, the orbits dashed, the probe on
// its transfer orbit (gold, dashed), a half ellipse from one orbit to the other. It travels the
// middle of the way, faster nearer the Sun, sending home a ring every 0.8 s; the planets go round.
// Success: a course correction at 2.0 (a puff), on the line all the way.
// Glitch: from 1.6 it drifts off the line; a correction at 3.0 brings it back by 3.8.
// Failure: the same drift, but the correction at 3.0 misfires: it veers away and goes quiet.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SX = 960, SY = 540, START = 180;
var R = {mercury: 95, venus: 150, earth: 215, mars: 290, jupiter: 440, saturn: 525};
var DRIFT = 1.6, FIX = glitch || failure ? 3.0 : 2.0;

put("paper", "screen");
put("m-orbits");
put("m-arc-" + ARC);
put("m-sun");
// Every cruise shows the same planets, Mercury too (2026-10-16, user): Mariner 10's two cruises play one
// after the other, and a map without Mercury reads Venus as the innermost planet.
put("m-orbit-mercury");
var names = ["mercury", "venus", "earth", "mars", "jupiter", "saturn"];
for (var i = 0; i < names.length; i++) put("m-" + names[i]);
for (i = 1; i <= 3; i++) put("m-ring-" + i);
put("m-puff");
put("m-probe");

// The transfer orbit (parts.py draws the same): its distance from the Sun nu degrees from its start.
var r1 = R[FROM], r2 = R[TO];
function transfer(nu) {
    var a = Math.min(r1, r2), b = Math.max(r1, r2), e = (b - a) / (b + a), p = 2 * a * b / (a + b);
    var c = Math.cos(nu * RAD);
    return r1 < r2 ? p / (1 + e * c) : p / (1 - e * c);
}
// How far along it the probe is: from 25 to 155 degrees over the clip, sweeping equal areas
// (faster nearer the Sun).
var NU0 = 25, NU1 = 155;
var nuAt = (function () {
    var steps = [], nu = NU0, total = 0;
    for (var k = 0; k < 2000; k++) { steps.push(total); var r = transfer(nu); total += r * r; nu += (NU1 - NU0) / 2000; }
    steps.push(total);
    return function (t) {
        var target = total * clamp(t / 6.4, 0, 1), lo = 0, hi = steps.length - 1;
        while (hi - lo > 1) { var mid = (lo + hi) >> 1; if (steps[mid] <= target) lo = mid; else hi = mid; }
        return NU0 + (NU1 - NU0) * lo / 2000;
    };
})();
// Off the line: out from the Sun (px), as the drift grows and a correction takes it back.
var off = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var d = 40 * clamp((t - DRIFT) / 1.4, 0, 1);
    if (glitch && t > FIX) d *= clamp(1 - (t - FIX) / 0.8, 0, 1);
    if (failure && t > FIX) d += 260 * Math.pow(t - FIX, 1.4);
    return d;
};
function where(t) {
    var nu = nuAt(t), ang = (START - nu) * RAD, r = transfer(nu) + off(t);
    // The failure veers off ahead as well as out.
    var ahead = failure && t > FIX ? -120 * Math.pow(t - FIX, 1.3) : 0;
    return [SX + r * Math.cos(ang) - ahead * Math.sin(ang), SY + r * Math.sin(ang) + ahead * Math.cos(ang)];
}
move("m-probe", function (t) { var p = where(t); return [p[0] - SX, p[1] - SY]; });
turn("m-probe", function (t) { return failure && t > FIX ? 140 * (t - FIX) : 0; });

// The course correction: a puff at the probe.
move("m-puff", function (t) { var p = where(Math.max(t, FIX)); return [p[0] - SX, p[1] - SY]; });
grow("m-puff", track([[FIX, 0.4], [FIX + 0.5, 1.6, "out"], [FIX + 1.1, 2.0, "lin"]]));
fade("m-puff", track([[FIX, 0, "snap"], [FIX + 0.02, 1, "snap"], [FIX + 0.5, 1], [FIX + 1.1, 0, "lin"]]));

// The planets go round, the inner ones faster (r^-1.5): from where they'd be for this transfer. The
// target is timed to meet the probe where the clip leaves it (2026-10-16, user).
for (i = 0; i < names.length; i++) {
    (function (n) {
        var rate = 9 * Math.pow(215 / R[n], 1.5);
        var at = n === FROM ? START - 20 : n === TO ? START - NU1 + rate * 6.4 : START - 60 - 47 * names.indexOf(n);
        move("m-" + n, function (t) {
            var a = (at - rate * t) * RAD;
            return [R[n] * Math.cos(a) - R[n], R[n] * Math.sin(a)];
        });
    })(names[i]);
}

// Home: a ring every 0.8 s, not while it's lost.
var sends = [];
for (var s = 0.4; s < DUR; s += 0.8) if (!(failure && s >= FIX)) sends.push(s);
for (i = 1; i <= 3; i++) {
    (function (n, k) {
        var mine = function (t) { var last = -1; for (var j = k; j < sends.length; j += 3) if (sends[j] <= t) last = sends[j]; return last; };
        move(n, function (t) { var te = mine(t); var p = where(te < 0 ? 0 : te); return [p[0] - SX, p[1] - SY]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 0.5 : 0.5 + 2.0 * clamp((t - te) / 0.9, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : 1 - clamp((t - te) / 0.9, 0, 1); });
    })("m-ring-" + i, i - 1);
}
