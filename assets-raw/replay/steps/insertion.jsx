// Orbit insertion: the last burn of the climb. In the Earth-orbit scene, the craft coasts up from the
// ascent (2026-10-16): too slow to stay up, it rises on its arc, slowing, nose up the slope, to the
// arc's top on its planned orbit (dashed) at 1.6. There it fires along its way (to 2.4) and speeds up
// to the orbit's pace (circularizing), then goes round on it as the Earth orbit does.
// Glitch: the burn cuts out at 1.95, short of the orbit's speed: it sags below the orbit until the
// relight at 2.7 finishes the burn and brings it back up.
// Failure: the same cut, and the relight only sputters (2.7 to 2.85): it falls back, faster and
// faster, glows as it hits the air and burns up.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.6, BURN_LEN = 0.8, CUT = 1.95, RELIGHT = 2.7, SPUTTER = 2.85, R0 = 290, AIR = 262;
// Degrees a second round the Earth: at the arc's top, and on the orbit.
var W_TOP = 41, W_ORBIT = 55;

orbitScene();
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
put("burst");
hide("burst");
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
turn("o-earth", function (t) { return -0.25 * t; });

function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < FIRE + BURN_LEN;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    if (glitch) return t < RELIGHT + BURN_LEN - (CUT - FIRE);
    return t < SPUTTER && jitter(t, 6) > 0.1;
}
// Burn seconds so far: the share of the speed it needs that the burn has given it.
var spent = [], b = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { spent.push(b); if (burning(f / FPS) && !(failure && f / FPS >= RELIGHT)) b += 1 / FPS; }
function gained(t) { return clamp(spent[clamp(Math.round(t * FPS), 0, spent.length - 1)] / BURN_LEN, 0, 1); }

// Its height: up the arc, slowing (a ballistic top at FIRE); then the orbit, or a sag after the cut
// (the glitch's back up once the relight is done; the failure's on down).
var BACK = RELIGHT + BURN_LEN - (CUT - FIRE);
var radius = function (t) {
    if (t < FIRE) return OR - (OR - R0) * Math.pow(1 - t / FIRE, 2);
    if (!(glitch || failure) || t < CUT) return OR;
    var sag = 45 * Math.pow((t - CUT) / (RELIGHT - CUT), 2);
    if (glitch && t > RELIGHT) sag = 45 * (1 - EASE.inout(clamp((t - RELIGHT) / (BACK + 0.3 - RELIGHT), 0, 1)));
    return Math.max(OR - sag, AIR - 1);
};
var BURNUP = failure ? (function () { for (var f = 0; f <= NFRAMES; f++) if (radius(f / FPS) < AIR) return f / FPS; return 99; })() : 99;
// Round at the pace its speed gives at its height (angular momentum kept: faster lower).
var phi = orbitAngles(205, function (t) {
    var r = radius(t);
    return (W_TOP + (W_ORBIT - W_TOP) * gained(t)) * Math.pow(OR / r, 2);
});
move("o-sat", function (t) { var p = orbitWhere(phi, radius, Math.min(t, BURNUP)); return [p[0] - OCX, p[1] - OCY]; });
// Nose along its way: the tangent, tilted out while it climbs and in while it falls.
turn("o-sat", function (t) {
    var u = Math.min(t, BURNUP), dt = 1 / FPS;
    var vr = (radius(u + dt) - radius(Math.max(0, u - dt))) / (u + dt - Math.max(0, u - dt));
    var vt = radius(u) * Math.abs(phi(u + dt) - phi(u)) / dt * RAD;
    return phi(u) + Math.atan2(vr, vt) / RAD;
});
// The flame while it burns; in the failure, the glow as it hits the air.
fade("o-streak", function (t) { return burning(t) || (failure && t > 3.1 && t < BURNUP) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
if (failure) {
    fade("o-sat", track([[BURNUP, 1], [BURNUP + 0.3, 0, "lin"]]));
    var end = orbitWhere(phi, radius, Math.min(BURNUP, DUR));
    move("burst", function () { return [end[0] - 960, end[1] - 600]; });
    grow("burst", track([[BURNUP, 0.2], [BURNUP + 0.35, 0.9, "out"], [BURNUP + 1.0, 1.1, "lin"]]));
    fade("burst", track([[BURNUP, 0, "snap"], [BURNUP + 0.02, 1, "snap"], [BURNUP + 0.4, 1], [BURNUP + 1.0, 0, "lin"]]));
}
