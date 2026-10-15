// A lander's landing on Mars (after orbit.jsx, for the radio rings), on from mars-descent's last frame:
// the lander comes down on its three engines onto the ground (m-ground, its line at 900 as the descent
// left it), kicking up dust; the engines cut at touchdown (2.4), and from 3.0 its dish sends home.
// Glitch (Viking 2): one foot comes down on a rock, and it stands tilted; it still sends.
// Failure (Mars 3): down safely, it sends once, and then nothing more.
// design.md 1.1.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DOWN = 2.4, SEND = 3.0;

put("paper", "screen");
put("m-ground");
put("m-stones");
put("m-rock");
for (var i = 1; i <= 4; i++) put("ring-" + i);
put("r-dust");
put("m-leg-l");
put("m-leg-r");
for (i = 1; i <= 3; i++) put("m-flame-" + i);
put("m-lander");

// The ground's line at 900 (it's drawn at 1100), the rock under the right foot in the glitch only.
move("m-ground", function () { return [0, -200]; });
move("m-stones", function () { return [0, -200]; });
move("m-rock", function () { return [0, -200]; });
if (!glitch) hide("m-rock");

// The lander (centre 440 at rest, its feet 73 below): from where the descent left it (+36) down to the
// ground, slowing; the legs give a little at touchdown. On the rock, the right side stands 24 higher.
var LAND = 900 - 73 - 440;
var fallTo = track([[0, 36], [DOWN, LAND, "out"]]);
var give = function (t) { return t < DOWN ? 0 : 6 * Math.sin(Math.PI * clamp((t - DOWN) / 0.35, 0, 1)); };
var tilt = glitch ? track([[DOWN - 0.05, 0], [DOWN + 0.35, -6, "out"]]) : function () { return 0; };
var at = function (t) { return [0, fallTo(t) + give(t) - (glitch ? 12 * clamp((t - DOWN + 0.05) / 0.4, 0, 1) : 0)]; };
var rot = function (t) { return tilt(t) + (t < DOWN ? 1.5 * Math.sin(2 * Math.PI * 0.7 * t) : 0); };
move("m-lander", at);
turn("m-lander", rot);
// A point of the lander's own drawing, offset from its centre by (dx, dy), where it is now.
function on(t, dx, dy) {
    var a = rot(t) * RAD, o = at(t);
    return [o[0] + dx * Math.cos(a) - dy * Math.sin(a), o[1] + dx * Math.sin(a) + dy * Math.cos(a)];
}
var legs = [["m-leg-l", -74], ["m-leg-r", 74]];
for (i = 0; i < 2; i++) {
    (function (n, dx) {
        move(n, function (t) { var p = on(t, dx, 10); return [p[0] - dx, p[1] - 10]; });
        turn(n, rot);
    })(legs[i][0], legs[i][1]);
}
// The engines, until touchdown.
for (i = 1; i <= 3; i++) {
    (function (n, dx, k) {
        move(n, function (t) { var p = on(t, dx, 14); return [p[0] - dx, p[1] - 14]; });
        turn(n, rot);
        grow(n, function (t) { return t < DOWN ? [1, 1 + 0.06 * jitter(t, k)] : 0.01; });
        fade(n, function (t) { return t < DOWN ? 1 : 0; });
    })("m-flame-" + i, (i - 2) * 44, i);
}
// The dust the engines blow off the ground as it comes down, settling after.
move("r-dust", function () { return [0, -200]; });
grow("r-dust", track([[1.2, 0.5], [DOWN, 1.8], [DOWN + 1.4, 2.3, "lin"]]));
fade("r-dust", track([[1.2, 0], [1.8, 0.8], [DOWN + 0.3, 1], [DOWN + 2.0, 0, "lin"]]));

// The dish's tip (30, -58 from the centre) sends home: every 0.75 s, or once (lost).
var sends = [];
for (var s = SEND; s < DUR; s += 0.75) { sends.push(s); if (failure) break; }
radioRingsAt(sends, function (t) { var p = on(t, 30, -58); return [960 + p[0], 440 + p[1]]; });
camera(function () { return 0; }, function (t) { return t >= DOWN && t < DOWN + 0.2 ? 3 : 0; });
