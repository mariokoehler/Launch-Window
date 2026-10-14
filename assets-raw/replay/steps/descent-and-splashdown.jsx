// Descent and splashdown: base down under the clouds (reentry's last frame), the capsule falls
// fast; the drogue opens at 0.8 and slows it, the main at 1.6, and it drifts down to the sea, which
// comes up from below; splashdown at 4.4, a splash and rings, and it floats under its chute.
// Glitch: the main comes out at 1.6 but streams, then fills halfway at 2.0 and the capsule swings
// under it, until it fills at 3.0; a later, harder splash (4.7).
// Failure: the same streamer at 1.6, but it never fills; the capsule keeps falling fast, hits the
// sea at 3.6 in a big splash and goes under.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROGUE = 0.8, MAIN = 1.6, HALF = 2.0, FULL = glitch ? 3.0 : MAIN;
var SPLASH = failure ? 3.6 : glitch ? 4.7 : 4.4;

put("paper", "screen");
for (var i = 1; i <= 6; i++) put("r-cloud-" + i);
put("r-sea");
put("r-drogue");
put("r-main");
put("r-main-half");
put("r-streamer");
put("r-carrier");
put("r-capsule-up", "r-carrier");
for (i = 1; i <= 3; i++) put("r-ring-" + i);
put("r-splash");

// The fall's speed (px/s against the clouds): fast, slowed by the drogue, then by the main.
var speed = failure ? track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [HALF, 320], [2.4, 380]])
          : glitch ? track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [HALF, 320], [2.6, 170], [FULL, 170], [3.6, 60]])
          : track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [2.4, 60]]);
var fallen = [], y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
function fall(t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; }
// The clouds go on up from where the reentry left them (500 - 900 px).
for (i = 1; i <= 6; i++) move("r-cloud-" + i, function (t) { return [0, -400 - fall(t)]; });

// The camera drops to keep the main in the frame once it's open (not for a streamer).
var drop = failure ? function () { return 0; } : glitch ? track([[HALF, 0], [3.0, 200]]) : track([[MAIN - 0.2, 0], [2.6, 200]]);
// The sea: off the bottom until it comes up to meet the capsule's base (618 px down on screen) at the splash.
var seaAt = function (t) {
    // The sea line, on screen: from far below, coming up in step with the fall, landing at SPLASH.
    var land = 618 + drop(SPLASH);
    return land + (fall(SPLASH) - fall(Math.min(t, SPLASH))) * (failure ? 1.4 : 3.2);
};
move("r-sea", function (t) { return [0, seaAt(t) - drop(t) - 1100]; });

// After the splash: bobbing, or (failure) going under.
var bob = function (t) { return t < SPLASH ? 0 : 6 * Math.sin(2 * Math.PI * 0.7 * (t - SPLASH)) * Math.exp(-(t - SPLASH) * 0.4); };
var sink = failure ? function (t) { return t < SPLASH ? 0 : 260 * (1 - Math.exp(-(t - SPLASH) * 2.2)); } : function () { return 0; };
var swing = glitch ? function (t) { return t < HALF || t > FULL + 0.6 ? 0 : 16 * Math.sin(2 * Math.PI * 0.9 * (t - HALF)) * Math.min(1, (FULL + 0.6 - t) / 0.6); }
                   : function () { return 0; };
move("r-carrier", function (t) { return [0, bob(t) + sink(t)]; });
turn("r-carrier", function (t) { return swing(t); });
fade("r-carrier", function () { return 1; });
fade("r-capsule-up", failure ? track([[SPLASH + 0.3, 1], [SPLASH + 0.9, 0, "lin"]]) : function () { return 1; });

// The chutes hang from the nose; they follow the capsule's bob, and sway with its swing.
function hangs(n) {
    move(n, function (t) { return [0, bob(t)]; });
    turn(n, function (t) { return swing(t) * 0.5; });
}
hangs("r-drogue"); hangs("r-main"); hangs("r-main-half"); hangs("r-streamer");
// The drogue: out at DROGUE, cut away when the main comes out.
grow("r-drogue", track([[DROGUE, 0.2], [DROGUE + 0.25, 1.05, "out"], [DROGUE + 0.4, 1]]));
fade("r-drogue", function (t) { return t >= DROGUE && t < MAIN ? 1 : 0; });
// The main: out at MAIN, filling (or half, then full; or a streamer).
// The streamer: the main out of its bag, in the glitch until it fills halfway, in the failure for good.
if (glitch || failure) {
    fade("r-streamer", function (t) { return t >= MAIN && t < (glitch ? HALF : SPLASH + 0.2) ? 1 : 0; });
    grow("r-streamer", function (t) { return [1 + 0.5 * Math.sin(2 * Math.PI * 3 * t), t < MAIN + 0.3 ? 0.3 + 0.7 * (t - MAIN) / 0.3 : 1]; });
} else {
    hide("r-streamer");
}
if (failure) {
    hide("r-main"); hide("r-main-half");
} else {
    if (glitch) {
        fade("r-main-half", function (t) { return t >= HALF && t < FULL ? 1 : 0; });
        grow("r-main-half", function (t) { return t < HALF + 0.3 ? 0.3 + 0.7 * Math.max(0, t - HALF) / 0.3 : [1 + 0.04 * Math.sin(2 * Math.PI * 4 * t), 1]; });
    } else {
        hide("r-main-half");
    }
    // After the splash the main sags down beside the capsule.
    fade("r-main", function (t) { return t < FULL ? 0 : t < SPLASH + 0.6 ? 1 : 1 - clamp((t - SPLASH - 0.6) / 0.8, 0, 1); });
    grow("r-main", track([[FULL, 0.15], [FULL + 0.35, 1.08, "out"], [FULL + 0.6, 1], [SPLASH, 1], [SPLASH + 1.2, 0.7, "in"]]));
    turn("r-main", function (t) { return swing(t) * 0.5 + (t > SPLASH ? 50 * clamp((t - SPLASH) / 1.4, 0, 1) : 0); });
}

// The splash and its rings, at the sea line under the capsule.
var big = failure ? 1.6 : glitch ? 1.25 : 1.0;
move("r-splash", function (t) { return [0, seaAt(SPLASH) - drop(t) - 1100 + drop(SPLASH) - drop(SPLASH)]; });
grow("r-splash", track([[SPLASH, 0.3 * big], [SPLASH + 0.3, big, "out"], [SPLASH + 0.8, big * 1.1, "lin"]]));
fade("r-splash", track([[SPLASH, 0, "snap"], [SPLASH + 0.02, 1, "snap"], [SPLASH + 0.4, 1], [SPLASH + 0.9, 0, "lin"]]));
for (i = 1; i <= 3; i++) {
    (function (n, k) {
        var t0 = SPLASH + 0.1 + 0.45 * k;
        move(n, function (t) { return [0, seaAt(SPLASH) - drop(t) - 1100]; });
        grow(n, function (t) { return t < t0 ? 0.2 : 0.2 + big * 1.4 * clamp((t - t0) / 1.6, 0, 1); });
        fade(n, function (t) { return t < t0 ? 0 : 1 - clamp((t - t0) / 1.6, 0, 1); });
    })("r-ring-" + i, i - 1);
}
camera(drop, function (t) { return t >= SPLASH && t < SPLASH + 0.3 ? (failure ? 10 : 4) : 0; });
