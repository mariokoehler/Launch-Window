// The touchdown under a full main and the recovery after it (GROUND, set by the step's own file first:
// the ground or the sea), on from the descent's last frame (under the main, the camera 200 px down,
// the sky clear). The ground comes up and it touches down at 2.4; a recovery helicopter comes in
// from the right at 3.2 and hovers over it.
// Glitch: a gust from 1.0 swings it, and it comes down hard at 2.6: on the ground it tips onto its
// side, at sea it splashes harder and lists, then rights itself.
// Failure: the same gust, and at 1.4 the main tears loose: it falls fast and hits at 2.2, crumpling
// (ground) or going under (sea).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var GUST = 1.0, TEAR = 1.4, SPLASH = failure ? 2.2 : glitch ? 2.6 : 2.4, HELI = 3.2;

put("paper", "screen");
put(GROUND ? "r-ground" : "r-sea");
put("r-main");
put("r-carrier");
put("r-capsule-up", "r-carrier");
if (GROUND) {
    put("r-dust");
    for (var i = 1; i <= 3; i++) put("r-chip-" + i);
} else {
    put("y-sea-front");
    for (i = 1; i <= 3; i++) put("r-ring-" + i);
    put("r-splash");
}
// A soft-landing rocket in the chute's lines (SOFT, project-voskhod.jsx; 2026-10-18): a flash under the base
// just before it touches; the glitch's fires too high (hence its hard landing), the failure's torn chute takes
// it away.
if (typeof SOFT !== "undefined" && SOFT && !failure) {
    put("r-soft", "r-carrier");
    var FIRE = glitch ? SPLASH - 0.7 : SPLASH - 0.15;
    fade("r-soft", function (t) { return t >= FIRE && t < FIRE + 0.25 ? 1 : 0; });
}
put("y-heli");
put("y-rotor", "y-heli");
camera(function () { return 200; }, function (t) { return t >= SPLASH && t < SPLASH + 0.3 ? (failure ? 10 : glitch ? 6 : 3) : 0; });

// The fall: slow under the main, faster once it's torn loose.
var speed = failure ? track([[TEAR, 60], [TEAR + 0.4, 420]]) : function () { return 60; };
var fallen = [], y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
function fall(t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; }
// The ground line on screen: from below the frame up to the capsule's base (818) at the touchdown.
var lineAt = function (t) { return 818 + (fall(SPLASH) - fall(Math.min(t, SPLASH))) * (failure ? 1.2 : 4.0); };
move(GROUND ? "r-ground" : "r-sea", function (t) { return [0, lineAt(t) - 200 - 1100]; });
if (!GROUND) {
    move("y-sea-front", function (t) { return [0, lineAt(t) - 200 - 1100]; });
    fade("y-sea-front", function () { return 1; });
}

// The gust's swing, the same in glitch and failure until the tear.
var swing = glitch || failure ? function (t) {
    if (t < GUST) return 0;
    var a = 14 * Math.sin(2 * Math.PI * 0.9 * (t - GUST)) * Math.min(1, (t - GUST) / 0.3);
    return glitch ? a * clamp((SPLASH - t) / 1.2, 0, 1) : t < TEAR ? a : a * clamp(1 - (t - TEAR) / 0.4, 0, 1);
} : function () { return 0; };
// After touchdown: the ground stops it (tips the glitch's over); the sea bobs it (lists the glitch's,
// swallows the failure's).
var tip = GROUND && glitch ? track([[SPLASH + 0.1, 0], [SPLASH + 0.7, 84, "in"], [SPLASH + 0.85, 78, "out"], [SPLASH + 1.0, 80]])
        : GROUND && failure ? track([[SPLASH, 0], [SPLASH + 0.12, 18, "out"]])
        : !GROUND && glitch ? track([[SPLASH, 0], [SPLASH + 0.5, 30, "out"], [SPLASH + 2.0, 0]]) : function () { return 0; };
var bob = GROUND ? function () { return 0; }
                 : function (t) { return t < SPLASH ? 0 : 6 * Math.sin(2 * Math.PI * 0.7 * (t - SPLASH)) * Math.exp(-(t - SPLASH) * 0.4); };
