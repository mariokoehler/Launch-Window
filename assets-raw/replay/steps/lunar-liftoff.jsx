// Lunar liftoff: on the surface, the ascent engine lights at 0.6 and the ascent stage lifts off the
// descent stage, which stays behind as its launch pad, flakes of its foil blown off; it climbs away,
// leaning onto its way, and is gone by 3.
// Glitch: the engine sputters at 0.6 and only lights properly at 1.6; it lifts off late.
// Failure: the same sputter, it lights and lifts at 1.6, and at 2.4, low over the surface, it dies: the
// ascent stage falls back and hits the ground beside the descent stage.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIGHT = 0.6, LATE = 1.6, DIES = 2.4;

put("paper", "screen");
put("n-ground");
put("n-rocks");
lander();
put("n-dust");
for (var i = 1; i <= 4; i++) put("r-chip-" + i);

var go = glitch || failure ? LATE : LIGHT + 0.1;
// The climb, up from the descent stage, leaning right; the failure's fall from where it died.
var rise = function (t) { var u = Math.max(0, t - go); return [30 * u * u, -(60 * u + 170 * u * u)]; };
var lean = function (t) { var u = Math.max(0, t - go - 0.4); return 14 * Math.min(u, 1.2); };
// The failure's fall: from where the engine died, still rising a little, then down to its side on the
// ground beside the descent stage. It turns 70 degrees about the lander's pivot (LC, below it), so it
// rests on its side at the offset REST.
var HIT = 99, at = rise(DIES), REST = [191, 99];
var fall = function (t) {
    var v = t - DIES, up = (60 + 340 * (DIES - go)) * v - 400 * v * v;
    return at[1] - up;
};
if (failure) {
    for (var f = 0; f <= NFRAMES; f++) { if (f / FPS > DIES + 0.05 && fall(f / FPS) >= REST[1]) { HIT = f / FPS; break; } }
}
var ascent = function (t) {
    if (!failure || t <= DIES) return rise(t);
    var w = clamp((t - DIES) / (HIT - DIES), 0, 1);
    return [at[0] + (REST[0] - at[0]) * w, t >= HIT ? REST[1] : fall(t)];
};
move("n-ascent", ascent);
move("n-flame-up", ascent);
var tilt = function (t) { return failure && t > DIES ? lean(DIES) + 70 * clamp((t - DIES) / (HIT - DIES), 0, 1) : lean(t); };
turn("n-ascent", tilt);
turn("n-flame-up", tilt);
// The engine: a sputter (glitch and failure) from 0.6, then burning until it dies.
var flame = function (t) {
    if (t < LIGHT) return 0;
    if ((glitch || failure) && t < LATE) return Math.floor(t * 9) % 3 === 0 ? 0.6 : 0;
    if (failure && t >= DIES) return 0;
    return 1;
};
fade("n-flame-up", function (t) { return flame(t) > 0 ? 1 : 0; });
grow("n-flame-up", function (t) { var k = Math.max(flame(t), 0.01); return [k, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)) * (1 + clamp((t - go) * 2, 0, 1.5))]; });
// Foil flakes blown off the descent stage's top as it lifts, and dust where it hits.
for (i = 1; i <= 4; i++) {
    (function (n, k) {
        var dir = [[-1, -0.6], [1, -0.8], [-0.7, -1], [0.8, -0.4]][k];
        move(n, function (t) { var u = Math.max(0, t - go); return [LC[0] - 962 + (k - 1.5) * 30 + 260 * dir[0] * u, LC[1] - 40 - 466 + 200 * dir[1] * u + 300 * u * u]; });
        turn(n, function (t) { return 400 * Math.max(0, t - go) * (k % 2 ? 1 : -1); });
        fade(n, function (t) { return t >= go && t < go + 1.2 ? 1 - (t - go) / 1.2 : 0; });
        grow(n, function () { return 0.6; });
    })("r-chip-" + i, i - 1);
}
if (failure) {
    move("n-dust", function () { return [REST[0] + 69, 0]; });
    fade("n-dust", function (t) { return t >= HIT && t < HIT + 1.2 ? 1 - (t - HIT) / 1.2 : 0; });
    grow("n-dust", function (t) { return 0.8 + 0.6 * clamp((t - HIT) / 0.6, 0, 1); });
} else {
    hide("n-dust");
}
