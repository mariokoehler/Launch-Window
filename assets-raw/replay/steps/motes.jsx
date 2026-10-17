// Motes (2026-10-16): faint dashes hanging in space round a craft in orbit, streaming past against
// its way, so which way it's going shows before a burn (drawn going right: they stream left). Their
// speed is speed(t) px a second, integrated frame by frame, and their length follows it, so a burn
// that slows the craft slows them. They wrap round off-screen. Put them before the Earth, which hides
// those below its limb.
// way(t) (2026-10-17, optional): the craft's way, in degrees from level (y down: negative climbs), for a
// craft on a slope (the suborbital arc); the dashes stream and lie along it. settle (optional): shift
// them so the last frame has them where a clip starting motes() afresh has its first, as the next
// step's (the suborbital flight's, before the retrofire's).
var MOTE_SPEED = 760, MOTE_AT = {};
// The Earth's fill lets them show through: hide each one while it's below the limb, the Earth (d-earth,
// round (960, 3100), its top at 795) raised by sink(t), or moved by it if it gives [dx, dy].
function motesBelow(sink) {
    for (var n in MOTE_AT) {
        (function (n) {
            fade(n, function (t) {
                var s = sink(t), o = s instanceof Array ? s : [0, -s];
                var p = MOTE_AT[n](t), dy = p[1] - (3100 + o[1]), dx = p[0] - (960 + o[0]);
                return Math.sqrt(dx * dx + dy * dy) > 3100 - 795 + 6 ? 1 : 0;
            });
        })(n);
    }
}
function motes(speed, way, settle) {
    if (typeof way === "undefined") way = function () { return 0; };
    var xs = [], ys = [], x = 0, y = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) {
        xs.push(x); ys.push(y);
        var a = way(f / FPS) * RAD;
        x -= speed(f / FPS) * Math.cos(a) / FPS;
        y -= speed(f / FPS) * Math.sin(a) / FPS;
    }
    if (settle) {
        var last = clamp(Math.round(DUR * FPS), 0, xs.length - 1), lx = xs[last], ly = ys[last];
        for (f = 0; f < xs.length; f++) { xs[f] -= lx; ys[f] -= ly; }
    }
    var seed = 7;
    function rnd() { seed = (seed * 16807) % 2147483647; return seed / 2147483647; }
    for (var i = 1; i <= 14; i++) {
        // Spread across the width (a slot each, jittered), at any height above the Earth's limb.
        var x0 = -150 + (i - 1 + 0.8 * rnd()) * 2220 / 14, y = 60 + 700 * rnd(), k = 0.6 + 0.8 * rnd();
        put("r-mote-" + i);
        (function (n, x0, y, k) {
            MOTE_AT[n] = function (t) {
                var f = clamp(Math.round(t * FPS), 0, xs.length - 1);
                return [((x0 + xs[f] + 150) % 2220 + 2220) % 2220 - 150, ((y + ys[f] - 60) % 700 + 700) % 700 + 60];
            };
            move(n, function (t) { var p = MOTE_AT[n](t); return [p[0] - 960, p[1] - 470]; });
            turn(n, way);
            grow(n, function (t) { return [Math.max(0.05, k * speed(t) / MOTE_SPEED), 1]; });
        })("r-mote-" + i, x0, y, k);
    }
}
