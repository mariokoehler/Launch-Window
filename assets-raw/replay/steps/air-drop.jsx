// The drop: at 0.8 the rocket plane is let go and falls away from the carrier's wing; its engine lights at
// 1.2 and it shoots ahead and up, out of sight, leaving the carrier behind.
// Glitch: no light at 1.2; it falls on, below the carrier, and lights at 2.2 at the second try.
// Failure: no light at 1.2, a sputter at 2.2 that dies: it glides down and away, its flight over.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROP = 0.8, LIGHT = glitch || failure ? 2.2 : 1.2;

airScene(false);
var carrier = function (t) { return [0, 4 * Math.sin(2 * Math.PI * 0.3 * t), 0]; };
// Falling free from the drop, slowing against the air (falling back a little); once lit, off ahead and up.
var fall = function (t) { var u = Math.max(0, t - DROP); return [-30 * u, 110 * u * u]; };
var drop = function (t) {
    var f = fall(Math.min(t, failure ? 99 : LIGHT));
    if (failure) return [f[0], f[1], 8 * Math.max(0, t - LIGHT)];
    var u = Math.max(0, t - LIGHT);
    return [f[0] + 260 * u + 300 * u * u, f[1] - 60 * u - 120 * u * u, -12 * clamp(u / 0.8, 0, 1)];
};
fly(carrier, drop);
flow(carrier, function () { return 900; });
clouds(function () { return [0, 0]; }, 260);
// The flame: on from the light; the failure's sputter dies.
fade("w-flame", function (t) {
    if (failure) return t >= LIGHT && t < LIGHT + 0.3 && Math.floor(t * 12) % 2 === 0 ? 1 : 0;
    return t >= LIGHT ? 1 : 0;
});
grow("w-flame", function (t) { return [1 + clamp((t - LIGHT) * 2, 0, 1.2), 1 + 0.1 * Math.sin(2 * Math.PI * 11 * t)]; });
airCamera(function () { return [0, 0]; });
