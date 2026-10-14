// The descent under parachutes, to the sea or the ground (GROUND, set by the step's own file
// first, with the variants' story). Shared, so the two only differ where they touch down.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROGUE = 0.8, MAIN = 1.6, HALF = 2.0, FULL = glitch ? 3.0 : MAIN;
var SPLASH = failure ? 3.6 : glitch ? 4.7 : 4.4;

put("paper", "screen");
for (var i = 1; i <= 6; i++) put("r-cloud-" + i);
put(GROUND ? "r-ground" : "r-sea");
put("r-drogue");
put("r-main");
put("r-main-half");
put("r-streamer");
put("r-carrier");
put("r-capsule-up", "r-carrier");
if (GROUND) {
    put("r-dust");
    for (i = 1; i <= 3; i++) put("r-chip-" + i);
} else {
    for (i = 1; i <= 3; i++) put("r-ring-" + i);
    put("r-splash");
}

// The fall's speed (px/s against the clouds): fast, slowed by the drogue, then by the main.
var speed = failure ? track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [HALF, 320], [2.4, 380]])
          : glitch ? track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [HALF, 320], [2.6, 170], [FULL, 170], [3.6, 60]])
          : track([[DROGUE, 450], [1.2, 300], [MAIN, 300], [2.4, 60]]);
var fallen = [], y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
function fall(t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; }
// The clouds go on up from where the reentry left them (500 - 900 px).
for (i = 1; i <= 6; i++) move("r-cloud-" + i, function (t) { return [0, -400 - fall(t)]; });
// Down, the sky clears: the clouds fade, so what follows (a recovery) starts on an empty sky.
for (i = 1; i <= 6; i++) fade("r-cloud-" + i, track([[SPLASH + 0.4, 1], [SPLASH + 1.4, 0, "lin"]]));

// The camera drops to keep the main in the frame once it's open (not for a streamer).
var drop = failure ? function () { return 0; } : glitch ? track([[HALF, 0], [3.0, 200]]) : track([[MAIN - 0.2, 0], [2.6, 200]]);
// The sea: off the bottom until it comes up to meet the capsule's base (618 px down on screen) at the splash.
var seaAt = function (t) {
    // The sea line, on screen: from far below, coming up in step with the fall, landing at SPLASH.
    var land = 618 + drop(SPLASH);
    return land + (fall(SPLASH) - fall(Math.min(t, SPLASH))) * (failure ? 1.4 : 3.2);
};
move(GROUND ? "r-ground" : "r-sea", function (t) { return [0, seaAt(t) - drop(t) - 1100]; });

// After the splash: bobbing, or (failure) going under.
var bob = GROUND ? function (t) { return t < SPLASH || t > SPLASH + 0.25 ? 0 : -10 * Math.sin(Math.PI * (t - SPLASH) / 0.25) * (failure ? 0 : 1); }
                 : function (t) { return t < SPLASH ? 0 : 6 * Math.sin(2 * Math.PI * 0.7 * (t - SPLASH)) * Math.exp(-(t - SPLASH) * 0.4); };
var sink = failure && !GROUND ? function (t) { return t < SPLASH ? 0 : 260 * (1 - Math.exp(-(t - SPLASH) * 2.2)); } : function () { return 0; };
// On the ground, the glitch's hard landing tips it onto its side (about its lower corner), and the
// failure's knocks it into a lean.
var tip = GROUND && glitch ? track([[SPLASH + 0.1, 0], [SPLASH + 0.7, 84, "in"], [SPLASH + 0.85, 78, "out"], [SPLASH + 1.0, 80]])
        : GROUND && failure ? track([[SPLASH, 0], [SPLASH + 0.12, 18, "out"]]) : function () { return 0; };
var swing = glitch ? function (t) { return t < HALF || t > FULL + 0.6 ? 0 : 16 * Math.sin(2 * Math.PI * 0.9 * (t - HALF)) * Math.min(1, (FULL + 0.6 - t) / 0.6); }
                   : function () { return 0; };
// Tipping about the lower corner (the base is 148 px below the centre, the side 90 px out).
function corner(t) {
    var a = tip(t) * RAD;
    return [90 - 90 * Math.cos(a) + 148 * Math.sin(a), 148 - 148 * Math.cos(a) - 90 * Math.sin(a)];
}
move("r-carrier", function (t) { var c = corner(t); return [c[0], bob(t) + sink(t) + c[1]]; });
turn("r-carrier", function (t) { return swing(t) + tip(t); });
fade("r-carrier", function () { return 1; });
fade("r-capsule-up", failure && !GROUND ? track([[SPLASH + 0.3, 1], [SPLASH + 0.9, 0, "lin"]]) : function () { return 1; });

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

if (GROUND) {
    // The dust kicked up at touchdown, bigger the harder it lands.
    var big = failure ? 1.8 : glitch ? 1.3 : 1.0;
    move("r-dust", function (t) { return [0, seaAt(SPLASH) - drop(t) - 1100]; });
    grow("r-dust", track([[SPLASH, 0.3 * big], [SPLASH + 0.35, big, "out"], [SPLASH + 1.4, big * 1.3, "lin"]]));
    fade("r-dust", track([[SPLASH, 0, "snap"], [SPLASH + 0.02, 1, "snap"], [SPLASH + 0.6, 0.9], [SPLASH + 1.6, 0, "lin"]]));
    // A crash throws pieces off.
    var flings = [[-260, -300, 280], [240, -340, -320], [-120, -420, 200]];
    for (i = 1; i <= 3; i++) {
        (function (n, v) {
            if (!failure) { hide(n); return; }
            var at = [0, seaAt(SPLASH) - 470 - 20];
            fade(n, function (t) { return t >= SPLASH && t < SPLASH + 1.6 ? 1 - Math.max(0, t - SPLASH - 1.1) / 0.5 : 0; });
            move(n, function (t) { var u = Math.max(0, t - SPLASH); return [at[0] + v[0] * u, at[1] - drop(t) + v[1] * u + 500 * u * u]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - SPLASH); });
        })("r-chip-" + i, flings[i - 1]);
    }
} else {
    // The splash and its rings, at the sea line under the capsule.
    var big = failure ? 1.6 : glitch ? 1.25 : 1.0;
    move("r-splash", function (t) { return [0, seaAt(SPLASH) - drop(t) - 1100 + drop(SPLASH) - drop(SPLASH)]; });
    grow("r-splash", track([[SPLASH, 0.3 * big], [SPLASH + 0.3, big, "out"], [SPLASH + 0.8, big * 1.1, "lin"]]));
    fade("r-splash", track([[SPLASH, 0, "snap"], [SPLASH + 0.02, 1, "snap"], [SPLASH + 0.4, 1], [SPLASH + 0.9, 0, "lin"]]));
    for (i = 1; i <= 3; i++) {
        (function (n, k) {
            // Three rings, done before the clip ends (the latest splash is at 4.7).
            var t0 = SPLASH + 0.1 + 0.3 * k;
            move(n, function (t) { return [0, seaAt(SPLASH) - drop(t) - 1100]; });
            grow(n, function (t) { return t < t0 ? 0.2 : 0.2 + big * 1.4 * clamp((t - t0) / 1.0, 0, 1); });
            fade(n, function (t) { return t < t0 ? 0 : 1 - clamp((t - t0) / 1.0, 0, 1); });
        })("r-ring-" + i, i - 1);
    }
}
camera(drop, function (t) { return t >= SPLASH && t < SPLASH + 0.3 ? (failure ? 10 : 4) : 0; });
