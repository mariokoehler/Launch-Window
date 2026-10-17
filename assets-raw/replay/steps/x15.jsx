// The X-15 on its own (2026-10-17), after moon.jsx and air.jsx: w-plane at full size, its pivot (WS) at PLANE, turned by
// pitch(t) and moved by off(t), and what rides on it but must leave it (the ventral), placed from the same pose.
var PLANE = [825, 580];

function x15Scene() {
    put("paper", "screen");
    put("d-earth");
    for (var i = 1; i <= 6; i++) put("r-cloud-" + i);
    for (i = 1; i <= 8; i++) put("a-flow-" + i);
    put(K.ventral);
    put(K.plane);
    put(K.skid, K.plane);
    put(K.flame, K.plane);
    var kids = [K.shock, K.heat, K.hot, K.rcsUp, K.rcsDown];
    for (i = 0; i < kids.length; i++) { put(kids[i], K.plane); hide(kids[i]); }
    hide(K.skid);
    hide(K.flame);
    for (i = 1; i <= 6; i++) hide("r-cloud-" + i);
    for (i = 1; i <= 8; i++) hide("a-flow-" + i);
}
// Where a point drawn at p on w-plane is, with the plane turned pitch and moved by o.
function onPlane(pitch, o, p) {
    var a = pitch * RAD, dx = p[0] - WS[0], dy = p[1] - WS[1];
    return [PLANE[0] + o[0] + dx * Math.cos(a) - dy * Math.sin(a), PLANE[1] + o[1] + dx * Math.sin(a) + dy * Math.cos(a)];
}
function x15Fly(pitch, off) {
    move(K.plane, function (t) { var o = off(t); return [PLANE[0] - WS[0] + o[0], PLANE[1] - WS[1] + o[1]]; });
    turn(K.plane, pitch);
}
// The ventral on the plane until `free` (99: never), then moved by away(u) ([dx, dy, turn]) from where it left.
function ventral(pitch, off, free, away) {
    var V = K.V;
    var at = function (t) { var u = Math.min(t, free); return onPlane(pitch(u), off(u), V); };
    var by = function (t) { return t > free ? away(t - free) : [0, 0, 0]; };
    move(K.ventral, function (t) { var p = at(t), a = by(t); return [p[0] - V[0] + a[0], p[1] - V[1] + a[1]]; });
    turn(K.ventral, function (t) { return pitch(Math.min(t, free)) + by(t)[2]; });
    return function (t) { var p = at(t), a = by(t); return [p[0] + a[0], p[1] + a[1]]; };
}
// A puff from an attitude jet: on for 0.25 s from each time in `times`.
function puffs(name, times) {
    fade(name, function (t) { for (var i = 0; i < times.length; i++) if (t >= times[i] && t < times[i] + 0.25) return 1; return 0; });
    grow(name, function (t) { return [1, 0.8 + 0.3 * Math.sin(2 * Math.PI * 14 * t)]; });
}
// Streaks of air past it at speed(t) px/s, in lanes about the screen (0 hides them).
function streaks(speed) {
    var xs = [], x = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { xs.push(x); x += speed(f / FPS) / FPS; }
    var gone = function (t) { return xs[clamp(Math.round(t * FPS), 0, xs.length - 1)]; };
    for (var i = 1; i <= 8; i++) {
        (function (n, k) {
            move(n, function (t) { var s = ((k * 251 + gone(t)) % 1600 + 1600) % 1600 - 800; return [-s, -300 + 70 * k - 20]; });
            fade(n, function (t) { return speed(t) > 50 ? 1 : 0; });
        })("a-flow-" + i, i);
    }
}
