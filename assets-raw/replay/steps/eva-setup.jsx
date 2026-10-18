// Setting up an experiment (2026-10-18; the first of three spacewalk steps): the hatch opens, the figure
// rises out of it and floats over to the package on the hull, whose collector plate rises out of it on its
// boom; its lamp comes on. It ends at work there (WORK), where the experiments begin.
// Glitch: the boom jams a third of the way up at 2.9; the hand goes to it, and a push at 3.4 frees it.
// Failure: the push doesn't, and at 3.6 the visor fogs over (Gemini 9A's, as the work wore its walker out):
// it's hauled back in, the boom left jammed.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var JAM = 2.9, PUSH = 3.4, ANSWER = 3.6;

spacewalkScene(false, true);

var out = [[0, [0, INSIDE, 0]], [0.8, [0, INSIDE, 0]], [1.4, [0, 0, 0]], [1.9, [50, -70, 25]], [2.4, WORK]];
var rise = track([[2.6, 0], [3.4, 1]]), jammed = rise(JAM);
var push = handOn([968, 346], 25), pose, boomAt, lampOn;
if (glitch) {
    pose = poses(out.concat([[3.1, WORK], [3.35, push], [3.5, push], [4.6, WORK]]));
    var rest = track([[PUSH + 0.1, jammed], [PUSH + 0.7, 1]]);
    boomAt = function (t) { return t < JAM ? rise(t) : t < PUSH + 0.1 ? jammed : rest(t); };
    lampOn = PUSH + 0.8;
} else if (failure) {
    var tried = poses(out.concat([[3.1, WORK], [3.35, push], [3.6, push]]));
    pose = function (t) {
        if (t <= 3.6) return tried(t);
        if (t <= 4.8) { var w = EASE.inout((t - 3.6) / 1.2); return mix(tried(3.6), [0, 0, 0], w); }
        return [0, INSIDE * EASE.inout(clamp((t - 4.8) / 0.6, 0, 1)), 0];
    };
    boomAt = function (t) { return t < JAM ? rise(t) : jammed; };
    lampOn = 99;
    fade("e-fog", track([[ANSWER, 0], [ANSWER + 0.3, 1, "out"]]));
} else {
    pose = poses(out);
    boomAt = rise;
    lampOn = 3.5;
}
walk(pose);
carry(pose, 99);
boom(boomAt);
fade("e-lamp", function (t) { return t >= lampOn ? 1 : 0; });
hatch(0.3, failure ? 5.6 : 99);
