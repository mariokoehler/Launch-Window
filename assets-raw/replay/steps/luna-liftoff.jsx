// Luna liftoff (2026-10-16): a Luna sample return's ascent from the surface. On from the sampler's last
// frame, the core sealed in the capsule: the return rocket's engine lights (0.4), and at 1.0 it lifts off
// the descent stage, which stays behind as its launch pad, and climbs out of the frame, faster and faster.
// Luna 16's went straight home, no orbit round the Moon first.
// Glitch: the engine sputters (0.4 to 1.6) before it catches; it lifts off at 1.8.
// Failure: it lifts off, starts to lean (1.3), the engine dies (1.7), and it falls back beside the stage.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIFT = glitch ? 1.8 : 1.0, DIES = 1.7, FALL = 2.1;

lunaScene("sampler");
lookAt(function () { return 1130; });
put("s-flame", "u-lander");
// Behind the stage, so it shows only once the rocket is clear of the deck.
layers["s-flame"].moveAfter(layers["u-stage"]);

var y, x, lean;
if (failure) {
    y = track([[LIFT, 0], [1.6, -60, "out"], [FALL, 30, "in"]]);
    x = track([[LIFT, 0], [FALL, 60, "in"]]);
    lean = track([[1.3, 0], [FALL, 70, "in"]]);
} else {
    y = track([[LIFT, 0], [LIFT + 2.4, -760, "in"]]);
    x = function () { return 0; };
    lean = glitch ? function (t) { return t > LIFT ? 3 * Math.sin(2 * Math.PI * 1.4 * (t - LIFT)) * clamp(1 - (t - LIFT) / 1.5, 0, 1) : 0; }
                  : function () { return 0; };
}
// The rocket, its flame and its capsule turn together about the rocket's foot (the capsule's centre is 98
// above it).
function place(name, up) {
    move(name, function (t) { var a = lean(t) * RAD; return [x(t) + up * Math.sin(a), y(t) - up * Math.cos(a) + up]; });
    turn(name, lean);
}
place("s-rocket", 0);
place("s-flame", 0);
place("s-capsule", 98);

// The engine: sputtering in the glitch before it catches, out once the failure's dies.
function burning(t) {
    if (t < 0.4) return 0;
    if (failure) return t < DIES ? 1 : 0;
    if (glitch && t < 1.6) return jitter(t, 6) > 0.3 ? 1 : 0;
    return 1;
}
// Its flame shows as the rocket rises clear of the deck (under it, it would read as the stage's engine).
fade("s-flame", function (t) { return burning(t) * clamp(-y(t) / 30, 0, 1); });
grow("s-flame", function (t) { var k = t < 0.8 ? 0.5 + (t - 0.4) * 1.2 : 1; return [1, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t))]; });
// The blast's dust on the deck from the moment it lights (flickering while the glitch's sputters).
move("u-kick", function () { return [960 - 870, DECK - 874]; });
fade("u-kick", function (t) { return t < LIFT + 0.6 ? burning(t) : clamp(1 - (t - LIFT - 0.6) / 0.6, 0, 1); });
grow("u-kick", function (t) { return 1.2 + 0.5 * clamp(t - LIFT, 0, 1); });

if (failure) {
    // It hits the surface beside the stage: a burst, and nothing left of it.
    fade("s-rocket", function (t) { return t < FALL + 0.05 ? 1 : 0; });
    fade("s-capsule", function (t) { return t < FALL + 0.05 ? 1 : 0; });
    move("burst", function () { return [x(FALL) + 60, 860 - 600]; });
    grow("burst", track([[FALL, 0.3], [FALL + 0.35, 1.0, "out"], [FALL + 1.2, 1.2, "lin"]]));
    fade("burst", track([[FALL, 0, "snap"], [FALL + 0.02, 1, "snap"], [FALL + 0.5, 1], [FALL + 1.2, 0, "lin"]]));
}
