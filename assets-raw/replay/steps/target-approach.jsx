// Target approach: the chaser comes in from the left towards the target's collar, its light blinking.
// At 1.8 a puff ahead of its shoulders brakes it: it stops by 2.4, a capsule's length off the collar,
// and holds there (where the docking starts).
// Glitch: the braking puff doesn't come; at 2.1 a hard one does, and it stops too close, then backs
// off to the holding point by 3.4.
// Failure: the same, but at 2.1 the brakes still don't answer: a puff on its top pushes it down, and it
// slides under the target and past it, out of reach.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var V = 470 / 2.1, BRAKE = 1.8, LATE = 2.1, HOLD = -150;

dockScene(false);
blink();

var x, y = function () { return 0; }, spin = function () { return 0; };
if (glitch) {
    // Stops 0.4 s after the late brake, then backs off.
    var stop = -620 + V * LATE + V * 0.2;
    x = function (t) {
        if (t <= LATE) return -620 + V * t;
        if (t <= LATE + 0.4) { var u = t - LATE; return -620 + V * LATE + V * u - V / 0.8 * u * u; }
        return track([[LATE + 0.4, stop], [3.4, HOLD, "inout"]])(t);
    };
} else if (failure) {
    x = function (t) { return -620 + V * t; };
    // Down at 240 px/s within 0.2 s, stopping once it's clear.
    var vy = function (t) { return t < LATE ? 0 : t < LATE + 0.2 ? 1200 * (t - LATE) : t < 2.7 ? 240 : t < 3.1 ? 240 - 600 * (t - 2.7) : 0; };
    var ys = [], acc = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { ys.push(acc); acc += vy(f / FPS) / FPS; }
    y = function (t) { return ys[clamp(Math.round(t * FPS), 0, ys.length - 1)]; };
    spin = track([[LATE, 0], [6.4, 24, "in"]]);
} else {
    x = function (t) {
        if (t <= BRAKE) return -620 + V * t;
        var u = Math.min(t - BRAKE, 0.6);
        return -620 + V * BRAKE + V * u - V / 1.2 * u * u;
    };
}
chaser(function (t) { return [x(t), y(t)]; }, spin);
puffs("k-push", [0.2]);
puffs("k-brake", glitch ? [LATE, 2.4, 3.0] : failure ? [] : [BRAKE]);
puffs("k-side", failure ? [LATE] : []);
