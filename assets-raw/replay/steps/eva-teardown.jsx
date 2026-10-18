// Taking the experiment down (2026-10-18; the last of three spacewalk steps): on from the experiments'
// last frame, the lamp goes off, the boom draws the collector plate back into the package, and the figure
// takes the package off its mount and brings it back in; the hatch closes.
// Glitch: the boom stalls at 0.8; the hand presses the plate down at 1.5, and the package comes in late.
// Failure: two presses don't move it, and at 2.0 the figure leaves the package on the hull and goes in.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var STALL = 0.8, PRESS = 1.5;

spacewalkScene(false, true);

var retract = track([[0.5, 1], [1.2, 0]]), stalled = retract(STALL);
var plate = handOn([975, 356 - 50 * stalled], 40), pressed = [plate[0], plate[1] + 4, plate[2]];
var away = function (t0) { return [[t0 + 0.6, [30, -70, 20]], [t0 + 1.2, [0, 0, 0]], [t0 + 2.0, [0, INSIDE, 0]]]; };
var pose, boomAt, grab, done;
if (glitch) {
    var down = track([[PRESS, stalled], [PRESS + 0.4, 0]]);
    pose = poses([[0, WORK], [1.0, WORK], [1.4, plate], [PRESS, pressed], [PRESS + 0.4, handOn([975, 354], 40)], [2.3, TAKE]].concat(away(2.3)));
    boomAt = function (t) { return t < STALL ? retract(t) : t < PRESS ? stalled : down(t); };
    grab = 2.3;
    done = 4.5;
} else if (failure) {
    pose = poses([[0, WORK], [1.0, WORK], [1.4, plate], [PRESS, pressed], [1.6, plate], [1.7, pressed], [1.8, plate],
                  [2.0, plate]].concat(away(2.0)));
    boomAt = function (t) { return t < STALL ? retract(t) : stalled; };
    grab = 99;
    done = 4.2;
} else {
    pose = poses([[0, WORK], [1.0, WORK], [1.5, TAKE]].concat(away(1.5)));
    boomAt = retract;
    grab = 1.5;
    done = 3.7;
}
walk(pose);
carry(pose, grab);
boom(boomAt);
fade("e-lamp", function (t) { return t < 0.3 ? 1 : 0; });
hatch(-1, done);
