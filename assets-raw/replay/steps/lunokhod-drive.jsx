// Lunokhod drive (2026-10-16): Luna 17's second surface operations. On from the deploy's last frame, its
// lid open to the Sun, the rover drives on, steered from Earth (a call every 1.2 s), the camera with it,
// up and over a crater's rim (the crest at 4.2); on top, a laser from Earth finds the reflector on its
// nose and bounces back (4.4 to 5.6). Lunokhod 1's reflector is still used, rediscovered in 2010.
// Glitch: on the rim's slope its wheels slip in the loose soil (2.8): it slides back, spinning, waits out
// the drivers' delay, and tries again (4.2), slowly, to the crest.
// Failure (Lunokhod 2's end, May 1973): backing off the rim (2.6), it lurches, and its open lid hits the
// slope behind and scoops up soil (3.0). It shuts the lid for the night (3.8 to 4.6), the soil lying on
// it over the radiator; it overheats (the glow, from 4.6), and its calls stop.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";

lunokhodScene(true);
// The ramps down, as the deploy left them.
turn("u-ramp-f", function () { return 30; });
turn("u-ramp-r", function () { return -30; });

var x, extra = null, pivot = null, spin = null, open;
if (failure) {
    x = track([[0, 1420], [2.4, 1860, "out"], [2.6, 1860], [3.2, 1820, "inout"]]);
    extra = track([[2.6, 0], [3.0, -24, "out"], [3.4, 0, "inout"]]);
    pivot = [-HALF, 0];
    open = track([[0, -115], [2.8, -115], [3.0, -150, "out"], [3.4, -115, "inout"], [3.8, -115], [4.6, 0, "inout"]]);
} else if (glitch) {
    x = track([[0, 1420], [2.8, 1900, "lin"], [3.6, 1868, "out"], [4.2, 1868], [6.4, 2020, "inout"]]);
    spin = function (t) { return 600 * clamp(t - 2.8, 0, 0.8); };
    open = function () { return -115; };
} else {
    x = track([[0, 1420], [6.4, 2290, "lin"]]);
    open = function () { return -115; };
}
var at = drive(x, extra, pivot, spin);
lid(open);
// The camera keeps the rover where the deploy left it.
lookAt(function (t) { return 1130 + x(t) - 1420; });

// Its calls: every 1.2 s, until the failure's overheat (the last before it).
sending(at, failure ? [0.4, 1.6, 2.8] : [0.4, 1.6, 2.8, 4.0, 5.2]);

// The dust: at the wheels as they slip (glitch), or where the lid hits the slope (failure).
if (glitch || failure) {
    var from = glitch ? 2.8 : 2.9, to = glitch ? 3.6 : 3.3;
    var where = glitch ? function (t) { return onRover(at, t, [HALF, 0]); }
                       : function (t) { return onRover(at, Math.min(t, to), [-150, -40]); };
    move("u-kick", function (t) { var p = where(Math.min(Math.max(t, from), to)); return [p[0] - 870, p[1] - 874]; });
    fade("u-kick", function (t) { return t < from ? 0 : t < to ? 1 : clamp(1 - (t - to) / 0.6, 0, 1); });
    grow("u-kick", function (t) { return 0.9 + 0.2 * Math.sin(2 * Math.PI * 4 * t); });
}
if (failure) {
    fade("u-soil", function (t) { return t >= 3.0 ? 1 : 0; });
    fade("u-glow", track([[4.6, 0], [5.8, 0.9, "lin"]]));
}

// The laser: from the reflector on its nose up to Earth (up right), on the crest.
if (!(glitch || failure)) {
    move("u-laser", function (t) { var p = onRover(at, t, [77, -62]); return [p[0], p[1]]; });
    fade("u-laser", function (t) { return t >= 4.4 && t < 5.6 ? (Math.floor(t * 8) % 2 === 0 ? 1 : 0.6) : 0; });
}
