// Solar measurements: in orbit round the Sun (big, at the left), the probe on its dashed orbit
// measures the solar wind streaming out past it, sending home a ring every 0.8 s.
// Glitch: at 2.0 it drops into safe mode, quiet and tumbling a little; it's back at 3.2.
// Failure: the same, but it never comes back: from 3.0 it goes dark, lost.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SAFE = 2.0, BACK = 3.2, LOST = 3.0;
var SX = 240, SY = 540, OR2 = 760, PX = 960, PY = 540;

put("paper", "screen");
put("s-orbit");
for (var i = 1; i <= 14; i++) put("s-wind-" + i);
put("s-sun");
for (i = 1; i <= 3; i++) put("m-ring-" + i);
put("m-probe");

// The Sun's rays turn slowly.
turn("s-sun", function (t) { return 2 * t; });

// The probe: on its orbit, a few degrees across the clip (going round anticlockwise, up the screen).
var ang = function (t) { return 4 - 1.6 * t; };
function where(t) { var a = ang(t) * RAD; return [SX + OR2 * Math.cos(a), SY + OR2 * Math.sin(a)]; }
var wobble = function (t) {
    if (!(glitch || failure) || t < SAFE) return 0;
    var a = 25 * Math.sin(2 * Math.PI * 1.1 * (t - SAFE));
    return glitch && t > BACK ? a * clamp(1 - (t - BACK) / 0.5, 0, 1) : a;
};
grow("m-probe", function () { return 2.4; });
move("m-probe", function (t) { var p = where(t); return [p[0] - 960, p[1] - 540]; });
turn("m-probe", function (t) { return ang(t) + 90 + wobble(t); });
var working = function (t) { return !(glitch || failure) || t < SAFE || (glitch && t >= BACK); };
fade("m-probe", failure ? track([[LOST + 0.4, 1], [LOST + 1.0, 0.35, "lin"]]) : function () { return 1; });

// The wind: each dash goes out along its own ray, 600 px a second, looping every 1.6 s.
for (i = 1; i <= 14; i++) {
    (function (n, k) {
        var a = -40 + 80 * (k - 1) / 13, phase = (k * 0.53) % 1.6;
        turn(n, function () { return a; });
        move(n, function (t) {
            var d = 140 + 600 * ((t + phase) % 1.6);
            return [d * Math.cos(a * RAD), d * Math.sin(a * RAD)];
        });
        fade(n, function (t) { var u = ((t + phase) % 1.6) / 1.6; return Math.sin(Math.PI * u); });
    })("s-wind-" + i, i);
}

// Home: a ring every 0.8 s while it works.
var sends = [];
for (var s = 0.4; s < DUR; s += 0.8) if (working(s)) sends.push(s);
for (i = 1; i <= 3; i++) {
    (function (n, k) {
        var mine = function (t) { var last = -1; for (var j = k; j < sends.length; j += 3) if (sends[j] <= t) last = sends[j]; return last; };
        move(n, function (t) { var te = mine(t); var p = where(te < 0 ? 0 : te); return [p[0] - 960, p[1] - 540]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 0.6 : 0.6 + 2.6 * clamp((t - te) / 0.9, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : 1 - clamp((t - te) / 0.9, 0, 1); });
    })("m-ring-" + i, i - 1);
}
