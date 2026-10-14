// The orbit scene shared by the steps that stay in orbit (experiments, biometric data, a day or days
// in orbit): earth-orbit.jsx's Earth, orbit and payload, going round at a speed the step sets, and its
// radio rings. They start where the Earth orbit's success ends (-125), so they cut straight on.
var OCX = 960, OCY = 560, OR = 390, OSTART = -125;

// The planet is the Earth unless named (p-venus, p-mars).
function orbitScene(planet) {
    put("paper", "screen");
    put("o-orbit");
    put(planet || "o-earth");
    put("o-sat");
    for (var i = 1; i <= 4; i++) put("ring-" + i);
}

// The payload's angle (degrees, falling as it goes round anticlockwise), integrated frame by frame
// from start at speed(t) degrees a second, so a speed that changes never jumps it.
function orbitAngles(start, speed) {
    var a = [], x = start;
    for (var f = 0; f <= NFRAMES + 1; f++) { a.push(x); x -= speed(f / FPS) / FPS; }
    return function (t) { return a[clamp(Math.round(t * FPS), 0, a.length - 1)]; };
}
function orbitWhere(phi, radius, t) {
    var p = phi(t) * RAD, r = radius ? radius(t) : OR;
    return [OCX + r * Math.cos(p), OCY + r * Math.sin(p)];
}
// Puts the payload on its way, nose along it.
function orbitFly(phi, radius) {
    move("o-sat", function (t) { var p = orbitWhere(phi, radius, t); return [p[0] - OCX, p[1] - OCY]; });
    turn("o-sat", function (t) { return phi(t); });
}
// The radio rings, sent at the given times from where the payload is, growing and fading over 0.9 s.
function radioRings(sends, phi) {
    for (var i = 1; i <= 4; i++) {
        (function (n, k) {
            var mine = function (t) {
                var last = -1;
                for (var j = k; j < sends.length; j += 4) if (sends[j] <= t) last = sends[j];
                return last;
            };
            move(n, function (t) { var te = mine(t); var p = orbitWhere(phi, null, te < 0 ? 0 : te); return [p[0] - OCX, p[1] - OCY]; });
            grow(n, function (t) { var te = mine(t); return te < 0 ? 0.3 : 0.3 + 1.6 * clamp((t - te) / 0.9, 0, 1); });
            fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : (1 - clamp((t - te) / 0.9, 0, 1)); });
        })("ring-" + i, i - 1);
    }
}
// Every 0.75 s from 0.5, except while quiet(t).
function sendTimes(quiet) {
    var s = [];
    for (var t = 0.5; t < DUR; t += 0.75) if (!quiet(t)) s.push(t);
    return s;
}
// How far the step's own overlays (night, gauges, a monitor strip) have faded in: a step that cuts
// in from the Earth orbit brings them in over its first half second (intro(true)).
var INTRO = function () { return 1; };
function intro(on) { INTRO = on ? function (t) { return clamp(t / 0.5, 0, 1); } : function () { return 1; }; }
function introFade(name, fn) { fade(name, function (t) { return INTRO(t) * (fn ? fn(t) : 1); }); }
// A gauge's gold fill: level(t) 0..1 of its width, blinking while blink(t).
function gauge(name, level, blink) {
    grow(name, function (t) { return [Math.max(0.001, level(t)), 1]; });
    introFade(name, function (t) { return blink && blink(t) && Math.floor(t * 6) % 2 === 0 ? 0.25 : 1; });
}
