// The Multiprobe's descents (9 December 1978; 2026-10-17), after multiprobe.jsx; BIG is set by the step's own file.
// None of the probes was built to land: each sends all the way down, and its signal ends where it hits (planned).
// BIG: the large probe falls through the cloud deck nose down, sending. At 0.5 its chute pulls the aft cover off and
// opens over it; at 1.1 the heat shield drops away below (at 67 km), and the sphere hangs under the chute. At 2.6 (about
// 45 km) the chute is cut loose and the sphere falls free, faster. The clouds thin, the ground comes up and it hits
// at 5.2 in a puff of dust, and the rings stop.
// Small: the three small probes (North, Day, Night) fall with their heat shields still on, no chute, sending; the
// ground comes up and they hit one after another. The Day probe keeps sending from the ground (it did, for 67
// minutes), every variant.
// Glitch (all four probes, about 12.5 km down): a spark at the temperature sensor's arm (BIG: the probe; small: North),
// and its signal goes on weaker. Failure: the spark, and its signal stops for good, short of the ground.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var CHUTE = 0.5, SHIELD = 1.1, CUT = 2.6, SPARK = 3.15, i;

put("paper", "screen");
for (i = 1; i <= 6; i++) put("y-band-" + i);
put("pv-ground");
put("r-main");
if (BIG) {
    put("pv-big");
    put("pv-big-sphere", "pv-big");
    put("pv-spark", "pv-big");
    put("pv-big-aft", "pv-big");
    put("pv-big-shield", "pv-big");
    put("pv-dust-1");
} else {
    for (i = 1; i <= 3; i++) { put("pv-small-" + i); put("pv-dust-" + i); }
    put("pv-spark");
}
for (i = 1; i <= 4; i++) put("ring-" + i);
for (i = 1; i <= 6; i++) put("pv-ring-" + i);
hide("pv-spark");

// The fall against the clouds: BIG slowed by its chute, then faster when it's cut; small falls fast throughout.
var speed = BIG ? track([[CHUTE, 900], [1.0, 300], [CUT, 300], [3.2, 760]]) : function () { return 820; };
var fallen = [], y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { fallen.push(y); y += speed(f / FPS) / FPS; }
var fall = function (t) { return fallen[clamp(Math.round(t * FPS), 0, fallen.length - 1)]; };
for (i = 1; i <= 6; i++) {
    (function (n, k) {
        move(n, function (t) { var p = (k * 220 - fall(t)) % 1320; if (p < 0) p += 1320; return [(k % 2 ? -60 : 60), p - 120 - 600]; });
        fade(n, track([[3.4, 1], [4.6, 0.25, "lin"]]));
    })("y-band-" + i, i);
}
// The ground comes up from below and stops when the last probe is down; a probe on it rises with it.
var groundAt = function (t) { return 0; };
var strength = function () { return 1; };
var sparkAt;

