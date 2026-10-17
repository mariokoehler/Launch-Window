// Surveyor's landing (2026-10-17), after orbit.jsx (its radio rings) and moon.jsx: the tripod comes down from
// above on its three verniers, sliding left onto the flat, the plumes blowing the dust. At 2.3 it hangs a few
// metres up and the verniers cut; it drops onto its pads at 2.55 and starts sending, its solar panel turning to
// the Sun (Surveyor 1, June 1966).
// Glitch (Surveyor 3, April 1967): the verniers don't cut. It touches down with them still burning, hops up
// and down again, hops once more, and the engines are shut off from Earth before it settles at 3.85.
// Failure: the same first hop, but it comes off it leaning, lands on one foot at 3.35 and topples over onto
// its antenna. Nothing more is heard from it.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CUT = 2.3, DOWN = 2.55, HIT = 3.35, OFF = 3.7, LAST = 3.85;
// The right footpad from the carrier's pivot, which it lands on and topples over in the failure.
var PAD = [148, 114], LEAN = 14, OVER = 104.8;

put("paper", "screen");
put("n-ground");
put("n-rocks");
put("sv-carrier");
var parts = ["sv-plume-1", "sv-plume-3", "sv-plume-2", "sv-legs", "sv-body", "sv-panel"];
for (var i = 0; i < parts.length; i++) put(parts[i], "sv-carrier");
put("n-dust");
put("sv-thud");
hide("sv-thud");
for (i = 1; i <= 4; i++) { put("ring-" + i); hide("ring-" + i); }

function rot(v, deg) {
    var a = deg * RAD;
    return [v[0] * Math.cos(a) - v[1] * Math.sin(a), v[0] * Math.sin(a) + v[1] * Math.cos(a)];
}

var come = track([[0, 340], [DOWN, 0, "out"]]);
var high = track([[0, -340], [CUT, -40, "out"]]);
var x, y, tilt = function () { return 0; }, off;
if (!glitch && !failure) {
    var drop = track([[CUT, -40], [DOWN, 0, "in"]]);
    x = come;
    y = function (t) { return t <= CUT ? high(t) : drop(t); };
    off = CUT;
} else {
    // Still burning: on down at an even rate, and up off the ground again.
    var on = track([[CUT, -40], [DOWN, 0, "lin"]]);
    var drift = track([[DOWN, 0], [3.45, -40, "inout"], [LAST, -50, "out"]]);
    var hops = track([[DOWN, 0], [3.0, -80, "out"], [3.45, 0, "in"], [3.65, -26, "out"], [LAST, 0, "in"]]);
    var air = function (t) { return t <= DOWN ? come(t) : drift(t); };
    if (glitch) {
        x = air;
        y = function (t) { return t <= CUT ? high(t) : t <= DOWN ? on(t) : hops(t); };
        off = OFF;
    } else {
        // It leans coming off the hop, and lands on its right foot with the carrier this high.
        var lean = track([[2.9, 0], [HIT, LEAN, "in"]]);
        var yHit = PAD[1] - rot(PAD, LEAN)[1];
        var fall = track([[3.0, -80], [HIT, yHit, "in"]]);
        var foot = [air(HIT) + rot(PAD, LEAN)[0], PAD[1]];
        var over = track([[HIT, LEAN], [3.95, OVER, "in"], [4.1, OVER - 4, "out"], [4.25, OVER, "in"]]);
        tilt = function (t) { return t <= HIT ? lean(t) : over(t); };
        // Once down, it turns about the foot, which stays where it landed.
        x = function (t) { return t <= HIT ? air(t) : foot[0] - rot(PAD, tilt(t))[0]; };
        y = function (t) {
            if (t <= CUT) return high(t);
            if (t <= DOWN) return on(t);
            if (t <= 3.0) return hops(t);
            return t <= HIT ? fall(t) : foot[1] - rot(PAD, tilt(t))[1];
        };
        off = HIT;
    }
}
move("sv-carrier", function (t) { return [x(t), y(t)]; });
turn("sv-carrier", tilt);
for (i = 1; i <= 3; i++) plume("sv-plume-" + i, function (t) { return t < off; }, function () { return 1; });

// The dust: blown while the plumes are within 160 px of the ground, settling over 0.8 s after they stop.
var near = function (t) { return clamp((160 + y(Math.min(t, off))) / 160, 0, 1); };
grow("n-dust", function (t) { return 0.6 + 0.4 * near(t) + 0.1 * Math.sin(2 * Math.PI * 3 * t); });
move("n-dust", function (t) { return [x(Math.min(t, off)), 0]; });
fade("n-dust", function (t) { return t < off ? near(t) : near(t) * clamp(1 - (t - off) / 0.8, 0, 1); });

if (failure) {
    // The antenna's far corner hits the ground: a puff there, settling.
    var corner = rot([1028 - 960, 572 - 760], OVER);
    move("sv-thud", function () { return [960 + x(4.25) + corner[0] - 980, 0]; });
    grow("sv-thud", track([[3.95, 0.5], [4.4, 1.2, "out"]]));
    fade("sv-thud", track([[3.95, 0, "snap"], [3.97, 1, "snap"], [4.4, 1], [5.4, 0, "lin"]]));
} else {
    // Down: it calls home from its antenna, and the panel turns to the Sun.
    var landed = glitch ? LAST : DOWN;
    radioRingsAt(glitch ? [4.1, 4.85, 5.6] : [2.9, 3.65, 4.4, 5.15],
                 function (t) { return [960 + x(t) + 44, 760 + y(t) - 164]; });
    turn("sv-panel", track([[landed + 0.6, 0], [landed + 1.6, 22, "inout"]]));
}
