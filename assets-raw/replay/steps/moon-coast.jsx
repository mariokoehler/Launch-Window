// A coast between the Earth and the Moon (LEG, set by the step's own file first: "out" or "home"),
// on the Earth-Moon map: the Earth at the left, the Moon on its orbit at the right, the way out below
// and the way home above (dashed). The craft travels most of its leg, sending home a ring every 0.8 s.
// Success: a course correction at 2.0 (a puff), on the line all the way.
// Glitch: from 1.6 it drifts off the line; a correction at 3.0 brings it back by 3.8.
// Failure: the same drift, but the correction at 3.0 misfires: it veers away and goes quiet.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DRIFT = 1.6, FIX = glitch || failure ? 3.0 : 2.0;

put("paper", "screen");
put("l-map-orbit");
put("l-path-" + LEG);
put("l-map-earth");
put("l-map-moon");
for (var i = 1; i <= 3; i++) put("m-ring-" + i);
put("m-puff");
put("m-probe");

// The leg (parts.py's points), by distance along it: from 8 % to 92 % over the clip.
var P = L._paths[LEG], acc = [0];
for (i = 1; i < P.length; i++) acc.push(acc[i - 1] + Math.sqrt(Math.pow(P[i][0] - P[i - 1][0], 2) + Math.pow(P[i][1] - P[i - 1][1], 2)));
var TOTAL = acc[acc.length - 1];
function on(d) {
    for (var k = 1; k < P.length; k++) {
        if (acc[k] >= d) {
            var u = (d - acc[k - 1]) / (acc[k] - acc[k - 1]);
            var dx = P[k][0] - P[k - 1][0], dy = P[k][1] - P[k - 1][1], len = Math.sqrt(dx * dx + dy * dy);
            return [P[k - 1][0] + dx * u, P[k - 1][1] + dy * u, -dy / len, dx / len];
        }
    }
    return [P[P.length - 1][0], P[P.length - 1][1], 0, 0];
}
var dist = function (t) { return TOTAL * (0.08 + 0.84 * clamp(t / 6.4, 0, 1)); };
var off = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var d = 40 * clamp((t - DRIFT) / 1.4, 0, 1);
    if (glitch && t > FIX) d *= clamp(1 - (t - FIX) / 0.8, 0, 1);
    if (failure && t > FIX) d += 240 * Math.pow(t - FIX, 1.4);
    return d;
};
// Off the line to the outside of the loop (the normal pointing away from the Earth-Moon line).
var SIDE = LEG === "out" ? 1 : -1;
function where(t) {
    var p = on(dist(failure && t > FIX ? FIX + 0.6 * (t - FIX) : t)), o = off(t) * SIDE;
    return [p[0] + p[2] * o, p[1] + p[3] * o];
}
grow("m-probe", function () { return 1.6; });
move("m-probe", function (t) { var p = where(t); return [p[0] - 960, p[1] - 540]; });
turn("m-probe", function (t) { return failure && t > FIX ? 140 * (t - FIX) : 0; });

move("m-puff", function (t) { var p = where(Math.max(t, FIX)); return [p[0] - 960, p[1] - 540]; });
grow("m-puff", track([[FIX, 0.6], [FIX + 0.5, 2.0, "out"], [FIX + 1.1, 2.4, "lin"]]));
fade("m-puff", track([[FIX, 0, "snap"], [FIX + 0.02, 1, "snap"], [FIX + 0.5, 1], [FIX + 1.1, 0, "lin"]]));

var sends = [];
for (var s = 0.4; s < DUR; s += 0.8) if (!(failure && s >= FIX)) sends.push(s);
for (i = 1; i <= 3; i++) {
    (function (n, k) {
        var mine = function (t) { var last = -1; for (var j = k; j < sends.length; j += 3) if (sends[j] <= t) last = sends[j]; return last; };
        move(n, function (t) { var te = mine(t); var p = where(te < 0 ? 0 : te); return [p[0] - 960, p[1] - 540]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 0.5 : 0.5 + 2.0 * clamp((t - te) / 0.9, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : 1 - clamp((t - te) / 0.9, 0, 1); });
    })("m-ring-" + i, i - 1);
}
