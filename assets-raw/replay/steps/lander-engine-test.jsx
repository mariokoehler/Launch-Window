// A lander's engine test in Earth orbit: the descent engine lights at 0.8 and burns steadily, pushing the
// lander up (away from its plume), then shuts down at 3.4.
// Glitch: it sputters from 0.8, the alarm lamp blinking, and only burns steadily from 1.6.
// Failure: the same sputter, and at 1.6 it goes out for good, the alarm lamp lit.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIGHT = 0.8, STEADY = 1.6, OFF = 3.4;

landerOrbit("alarm");
var rough = glitch || failure;
var burning = function (t) {
    if (t < LIGHT || t >= OFF) return 0;
    if (rough && t < STEADY) return Math.floor(t * 9) % 3 === 0 ? 0.5 : 0;
    if (failure) return 0;
    return 1;
};
// The burn pushes it up: speed builds while it burns, and it coasts on after.
var ys = [], v = 0, y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { ys.push(y); v += 22 * burning(f / FPS) / FPS; y -= v / FPS; }
move("n-carrier", function (t) { return [LO[0], LO[1] + ys[clamp(Math.round(t * FPS), 0, ys.length - 1)]]; });
turn("n-carrier", function (t) { return 4 * Math.sin(2 * Math.PI * 0.15 * t); });
fade("n-plume", function (t) { return burning(t) > 0 ? 1 : 0; });
grow("n-plume", function (t) { var k = Math.max(burning(t), 0.01); return [k, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t))]; });
fade("n-lamp", function (t) {
    if (!rough || t < LIGHT) return 0;
    if (t < STEADY) return Math.floor(t * 5) % 2 === 0 ? 1 : 0.15;
    return failure ? 1 : 0;
});
