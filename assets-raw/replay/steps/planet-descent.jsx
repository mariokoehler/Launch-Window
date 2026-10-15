// A lander's descent at another planet (PLANET, set by the step's own file first: "venus" or "mars"),
// after its entry (planet-entry.jsx) and before its landing (probe-landing.jsx). design.md 1.1.
//
// Venus: Venera's sphere falls through the cloud deck; its chute comes out reefed at 0.8 and opens fully
// at 2.2 (Venera 7's reefing line melted as designed). Down below the deck the bands thin out, and it
// ends under the open chute, where the landing starts.
// Glitch (Venera 7): at 3.2 the canopy tears; the chute sags to half, swinging, and holds: it falls faster.
// Failure: at 3.2 the chute tears away and the sphere drops out of the picture.
//
// Mars (Viking's way down; Mars 3's was alike): the aeroshell under its chute at 0.5, the heat shield
// dropped at 1.4, the legs out at 1.9; at 2.8 the lander leaves the backshell and its three engines
// light at 2.95, the ground coming up.
// Glitch: one engine lights late (3.5) and the lander tips, then levels.
// Failure: the engines never light: it falls away, and dust rises from the ground at 5.0.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var mars = PLANET === "mars";
var i;

if (!mars) {
    var REEF = 0.8, OPEN = 2.2, TEAR = 3.2;
    var K = 0.96;   // the chute's size against the sphere's 1.6 (the landing's)
    put("paper", "screen");
    for (i = 1; i <= 6; i++) put("y-band-" + i);
    put("r-main-half");
    put("r-main");
    put("y-rip", "r-main");
    put("v-pod");

    // The fall's speed (px/s against the bands): fast, slowed by the reefed chute, then the open one.
    var speed = failure ? track([[REEF, 1100], [1.2, 520], [OPEN, 520], [2.6, 240], [TEAR, 240], [3.8, 1300]])
              : glitch ? track([[REEF, 1100], [1.2, 520], [OPEN, 520], [2.6, 240], [TEAR, 240], [3.6, 420]])
              : track([[REEF, 1100], [1.2, 520], [OPEN, 520], [2.6, 240]]);
    var fallen = [], y = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
    var fall = function (t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; };
    // The cloud deck's bands stream up, wrapping round, and thin out below it (not when it's lost).
    for (i = 1; i <= 6; i++) {
        (function (n, k) {
            move(n, function (t) { var p = (k * 220 - fall(t)) % 1320; if (p < 0) p += 1320; return [(k % 2 ? -60 : 60), p - 120 - 600]; });
            fade(n, failure ? function () { return 1; } : track([[4.0, 1], [5.6, 0.3, "lin"]]));
        })("y-band-" + i, i);
    }

    // After the tear: the glitch swings, the failure's sphere drops away.
    var swing = glitch ? function (t) { return t < TEAR ? 0 : 14 * Math.sin(2 * Math.PI * 0.8 * (t - TEAR)) * Math.exp(-(t - TEAR) * 0.5); }
                       : function () { return 0; };
    var drop = failure ? function (t) { var u = Math.max(0, t - TEAR - 0.1); return 160 * u + 700 * u * u; } : function () { return 0; };
    var shake = function (t) { return t < REEF ? 3 * jitter(t, 4) : 0; };
    // The sphere (pivot 960 506) at 1.6, its centre at 640; the chutes hang from its top (563).
    move("v-pod", function (t) { return [shake(t), 134 + drop(t)]; });
    grow("v-pod", function () { return 1.6; });
    turn("v-pod", function (t) { return swing(t) + (failure ? 60 * Math.max(0, t - TEAR) : 0); });
    var hang = [0, 563 - 322];
    // The reefed chute: out at REEF; at OPEN it gives way to the full one. Torn (glitch), it's back,
    // fluttering.
    move("r-main-half", function (t) { return [hang[0] + shake(t), hang[1]]; });
    turn("r-main-half", function (t) { return swing(t) * 0.5; });
    fade("r-main-half", function (t) { return t >= REEF && t < OPEN || glitch && t >= TEAR + 0.12 ? 1 : 0; });
    grow("r-main-half", function (t) {
        if (t < OPEN) return K * (t < REEF + 0.3 ? 0.15 + 0.85 * Math.max(0, t - REEF) / 0.3 : 1);
        return [K * (1 + 0.08 * Math.sin(2 * Math.PI * 6 * t)), K * 0.92];
    });
    // The open chute: at OPEN, until the tear (glitch), or torn away up and out (failure).
    var away = failure ? function (t) { var u = Math.max(0, t - TEAR); return [80 * u, -(260 * u + 300 * u * u)]; } : function () { return [0, 0]; };
    move("r-main", function (t) { var a = away(t); return [hang[0] + a[0], hang[1] + a[1]]; });
    turn("r-main", function (t) { return swing(t) * 0.5 + (failure ? 35 * Math.max(0, t - TEAR) : 0); });
    fade("r-main", function (t) {
        if (t < OPEN) return 0;
        if (glitch) return t < TEAR + 0.12 ? 1 : 0;
        if (failure) return t < TEAR + 0.6 ? 1 : 1 - clamp((t - TEAR - 0.6) / 0.6, 0, 1);
        return 1;
    });
    grow("r-main", track([[OPEN, K * 0.4], [OPEN + 0.3, K * 1.06, "out"], [OPEN + 0.5, K]]));
    // The tear: a gold flash across the canopy.
    if (glitch || failure) {
        fade("y-rip", track([[TEAR, 0, "snap"], [TEAR + 0.02, 1, "snap"], [TEAR + 0.4, 1], [TEAR + 0.8, 0, "lin"]]));
    } else {
        hide("y-rip");
    }
    camera(function () { return 0; }, function (t) { return t >= TEAR && t < TEAR + 0.25 && (glitch || failure) ? 6 : 0; });
} else {
    var CHUTE = 0.5, HEAT = 1.4, LEGS = 1.9, SEP = 2.8, FIRE = 2.95, LATE = 3.5, DUST = 5.0;
    put("paper", "screen");
    for (i = 1; i <= 8; i++) put("a-flow-" + i);
    put("m-ground");
    put("r-dust");
    put("r-main");
    put("m-leg-l");
    put("m-leg-r");
    for (i = 1; i <= 3; i++) put("m-flame-" + i);
    put("m-lander");
    put("m-back");
    put("m-heat");

    // The fall's speed against the air: fast, slowed by the chute, then (engines on) slowly down.
    var speed = failure ? track([[CHUTE, 900], [1.0, 360], [SEP, 360], [3.6, 900]])
              : track([[CHUTE, 900], [1.0, 360], [SEP, 360], [3.4, 160]]);
    var fallen = [], y = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
    var fall = function (t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; };
    // Dust in the thin air streaming up past it: the speed lines, stood on end, each in its own lane.
    for (i = 1; i <= 8; i++) {
        (function (n, k) {
            var lane = (k % 2 ? -1 : 1) * (180 + 95 * k);
            move(n, function (t) { var p = (k * 170 - fall(t)) % 1360; if (p < 0) p += 1360; return [lane, p - 140 - 540]; });
            turn(n, function () { return 90; });
            fade(n, function () { return 0.7; });
        })("a-flow-" + i, i);
    }
    // The ground comes up from below.
    var ground = track([[0.8, 520], [6.0, -200, "lin"]]);
    move("m-ground", function (t) { return [0, ground(t)]; });

    var sway = function (t) { return 3 * Math.sin(2 * Math.PI * 0.6 * t); };
    // The backshell and its chute: with the craft, then (SEP) left behind, slowed by the chute.
    var behind = function (t) { var u = Math.max(0, t - SEP); return -(150 * u + 220 * u * u); };
    move("m-back", function (t) { return [0, behind(t)]; });
    turn("m-back", sway);
    fade("m-back", track([[SEP + 1.0, 1], [SEP + 1.8, 0, "lin"]]));
    move("r-main", function (t) { return [0, 78 + behind(t)]; });
    turn("r-main", function (t) { return sway(t) * 0.5; });
    grow("r-main", track([[CHUTE, 0.12], [CHUTE + 0.35, 0.86, "out"], [CHUTE + 0.5, 0.8]]));
    fade("r-main", function (t) { return t < CHUTE ? 0 : t < SEP + 1.0 ? 1 : 1 - clamp((t - SEP - 1.0) / 0.8, 0, 1); });
    // The heat shield: under the craft, then (HEAT) dropped, tumbling away below.
    move("m-heat", function (t) { var u = Math.max(0, t - HEAT); return [30 * u, 140 * u + 420 * u * u]; });
    turn("m-heat", function (t) { return sway(t) + 30 * Math.max(0, t - HEAT); });
    fade("m-heat", track([[HEAT + 0.8, 1], [HEAT + 1.2, 0, "lin"]]));
    // The lander: inside until the heat shield goes (it shows under the backshell), on its own from SEP.
    var tilt = glitch ? track([[FIRE, 0], [LATE - 0.1, 14, "out"], [LATE + 0.7, 0]]) : function () { return 0; };
    var falls = failure ? function (t) { var u = Math.max(0, t - SEP); return 100 * u + 360 * u * u; } : function () { return 0; };
    var at = function (t) { return [0, 36 + falls(t)]; };
    var rot = function (t) { return (t < SEP ? sway(t) : 0) + tilt(t) + (failure ? 20 * Math.max(0, t - SEP) : 0); };
    var shown = function (t) { return t >= HEAT ? 1 : 0; };
    move("m-lander", at);
    turn("m-lander", rot);
    fade("m-lander", shown);
    // The legs swing out from folded under the body, turning with it about its centre.
    var legs = track([[LEGS, 1], [LEGS + 0.4, 0, "out"]]);
    (function () {
        var pairs = [["m-leg-l", -74, -60], ["m-leg-r", 74, 60]];
        for (var j = 0; j < 2; j++) {
            (function (n, dx, folded) {
                move(n, function (t) { var a = rot(t) * RAD, o = at(t); return [o[0] + dx * Math.cos(a) - 10 * Math.sin(a) - dx, o[1] + dx * Math.sin(a) + 10 * Math.cos(a) - 10]; });
                turn(n, function (t) { return rot(t) + folded * legs(t); });
                fade(n, shown);
            })(pairs[j][0], pairs[j][1], pairs[j][2]);
        }
    })();
    // The engines: three flames under the body (none, lost; the third late, the glitch).
    for (i = 1; i <= 3; i++) {
        (function (n, dx, k) {
            var on = failure ? -1 : glitch && k === 3 ? LATE : FIRE;
            move(n, function (t) { var a = rot(t) * RAD, o = at(t); return [o[0] + dx * Math.cos(a) - 14 * Math.sin(a) - dx, o[1] + dx * Math.sin(a) + 14 * Math.cos(a) - 14]; });
            turn(n, rot);
            grow(n, function (t) {
                if (on < 0 || t < on) return 0.01;
                return [1, Math.min(1, 0.2 + (t - on) / 0.2) * (1 + 0.06 * jitter(t, k))];
            });
            fade(n, function (t) {
                if (on < 0) return 0;
                // The late one sputters first.
                if (glitch && k === 3 && t >= LATE - 0.4 && t < LATE) return jitter(t, 9) > 0.3 ? 0.8 : 0;
                return t >= on ? 1 : 0;
            });
        })("m-flame-" + i, (i - 2) * 44, i);
    }
    // The failure's lander hits far below: dust rises from the ground.
    if (failure) {
        move("r-dust", function (t) { return [0, ground(t)]; });
        grow("r-dust", track([[DUST, 0.4], [DUST + 0.4, 1.6, "out"], [DUST + 1.0, 1.9, "lin"]]));
        fade("r-dust", track([[DUST, 0, "snap"], [DUST + 0.02, 1, "snap"], [DUST + 0.6, 0.9], [6.0, 0.4, "lin"]]));
    } else {
        hide("r-dust");
    }
    camera(function () { return 0; }, function (t) { return t < CHUTE ? 3 : t >= CHUTE && t < CHUTE + 0.2 ? 5 : 0; });
}
