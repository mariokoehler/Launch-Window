// High-orbit injection (2026-10-18; circumlunar Gemini's heat-shield test, user): in the Earth orbit scene, the
// camera pulling back in the first second to fit the long ellipse planned (dashed), the craft fires at 1.0, and
// the burn, done at 2.6, puts it on that ellipse: it climbs away towards the high point, slowing, still the
// Earth's (high-orbit-scene.jsx).
// Glitch: the burn cuts out at 1.6 and relights at 2.2; it reaches the same ellipse late, by 3.2.
// Failure: the same cut, and the relight dies at 2.6: the burn falls short, and the craft climbs a much smaller
// ellipse, well inside the planned one.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.0, CUT = 1.6, RELIGHT = 2.2, DIES = 2.6, LATE = 3.2;

hoScene(true);
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];

function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < HO_END;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    return glitch ? t < LATE : t < DIES;
}
// Round the circular orbit until the burn ends, quicker while it burns (55 degrees a second, up to the
// ellipse's 67 at its low point).
var angle = orbitAngles(OSTART, function (t) {
    var b = 0;
    for (var s = FIRE; s < t; s += 1 / FPS) if (burning(s)) b += 1 / FPS;
    return 55 + 7.5 * Math.min(b, 1.6);
});
var BIG = kepler(HO_RP, HO_RA), PERI = angle(HO_END);
fade("o-ellipse", function () { return 1; });
turn("o-ellipse", function () { return PERI - 90; });
var big = function (t) { return onEllipse(BIG, PERI, t - HO_END); };
var round = function (t) { return orbitWhere(angle, null, t); };
var where;
if (glitch || failure) {
    // From the relight it eases onto the planned ellipse, where the success is, the glitch by LATE; the
    // failure's burn dies at DIES, and from there it climbs a smaller one.
    var eased = function (t) {
        if (t <= RELIGHT) return round(t);
        var w = EASE.inout(clamp((t - RELIGHT) / (LATE - RELIGHT), 0, 1)), a = round(t), b = big(Math.max(t, HO_END));
        return [mix(a[0], b[0], w), mix(a[1], b[1], w)];
    };
    if (glitch) {
        where = function (t) { return t < LATE ? eased(t) : big(t); };
    } else {
        var p = eased(DIES), rp = Math.sqrt(Math.pow(p[0] - OCX, 2) + Math.pow(p[1] - OCY, 2));
        var small = kepler(rp, 650), peri = Math.atan2(p[1] - OCY, p[0] - OCX) / RAD;
        where = function (t) { return t < DIES ? eased(t) : onEllipse(small, peri, t - DIES); };
    }
} else {
    where = function (t) { return t < HO_END ? round(t) : big(t); };
}
flyAlong(where);
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
