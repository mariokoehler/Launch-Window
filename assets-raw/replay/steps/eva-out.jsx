// Out on the tether (2026-10-18; the lone spacewalk, and the first of two): the hatch opens, the figure
// rises out of it, pushes off and drifts up and back over the adapter, rolling slowly head over heels, and
// ends floating there (FLOAT), where the photo step begins. STANDING (eva-out-standing.jsx): it starts
// standing in the open hatch, as the stand-up ends, the timeline 1.4 s on.
// Glitch: on the way out, at 2.4, it starts to tumble at the end of its tether; at 3.0 puffs from its
// hand-held gun (Gemini 4's spacewalk had one) steady it, and it drifts on to the same place.
// Failure: the same tumble, and at 3.0 its visor fogs over (as on Gemini 9A): it's hauled back in.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var S = typeof STANDING !== "undefined" && STANDING ? 1.4 : 0;
var TUMBLE = 2.4, ANSWER = 3.0;
var FLOAT = [-60, -110, -345];

spacewalkScene(false, true);

var out = [[0, [0, INSIDE, 0]], [0.8, [0, INSIDE, 0]], [1.4, [0, 0, 0]], [1.8, [0, 0, 0]], [2.3, [25, -60, 10]]];
var full;
if (glitch || failure) {
    var before = poses(out), at = before(TUMBLE);
    // The tumble: drifting up and away, spinning faster; the glitch's gun stops the spin by 3.6.
    var spin = function (u) {
        if (u <= 0.6) return 160 * u + 120 * u * u;
        var v = Math.min(u - 0.6, 0.6);
        return glitch ? 139 + 304 * v - 253 * v * v : 139 + 304 * (u - 0.6) - 90 * Math.pow(u - 0.6, 2);
    };
    var tumble = function (t) {
        var u = t - TUMBLE, k = Math.min(u, 1.2);
        return [at[0] + 40 * k - 14 * k * k, at[1] - 60 * k + 20 * k * k, at[2] + spin(u)];
    };
    var steadied = tumble(3.6);
    if (glitch) {
        // On to the same place, the roll's angle a whole turn on from the success's (the same picture).
        var on = poses([[3.6, steadied], [5.4, [FLOAT[0], FLOAT[1], FLOAT[2] + 720]]]);
        full = function (t) { return t <= TUMBLE ? before(t) : t <= 3.6 ? tumble(t) : on(t); };
    } else {
        // Hauled in from 3.6: the tumble eases into the pose in the hatch by 4.8, a whole turn on.
        var home = [0, 0, 720];
        full = function (t) {
            if (t <= TUMBLE) return before(t);
            if (t <= 4.8) {
                var p = tumble(t), w = EASE.inout(clamp((t - 3.6) / 1.2, 0, 1));
                return [mix(p[0], home[0], w), mix(p[1], home[1], w), mix(p[2], home[2], w)];
            }
            return [0, INSIDE * EASE.inout(clamp((t - 4.8) / 0.6, 0, 1)), 720];
        };
    }
} else {
    full = poses(out.concat([[3.3, [10, -110, -90]], [4.3, [-40, -125, -250]], [5.0, FLOAT]]));
}
var pose = function (t) { return full(t + S); };
walk(pose);
carry(pose, 99);
// The hatch: open from 0.3 (already open when standing); the failure's closes behind the figure.
var hatchAt = track([[0.3, 0], [0.9, 110, "out"]].concat(failure ? [[5.6, 110], [6.2, 0, "in"]] : []));
turn("e-hatch", function (t) { return hatchAt(t + S); });
if (glitch) {
    fade("e-gun", function (t) { var u = t + S; return u >= ANSWER && u < 3.6 && Math.floor(u * 10) % 2 === 0 ? 1 : 0; });
}
if (failure) fade("e-fog", function (t) { return track([[ANSWER, 0], [ANSWER + 0.3, 1, "out"]])(t + S); });
