// The air launch's scenes (after moon.jsx, for the lamp): the carrier plane with the rocket plane on a pylon
// under its wing, seen from further out (0.7), the camera riding along with the carrier. The rocket plane
// (w-plane at 0.6) is placed from the carrier's pose: where it hangs, plus its own offset once dropped.
var AC = [700, 772], WS = [830, 880], HANG = [859, 716], XK = 0.6, ZOOM = 0.7;

// ground: the runway (takeoff) or clouds below (in flight). Then the carrier, the rocket plane and its
// flame, the smoke, the speed lines.
function airScene(ground) {
    put("paper", "screen");
    if (ground) {
        put("a-ground");
    } else {
        for (var i = 1; i <= 6; i++) put("r-cloud-" + i);
    }
    for (i = 1; i <= 8; i++) put("a-flow-" + i);
    put("a-smoke");
    put("a-carrier");
    put("a-gear", "a-carrier");
    // The gear is down on the runway, put away in flight.
    if (!ground) hide("a-gear");
    put("w-plane");
    put("w-skid", "w-plane");
    // The X-15's lower fin under its tail (2026-10-17), dropped before landing (x15-descent).
    put("x15-ventral", "w-plane");
    put("w-flame", "w-plane");
    hide("a-smoke");
    hide("w-flame");
    grow("w-plane", function () { return XK; });
}
// The camera on the carrier's offset c(t) ([x, y]): the world's (960 600) at the screen's (960 sy) when c
// is 0 (560, or lower over the runway, so its ground fills the frame's foot).
function airCamera(c, sy) {
    var y0 = sy || 560;
    tf(world, "Scale").setValue([100 * ZOOM, 100 * ZOOM]);
    bake(tf(world, "Position"), function (t) { var o = c(t); return [960 - ZOOM * (960 + o[0]), y0 - ZOOM * (600 + o[1])]; });
}
// The carrier's pose [x, y, pitch] and the rocket plane hung under it, or let go and moved by drop(t)
// ([dx, dy, pitch]) from where it was hung.
function fly(carrier, drop) {
    move("a-carrier", function (t) { var p = carrier(t); return [p[0], p[1]]; });
    turn("a-carrier", function (t) { return carrier(t)[2]; });
    move("w-plane", function (t) {
        var p = carrier(t), a = p[2] * RAD, dx = HANG[0] - AC[0], dy = HANG[1] - AC[1];
        var h = [AC[0] + p[0] + dx * Math.cos(a) - dy * Math.sin(a), AC[1] + p[1] + dx * Math.sin(a) + dy * Math.cos(a)];
        var d = drop ? drop(t) : [0, 0, 0];
        return [h[0] - WS[0] + d[0], h[1] - WS[1] + d[1]];
    });
    turn("w-plane", function (t) { return carrier(t)[2] + (drop ? drop(t)[2] : 0); });
}
// The speed lines: streaming back past the carrier at `speed` px/s (0 to hide them), spread about it.
function flow(carrier, speed) {
    for (var i = 1; i <= 8; i++) {
        (function (n, k) {
            var y0 = -260 + 70 * k, x0 = (k * 397) % 1600;
            move(n, function (t) {
                var p = carrier(t), s = speed(t);
                var x = ((x0 - s * t) % 1600 + 1600) % 1600 - 800;
                return [p[0] + x, p[1] + y0];
            });
            fade(n, function (t) { return speed(t) > 50 ? 1 : 0; });
        })("a-flow-" + i, i);
    }
}
// The clouds below, going by at `speed` px/s, wrapped round 2800 px.
function clouds(carrier, speed) {
    for (var i = 1; i <= 6; i++) {
        (function (n, k) {
            move(n, function (t) {
                var p = carrier(t), L = layers[n].rest;
                var x = ((k * 470 - speed * t) % 2800 + 2800) % 2800 - 1100;
                return [p[0] + x - L[0] + 960, p[1] + 900 + 60 * (k % 3) - L[1]];
            });
        })("r-cloud-" + i, i);
    }
}
