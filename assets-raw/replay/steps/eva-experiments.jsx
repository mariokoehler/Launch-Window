// The experiments (2026-10-18; the middle of three spacewalk steps): on from the setup's last frame, the
// figure reaches up to the collector plate as micrometeoroids spark on it, the package's lamp on while it
// records, then goes back to where it works (WORK), where the teardown begins.
// Glitch: the lamp goes out at 1.4; the hand goes to the package and taps it at 2.0, and it comes back on.
// Failure: two taps and it stays dark: the package has stopped recording.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DARK = 1.4, TAP = 2.0;

spacewalkScene(false, true);
boom(function () { return 1; });

var read = handOn([956, 318], 15), tap = handOn([975, 358], 50);
var tapped = [tap[0] + 3, tap[1] + 4, tap[2]];
var start = [[0, WORK], [0.4, WORK], [1.0, read]];
var pose, lampOn, hits = [[0.7, -14, -2], [1.2, 12, 3]];
if (glitch) {
    pose = poses(start.concat([[1.7, read], [1.95, tap], [2.05, tapped], [2.2, tap], [2.6, read], [3.6, read], [4.2, WORK]]));
    lampOn = function (t) { return t < DARK || t >= TAP + 0.1; };
    hits = hits.concat([[2.7, 4, -3], [3.3, -8, 2]]);
} else if (failure) {
    pose = poses(start.concat([[1.7, read], [1.95, tap], [2.05, tapped], [2.2, tap], [2.45, tap], [2.55, tapped],
                               [2.7, tap], [3.4, tap], [4.0, WORK]]));
    lampOn = function (t) { return t < DARK; };
} else {
    pose = poses(start.concat([[3.6, read], [4.2, WORK]]));
    lampOn = function () { return true; };
    hits = hits.concat([[1.9, 4, -3], [2.6, -8, 2], [3.3, 16, -1]]);
}
walk(pose);
carry(pose, 99);
sparks(hits);
fade("e-lamp", function (t) { return lampOn(t) ? 1 : 0; });
hatch(-1, 99);
