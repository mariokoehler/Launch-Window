// A moonwalk (and the other steps out on the surface): the lander on the Moon, close in. The suited
// figure steps out from behind the ascent stage onto the porch, climbs down the ladder, hops out with
// the experiment package and sets it on the ground at 2.4, then hops back and climbs in.
// Glitch: at 2.0, mid-hop, it trips and falls flat (as Apollo 16's crew did), the package thrown; it
// gets up at 2.6 and sets the package out after all.
// Failure: the same fall, and at 2.6 its visor fogs over: it gets up at last and goes back in, the
// package left lying where it fell.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var TRIP = 2.0, ANSWER = 2.6, S = 0.45;
var EP = [870, 300], FP = [876, 366], EH = [916, 296], BOX = [975, 360], BOXK = 0.6;

put("paper", "screen");
put("n-ground");
put("n-rocks");
flagPlanted();
put("e-suit");
put("e-fog", "e-suit");
hide("e-fog");
put("e-box");
lander();
put("n-ladder");
// The camera in close on the ladder and the ground to its right: twice, (1130 800) at (960 600).
tf(world, "Scale").setValue([200, 200]);
tf(world, "Position").setValue([960 - 2 * 1130, 600 - 2 * 800]);

// A pose is [feet x, feet y, turn]: where the figure's feet are (world px) and how far it leans.
function at(pose, p) {
    var a = pose[2] * RAD, x = S * (p[0] - FP[0]), y = S * (p[1] - FP[1]);
    return [pose[0] + x * Math.cos(a) - y * Math.sin(a), pose[1] + x * Math.sin(a) + y * Math.cos(a)];
}
function keys3(keys) {
    var f = [];
    for (var i = 0; i < 3; i++) {
        var ks = [];
        for (var j = 0; j < keys.length; j++) ks.push([keys[j][0], keys[j][1][i], keys[j][2] || "inout"]);
        f.push(track(ks));
    }
    return function (t) { return [f[0](t), f[1](t), f[2](t)]; };
}
// Hops: the feet leave the ground in arcs, n of them between t0 and t1.
function hops(pose, t0, t1, n) {
    return function (t) {
        var p = pose(t);
        if (t <= t0 || t >= t1) return p;
        return [p[0], p[1] - 16 * Math.abs(Math.sin(Math.PI * n * (t - t0) / (t1 - t0))), p[2]];
    };
}
var INSIDE = [962, 722, 0], PORCH = [1032, 722, 0], FOOT = [1112, 880, 0], SPOT = [1236, 880, 0];
var out = [[0, INSIDE], [0.6, PORCH], [1.4, FOOT], [2.4, SPOT]];
var walk, set = 99, lie;
if (glitch || failure) {
    var going = hops(keys3(out), 1.4, 2.4, 3), down = going(TRIP);
    var fallen = [down[0] + 20, 880, 84];
    var up = glitch ? ANSWER : 3.0, home = glitch ? 3.4 : 3.3;
    var back = keys3([[TRIP, down], [TRIP + 0.3, fallen, "in"], [up, fallen], [up + 0.4, [down[0] + 20, 880, 0]],
                      [home, [down[0] + 20, 880, 0]], [home + 0.9, FOOT], [home + 1.7, PORCH], [home + 2.1, INSIDE]]);
    walk = function (t) { return t <= TRIP ? going(t) : hops(back, home, home + 0.9, 3)(t); };
    if (glitch) set = 3.2;
} else {
    var all = keys3(out.concat([[2.6, [SPOT[0], SPOT[1], 24]], [2.8, SPOT], [3.8, FOOT], [4.6, PORCH], [5.0, INSIDE]]));
    walk = function (t) { return hops(hops(all, 1.4, 2.4, 3), 2.8, 3.8, 3)(t); };
    set = 2.5;
}
move("e-suit", function (t) { var p = walk(t), e = at(p, EP); return [e[0] - EP[0], e[1] - EP[1]]; });
turn("e-suit", function (t) { return walk(t)[2]; });
grow("e-suit", function () { return S; });
// The package: in the hand, then set on the ground beside the spot (or, tripping, thrown ahead and
// left lying on its side, until the glitch sets it out).
var REST = [SPOT[0] + 34, 880 - BOXK * 20];
grow("e-box", function () { return BOXK; });
var thrown = [REST[0] + 30, 880 - BOXK * 12];
move("e-box", function (t) {
    var p;
    if (t < Math.min(set - 0.1, TRIP) || !(glitch || failure) && t < set - 0.1) { p = at(walk(t), EH); }
    else if (!(glitch || failure)) { var h = at(walk(set - 0.1), EH), w = clamp((t - set + 0.1) / 0.2, 0, 1); p = [mix(h[0], REST[0], w), mix(h[1], REST[1], w)]; }
    else {
        var h2 = at(walk(TRIP), EH), w2 = EASE.out(clamp((t - TRIP) / 0.4, 0, 1));
        p = [mix(h2[0], thrown[0], w2), mix(h2[1], thrown[1], w2) - 30 * Math.sin(Math.PI * w2)];
        if (glitch && t >= set - 0.3) { var w3 = clamp((t - set + 0.3) / 0.3, 0, 1); p = [mix(p[0], REST[0], w3), mix(p[1], REST[1], w3)]; }
    }
    return [p[0] - BOX[0], p[1] - BOX[1]];
});
turn("e-box", function (t) {
    if (!(glitch || failure) || t < TRIP) return 0;
    var spin = 90 * EASE.out(clamp((t - TRIP) / 0.4, 0, 1));
    return glitch && t >= set - 0.3 ? spin * (1 - clamp((t - set + 0.3) / 0.3, 0, 1)) : spin;
});
if (failure) fade("e-fog", track([[ANSWER, 0], [2.9, 1, "out"]]));
