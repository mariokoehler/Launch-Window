// The rocket plane on its own (its peak, its fast run, a new wing's test): high over the Earth's limb,
// gliding level, the air streaming past.
// Glitch: from 1.4 it starts to wallow, rocking nose up and down; the rocking dies away by 3.0.
// Failure: the same rocking grows, and at 2.4 it departs into a spin and falls away out of sight (as the
// X-15 lost with Mike Adams in 1967).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var ROCK = 1.4, SPIN = 2.4;

put("paper", "screen");
put("d-earth");
for (var i = 1; i <= 8; i++) put("a-flow-" + i);
put("w-plane");
put("w-skid", "w-plane");
put("w-flame", "w-plane");
hide("w-skid"); hide("w-flame");
turn("d-earth", function (t) { return -0.25 * t; });
move("d-earth", function () { return [0, 200]; });

var rock = function (t) {
    if (!(glitch || failure) || t < ROCK) return 0;
    // The same rocking until 2.2, when the glitch's starts to die away and the failure's to grow.
    var u = t - ROCK, amp = 9 * Math.min(u / 0.5, 1) * (glitch ? clamp((3.0 - t) / 0.8, 0, 1) : 1 + 1.4 * Math.max(0, t - 2.2));
    return amp * Math.sin(2 * Math.PI * 1.4 * u);
};
var pitch = function (t) {
    var p = -3 + rock(t);
    if (failure && t > SPIN) { var u = t - SPIN; p = rock(SPIN) + 200 * u + 160 * u * u; }
    return p;
};
var drop = function (t) { var u = failure ? Math.max(0, t - SPIN) : 0; return [60 * u, 160 * u * u]; };
move("w-plane", function (t) { var d = drop(t); return [960 - 965 + d[0], 520 - 820 + d[1]]; });
turn("w-plane", pitch);
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        move(n, function (t) { var s = (k * 251 + 1100 * t) % 1600 - 800; return [-s, -300 + 70 * k - 20]; });
        fade(n, function (t) { return 1; });
    })("a-flow-" + i, i);
}
