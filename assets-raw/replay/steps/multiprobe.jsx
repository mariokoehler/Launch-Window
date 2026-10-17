// Pioneer Venus Multiprobe's scenes (2026-10-17), after orbit.jsx (its radio rings): the bus spinning about its axis
// (lying left to right, its forward end right) with its probes on that end, and the probes on their own, nose down.
// parts.py's multiprobe_parts() draws them.
var MP = {cy: 540, R: 175, r: 150, base: 840, tilt: 0.15};
// The probes' names, left to right in the entry and descent: North (the one the trouble comes to), Day, Night.
var NORTH = 1, DAY = 2, NIGHT = 3;
// The ground line of pv-ground at rest (parts.py's GROUND_Y).
var GROUND_Y = 880;

// An angle (degrees) integrated frame by frame from start at rate(t) degrees a second.
function spinAngles(start, rate) {
    var a = [], x = start;
    for (var f = 0; f <= NFRAMES + 1; f++) { a.push(x); x += rate(f / FPS) / FPS; }
    return function (t) { return a[clamp(Math.round(t * FPS), 0, a.length - 1)]; };
}

// The bus and what it carries. A small probe goes round in two copies, one behind the large probe (b) and one in
// front, the one shown by which half it's in.
function busScene(withBig) {
    put("paper", "screen");
    put("pv-bus");
    for (var i = 1; i <= 8; i++) put("pv-cell-" + i);
    for (i = 1; i <= 3; i++) put("pv-small-" + i + "b");
    if (withBig) {
        put("pv-big");
        put("pv-big-sphere", "pv-big");
        hide("pv-big-sphere");
        put("pv-big-aft", "pv-big");
        put("pv-big-shield", "pv-big");
    }
    for (i = 1; i <= 3; i++) put("pv-small-" + i);
    put("pv-latch");
    hide("pv-latch");
    for (i = 1; i <= 3; i++) { put("pv-clamp-" + i); hide("pv-clamp-" + i); }
    mpCamera(1.5, 860, 540);
}
// The camera in k times on (x, y), put at the screen's centre. After every put() (see probe.jsx's probeCamera).
function mpCamera(k, x, y) {
    tf(world, "Scale").setValue([100 * k, 100 * k]);
    tf(world, "Position").setValue([960 - k * x, 540 - k * y]);
}

// The bus turned spin(t) degrees, moved by shift(t) px along its axis: the cells go round its side.
function busSpin(spin, shift) {
    move("pv-bus", function (t) { return [shift(t), 0]; });
    for (var i = 1; i <= 8; i++) {
        (function (n, k) {
            var phi = function (t) { return (spin(t) + 45 * k) * RAD; };
            move(n, function (t) { return [shift(t), MP.R * Math.sin(phi(t))]; });
            fade(n, function (t) { return clamp(Math.cos(phi(t)) * 1.6, 0, 1); });
        })("pv-cell-" + i, i);
    }
}

// Where a small probe is (x, y, z: z towards us) at its angle phi (degrees) on the bus moved by dx.
function onBus(phi, dx) {
    var a = phi * RAD;
    return [MP.base + dx, MP.cy + MP.r * Math.sin(a), MP.r * Math.cos(a)];
}
// Puts small probe k at p(t) = [x, y, z], seen a little from the right, nearer ones bigger; its copy in front shows
// while z >= 0.
function placeSmall(k, p) {
    var at = function (t) { var q = p(t); return [q[0] + MP.tilt * q[2] + 8 - 968, q[1] - 540]; };
    var size = function (t) { return 1 + p(t)[2] / 2400; };
    var names = ["pv-small-" + k, "pv-small-" + k + "b"];
    for (var j = 0; j < 2; j++) {
        (function (n, front) {
            move(n, at);
            grow(n, size);
            fade(n, function (t) { return (p(t)[2] >= 0) === front ? 1 : 0; });
        })(names[j], j === 0);
    }
    return at;
}

// Radio rings from where(t) at the given times, using the rings named (two are enough for one every 0.75 s).
function ringsAt(names, sends, where, strength) {
    for (var i = 0; i < names.length; i++) {
        (function (n, k) {
            var mine = function (t) {
                var last = -1;
                for (var j = k; j < sends.length; j += names.length) if (sends[j] <= t) last = sends[j];
                return last;
            };
            move(n, function (t) { var te = mine(t); var p = where(te < 0 ? 0 : te); return [p[0] - 960, p[1] - 560]; });
            grow(n, function (t) { var te = mine(t); return te < 0 ? 0.3 : 0.3 + 1.6 * clamp((t - te) / 0.9, 0, 1); });
            fade(n, function (t) {
                var te = mine(t);
                return te < 0 ? 0 : (1 - clamp((t - te) / 0.9, 0, 1)) * (strength ? strength(te) : 1);
            });
        })(names[i], i);
    }
}
// The times in list before b.
function until(list, b) { var out = []; for (var i = 0; i < list.length; i++) if (list[i] < b) out.push(list[i]); return out; }
// Every step s from a to b.
function every(a, b, s) { var out = []; for (var t = a; t < b; t += s) out.push(t); return out; }

// The large probe nose down at scale k, its shield's base (the carrier's pivot) at (x, y): a point drawn at (px, py)
// in its nose-right drawing lands at ...
function bigPoint(base, k, a, px, py) {
    var r = rot2([(px - 870) * k, (py - 540) * k], 90 + a);
    return [base[0] + r[0], base[1] + r[1]];
}
// ... and a small probe's (its pivot, its middle, at c).
function smallPoint(c, k, a, px, py) {
    var r = rot2([(px - 968) * k, (py - 540) * k], 90 + a);
    return [c[0] + r[0], c[1] + r[1]];
}
function rot2(v, deg) {
    var a = deg * RAD;
    return [v[0] * Math.cos(a) - v[1] * Math.sin(a), v[0] * Math.sin(a) + v[1] * Math.cos(a)];
}
// Where the probes fall in the entry and the descent: [x, y, size] each, North, Day, Night.
var SMALLS = [[540, 430, 1.4], [1050, 500, 1.6], [1450, 400, 1.3]];
