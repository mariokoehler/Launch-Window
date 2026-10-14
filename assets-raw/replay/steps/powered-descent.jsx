// Powered descent: from low orbit over the Moon's limb, the lander fires its descent engine against
// its way (engine first), slowing and coming down, and pitches upright as it nears the surface.
// Glitch: at 2.0 the alarm lamp blinks (Apollo 11's computer alarms in its descent); it goes on and
// the lamp goes out at 3.2.
// Failure: the same alarm, and at 2.8 the descent engine stops: at 3.0 the crew aborts, the ascent
// stage firing off the descent stage and climbing back to orbit, the descent stage falling away.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var ALARM = 2.0, CUT = 2.8, ABORT = 3.0, K = 0.7;

put("paper", "screen");
put("n-limb");
lander();
turn("n-limb", function (t) { return -0.3 * t; });

var x = track([[0, -560], [6.4, 0, "out"]]), y = track([[0, -520], [6.4, -260, "out"]]);
var pitch = track([[1.6, -90], [4.6, 0, "inout"]]);
grow("n-carrier", function () { return K; });
if (failure) {
    // After the abort the descent stage falls away, turning; the ascent stage climbs off it (in the
    // carrier's frame, so its offsets are along the lander's own axis, before the carrier's scale).
    move("n-carrier", function (t) { var u = Math.max(0, t - CUT); return [x(Math.min(t, CUT)) + 160 * u, y(Math.min(t, CUT)) + 25 * u * u]; });
    turn("n-carrier", function (t) { var u = Math.max(0, t - CUT); return pitch(Math.min(t, CUT)) + 30 * u + 12 * u * u; });
    move("n-ascent", function (t) { var u = Math.max(0, t - ABORT); return [0, -(160 * u + 120 * u * u)]; });
    move("n-flame-up", function (t) { var u = Math.max(0, t - ABORT); return [0, -(160 * u + 120 * u * u)]; });
    plume("n-plume", function (t) { return t < CUT; });
    plume("n-flame-up", function (t) { return t >= ABORT; });
} else {
    move("n-carrier", function (t) { return [x(t), y(t)]; });
    turn("n-carrier", pitch);
    plume("n-plume", function () { return true; });
}
lamp("alarm", failure ? function (t) { return t >= ALARM; } : null,
     glitch ? function (t) { return t >= ALARM && t < 3.2; } : failure ? function (t) { return t >= ALARM && t < ABORT; } : null);
