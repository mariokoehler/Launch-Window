// The drop: at 0.8 the rocket plane is let go and falls away from the carrier's wing; its engine lights at
// 1.2 and it runs on ahead, level, until it's clear of the carrier's nose, then pulls up and away (2026-10-17: it
// used to climb straight through the carrier). The carrier falls behind from the drop, and the camera eases ahead.
// Glitch: no light at 1.2; it falls on, below the carrier, and lights at 2.2 at the second try.
// Failure: no light at 1.2, a sputter at 2.2 that dies: it glides down and away, its flight over.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROP = 0.8, LIGHT = glitch || failure ? 2.2 : 1.2;

airScene(false);
// Falling free from the drop, slowing against the air (falling back a little); once lit, off ahead and up.
var fall = function (t) { var u = Math.max(0, t - DROP); return [-30 * u, 110 * u * u]; };
// Lit, it runs ahead level (PULL s, by then its tail is past the carrier's nose), then pulls up.
var PULL = 0.9;
// The carrier falls behind once it's let go.
var behind = function (t) { var u = Math.max(0, t - DROP - 0.4); return -26 * u * u; };
// Its offsets are from where it hung, which falls behind with the carrier: they take that back out.
var drop = function (t) {
    var f = fall(Math.min(t, failure ? 99 : LIGHT));
    f = [f[0] - behind(t), f[1]];
    if (failure) return [f[0], f[1], 8 * Math.max(0, t - LIGHT)];
    var u = Math.max(0, t - LIGHT), w = Math.max(0, u - PULL);
    return [f[0] + 300 * u + 500 * u * u, f[1] + 20 * Math.min(u, PULL) - 260 * w * w - 120 * w, -16 * EASE.inout(clamp(w / 0.6, 0, 1))];
};
var carrier = function (t) { return [behind(t), 4 * Math.sin(2 * Math.PI * 0.3 * t), 0]; };
fly(carrier, drop);
flow(function (t) { return [220 * EASE.inout(clamp((t - 1.2) / 2.0, 0, 1)), 0]; }, function () { return 900; });
clouds(function () { return [0, 0]; }, 260);
// The flame: on from the light; the failure's sputter dies.
fade(K.flame, function (t) {
    if (failure) return t >= LIGHT && t < LIGHT + 0.3 && Math.floor(t * 12) % 2 === 0 ? 1 : 0;
    return t >= LIGHT ? 1 : 0;
});
grow(K.flame, function (t) { return [1 + clamp((t - LIGHT) * 2, 0, 1.2), 1 + 0.1 * Math.sin(2 * Math.PI * 11 * t)]; });
// The camera eases ahead, to give the climb room.
airCamera(function (t) { return [220 * EASE.inout(clamp((t - 1.2) / 2.0, 0, 1)), 0]; });