if (BIG) {
    var K = 1.4, base = [960, 470], HIT = 5.2;
    var sway = function (t) { return t < CUT ? 4 * Math.sin(2 * Math.PI * 0.5 * t) : 0; };
    // The sphere's nose (its lowest point once the shield's gone) is drawn at x 914.
    var low = bigPoint(base, K, 0, 914, 540)[1];
    var ground = track([[3.9, 520], [HIT, low - GROUND_Y, "lin"]]);
    move("pv-ground", function (t) { return [0, ground(t)]; });
    move("pv-big", function (t) { return [base[0] - 870, base[1] - 540]; });
    turn("pv-big", function (t) { return 90 + sway(t); });
    grow("pv-big", function () { return K; });
    // The sphere's arms would show through the aft cover until it's gone.
    fade("pv-big-sphere", function (t) { return t >= CHUTE + 0.1 ? 1 : 0; });
    // The aft cover goes up and away with the chute; the shield drops away below, tumbling (in the probe's frame,
    // +x is down).
    move("pv-big-aft", function (t) { var u = Math.max(0, t - CHUTE); return [-(300 * u + 200 * u * u), 30 * u]; });
    turn("pv-big-aft", function (t) { return 60 * Math.max(0, t - CHUTE); });
    fade("pv-big-aft", track([[CHUTE + 0.6, 1], [CHUTE + 1.0, 0, "lin"]]));
    move("pv-big-shield", function (t) { var u = Math.max(0, t - SHIELD); return [160 * u + 500 * u * u, 20 * u]; });
    turn("pv-big-shield", function (t) { return 40 * Math.max(0, t - SHIELD); });
    fade("pv-big-shield", track([[SHIELD + 0.7, 1], [SHIELD + 1.1, 0, "lin"]]));
    // The chute: its lines meet at its pivot (960 322), on the towers at the sphere's top; cut loose at CUT, it
    // goes up and away.
    var top = bigPoint(base, K, 0, 800, 540);
    var away = function (t) { var u = Math.max(0, t - CUT); return [70 * u, -(300 * u + 260 * u * u)]; };
    move("r-main", function (t) { var a = away(t); return [top[0] - 960 + a[0], top[1] - 322 - 40 * Math.max(0, 0.3 - (t - CHUTE)) + a[1]]; });
    turn("r-main", function (t) { return sway(t) * 0.5 + 25 * Math.max(0, t - CUT); });
    grow("r-main", track([[CHUTE, 0.2], [CHUTE + 0.3, 1.0, "out"], [CHUTE + 0.5, 0.94]]));
    fade("r-main", function (t) { return t < CHUTE ? 0 : t < CUT + 0.6 ? 1 : 1 - clamp((t - CUT - 0.6) / 0.6, 0, 1); });
    // Sends all the way down (the failure's stop at the spark), until it hits.
    var sends = every(0.3, HIT, 0.75);
    if (failure) sends = until(sends, SPARK);
    if (glitch) strength = function (te) { return te > SPARK ? 0.4 : 1; };
    ringsAt(["ring-1", "ring-2"], sends, function (t) { return bigPoint(base, K, sway(t), 784, 540); }, strength);
    for (i = 3; i <= 4; i++) hide("ring-" + i);
    for (i = 1; i <= 6; i++) hide("pv-ring-" + i);
    // The dust where it hits.
    move("pv-dust-1", function () { return [base[0] - 970, low - GROUND_Y]; });
    grow("pv-dust-1", track([[HIT, 0.6], [HIT + 0.4, 1.4, "out"]]));
    fade("pv-dust-1", track([[HIT, 0, "snap"], [HIT + 0.02, 1, "snap"], [HIT + 0.3, 1], [HIT + 0.8, 0, "lin"]]));
    camera(function () { return 0; }, function (t) { return t >= HIT && t < HIT + 0.25 ? 6 : 0; });
} else {
    // Each probe's nose is drawn at x 1009; the ground rises at a steady rate until the last is down.
    var noses = [], last = 0;
    for (i = 0; i < 3; i++) { noses.push(smallPoint([SMALLS[i][0], SMALLS[i][1]], SMALLS[i][2], 0, 1009, 540)[1]); }
    for (i = 0; i < 3; i++) last = i === 0 ? noses[0] : Math.min(last, noses[i]);
    var START = 4.0, RATE = 470;
    var rise = function (t) { return Math.max(last - GROUND_Y, 380 - RATE * Math.max(0, t - START)); };
    move("pv-ground", function (t) { return [0, rise(t)]; });
    for (i = 0; i < 3; i++) {
        (function (k) {
            var s = SMALLS[k], n = "pv-small-" + (k + 1), hit = START + (380 - (noses[k] - GROUND_Y)) / RATE;
            var sway = function (t) { return t < hit ? 3 * Math.sin(2 * Math.PI * 0.7 * t + k) : 0; };
            // Down, it rides up with the ground.
            var lift = function (t) { return Math.min(0, GROUND_Y + rise(t) - noses[k]); };
            move(n, function (t) { return [s[0] - 968, s[1] - 540 + lift(t)]; });
            turn(n, function (t) { return 90 + sway(t); });
            grow(n, function () { return s[2]; });
            var north = k === NORTH - 1, day = k === DAY - 1;
            var sends = every(0.3 + 0.25 * k, day ? DUR : hit, 0.75);
            if (north && failure) sends = until(sends, SPARK);
            ringsAt(["pv-ring-" + (2 * k + 1), "pv-ring-" + (2 * k + 2)], sends,
                    function (t) { var c = [s[0], s[1] + lift(t)]; return smallPoint(c, s[2], sway(t), 920, 540); },
                    north && glitch ? function (te) { return te > SPARK ? 0.4 : 1; } : null);
            var d = "pv-dust-" + (k + 1);
            move(d, function (t) { return [s[0] - 970, noses[k] - GROUND_Y + lift(t)]; });
            grow(d, track([[hit, 0.4 * s[2]], [hit + 0.4, 0.9 * s[2], "out"]]));
            fade(d, track([[hit, 0, "snap"], [hit + 0.02, 1, "snap"], [hit + 0.3, 1], [hit + 0.8, 0, "lin"]]));
            if (north) sparkAt = function () { return smallPoint([s[0], s[1]], s[2], 0, 930, 590); };
        })(i);
    }
    for (i = 1; i <= 4; i++) hide("ring-" + i);
    hide("r-main");
}
// The spark: at the temperature sensor's arm (a child of the large probe; on North's flank, small).
if (glitch || failure) {
    fade("pv-spark", track([[SPARK, 0, "snap"], [SPARK + 0.02, 1, "snap"], [SPARK + 0.3, 1], [SPARK + 0.6, 0, "lin"]]));
    grow("pv-spark", track([[SPARK, BIG ? 0.6 : 0.9], [SPARK + 0.3, BIG ? 1.0 : 1.4, "out"]]));
    if (!BIG) move("pv-spark", function () { var q = sparkAt(); return [q[0] - 846, q[1] - 624]; });
}
