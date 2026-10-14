// Lunar orbit insertion: the craft falls past the Moon on a fast, open path from below, turns round
// (engine first) and fires its engine behind the Moon at 1.6, out of contact, to be captured. It
// comes out on a circular orbit.
// Glitch: the burn stutters and cuts out at 1.9; a relight (2.0 to 2.15) still captures it, on a
// wider ellipse.
// Failure: the same stutter, but no relight: it comes out still on its open path, and leaves.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.6, STUTTER = 1.9, RELIGHT = 2.0, DONE = glitch ? 2.15 : failure ? STUTTER : 2.2;

orbitScene("p-moon");
put("l-shadow");
layers["l-shadow"].moveAfter(layers["p-moon"]);
put("l-earth-far", "screen");
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
fade("o-orbit", function () { return 0.6; });

// The paths, in polar form round the Moon with their low point behind it (angle 0): r = p / (1 + e cos).
function conic(rp, e) { return function (th) { return rp * (1 + e) / (1 + e * Math.cos(th * RAD)); }; }
var open = conic(390, 1.6), wide = conic(390, 0.25), round = conic(390, 0);
// The angle, going round anticlockwise (falling), at a speed that keeps equal areas: on the open path
// up to the burn (its low point at 1.6), then on whichever path the burn left it.
var after = failure ? open : glitch ? wide : round;
// The open path's pace puts the craft at 60 degrees (low on the right, in view) at the start.
var H_OPEN = 50 * 390 * 390, H_AFTER = failure ? H_OPEN : glitch ? 61.5 * 390 * 390 : 55 * 390 * 390;
var angle = (function () {
    var a = [], th = 0, f, n = NFRAMES + 2, k = Math.round(FIRE * FPS);
    a[k] = 0;
    // Back from the burn to the start, on the open path.
    for (f = k; f > 0; f--) { var r = open(th); th += H_OPEN / (r * r) / FPS; a[f - 1] = th; }
    // On from the burn: the open path's speed until the burn's done, then the new path's.
    th = 0;
    for (f = k; f < n; f++) {
        var t = f / FPS, path = t < DONE ? open : after, h = t < DONE ? H_OPEN : H_AFTER, rr = path(th);
        a[f + 1] = th - h / (rr * rr) / FPS;
        th = a[f + 1];
    }
    return function (t) { return a[clamp(Math.round(t * FPS), 0, a.length - 1)]; };
})();
function pathAt(t) { return t < DONE ? open : after; }
function where(t) { var th = angle(t), r = pathAt(t)(th); return [OCX + r * Math.cos(th * RAD), OCY + r * Math.sin(th * RAD)]; }
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
// Engine first for the burn: it turns round from 0.8 to 1.4, and back once it's done.
var flip = track([[0.8, 0], [1.4, 180], [DONE + 0.3, 180], [DONE + 0.9, 0]]);
turn("o-sat", function (t) { return angle(t) + flip(t); });

function burning(t) {
    if (t < FIRE) return false;
    if (glitch || failure) {
        if (t < STUTTER) return jitter(t, 3) > -0.3;
        return glitch && t >= RELIGHT && t < DONE;
    }
    return t < DONE;
}
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });

// Home: a ring every 0.75 s, none from behind the Moon.
radioRingsAt(sendTimes(function (t) { return inShadow(where(t)); }), where);
