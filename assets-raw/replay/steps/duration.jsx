// Time in orbit for a crew, a day (LEVEL 1) or several (LEVEL 2), set by the step's own file first
// with the variants' story: the craft goes round faster and faster (time passing), night on the
// Earth's far side, and the crew's oxygen, power and food going down on their gauges. A day ends
// where several days begin.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DIP = 2.2, SAVE = 3.4, OUT = 3.0, ABORT = 3.8;

orbitScene();
put("x-night");
put("x-supplies");
for (var i = 1; i <= 3; i++) put("x-bar-" + i, "screen");
// The night shades the Earth, under the craft.
layers["x-night"].moveAfter(layers["o-sat"]);
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
// A day cuts in from the Earth orbit: its night and gauges fade in; several days already have them.
intro(LEVEL === 1);
introFade("x-night");
introFade("x-supplies");

// The speed: from the Earth orbit's 55 degrees a second up to a time-lapse, until a failure's abort
// brings it back to real time.
function speedOf(variant) {
    return function (t) {
        var s = 55 + 165 * clamp(t / 2.0, 0, 1) * clamp(t / 2.0, 0, 1);
        if (variant === "failure" && t > ABORT - 0.4) s = 55 + (s - 55) * clamp((ABORT - t) / 0.4, 0, 1);
        return s;
    };
}
// Where a day leaves off, for several days to begin: the day's success, run to its end.
var start = OSTART;
if (LEVEL >= 2) {
    var dayLength = 6.4, x = OSTART, sp = speedOf("success");
    for (var d = 1; d < LEVEL; d++)
        for (var f = 0; f < Math.round(dayLength * FPS); f++) x -= sp(f / FPS) / FPS;
    start = x;
}
var phi = orbitAngles(start, speedOf(VARIANT));
// A failure's abort: the craft drops out of its orbit, and fades as it reaches the air.
var radius = failure ? track([[ABORT, OR], [ABORT + 1.4, 262, "in"]]) : null;
orbitFly(phi, radius);
if (failure) fade("o-sat", track([[ABORT + 1.1, 1], [ABORT + 1.5, 0, "lin"]]));
turn("o-earth", function (t) { return -0.25 * t; });

// The supplies, oxygen, power and food: from where they were to where this stretch leaves them.
var from = typeof SUPPLY !== "undefined" ? SUPPLY[0] : LEVEL === 1 ? [0.95, 0.92, 0.95] : [0.72, 0.66, 0.75];
var to = typeof SUPPLY !== "undefined" ? SUPPLY[1] : LEVEL === 1 ? [0.72, 0.66, 0.75] : [0.38, 0.30, 0.42];
function supply(n) {
    return function (t) {
        var u = clamp(t / 6.4, 0, 1), l = from[n] + (to[n] - from[n]) * u;
        if (!(glitch || failure)) return l;
        // The power dips at 2.2 in both; the glitch's is saved at 3.4, the failure's oxygen runs out from 3.0.
        if (n === 1 && t >= DIP) l -= 0.22 * clamp((t - DIP) / 0.3, 0, 1) - (glitch && t >= SAVE ? 0.15 * clamp((t - SAVE) / 0.4, 0, 1) : 0);
        if (n === 0 && failure && t >= OUT) l *= 1 - clamp((t - OUT) / 0.8, 0, 1);
        return Math.max(0.02, l);
    };
}
gauge("x-bar-1", supply(0), function (t) { return failure && t >= OUT; });
gauge("x-bar-2", supply(1), function (t) { return (glitch || failure) && t >= DIP && t < (glitch ? SAVE + 0.4 : OUT); });
gauge("x-bar-3", supply(2));
