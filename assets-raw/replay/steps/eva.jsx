// Spacewalk: the hatch opens, the figure rises out of it on its tether, floats over to the experiment
// package on the hull, takes it at 2.3 and brings it back in; the hatch closes.
// Glitch: on the way out, at 2.0, it starts to tumble at the end of its tether; at 2.6 puffs from its
// hand-held gun (Gemini 4's spacewalk had one) steady it, and it takes the package after all, late.
// Failure: the same tumble, and at 2.6 its visor fogs over (as on Gemini 9A): it's hauled back in on the
// tether, the package left behind.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var TUMBLE = 2.0, ANSWER = 2.6, BOX = [975, 360];

spacewalkScene();

var out = [[0, [0, INSIDE, 0]], [0.8, [0, INSIDE, 0]], [1.4, [0, 0, 0]], [1.9, [50, -70, 25]], [2.3, handOn(BOX, 60)]];
var pose, grab = 99;
if (glitch || failure) {
    var before = poses(out), at = before(TUMBLE);
    // The tumble: drifting up and away, spinning faster; the glitch's gun stops the spin by 3.2.
    var spin = function (u) {
        if (u <= 0.6) return 160 * u + 120 * u * u;
        var v = Math.min(u - 0.6, 0.6);
        return glitch ? 139 + 304 * v - 253 * v * v : 139 + 304 * (u - 0.6) - 90 * Math.pow(u - 0.6, 2);
    };
    var tumble = function (t) {
        var u = t - TUMBLE, k = Math.min(u, 1.2);
        return [at[0] + 40 * k - 14 * k * k, at[1] - 60 * k + 20 * k * k, at[2] + spin(u)];
    };
    var steadied = tumble(3.2);
    if (glitch) {
        grab = 3.9;
        var back = poses([[3.2, steadied], [3.9, handOn(BOX, 420)], [4.0, handOn(BOX, 420)], [4.6, [30, -70, 380]],
                          [5.2, [0, 0, 360]], [5.8, [0, INSIDE, 360]]]);
        pose = function (t) { return t <= TUMBLE ? before(t) : t <= 3.2 ? tumble(t) : back(t); };
    } else {
        // Hauled in from 3.2: the tumble eases into the pose in the hatch by 4.4, a whole turn on.
        var home = [0, 0, 720];
        pose = function (t) {
            if (t <= TUMBLE) return before(t);
            if (t <= 4.4) {
                var p = tumble(t), w = EASE.inout(clamp((t - 3.2) / 1.2, 0, 1));
                return [mix(p[0], home[0], w), mix(p[1], home[1], w), mix(p[2], home[2], w)];
            }
            return [0, INSIDE * EASE.inout(clamp((t - 4.4) / 0.6, 0, 1)), 720];
        };
    }
} else {
    grab = 2.3;
    pose = poses(out.concat([[2.4, handOn(BOX, 60)], [3.0, [30, -70, 20]], [3.6, [0, 0, 0]], [4.4, [0, INSIDE, 0]]]));
}
walk(pose);
carry(pose, grab);
hatch(0.3, glitch ? 6.0 : failure ? 5.2 : 4.6);
if (glitch) {
    fade("e-gun", function (t) { return t >= ANSWER && t < 3.2 && Math.floor(t * 10) % 2 === 0 ? 1 : 0; });
}
if (failure) fade("e-fog", track([[ANSWER, 0], [2.9, 1, "out"]]));
