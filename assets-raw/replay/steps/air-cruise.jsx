// Preflight under the wing: high over the clouds, the carrier flies on towards the drop point while the
// rocket plane is readied, the air streaming past.
// Glitch: at 1.4 the alarm lamp blinks; the trouble is cleared by 2.4 and the lamp goes out.
// Failure: the same alarm, and at 2.4 it stays lit: the drop is called off and the carrier turns for home,
// nosing down and away.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var ALARM = 1.4, CLEAR = 2.4;

airScene(false);
var bob = function (t) { return 6 * Math.sin(2 * Math.PI * 0.3 * t); };
var away = function (t) { var u = Math.max(0, t - CLEAR); return failure ? [-40 * u * u, 30 * u * u, 6 * clamp(u / 1.2, 0, 1)] : [0, 0, 0]; };
var carrier = function (t) { var a = away(t); return [a[0], bob(t) + a[1], a[2]]; };
fly(carrier, null);
flow(carrier, function () { return 900; });
clouds(function () { return [0, 0]; }, 260);
lamp("alarm", failure ? function (t) { return t >= CLEAR; } : null,
     glitch || failure ? function (t) { return t >= ALARM && t < CLEAR; } : null);
airCamera(function () { return [0, 0]; });
