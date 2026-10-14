// Biometric data transmission: the passenger's heartbeat comes down with the radio. The payload
// goes round sending (a ring every 0.75 s), and a monitor strip top left draws the heartbeat: a
// head sweeps left to right, rewriting one beat a slot (a beat every 0.6 s, a sweep of four every 2.4 s).
// Glitch: the signal breaks up at 2.2 (static on the strip, no rings), and is back at 3.4.
// Failure: the same, but it never comes back: from 3.4 the head writes nothing, the strip empties
// and the radio stays quiet. The signal is lost; what happened aboard isn't shown.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LOST = 2.2, BACK = 3.4, P = 0.6, SLOTS = 4, SWEEP = 2.4, GAP = 0.12;

orbitScene();
put("x-ecg-frame");
for (var i = 1; i <= SLOTS; i++) { put("x-beat-" + i); put("x-static-" + i); }
put("x-ecg-head");
// It cuts in from the Earth orbit: the strip fades in.
intro(true);
introFade("x-ecg-frame");
introFade("x-ecg-head");

var phi = orbitAngles(OSTART, function () { return 55; });
orbitFly(phi);
turn("o-earth", function (t) { return -0.25 * t; });

// What a write at time w puts down: a beat, static, or nothing.
function kind(w) {
    if (!(glitch || failure) || w < LOST) return "beat";
    if (w < BACK) return "static";
    return glitch ? "beat" : "none";
}
for (i = 0; i < SLOTS; i++) {
    (function (k) {
        // The latest write of this slot by time t (a sweep before the clip filled the strip already).
        var last = function (t) { var o = k * P; return o + Math.floor((t - o) / SWEEP) * SWEEP; };
        var shown = function (t, what) { var w = last(t); return kind(w) === what && t - w < SWEEP - GAP ? 1 : 0; };
        introFade("x-beat-" + (k + 1), function (t) { return shown(t, "beat"); });
        introFade("x-static-" + (k + 1), function (t) { return shown(t, "static"); });
    })(i);
}
move("x-ecg-head", function (t) { return [160 * SLOTS * ((t % SWEEP) / SWEEP), 0]; });

radioRings(sendTimes(function (t) { return (glitch || failure) && t >= LOST && (failure || t < BACK); }), phi);
