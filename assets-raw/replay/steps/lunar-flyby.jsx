// Lunar flyby: swinging round the Moon on its open path (dashed), seen from further out: in from the
// lower left, behind the Moon at 3.0 (out of contact), and out towards the upper left, its path bent
// back towards the Earth.
// Glitch: it comes out drifting off the line; a correction at 4.0 brings it back.
// Failure: the same drift, and the correction misfires: it's thrown off, and lost.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
// Round the Moon unless a planet's step names one first (PLANET: "venus", "mars", ...): no radio
// shadow there, and no Earth in view.
var BODY = typeof PLANET === "undefined" ? "moon" : PLANET, MOON = BODY === "moon";
var LOW = 3.0, FIX = 4.0, K = 0.55;

orbitScene("p-" + BODY);
hide("o-orbit");
if (MOON) {
    put("l-shadow");
    layers["l-shadow"].moveAfter(layers["p-moon"]);
}
put("l-open");
layers["l-open"].moveAfter(layers["o-sat"]);
if (MOON) put("l-earth-far", "screen");
put("m-puff");

function open(th) { return 390 * 2.6 / (1 + 1.6 * Math.cos(th * RAD)); }
var H = 88 * 390 * 390;
// The angle, falling at equal areas, 0 (behind the Moon) at 3.0.
var angle = (function () {
    var a = [], k = Math.round(LOW * FPS), th = 0, f;
    a[k] = 0;
    for (f = k; f > 0; f--) { var r = open(th); th += H / (r * r) / FPS; a[f - 1] = th; }
    th = 0;
    for (f = k; f <= NFRAMES; f++) { var rr = open(th); th -= H / (rr * rr) / FPS; a[f + 1] = th; }
    return function (t) { return a[clamp(Math.round(t * FPS), 0, a.length - 1)]; };
})();
var off = function (t) {
    if (!(glitch || failure) || t < LOW) return 0;
    var d = 90 * clamp((t - LOW) / 1.0, 0, 1);
    if (glitch && t > FIX) d *= clamp(1 - (t - FIX) / 0.8, 0, 1);
    if (failure && t > FIX) d += 400 * Math.pow(t - FIX, 1.4);
    return d;
};
function where(t) { var th = angle(t), r = open(th) + off(t); return [OCX + r * Math.cos(th * RAD), OCY + r * Math.sin(th * RAD)]; }
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function (t) { return angle(t) + (failure && t > FIX ? 120 * (t - FIX) : 0); });

// Seen from further out: the world at 0.55 of its size, about the Moon.
bake(tf(world, "Scale"), function () { return [100 * K, 100 * K]; });
bake(tf(world, "Position"), function () { return [OCX * (1 - K), OCY * (1 - K)]; });

move("m-puff", function (t) { var p = where(Math.max(t, FIX)); return [p[0] - 960, p[1] - 540]; });
grow("m-puff", track([[FIX, 0.8], [FIX + 0.5, 3.0, "out"], [FIX + 1.1, 3.6, "lin"]]));
fade("m-puff", (glitch || failure) ? track([[FIX, 0, "snap"], [FIX + 0.02, 1, "snap"], [FIX + 0.5, 1], [FIX + 1.1, 0, "lin"]])
                                   : function () { return 0; });

radioRingsAt(sendTimes(function (t) { return MOON && inShadow(where(t)) || (failure && t > FIX); }), where);
