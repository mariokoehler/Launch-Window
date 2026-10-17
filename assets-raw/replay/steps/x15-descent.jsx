// The X-15's descent (2026-10-17): unpowered, nose down, it comes back into the air and down through the clouds, the limb
// rising as it falls, and pulls out towards the lakebed; at 3.6 it drops the lower fin under its tail, which comes down
// on a small chute of its own (the skid it lands on is under there).
// Glitch: from 1.8 its nose starts to bob, faster and wider; the damper catches it from 2.4 and it's steady by 3.2.
// Failure: the bobbing grows on, and at 2.9 the loads break it up (as Mike Adams's X-15 broke up on its way down from
// its spin on 15 November 1967).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var BOB = 1.8, DAMP = 2.4, BREAK = 2.9, DROP = glitch ? 4.0 : 3.6;

x15Scene();
put("r-main");
hide("r-main");
put("burst");
for (var i = 1; i <= 4; i++) put("r-chip-" + i);
hide("burst");
for (i = 1; i <= 4; i++) hide("r-chip-" + i);
move("d-earth", function (t) { return [0, 220 - 90 * t]; });
turn("d-earth", function (t) { return -0.2 * t; });
// The clouds come up from below and stream back.
for (i = 1; i <= 6; i++) {
    (function (n, k) {
        var L = layers[n].rest;
        move(n, function (t) {
            // On the Earth below its limb (whose top is at 1015 - 90 t), rising with it.
            var x = ((k * 470 - 700 * t) % 2800 + 2800) % 2800 - 1100, y = 1015 - 90 * t + 50 + 80 * (k % 3);
            return [x - L[0] + 960, y - L[1]];
        });
        fade(n, function () { return 1; });
    })("r-cloud-" + i, i);
}
streaks(function (t) { return 1500 - 120 * t; });
var bob = function (t) {
    if (!(glitch || failure) || t < BOB) return 0;
    var a = 5 + 6 * (t - BOB);
    if (glitch) a *= clamp(1 - (t - DAMP) / 0.8, 0, 1);
    return a * Math.sin(2 * Math.PI * 2.2 * (t - BOB));
};
var gone = function (t) { return failure && t >= BREAK; };
var pitch = function (t) { return 18 - 14 * EASE.inout(clamp((t - 0.6) / 3.0, 0, 1)) + bob(Math.min(t, failure ? BREAK : 99)); };
var off = function () { return [0, 0]; };
x15Fly(pitch, off);
var vat = ventral(pitch, off, failure ? 99 : DROP, function (u) {
    return [-260 * u - 60 * u * u, 70 * u + 160 * u * u * Math.max(0, 1 - u / 1.2), 40 * Math.min(u, 0.6)];
});
if (failure) {
    // Parenting doesn't pass on opacity: every part goes.
    var parts = ["w-plane", "x15-ventral", "x15-shock", "x15-heat", "x15-hot", "x15-rcs-up", "x15-rcs-down"];
    for (i = 0; i < 2; i++) fade(parts[i], function (t) { return gone(t) ? 0 : 1; });
    var c = onPlane(pitch(BREAK), [0, 0], [980, 820]);
    move("burst", function () { return [c[0] - 960, c[1] - 600]; });
    grow("burst", track([[BREAK, 0.6], [BREAK + 0.35, 2.4, "out"], [BREAK + 1.0, 2.8, "lin"]]));
    fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
    var flings = [[-520, -160, 300], [-380, 220, -380], [-200, -320, 240], [-640, 60, -260]];
    for (i = 1; i <= 4; i++) {
        (function (n, v) {
            fade(n, function (t) { return t >= BREAK && t < BREAK + 2.2 ? 1 - Math.max(0, t - BREAK - 1.6) / 0.6 : 0; });
            move(n, function (t) { var u = Math.max(0, t - BREAK); return [c[0] - 962 + v[0] * u, c[1] - 466 + v[1] * u + 120 * u * u]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - BREAK); });
            grow(n, function () { return 1.6; });
        })("r-chip-" + i, flings[i - 1]);
    }
} else {
    // The ventral's chute opens above it 0.4 s after it drops (r-main's lines meet at its pivot, 960 322).
    move("r-main", function (t) { var p = vat(t); return [p[0] - 960, p[1] - 322 - 24]; });
    grow("r-main", track([[DROP + 0.4, 0.04], [DROP + 0.7, 0.2, "out"]]));
    fade("r-main", function (t) { return t >= DROP + 0.4 ? 1 : 0; });
}