var sink = failure && !GROUND ? function (t) { return t < SPLASH ? 0 : 300 * (1 - Math.exp(-(t - SPLASH) * 1.6)); } : function () { return 0; };
function corner(t) {
    if (!GROUND) return [0, 0];
    var a = tip(t) * RAD;
    return [90 - 90 * Math.cos(a) + 148 * Math.sin(a), 148 - 148 * Math.cos(a) - 90 * Math.sin(a)];
}
move("r-carrier", function (t) { var c = corner(t); return [c[0], bob(t) + sink(t) + c[1]]; });
turn("r-carrier", function (t) { return swing(t) + tip(t); });
fade("r-carrier", function () { return 1; });
fade("r-capsule-up", failure && !GROUND ? track([[SPLASH + 0.3, 1], [SPLASH + 1.0, 0.3, "lin"], [SPLASH + 1.6, 0, "lin"]]) : function () { return 1; });

// The main: full over it, swaying with the gust; torn loose (failure) it flies off up and away;
// after touchdown it sags down beside the capsule.
move("r-main", function (t) { var u = failure ? Math.max(0, t - TEAR) : 0; return [160 * u, bob(t) - 380 * u * u]; });
turn("r-main", function (t) {
    if (failure && t > TEAR) return swing(TEAR) * 0.5 + 40 * (t - TEAR);
    return swing(t) * 0.5 + (t > SPLASH ? 50 * clamp((t - SPLASH) / 1.4, 0, 1) : 0);
});
grow("r-main", function (t) { return !failure && t > SPLASH ? 1 - 0.3 * clamp((t - SPLASH) / 1.2, 0, 1) : 1; });
fade("r-main", function (t) {
    if (failure) return t < TEAR + 0.6 ? 1 : 1 - clamp((t - TEAR - 0.6) / 0.6, 0, 1);
    return t < SPLASH + 0.6 ? 1 : 1 - clamp((t - SPLASH - 0.6) / 0.8, 0, 1);
});

if (GROUND) {
    var big = failure ? 1.8 : glitch ? 1.3 : 1.0;
    move("r-dust", function () { return [0, 818 - 200 - 1100]; });
    grow("r-dust", track([[SPLASH, 0.3 * big], [SPLASH + 0.35, big, "out"], [SPLASH + 1.4, big * 1.3, "lin"]]));
    fade("r-dust", track([[SPLASH, 0, "snap"], [SPLASH + 0.02, 1, "snap"], [SPLASH + 0.6, 0.9], [SPLASH + 1.6, 0, "lin"]]));
    var flings = [[-260, -300, 280], [240, -340, -320], [-120, -420, 200]];
    for (i = 1; i <= 3; i++) {
        (function (n, v) {
            if (!failure) { hide(n); return; }
            var at = [0, 818 - 470 - 200 - 20];
            fade(n, function (t) { return t >= SPLASH && t < SPLASH + 1.6 ? 1 - Math.max(0, t - SPLASH - 1.1) / 0.5 : 0; });
            move(n, function (t) { var u = Math.max(0, t - SPLASH); return [at[0] + v[0] * u, at[1] + v[1] * u + 500 * u * u]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - SPLASH); });
        })("r-chip-" + i, flings[i - 1]);
    }
} else {
    var big = failure ? 1.6 : glitch ? 1.25 : 1.0;
    move("r-splash", function () { return [0, 818 - 200 - 1100]; });
    grow("r-splash", track([[SPLASH, 0.3 * big], [SPLASH + 0.3, big, "out"], [SPLASH + 0.8, big * 1.1, "lin"]]));
    fade("r-splash", track([[SPLASH, 0, "snap"], [SPLASH + 0.02, 1, "snap"], [SPLASH + 0.4, 1], [SPLASH + 0.9, 0, "lin"]]));
    for (i = 1; i <= 3; i++) {
        (function (n, k) {
            var t0 = SPLASH + 0.1 + 0.3 * k;
            move(n, function () { return [0, 818 - 200 - 1100]; });
            grow(n, function (t) { return t < t0 ? 0.2 : 0.2 + big * 1.4 * clamp((t - t0) / 1.0, 0, 1); });
            fade(n, function (t) { return t < t0 ? 0 : 1 - clamp((t - t0) / 1.0, 0, 1); });
        })("r-ring-" + i, i - 1);
    }
}

// The recovery helicopter: in from the right, hovering over the capsule (or where it went under).
var heliX = track([[HELI, 1500], [HELI + 1.6, 120, "out"]]);
move("y-heli", function (t) { return [heliX(t), -60 + 4 * Math.sin(2 * Math.PI * 0.8 * t)]; });
grow("y-rotor", function (t) { return [0.25 + 0.75 * Math.abs(Math.cos(2 * Math.PI * 7 * t)), 1]; });
