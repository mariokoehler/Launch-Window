// Luna 17 and Lunokhod 1's scene (2026-10-16), after orbit.jsx (whose radio rings it sends): the Moon's
// surface close up, the descent stage with its two ramps, and the rover. The rover is posed on its path by
// x, the middle of its wheels: the deck, the front ramp (down to the ground at 30), the ground, and a
// crater's rim further on (lunokhod-drive). Its wheels roll with the distance, and turn on when they slip.
var DECK = 776, HINGE = 1060, RAMP_FOOT = 1240, GROUND = 880, RIM = [1820, 1990, 2160], RIM_H = 48;
var HALF = 54, WHEEL_R = 15, SCALE = 1.7;
var LIDS = ["u-lid", "u-soil", "u-glow"];

function lunokhodScene(rim) {
    put("paper", "screen");
    put("n-ground");
    put("n-rocks");
    if (rim) put("u-rim");
    put("u-ramp-r");
    put("u-stage");
    put("u-ramp-f");
    for (var i = 1; i <= 4; i++) put("ring-" + i);
    put("u-kick");
    hide("u-kick");
    put("u-rover");
    var parts = ["u-ant", "u-lid", "u-soil", "u-glow", "u-tub", "u-wheel-1", "u-wheel-2", "u-wheel-3", "u-wheel-4"];
    for (i = 0; i < parts.length; i++) put(parts[i], "u-rover");
    hide("u-soil");
    hide("u-glow");
    put("u-laser");
    hide("u-laser");
}
// The surface's height under x (world px, y down): the deck, the ramp, the ground, the rim.
function groundY(x) {
    if (x <= HINGE) return DECK;
    if (x <= RAMP_FOOT) return DECK + (x - HINGE) * (GROUND - DECK) / (RAMP_FOOT - HINGE);
    if (x > RIM[0] && x < RIM[2]) { var u = (x - RIM[1]) / (RIM[1] - RIM[0]); return GROUND - RIM_H * (1 - u * u); }
    return GROUND;
}
// The rover on its path at x: where its wheels' foot is (between the front and back wheels) and how far it
// leans (degrees, nose down positive).
function pose(x) {
    var yb = groundY(x - HALF), yf = groundY(x + HALF);
    return [x, (yb + yf) / 2, Math.atan2(yf - yb, 2 * HALF) / RAD];
}
// Where the rover's foot goes, posed at p and leaning a further extra degrees about the point pivot of its
// own drawing (offsets from its foot): that point stays put, the foot swings round it.
function leanFoot(p, extra, pivot) {
    var c = pivot || [0, 0], a0 = p[2] * RAD, a = (p[2] + extra) * RAD;
    return [p[0] + c[0] * Math.cos(a0) - c[1] * Math.sin(a0) - (c[0] * Math.cos(a) - c[1] * Math.sin(a)),
            p[1] + c[0] * Math.sin(a0) + c[1] * Math.cos(a0) - (c[0] * Math.sin(a) + c[1] * Math.cos(a))];
}
// Drives the rover: x(t) its place on the path, extra(t) a lean on top of the path's (about the point
// pivot of its own drawing, or its foot), spin(t) degrees its wheels turn on top of what the distance
// turns them (a slip). Returns the per-frame poses, for the antenna and the dust.
function drive(x, extra, pivot, spin) {
    var turned = [], w = 0, last = x(0);
    for (var f = 0; f <= NFRAMES + 1; f++) {
        var t = f / FPS, now = x(t), p = pose(now);
        w += (now - last) / Math.cos(p[2] * RAD) / WHEEL_R / RAD;
        turned.push(w + (spin ? spin(t) : 0));
        last = now;
    }
    var at = function (t) {
        var p = pose(x(t)), e = extra ? extra(t) : 0;
        var o = leanFoot(p, e, pivot);
        return [o[0], o[1], p[2] + e];
    };
    move("u-rover", function (t) { var o = at(t); return [o[0] - 960, o[1] - DECK]; });
    turn("u-rover", function (t) { return at(t)[2]; });
    for (var i = 1; i <= 4; i++) turn("u-wheel-" + i, function (t) { return turned[clamp(Math.round(t * FPS), 0, turned.length - 1)]; });
    return at;
}
// A point of the rover's drawing in the world at t, from drive's poses.
function onRover(at, t, d) {
    var o = at(t), a = o[2] * RAD;
    return [o[0] + d[0] * Math.cos(a) - d[1] * Math.sin(a), o[1] + d[0] * Math.sin(a) + d[1] * Math.cos(a)];
}
// The lid's opening (degrees, 0 shut, -115 open), with the soil and the glow that ride on it.
function lid(open) {
    for (var i = 0; i < LIDS.length; i++) turn(LIDS[i], open);
}
// The camera: in 1.7 times, looking at fx(t) (world x) with the surface low in the frame.
function lookAt(fx) {
    tf(world, "Scale").setValue([100 * SCALE, 100 * SCALE]);
    bake(tf(world, "Position"), function (t) { return [960 - SCALE * fx(t), 600 - SCALE * 790]; });
}
// The antenna's tip, sending radio rings at the given times.
function sending(at, sends) {
    radioRingsAt(sends, function (t) { return onRover(at, t, [-44, -152]); });
}
