// The Multiprobe's entries (9 December 1978; 2026-10-17), after multiprobe.jsx; BIG is set by the step's own file.
// BIG: the large probe falls nose (heat shield) first towards Venus, whose limb comes up and settles under it,
// sending as it comes. It meets the air at 1.0: a bow of hot air builds under it, the air streams past and it
// shakes, and its radio falls silent in the heat (every variant). The glow dies by 3.9; its signal is back at 3.6.
// Small: the three small probes the same, side by side (North, Day, Night), each sending, each in its own glow.
// Glitch: the probe (BIG) or North starts to wobble at 2.4, its glow swinging with it, and is steady by 3.5.
// Failure: the wobble grows; at 2.7 it goes sideways into the glow and breaks up at 3.1. The others carry on.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var AIR = 1.0, WOBBLE = 2.4, BREAK = 3.1, BACK = 3.6;
var i;

put("paper", "screen");
put("b-limb");
for (i = 1; i <= 8; i++) put("r-streak-" + i);
var N = BIG ? 1 : 3;
for (i = 1; i <= N; i++) put("pv-glow-" + i);
if (BIG) {
    put("pv-big");
    put("pv-big-sphere", "pv-big");
    hide("pv-big-sphere");
    put("pv-big-aft", "pv-big");
    put("pv-big-shield", "pv-big");
} else {
    for (i = 1; i <= 3; i++) put("pv-small-" + i);
}
for (i = 1; i <= 4; i++) put("ring-" + i);
for (i = 1; i <= 6; i++) put("pv-ring-" + i);
for (i = 1; i <= 4; i++) put("r-chip-" + i);
put("burst");

move("b-limb", function (t) { return [0, -300 * (1 - Math.exp(-t / 1.4))]; });
var heat = track([[AIR, 0], [2.0, 1], [3.0, 1], [3.9, 0]]);
var lost = function (t) { return failure && t >= BREAK; };
// The probe the trouble comes to (BIG: the one; small: North) wobbles; the failure's goes sideways.
var osc = function (t) {
    if (!(glitch || failure) || t < WOBBLE) return 0;
    var a = 20 * Math.min(1, (t - WOBBLE) / 0.3);
    if (glitch) a *= clamp((3.5 - t) / 0.8, 0, 1);
    return a * Math.sin(2 * Math.PI * 1.4 * (t - WOBBLE));
};
var sideways = failure ? track([[2.7, 0], [BREAK, 85, "in"]]) : function () { return 0; };
var troubled = function (t) { var u = Math.min(t, failure ? BREAK : 99); return osc(u) + sideways(t); };
function shake(t, seed) { return 3 * heat(t) * Math.sin(2 * Math.PI * 11 * t + seed); }

// Each probe: its nose (where its glow sits) and its angle, and a point on it for its rings.
var probes = [];
if (BIG) {
    var K = 1.6, base = [960, 520 - 92 * K];
    probes.push({angle: troubled, k: K, at: function (t) { return [base[0] + shake(t, 0), base[1] + shake(t, 1.3)]; },
                 glow: 1.3, nose: function (t) { return bigPoint(this.at(t), K, this.angle(t), 962, 540); },
                 aerial: function (t) { return bigPoint(this.at(t), K, this.angle(t), 814, 540); }, rings: ["ring-1", "ring-2"],
                 trouble: true});
    move("pv-big", function (t) { var p = probes[0].at(t); return [p[0] - 870, p[1] - 540]; });
    turn("pv-big", function (t) { return 90 + troubled(t); });
    grow("pv-big", function () { return K; });
    // Parenting doesn't pass on opacity: every part goes.
    fade("pv-big-aft", function (t) { return lost(t) ? 0 : 1; });
    fade("pv-big-shield", function (t) { return lost(t) ? 0 : 1; });
} else {
    for (i = 0; i < 3; i++) {
        (function (k) {
            var s = SMALLS[k];
            var me = {k: s[2], glow: 0.55 * s[2], trouble: k === NORTH - 1, rings: ["pv-ring-" + (2 * k + 1), "pv-ring-" + (2 * k + 2)]};
            me.angle = me.trouble ? troubled : function () { return 0; };
            me.at = function (t) { return [s[0] + shake(t, k), s[1] + shake(t, k + 1.3)]; };
            me.nose = function (t) { return smallPoint(me.at(t), me.k, me.angle(t), 1009, 540); };
            me.aerial = function (t) { return smallPoint(me.at(t), me.k, me.angle(t), 920, 540); };
            probes.push(me);
            var n = "pv-small-" + (k + 1);
            move(n, function (t) { var p = me.at(t); return [p[0] - 968, p[1] - 540]; });
            turn(n, function (t) { return 90 + me.angle(t); });
            grow(n, function () { return me.k; });
            fade(n, function (t) { return me.trouble && lost(t) ? 0 : 1; });
        })(i);
    }
    for (i = 1; i <= 4; i++) hide("ring-" + i);
}
for (i = 1; i <= 6; i++) if (BIG) hide("pv-ring-" + i);
if (BIG) for (i = 3; i <= 4; i++) hide("ring-" + i);

