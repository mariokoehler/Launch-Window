// A lander's ascent stage test in Earth orbit: at 0.8 the ascent engine lights on top of the descent stage
// and the ascent stage climbs off it, leaving the descent stage behind, drifting and turning.
// Glitch: the engine sputters from 0.8, the alarm lamp blinking, and only lights properly at 1.6.
// Failure: the same sputter, and when it lights at 1.6 the stages don't part: pushed together, the stack
// starts to tumble, and the engine is shut down at 2.8, the alarm lamp lit.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIGHT = 0.8, LATE = 1.6, CUT = 2.8;
// On from an engine test's last frame (2026-10-19; FROM, a step's file sets it): where its burn left the lander
// (up by FROM[0]), its turn (FROM[1]) and the Earth's (FROM[2]); none when the staging opens the scene.
var FROM = typeof FROM !== "undefined" ? FROM : [0, 0, 0];

landerOrbit("alarm");
LO = [LO[0], LO[1] + FROM[0]];
turn("d-earth", function (t) { return FROM[2] - 0.25 * t; });
var rough = glitch || failure;
var go = rough ? LATE : LIGHT + 0.1;
var flame = function (t) {
    if (t < LIGHT) return 0;
    if (rough && t < LATE) return Math.floor(t * 9) % 3 === 0 ? 0.6 : 0;
    if (failure && t >= CUT) return 0;
    return 1;
};
// The ascent stage's climb off the descent stage, in the lander's frame (before its scale).
var climb = function (t) { var u = Math.max(0, t - go); return [0, -(80 * u + 160 * u * u)]; };
var spin;
if (failure) {
    move("n-ascent", function () { return [0, 0]; });
    move("n-flame-up", function () { return [0, 0]; });
    spin = function (t) { var u = Math.max(0, t - LATE); return 20 * u * u + 12 * u; };
    move("n-carrier", function (t) { var u = Math.max(0, t - LATE); return [LO[0] + 30 * u, LO[1] - 40 * u]; });
} else {
    move("n-ascent", climb);
    move("n-flame-up", climb);
    // The descent stage, let go, drifts down and turns slowly.
    spin = function (t) { return 6 * Math.max(0, t - go); };
    move("n-carrier", function (t) { var u = Math.max(0, t - go); return [LO[0] - 10 * u, LO[1] + 30 * u]; });
}
turn("n-carrier", function (t) { return FROM[1] + spin(t); });
fade("n-flame-up", function (t) { return flame(t) > 0 ? 1 : 0; });
grow("n-flame-up", function (t) { var k = Math.max(flame(t), 0.01); return [k, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)) * (1 + clamp((t - go) * 2, 0, 1.5))]; });
fade("n-lamp", function (t) {
    if (!rough || t < LIGHT) return 0;
    if (t < LATE) return Math.floor(t * 5) % 2 === 0 ? 1 : 0.15;
    return failure && t >= CUT ? 1 : 0;
});
