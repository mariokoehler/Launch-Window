// Landing on the Moon (the landing after a powered descent, or a robot's after its approach): close
// over the surface, the lander comes down from above a boulder field, sliding left onto the flat;
// the plume starts blowing dust; it touches down at 2.6, the engine stops and the dust settles.
// Glitch: at 2.0 the fuel lamp blinks and it slows to a hover (Apollo 11 landed with seconds of fuel
// left); it comes down at last at 3.4, the lamp still lit.
// Failure: the same, and at 2.8 the engine dies about 70 px up: it drops, lands hard at 3.1 and tips
// over on its buckled legs.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LOW = 2.0, DOWN = glitch ? 3.4 : 2.6, DIES = 2.8;

put("paper", "screen");
put("n-ground");
put("n-rocks");
lander();
put("n-dust");

var come = poses2([[0, 340, -340], [LOW, 50, -90]]);
var x, y, tilt = function () { return 0; }, off;
if (glitch || failure) {
    var hover = poses2([[LOW, 50, -90], [2.4, 30, -78], [DIES, 16, -70]]);
    if (glitch) {
        var last = poses2([[DIES, 16, -70], [DOWN, 0, 0]]);
        x = function (t) { return t <= LOW ? come(t)[0] : t <= DIES ? hover(t)[0] : last(t)[0]; };
        y = function (t) { return t <= LOW ? come(t)[1] : t <= DIES ? hover(t)[1] : last(t)[1]; };
        off = DOWN;
    } else {
        // Free fall from 70 px over 0.3 s, then the tip: it leans over onto its right legs.
        var HIT = 3.1;
        tilt = track([[HIT, 0], [HIT + 0.5, 16, "out"]]);
        // It tips over its right footpad (142, 118 from the carrier's pivot), which stays where it hit,
        // a little sunk where the leg buckles.
        var pad = function (t) {
            var a = tilt(t) * RAD;
            return [142 - (142 * Math.cos(a) - 118 * Math.sin(a)), 118 - (142 * Math.sin(a) + 118 * Math.cos(a))];
        };
        x = function (t) { return t <= LOW ? come(t)[0] : t <= DIES ? hover(t)[0] : 16 + pad(t)[0]; };
        y = function (t) {
            if (t <= LOW) return come(t)[1];
            if (t <= DIES) return hover(t)[1];
            var u = t - DIES;
            return t < HIT ? -70 + 70 * u * u / 0.09 : pad(t)[1] + 8 * EASE.out(clamp((t - HIT) / 0.4, 0, 1));
        };
        off = DIES;
    }
} else {
    var settle = poses2([[LOW, 50, -90], [DOWN, 0, 0]]);
    x = function (t) { return t <= LOW ? come(t)[0] : settle(t)[0]; };
    y = function (t) { return t <= LOW ? come(t)[1] : settle(t)[1]; };
    off = DOWN;
}
move("n-carrier", function (t) { return [x(t), y(t)]; });
turn("n-carrier", tilt);
plume("n-plume", function (t) { return t < off; });
// The dust: blown while the plume is within 160 px of the ground, settling over 0.8 s after it stops.
grow("n-dust", function (t) { return 0.7 + 0.5 * clamp((160 + y(t)) / 160, 0, 1) + 0.1 * Math.sin(2 * Math.PI * 3 * t); });
move("n-dust", function (t) { return [x(t), 0]; });
fade("n-dust", function (t) {
    var near = clamp((160 + y(Math.min(t, off))) / 160, 0, 1);
    return t < off ? near : near * clamp(1 - (t - off) / 0.8, 0, 1) + (failure && t >= 3.1 && t < 3.9 ? 1 - (t - 3.1) / 0.8 : 0);
});
lamp("fuel", function (t) { return glitch && t >= DOWN; },
     glitch || failure ? function (t) { return t >= LOW && t < (glitch ? DOWN : DIES); } : null);

// Keys [[t, x, y], ...] eased in and out between them.
function poses2(keys) {
    var a = [], b = [];
    for (var i = 0; i < keys.length; i++) { a.push([keys[i][0], keys[i][1], "inout"]); b.push([keys[i][0], keys[i][2], "inout"]); }
    var fx = track(a), fy = track(b);
    return function (t) { return [fx(t), fy(t)]; };
}
