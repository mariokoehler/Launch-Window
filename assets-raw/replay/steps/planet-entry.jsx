// A lander's entry at another planet (PLANET, set by the step's own file first: "venus" or "mars"): the
// lander falls heat shield first, and the planet's limb comes up and settles under it. A bow of hot air
// builds under it from 0.6, the air streams past (peak 2.0 to 3.4), and the glow dies by 4.6.
// Venera's lander is a sphere (v-pod); Viking's and Mars 3's ride in a blunt cone (e-shell).
// Glitch: it starts to oscillate at 2.4, the glow swinging with it, and is steady again by 3.9.
// Failure: the same oscillation, but it grows: at 2.9 it goes sideways into the glow and breaks up at 3.4.
// (The capsule's reentry, reentry.jsx, is the same story at the Earth.)
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var BREAK = 3.4;
var mars = PLANET === "mars";
var CRAFT = mars ? "e-shell" : "v-pod";

put("paper", "screen");
put(mars ? "e-limb-mars" : "b-limb");
for (var i = 1; i <= 8; i++) put("r-streak-" + i);
put("e-glow");
put(CRAFT);
for (i = 1; i <= 4; i++) put("r-chip-" + i);
put("burst");

// The limb comes up and settles just under the lander.
move(mars ? "e-limb-mars" : "b-limb", function (t) { return [0, -300 * (1 - Math.exp(-t / 1.4))]; });

var heat = failure ? track([[0.6, 0], [2.0, 1], [BREAK, 1]]) : track([[0.6, 0], [2.0, 1], [3.4, 1], [4.6, 0]]);
var osc = function (t) {
    if (!(glitch || failure) || t < 2.4) return 0;
    var a = 20 * Math.min(1, (t - 2.4) / 0.3);
    if (glitch) a *= clamp((3.9 - t) / 0.9, 0, 1);
    return a * Math.sin(2 * Math.PI * 1.4 * (t - 2.4));
};
var sideways = failure ? track([[2.8, 0], [BREAK, 85, "in"]]) : function () { return 0; };
function angle(t) { var u = Math.min(t, failure ? BREAK : 99); return osc(u) + sideways(t); }
function shake(t, seed) { return failure && t >= BREAK ? 0 : 3 * heat(t) * Math.sin(2 * Math.PI * 11 * t + seed); }

// The lander about its centre (960, 440): the sphere is drawn lower, at 506, and twice its size here.
move(CRAFT, function (t) { return [shake(t, 0), (mars ? 0 : -66) + shake(t, 1.3)]; });
if (!mars) grow(CRAFT, function () { return 2; });
turn(CRAFT, angle);
fade(CRAFT, function (t) { return failure && t >= BREAK ? 0 : 1; });
// The bow of hot air turns with it (it's always ahead of the heat shield).
move("e-glow", function (t) { return [shake(t, 0), shake(t, 1.3)]; });
turn("e-glow", angle);
fade("e-glow", function (t) { return failure && t >= BREAK ? 0 : heat(t); });
grow("e-glow", function (t) { var k = 0.7 + 0.3 * heat(t); return [k * (1 + 0.05 * Math.sin(2 * Math.PI * 9 * t)), k]; });

if (failure) {
    move("burst", function () { return [0, -160]; });
    grow("burst", track([[BREAK, 0.3], [BREAK + 0.35, 2.0, "out"], [BREAK + 1.0, 2.4, "lin"]]));
    fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
    var flings = [[-420, -260, 300], [-300, -420, -380], [320, -380, 240], [200, -520, -260]];
    for (i = 1; i <= 4; i++) {
        (function (n, v) {
            fade(n, function (t) { return t >= BREAK && t < BREAK + 2.2 ? 1 - Math.max(0, t - BREAK - 1.6) / 0.6 : 0; });
            move(n, function (t) { var u = Math.max(0, t - BREAK); return [-2 + v[0] * u, -26 + v[1] * u]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - BREAK); });
        })("r-chip-" + i, flings[i - 1]);
    }
} else {
    hide("burst");
    for (i = 1; i <= 4; i++) hide("r-chip-" + i);
}

// The air streaming up past it, each streak looping every 0.5 s in its own lane, turned with the lander.
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        var lane = (k % 2 ? 1 : -1) * (20 + 22 * k), phase = k * 0.13;
        function at(t) {
            var a = angle(Math.min(t, failure ? BREAK : 99)) * RAD, u = ((t + phase) % 0.5) / 0.5;
            var along = 170 - 520 * u;   // from under the heat shield up past the lander
            return [lane * Math.cos(a) - along * Math.sin(a), lane * Math.sin(a) + along * Math.cos(a)];
        }
        move(n, function (t) { var p = at(t); return [p[0], p[1] - 30]; });
        turn(n, function (t) { return angle(Math.min(t, failure ? BREAK : 99)) - 90; });
        fade(n, function (t) {
            var u = ((t + phase) % 0.5) / 0.5, h = failure && t > BREAK ? heat(BREAK) * Math.max(0, 1 - (t - BREAK) / 1.2) : heat(t);
            return 0.9 * h * Math.sin(Math.PI * u);
        });
    })("r-streak-" + i, i);
}
camera(function () { return 0; }, function (t) { return 3 * heat(t) * (failure && t >= BREAK ? 0 : 1) + (failure && t >= BREAK && t < BREAK + 0.4 ? 8 : 0); });
