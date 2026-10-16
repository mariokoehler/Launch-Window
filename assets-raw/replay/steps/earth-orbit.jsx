// Earth orbit: the payload circles the Earth on its planned orbit (dashed), sending a radio ring
// every 0.75 s. Glitch: the rings stop at 2.0 and the payload wobbles, until it's back at 3.4.
// Failure: the rings stop at 2.0 for good, and from 2.4 the orbit decays: the payload drops,
// speeds up, glows as it hits the air and burns up at about 4.4.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CX = 960, CY = 560;

put("paper", "screen");
put("o-orbit");
put("o-earth");
put("o-streak");
put("o-sat");
layers["o-streak"].parent = layers["o-sat"];
for (var i = 1; i <= 4; i++) put("ring-" + i);
put("burst");
hide("burst");

// The orbit's radius, and the angle (degrees, y down, so it falls as the payload goes round
// anticlockwise), integrated frame by frame: lower is faster (r^-1.5).
// The drop starts at 2.4, steady at first so it shows by the reveal (3.0), then faster.
var radius = failure ? track([[2.4, 390], [3.0, 370, "lin"], [4.4, 262, "in"]]) : function () { return 390; };
var BURN = 4.4;
// Only the failure stops there; the others go round to the end (-125, where orbit.jsx starts).
var STOP = failure ? BURN : DUR;
var angle = [], a = 205;
for (var f = 0; f <= NFRAMES + 1; f++) {
    angle.push(a);
    a -= 55 / FPS * Math.pow(390 / radius(f / FPS), 1.5);
}
function phi(t) { return angle[clamp(Math.round(t * FPS), 0, angle.length - 1)]; }
function where(t) { var p = phi(t) * RAD, r = radius(t); return [CX + r * Math.cos(p), CY + r * Math.sin(p)]; }

var wobble = glitch ? function (t) { return t >= 2.0 && t < 3.4 ? 18 * Math.sin(2 * Math.PI * 1.6 * (t - 2.0)) * Math.min(1, (3.4 - t) / 0.4) : 0; }
                    : function () { return 0; };
move("o-sat", function (t) { var p = where(Math.min(t, STOP)); return [p[0] - CX, p[1] - CY]; });
// Nose along its way: the tangent, turned in a little as it falls.
turn("o-sat", function (t) { return phi(Math.min(t, STOP)) + wobble(t) + (failure ? -18 * clamp((t - 2.4) / 2.0, 0, 1) : 0); });
if (failure) {
    grow("o-streak", function (t) { return [1, 1 + 0.08 * Math.sin(2 * Math.PI * 10 * t)]; });
    // Parenting doesn't pass on opacity: the glow fades with the payload.
    fade("o-sat", track([[BURN, 1], [BURN + 0.3, 0, "lin"]]));
    fade("o-streak", track([[3.5, 0], [4.1, 1, "lin"], [BURN, 1], [BURN + 0.3, 0, "lin"]]));
    var end = where(BURN);
    move("burst", function () { return [end[0] - 960, end[1] - 600]; });
    grow("burst", track([[BURN, 0.2], [BURN + 0.35, 0.9, "out"], [BURN + 1.0, 1.1, "lin"]]));
    fade("burst", track([[BURN, 0, "snap"], [BURN + 0.02, 1, "snap"], [BURN + 0.4, 1], [BURN + 1.0, 0, "lin"]]));
} else {
    hide("o-streak");
}

// The rings: each sent from where the payload is, growing and fading over 0.9 s.
var sends = [];
for (var s = 0.5; s < DUR; s += 0.75) {
    if ((glitch && s >= 2.0 && s < 3.4) || (failure && s >= 2.0)) continue;
    sends.push(s);
}
if (glitch) { sends.push(3.4); sends.sort(function (x, y) { return x - y; }); }
for (i = 1; i <= 4; i++) {
    (function (n, k) {
        // The latest send this ring carries (every fourth), or -1 before its first.
        var mine = function (t) {
            var last = -1;
            for (var j = k; j < sends.length; j += 4) if (sends[j] <= t) last = sends[j];
            return last;
        };
        move(n, function (t) { var te = mine(t); var p = where(te < 0 ? 0 : te); return [p[0] - CX, p[1] - CY]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 0.3 : 0.3 + 1.6 * clamp((t - te) / 0.9, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : (1 - clamp((t - te) / 0.9, 0, 1)); });
    })("ring-" + i, i - 1);
}
