// Motes (2026-10-16): faint dashes hanging in space round a craft in orbit, streaming past against
// its way, so which way it's going shows before a burn (drawn going right: they stream left). Their
// speed is speed(t) px a second, integrated frame by frame, and their length follows it, so a burn
// that slows the craft slows them. They wrap round off-screen. Put them before the Earth, which hides
// those below its limb.
var MOTE_SPEED = 760, MOTE_AT = {};
// The Earth's fill lets them show through: hide each one while it's below the limb, the Earth (d-earth,
// round (960, 3100), its top at 795) raised by sink(t).
function motesBelow(sink) {
    for (var n in MOTE_AT) {
        (function (n) {
            fade(n, function (t) {
                var p = MOTE_AT[n](t), dy = p[1] - (3100 - sink(t)), dx = p[0] - 960;
                return Math.sqrt(dx * dx + dy * dy) > 3100 - 795 + 6 ? 1 : 0;
            });
        })(n);
    }
}
function motes(speed) {
    var xs = [], x = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { xs.push(x); x -= speed(f / FPS) / FPS; }
    var seed = 7;
    function rnd() { seed = (seed * 16807) % 2147483647; return seed / 2147483647; }
    for (var i = 1; i <= 14; i++) {
        // Spread across the width (a slot each, jittered), at any height above the Earth's limb.
        var x0 = -150 + (i - 1 + 0.8 * rnd()) * 2220 / 14, y = 60 + 700 * rnd(), k = 0.6 + 0.8 * rnd();
        put("r-mote-" + i);
        (function (n, x0, y, k) {
            MOTE_AT[n] = function (t) {
                var d = xs[clamp(Math.round(t * FPS), 0, xs.length - 1)];
                return [((x0 + d + 150) % 2220 + 2220) % 2220 - 150, y];
            };
            move(n, function (t) { var p = MOTE_AT[n](t); return [p[0] - 960, p[1] - 470]; });
            grow(n, function (t) { return [Math.max(0.05, k * speed(t) / MOTE_SPEED), 1]; });
        })("r-mote-" + i, x0, y, k);
    }
}