for (i = 0; i < probes.length; i++) {
    (function (p, n) {
        // The bow of hot air ahead of it, turned with it.
        // Its inner curve's middle is 30 px under its pivot, at its size: there, on the nose.
        move(n, function (t) { var q = p.nose(t); return [q[0] - 960, q[1] - 500 - 34 * p.glow]; });
        turn(n, p.angle);
        fade(n, function (t) { return p.trouble && lost(t) ? 0 : heat(t); });
        grow(n, function (t) { var k = p.glow * (0.7 + 0.3 * heat(t)); return [k * (1 + 0.05 * Math.sin(2 * Math.PI * 9 * t)), k]; });
        // Sending until the air, silent in the heat, back at BACK (never, lost); each a little after the last.
        var off = 0.25 * (probes.length > 1 ? (p === probes[0] ? 0 : p === probes[1] ? 1 : 2) : 0);
        var sends = every(0.3 + off, AIR, 0.75).concat(failure && p.trouble ? [] : every(BACK + off, DUR, 0.75));
        ringsAt(p.rings, sends, function (t) { return p.aerial(t); });
    })(probes[i], "pv-glow-" + (i + 1));
}

// The air streaming up past them, each streak looping every 0.5 s in its own lane beside a probe.
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        var p = probes[(k - 1) % probes.length];
        var lane = (k % 2 ? 1 : -1) * (BIG ? 20 + 22 * k : 30 + 14 * k) * (BIG ? 1 : p.k), phase = k * 0.13;
        function at(t) {
            var a = p.angle(Math.min(t, failure ? BREAK : 99)) * RAD, u = ((t + phase) % 0.5) / 0.5;
            var along = (BIG ? 170 - 520 * u : 80 - 300 * u), q = p.nose(t);
            return [q[0] + lane * Math.cos(a) - along * Math.sin(a), q[1] + lane * Math.sin(a) + along * Math.cos(a)];
        }
        // r-streak's middle is about (960, 470) at rest, upright once turned.
        move(n, function (t) { var q = at(t); return [q[0] - 960, q[1] - 470]; });
        turn(n, function (t) { return p.angle(Math.min(t, failure ? BREAK : 99)) - 90; });
        grow(n, function () { return BIG ? 1 : 0.6; });
        fade(n, function (t) {
            var u = ((t + phase) % 0.5) / 0.5;
            var h = p.trouble && failure && t > BREAK ? heat(BREAK) * Math.max(0, 1 - (t - BREAK) / 1.2) : heat(t);
            return 0.9 * h * Math.sin(Math.PI * u);
        });
    })("r-streak-" + i, i);
}

if (failure) {
    var hit;
    for (i = 0; i < probes.length; i++) if (probes[i].trouble) hit = probes[i];
    var c = hit.at(BREAK), sz = BIG ? 2.0 : 1.0;
    move("burst", function () { return [c[0] - 960, c[1] - 600 + 20]; });
    grow("burst", track([[BREAK, 0.3 * sz], [BREAK + 0.35, sz, "out"], [BREAK + 1.0, 1.2 * sz, "lin"]]));
    fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
    var flings = [[-420, -260, 300], [-300, -420, -380], [320, -380, 240], [200, -520, -260]];
    for (i = 1; i <= 4; i++) {
        (function (n, v) {
            fade(n, function (t) { return t >= BREAK && t < BREAK + 2.2 ? 1 - Math.max(0, t - BREAK - 1.6) / 0.6 : 0; });
            move(n, function (t) { var u = Math.max(0, t - BREAK); return [c[0] - 960 + v[0] * u * sz / 2, c[1] - 466 + v[1] * u * sz / 2]; });
            turn(n, function (t) { return v[2] * Math.max(0, t - BREAK); });
            grow(n, function () { return BIG ? 1 : 0.6; });
        })("r-chip-" + i, flings[i - 1]);
    }
} else {
    hide("burst");
    for (i = 1; i <= 4; i++) hide("r-chip-" + i);
}
camera(function () { return 0; }, function (t) { return 3 * heat(t); });
