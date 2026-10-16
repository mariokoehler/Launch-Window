// A Luna landing (2026-10-16): the Ye-8 stage (PAYLOAD, set by the step's own file: Luna 17's rover or
// Luna 15's sampler) comes down from above on its engine, sliding in from the left, the plume blowing
// the dust; the engine cuts just above the surface and it drops onto its legs at 2.6. It ends where the
// surface clips start (the camera of lunokhod-deploy).
// Glitch: the engine cuts too high (2.3): it drops, hits hard, bounces and rocks on its legs, and settles.
// Failure (Luna 15, 21 July 1969, while Apollo 11 was on the surface): coming in steep and fast, the
// engine sputters out (1.4) and it flies into the ground at 2.1.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DOWN = failure ? 2.1 : 2.6, CUT = failure ? 1.4 : glitch ? 2.3 : 2.5;

lunaScene(PAYLOAD);
lookAt(function () { return 1130; });

var y, x, lean;
if (failure) {
    y = track([[0, -460], [1.4, -210, "out"], [DOWN, 0, "in"]]);
    x = track([[0, -140], [DOWN, 40, "lin"]]);
    lean = track([[1.4, 0], [DOWN, 22, "in"]]);
} else if (glitch) {
    y = track([[0, -460], [2.0, -60, "out"], [2.3, -30, "lin"], [2.55, 0, "in"], [2.75, -16, "out"], [2.95, 0, "in"]]);
    x = track([[0, -140], [2.3, 0, "out"]]);
    lean = track([[2.55, 0], [2.75, -9, "out"], [3.1, 6, "inout"], [3.5, -2, "inout"], [3.8, 0, "inout"]]);
} else {
    y = track([[0, -460], [2.0, -60, "out"], [DOWN, 0, "in"]]);
    x = track([[0, -140], [2.4, 0, "out"]]);
    lean = function () { return 0; };
}
move("u-lander", function (t) { return [x(t), y(t)]; });
turn("u-lander", lean);

// The plume: on until the cut, flickering; the failure's sputters before it dies.
fade("u-plume", function (t) {
    if (t >= CUT) return 0;
    return failure && t > 1.0 ? (jitter(t, 6) > 0 ? 1 : 0.2) : 1;
});
grow("u-plume", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
// The dust it blows off the surface as it comes close, settling once it's down; the failure's is where it hits.
if (failure) {
    fade("n-dust", track([[DOWN, 0], [DOWN + 0.2, 1, "out"], [DOWN + 1.6, 0, "lin"]]));
    move("n-dust", function () { return [x(DOWN), 0]; });
    grow("n-dust", function () { return 1.6; });
} else {
    fade("n-dust", track([[1.6, 0], [2.2, 1, "lin"], [CUT, 1], [3.6, 0, "lin"]]));
    grow("n-dust", function (t) { return 0.8 + 0.3 * clamp((t - 1.6) / 1.0, 0, 1); });
}

if (failure) {
    // It flies into the ground: a burst where it hits, and nothing left of it (parenting doesn't pass on
    // opacity, so every part goes).
    var gone = ["u-stage"].concat(PAYLOAD === "rover" ? ["u-ramp-f", "u-ramp-r", "u-ant", "u-lid", "u-tub",
        "u-wheel-1", "u-wheel-2", "u-wheel-3", "u-wheel-4"] : ["s-rocket", "s-capsule", "s-arm"]);
    for (var i = 0; i < gone.length; i++) fade(gone[i], function (t) { return t < DOWN + 0.05 ? 1 : 0; });
    move("burst", function () { return [x(DOWN), 860 - 600]; });
    grow("burst", track([[DOWN, 0.3], [DOWN + 0.35, 1.2, "out"], [DOWN + 1.2, 1.4, "lin"]]));
    fade("burst", track([[DOWN, 0, "snap"], [DOWN + 0.02, 1, "snap"], [DOWN + 0.5, 1], [DOWN + 1.2, 0, "lin"]]));
}
