// Earth departure (2026-10-16): after the injection burn, the probe on its own, close up and nose right,
// the Earth shrinking behind it on the left. BASPM gives the step to the probe alone (no booster seat;
// navigation weighs most), so it comes alive: puffs spin it up (0.6), its boom unfolds (1.2 to 2.0), its
// dish swings round to the Earth (2.2 to 2.8), a tracking pulse goes out from the Earth (3.0) and comes
// back from the probe (3.7): the path is confirmed. Its lamps come on and it calls home from 4.3.
// Glitch: the boom sticks a third of the way out (1.45); a jolt frees it at 2.5, and the rest follows, later.
// Failure: the boom sticks, and the jolt at 2.5 doesn't free it: the probe starts to wobble (2.8), its
// dish swings about looking for the Earth, and the Earth's pulses reach it and get no answer.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var GX = 1120, GY = 500, STUCK = 1.45, JOLT = 2.5;
var DISH = glitch ? 3.1 : 2.2, OUT = glitch ? 3.7 : 3.0, TRIP = 0.6, BACK = OUT + TRIP + 0.1, LOCK = BACK + TRIP;

put("paper", "screen");
put("o-earth");
for (var i = 1; i <= 4; i++) put("ring-" + i);
put("g-pulse-out");
put("g-pulse-back");
put("g-probe");
put("g-boom", "g-probe");
put("g-dish", "g-probe");
put("g-lamps", "g-probe");
put("g-puff-1", "g-probe");
put("g-puff-2", "g-probe");

// The Earth falls away: smaller and nearer its vanishing point (560 620) as it goes.
var E0 = 1.25;
function earthScale(t) { return E0 / (1 + 0.45 * t); }
function earthAt(t) { var k = earthScale(t) / E0; return [560 + (230 - 560) * k, 620 + (760 - 620) * k]; }
move("o-earth", function (t) { var c = earthAt(t); return [c[0] - 960, c[1] - 560]; });
grow("o-earth", earthScale);
turn("o-earth", function (t) { return -0.25 * t; });

// The probe drifts on, slowly; the glitch's jolt shakes it, the failure's wobble grows.
function probeAt(t) { return [GX + 10 * t, GY - 4 * t]; }
function rot(t) {
    var r = 0;
    if ((glitch || failure) && t > JOLT) r += 5 * Math.exp(-3 * (t - JOLT)) * Math.sin(2 * Math.PI * 3 * (t - JOLT));
    if (failure && t > 2.8) r += 16 * clamp((t - 2.8) / 1.8, 0, 1) * Math.sin(2 * Math.PI * 1.1 * (t - 2.8)) + 4 * (t - 2.8);
    return r;
}
move("g-probe", function (t) { var p = probeAt(t); return [p[0] - GX, p[1] - GY]; });
turn("g-probe", rot);

// Spin-up puffs.
for (i = 1; i <= 2; i++) {
    grow("g-puff-" + i, track([[0.6, 0.4], [1.0, 1.4, "out"], [1.4, 1.8, "lin"]]));
    fade("g-puff-" + i, track([[0.6, 0, "snap"], [0.62, 1, "snap"], [1.0, 1], [1.4, 0, "lin"]]));
}

// The boom: folded forward along the hull (88), out to -25.
var unfold = track([[1.2, 88], [2.0, -25]]);
var boom = function (t) {
    if (!(glitch || failure) || t < STUCK) return unfold(t);
    var held = unfold(STUCK);
    if (glitch && t > JOLT) return track([[JOLT, held], [JOLT + 0.5, -25, "out"]])(t);
    return held + (t > JOLT ? 3 * Math.exp(-4 * (t - JOLT)) * Math.sin(2 * Math.PI * 4 * (t - JOLT)) : 0);
};
turn("g-boom", boom);

// The dish: folded forward under the hull (-90), round to face the Earth (78); the failure's hunts.
var dish = failure
    ? function (t) { return t < 3.0 ? -90 : track([[3.0, -90], [3.5, 20, "out"]])(t) + (t > 3.5 ? 55 * Math.sin(2 * Math.PI * 0.45 * (t - 3.5)) : 0); }
    : track([[DISH, -90], [DISH + 0.6, 78, "out"]]);
turn("g-dish", dish);

// Where things are in the world: a point of the probe's drawing (at rest), as the probe moves and turns.
function onProbe(x, y, t) {
    var p = probeAt(t), a = rot(t) * RAD, dx = x - GX, dy = y - GY;
    return [p[0] + dx * Math.cos(a) - dy * Math.sin(a), p[1] + dx * Math.sin(a) + dy * Math.cos(a)];
}
function dishTip(t) {
    var f = onProbe(GX - 95, GY + 74, t), a = (rot(t) + dish(t)) * RAD;
    return [f[0] - 80 * Math.sin(a), f[1] + 80 * Math.cos(a)];
}
function earthEdge(t, to) {
    var c = earthAt(t), dx = to[0] - c[0], dy = to[1] - c[1], d = Math.sqrt(dx * dx + dy * dy), r = 268 * earthScale(t);
    return [c[0] + r * dx / d, c[1] + r * dy / d];
}
// A pulse on its way: sent at each of sends, TRIP seconds from one end to the other.
function pulse(name, sends, from, to) {
    function trip(t) { var last = -1; for (var j = 0; j < sends.length; j++) if (sends[j] <= t) last = sends[j]; return last; }
    function at(t) {
        var s = trip(t), u = s < 0 ? 0 : clamp((t - s) / TRIP, 0, 1), a = from(t), b = to(t);
        return [a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u];
    }
    grow(name, function () { return 1.5; });
    move(name, function (t) { var p = at(t); return [p[0] - 960, p[1] - 560]; });
    turn(name, function (t) { var a = from(t), b = to(t); return Math.atan2(b[1] - a[1], b[0] - a[0]) / RAD; });
    fade(name, function (t) { var s = trip(t); if (s < 0) return 0; var u = (t - s) / TRIP; return u > 1 ? 0 : clamp(u / 0.1, 0, 1) * clamp((1 - u) / 0.15, 0, 1); });
}
pulse("g-pulse-out", failure ? [3.6, 4.8] : [OUT], function (t) { return earthEdge(t, dishTip(t)); }, dishTip);
if (failure) hide("g-pulse-back");
else pulse("g-pulse-back", [BACK], dishTip, function (t) { return earthEdge(t, dishTip(t)); });

// Locked on: the lamps come on and it calls home.
fade("g-lamps", function (t) { return failure ? 0 : clamp((t - LOCK) / 0.3, 0, 1); });
var sends = [];
if (!failure) for (var s = LOCK + 0.1; s < DUR; s += 0.75) sends.push(s);
radioRingsAt(sends, dishTip);
