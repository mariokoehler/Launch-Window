// The X-15's high-speed run (2026-10-17): low and level over the clouds, its engine burning, the air streaming past
// fast, a shock cone off its nose, its nose and leading edges glowing hot as it goes on accelerating. (William Knight
// flew it to Mach 6.7 on 3 October 1967; the heat burned through the dummy ramjet slung under its tail.)
// Glitch: at 2.2 a hot spot flares on its belly; the pilot throttles back (2.6) and it fades by 3.6.
// Failure: the same hot spot, and at 2.8 the lower fin under its tail tears away; the engine is shut down and it slows.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var HOT = 2.2, BACK = 2.6, TEAR = 2.8;

x15Scene();
move("d-earth", function () { return [0, 60]; });
turn("d-earth", function (t) { return -0.4 * t; });
for (var i = 1; i <= 6; i++) fade("r-cloud-" + i, function () { return 1; });
// The same until the hot spot; then on faster, eased back (glitch) or slowing once the engine's off (failure).
var speed = failure ? track([[0, 2000], [HOT, 2200], [TEAR, 2260], [TEAR + 1.5, 900]])
          : glitch ? track([[0, 2000], [HOT, 2200], [BACK, 2250], [BACK + 1.0, 1900]]) : track([[0, 2000], [HOT, 2200], [DUR, 2500]]);
clouds(function () { return [0, 0]; }, 1500);
streaks(speed);
var pitch = function (t) { return -2 + 1.2 * Math.sin(2 * Math.PI * 0.5 * t) - (failure ? 8 * EASE.inout(clamp((t - TEAR) / 1.2, 0, 1)) : 0); };
var off = function () { return [0, 0]; };
x15Fly(pitch, off);
ventral(pitch, off, failure ? TEAR : 99, function (u) { return [-420 * u - 200 * u * u, 120 * u + 300 * u * u, -260 * u]; });
var burning = function (t) { return failure ? t < TEAR + 0.1 : true; };
fade(K.flame, function (t) { return burning(t) ? 1 : 0; });
grow(K.flame, function (t) { var k = glitch && t > BACK ? 1.1 : 1.8; return [k, 1 + 0.1 * Math.sin(2 * Math.PI * 11 * t)]; });
fade(K.shock, function () { return 1; });
fade(K.heat, function (t) {
    var h = clamp((t - 0.3) / 2.0, 0, 1);
    if (failure && t > TEAR) h *= Math.max(0.3, 1 - (t - TEAR) / 1.5);
    return h * (0.85 + 0.15 * Math.sin(2 * Math.PI * 7 * t));
});
if (glitch || failure) {
    fade(K.hot, function (t) {
        if (t < HOT) return 0;
        var h = clamp((t - HOT) / 0.3, 0, 1);
        if (glitch) h *= 1 - clamp((t - 3.0) / 0.6, 0, 1);
        if (failure && t > TEAR) h *= Math.max(0, 1 - (t - TEAR) / 0.4);
        return h;
    });
    grow(K.hot, function (t) { return 0.6 + 0.8 * clamp((t - HOT) / 0.5, 0, 1); });
}
