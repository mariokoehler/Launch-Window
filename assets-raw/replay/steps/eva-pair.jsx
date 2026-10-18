// A spacewalk on a rendezvous (2026-10-18, user): the two capsules a few metres apart, nose to nose, as the
// rendezvous leaves them. The hatch opens and the figure rises out of it and drifts up over the noses on its
// tether; the other capsule's hatch opens and its crewman stands up in it with a camera, turns it on the
// walker and takes two pictures. It ends with the walker still out (the lone spacewalk's way, eva-out.jsx).
// Glitch: at 2.7 the walker starts to tumble at the end of its tether; puffs from its hand-held gun steady it
// at 3.3, and the pictures come once it's still. Failure: the same tumble, and at 3.3 its visor fogs over:
// it's hauled back in, and the photographer lowers the camera and goes back in too.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var TUMBLE = 2.7, ANSWER = 3.3, STILL = 3.9;
var DRIFT = [110, -140, -25];

spacewalkScene(false, true, true);

// The walker: out the same way in every variant until the tumble (or not) at TUMBLE.
var out = [[0, [0, INSIDE, 0]], [0.8, [0, INSIDE, 0]], [1.4, [0, 0, 0]], [1.8, [0, 0, 0]], [2.7, [70, -95, 10]]];
var full;
if (glitch || failure) {
    var before = poses(out), at = before(TUMBLE);
    // The tumble (eva-out.jsx's): drifting up and away, spinning faster; the glitch's gun stops the spin.
    var spin = function (u) {
        if (u <= 0.6) return 160 * u + 120 * u * u;
        var v = Math.min(u - 0.6, 0.6);
        return glitch ? 139 + 304 * v - 253 * v * v : 139 + 304 * (u - 0.6) - 90 * Math.pow(u - 0.6, 2);
    };
    var tumble = function (t) {
        var u = t - TUMBLE, k = Math.min(u, 1.2);
        return [at[0] + 40 * k - 14 * k * k, at[1] - 60 * k + 20 * k * k, at[2] + spin(u)];
    };
    if (glitch) {
        var on = poses([[STILL, tumble(STILL)], [5.6, [DRIFT[0], DRIFT[1], DRIFT[2] + 720]]]);
        full = function (t) { return t <= TUMBLE ? before(t) : t <= STILL ? tumble(t) : on(t); };
    } else {
        // Hauled in: the tumble eases into the pose in the hatch by 5.1, a whole turn on, and down by 5.7.
        var home = [0, 0, 720];
        full = function (t) {
            if (t <= TUMBLE) return before(t);
            if (t <= 5.1) {
                var p = tumble(t), w = EASE.inout(clamp((t - STILL) / 1.2, 0, 1));
                return [mix(p[0], home[0], w), mix(p[1], home[1], w), mix(p[2], home[2], w)];
            }
            return [0, INSIDE * EASE.inout(clamp((t - 5.1) / 0.6, 0, 1)), 720];
        };
    }
} else {
    full = poses(out.concat([[4.2, [100, -130, -10]], [5.6, DRIFT]]));
}
walk(full);
carry(full, 99);
turn("e-hatch", track([[0.3, 0], [0.9, 110, "out"]].concat(failure ? [[5.9, 110], [6.5, 0, "in"]] : [])));
if (glitch) fade("e-gun", function (t) { return t >= ANSWER && t < STILL && Math.floor(t * 10) % 2 === 0 ? 1 : 0; });
if (failure) fade("e-fog", track([[ANSWER, 0], [ANSWER + 0.3, 1, "out"]]));

// The photographer: its hatch opens at 1.2, it stands up by 2.4 and raises the camera at the walker (the
// hand turned up, AIM) by 2.7; it takes its pictures (the glitch's once the walker is still), or, in the
// failure, lowers the camera and goes back in, its hatch shut behind it.
var AIM = 12;
var bKeys = [[1.8, [0, INSIDE, 0]], [2.4, [0, 0, 0]], [2.7, [0, 0, AIM]]];
var shots = glitch ? [4.4, 5.1] : failure ? [] : [2.9, 3.6];
if (failure) bKeys = bKeys.concat([[3.6, [0, 0, AIM]], [4.1, [0, 0, 0]], [5.0, [0, 0, 0]], [5.6, [0, INSIDE, 0]]]);
var b = poses(bKeys);
move("e-suit-b", function (t) { var p = b(t); return [p[0], p[1]]; });
turn("e-suit-b", function (t) { return b(t)[2]; });
var shown = function (t) { return b(t)[1] > INSIDE - 10 ? 0 : 1; };
fade("e-suit-b", shown);
fade("e-camera-b", shown);
fade("e-flash-b", function (t) { for (var i = 0; i < shots.length; i++) if (t >= shots[i] && t < shots[i] + 0.1) return 1; return 0; });
turn("e-hatch-pair", track([[1.2, 0], [1.8, -110, "out"]].concat(failure ? [[5.8, -110], [6.4, 0, "in"]] : [])));
