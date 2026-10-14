// Impact: the probe falls straight at the Moon from the upper left, sending pictures (a ring every
// 0.5 s) until it strikes, at 4.0, in a flash.
// Glitch: the signal drops at 1.8 and is back at 2.8, still on target.
// Failure (Ranger 3): the same dropout, and when the signal's back it has drifted off its aim: it
// misses, passing the Moon's edge and flying on.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROP = 1.8, BACK = 2.8, HIT = 4.0;

orbitScene("p-moon");
hide("o-orbit");
put("burst");
hide("burst");

// Where it strikes: on the Moon's edge facing it, up and left (angle 215).
var A = 215 * RAD, TARGET = [OCX + 250 * Math.cos(A), OCY + 250 * Math.sin(A)];
var FROM = [OCX + 880 * Math.cos(A), OCY + 880 * Math.sin(A)];
// Along the line, accelerating towards the Moon; the failure drifts sideways from 2.6, as the signal
// comes back, enough to miss.
var along = function (t) { var u = clamp(t / HIT, 0, 1.6); return u * u * (0.35 + 0.65 * u) / 1.0; };
var side = failure ? function (t) { return t < 2.6 ? 0 : 380 * Math.pow((t - 2.6) / (HIT - 2.6), 1.5); } : function () { return 0; };
function where(t) {
    var k = along(t), x = FROM[0] + (TARGET[0] - FROM[0]) * k, y = FROM[1] + (TARGET[1] - FROM[1]) * k;
    // Sideways: across the line, towards the Moon's top.
    var s = side(t), n = [Math.sin(A), -Math.cos(A)];
    return [x + s * n[0], y + s * n[1]];
}
var heading = Math.atan2(TARGET[1] - FROM[1], TARGET[0] - FROM[0]) / RAD + 90;
move("o-sat", function (t) { var p = where(failure ? t : Math.min(t, HIT)); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function () { return heading; });
if (!failure) {
    fade("o-sat", function (t) { return t < HIT ? 1 : 0; });
    move("burst", function () { return [TARGET[0] - 960, TARGET[1] - 600]; });
    grow("burst", track([[HIT, 0.3], [HIT + 0.35, 1.6, "out"], [HIT + 1.0, 1.9, "lin"]]));
    fade("burst", track([[HIT, 0, "snap"], [HIT + 0.02, 1, "snap"], [HIT + 0.5, 1], [HIT + 1.1, 0, "lin"]]));
}
// Pictures home: every 0.5 s, not while the signal's out, none after the strike.
var sends = [];
for (var s = 0.3; s < DUR; s += 0.5) {
    if ((glitch || failure) && s >= DROP && s < BACK) continue;
    if (!failure && s >= HIT) continue;
    sends.push(s);
}
radioRingsAt(sends, where);
