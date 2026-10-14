// Reentry: the capsule meets the air base first. The Earth's limb comes up and is gone; the capsule
// turns its heat shield into its way down (225 by 2.0), a glow builds at its base and the air
// streams past (peak 2.2 to 3.4). The glow dies by 4.6, it turns base down (270 by 5.4), and the
// clouds come up from below.
// Glitch: it starts to oscillate at 2.4, the glow spilling up its side, and is steady again by 3.9.
// Failure: the same oscillation, but it grows: at 2.9 the capsule goes sideways into the glow and
// breaks up at 3.4.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var BREAK = 3.4;

put("paper", "screen");
put("d-earth");
for (var i = 1; i <= 6; i++) put("r-cloud-" + i);
for (i = 1; i <= 8; i++) put("r-streak-" + i);
put("r-carrier");
var parts = ["r-glow-side", "r-glow", "r-capsule"];
for (i = 0; i < parts.length; i++) put(parts[i], "r-carrier");
for (i = 1; i <= 4; i++) put("r-chip-" + i);
put("r-half-nose");
put("r-half-body");
hide("r-half-nose"); hide("r-half-body");
put("burst");
for (i = 1; i <= 4; i++) hide("r-chip-" + i);
hide("burst");

// The limb, from where the jettison left it (231 px up), comes up and fades: the capsule is in the air.
move("d-earth", function (t) { return [0, -231 - 50 * t - 60 * t * t]; });
fade("d-earth", track([[0.2, 1], [1.1, 0, "lin"]]));
turn("d-earth", function (t) { return -4.176 - 0.25 * t; });

// How hot: the glow and the streaks.
var heat = failure ? track([[0.6, 0], [2.2, 1], [BREAK, 1]]) : track([[0.6, 0], [2.2, 1], [3.4, 1], [4.6, 0]]);
// The oscillation: the same in glitch and failure until 2.8; the glitch's dies away by 3.9, the
// failure's grows into a turn sideways.
var osc = function (t) {
    if (!(glitch || failure) || t < 2.4) return 0;
    var a = 20 * Math.min(1, (t - 2.4) / 0.3);
    if (glitch) a *= clamp((3.9 - t) / 0.9, 0, 1);
    return a * Math.sin(2 * Math.PI * 1.4 * (t - 2.4));
};
var sideways = failure ? track([[2.8, 0], [BREAK, 85, "in"]]) : function () { return 0; };
var attitude = track([[0.3, 180], [2.0, 225], [4.0, 225], [5.4, 270]]);
function angle(t) { return attitude(Math.min(t, failure ? BREAK : 99)) + osc(Math.min(t, failure ? BREAK : 99)) + sideways(t); }
turn("r-carrier", angle);

fade("r-glow", function (t) { return failure && t >= BREAK ? 0 : heat(t); });
grow("r-glow", function (t) { var k = 0.7 + 0.3 * heat(t); return [k * (1 + 0.05 * Math.sin(2 * Math.PI * 9 * t)), k]; });
// The glow spills up the side when the capsule swings off its heat shield.
fade("r-glow-side", function (t) {
    if (failure && t >= BREAK) return 0;
    var off = Math.abs(osc(t)) / 20 + sideways(t) / 60;
    return heat(t) * clamp(off, 0, 1);
});
if (failure) {
    fade("r-capsule", function (t) { return t < BREAK ? 1 : 0; });
    move("burst", function () { return [0, -130]; });
    grow("burst", track([[BREAK, 0.3], [BREAK + 0.35, 2.4, "out"], [BREAK + 1.0, 2.8, "lin"]]));
    fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
    // The capsule comes apart at its seam: the nose cone and the body tumble away from each other.
    // [x, y speed, spin, where its pivot sits along the capsule from its centre (parts.py)]
    var halves = {"r-half-nose": [-260, -120, 260, 32], "r-half-body": [120, 160, -200, -92]};
    for (var h in halves) {
        (function (n, v) {
            fade(n, function (t) { return t >= BREAK ? 1 : 0; });
            // From where the seam was, the capsule's own direction then apart along it.
            move(n, function (t) {
                var u = Math.max(0, t - BREAK), a = angle(BREAK) * RAD, d = v[3];
                // Its pivot turned with the capsule about the capsule's centre, then thrown.
                return [d * Math.cos(a) - d + v[0] * u, d * Math.sin(a) + v[1] * u + 60 * u * u];
            });
            turn(n, function (t) { return angle(BREAK) + v[2] * Math.max(0, t - BREAK); });
        })(h, halves[h]);
    }
    // The small pieces stream away behind it, up and to the left, tumbling.
    var flings = [[-420, -260, 300], [-300, -420, -380], [-520, -120, 240], [-200, -320, -260]];
    for (i = 1; i <= 4; i++) {
        (function (n, v) {
            fade(n, function (t) { return t >= BREAK && t < BREAK + 2.2 ? 1 - Math.max(0, t - BREAK - 1.6) / 0.6 : 0; });
            move(n, function (t) { var u = Math.max(0, t - BREAK); return [v[0] * u, v[1] * u]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - BREAK); });
        })("r-chip-" + i, flings[i - 1]);
    }
}

// The air streaming past: from ahead of the heat shield, back past the nose, along the capsule's
// axis; each streak loops every 0.5 s at its own place across the flow.
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        var lane = (k % 2 ? 1 : -1) * (40 + 30 * k), phase = k * 0.13;
        function at(t) {
            var a = angle(Math.min(t, failure ? BREAK : 99)) * RAD, u = ((t + phase) % 0.5) / 0.5;
            var along = -260 + 700 * u;
            // The nose's way is the carrier's x; across is its y.
            return [along * Math.cos(a) - lane * Math.sin(a), along * Math.sin(a) + lane * Math.cos(a)];
        }
        move(n, function (t) { var p = at(t); return [p[0], p[1]]; });
        turn(n, function (t) { return angle(Math.min(t, failure ? BREAK : 99)); });
        fade(n, function (t) {
            var u = ((t + phase) % 0.5) / 0.5, h = failure && t > BREAK ? heat(BREAK) * Math.max(0, 1 - (t - BREAK) / 1.2) : heat(t);
            return 0.9 * h * Math.sin(Math.PI * u);
        });
    })("r-streak-" + i, i);
}

// The clouds come up from below as it slows (not after a break-up).
for (i = 1; i <= 6; i++) {
    if (failure) { hide("r-cloud-" + i); continue; }
    move("r-cloud-" + i, function (t) { return [0, 500 - 450 * Math.max(0, t - 4.4)]; });
}
camera(function () { return 0; }, function (t) { return 3 * heat(t) * (failure && t >= BREAK ? 0 : 1) + (failure && t >= BREAK && t < BREAK + 0.4 ? 8 : 0); });
