// The suited figure out on the surface beside the lander, for Apollo's moonwalks (flag.jsx, samples.jsx; the
// experiment package's moonwalk.jsx has its own copy). Put "e-suit" before the lander so it steps out from
// behind the ascent stage. A pose is [feet x, feet y, turn]: where the figure's feet are (world px) and how far
// it leans (forward is positive).
var S = 0.45;
var EP = [870, 300], FP = [876, 366], EH = [916, 296];
var INSIDE = [962, 722, 0], PORCH = [1032, 722, 0], FOOT = [1112, 880, 0];

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
// Several hop spans on one walk: spans [[t0, t1, n], ...].
function hopping(pose, spans) {
    var w = pose;
    for (var i = 0; i < spans.length; i++) w = hops(w, spans[i][0], spans[i][1], spans[i][2]);
    return w;
}
// The figure on its walk.
function walker(walk) {
    move("e-suit", function (t) { var p = walk(t), e = at(p, EP); return [e[0] - EP[0], e[1] - EP[1]]; });
    turn("e-suit", function (t) { return walk(t)[2]; });
    grow("e-suit", function () { return S; });
}
// A thing in the hand (its pivot where the hand holds it) at scale k: held(t) says whether it is.
function inHand(name, walk, k, rest, held) {
    grow(name, function () { return k; });
    var r = L[drawn(name)];
    move(name, function (t) {
        var p = held(t) ? at(walk(t), EH) : rest(t);
        return [p[0] - r.px, p[1] - r.py];
    });
}
