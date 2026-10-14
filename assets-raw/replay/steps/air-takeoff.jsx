// Takeoff with the rocket plane under the wing: the carrier rolls down the runway, gathering speed, and
// lifts its nose at 1.6; off the ground at 2.0, it climbs away.
// Glitch: at 1.6 an engine smokes and it gathers speed slowly; it lifts off late, at 3.0, far down the runway.
// Failure: the same smoking engine, and at 2.4 the takeoff is abandoned: it brakes to a stop on the runway.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SMOKE = 1.6, A = 150;

airScene(true);

var trouble = glitch || failure;
var lift = trouble ? 3.0 : 2.0, rot = lift - 0.4;
// Distance along the runway: steady acceleration, slower once an engine smokes; the failure brakes.
var xs = [], x = 0, v = 0;
for (var f = 0; f <= NFRAMES + 1; f++) {
    var t = f / FPS, a = trouble && t >= SMOKE ? A * 0.5 : A;
    if (failure && t >= 2.4) a = v > 0 ? -320 : 0;
    xs.push(x);
    v = Math.max(0, v + a / FPS);
    x += v / FPS;
}
var X = function (t) { return xs[clamp(Math.round(t * FPS), 0, xs.length - 1)]; };
var pitch = failure ? function () { return 0; } : track([[rot, 0], [lift, -9, "inout"], [lift + 1.2, -12, "inout"]]);
var up = function (t) { return failure || t < lift ? 0 : -(40 * (t - lift) + 45 * (t - lift) * (t - lift)); };
var carrier = function (t) { return [X(t), up(t), pitch(t)]; };
fly(carrier, null);
// The gear comes up once it's climbing.
fade("a-gear", function (t) { return failure ? 1 : 1 - clamp((t - lift - 0.8) / 0.4, 0, 1); });
flow(carrier, function () { return 0; });
airCamera(function (t) { return [X(t), 0.6 * up(t)]; }, 640);
// The smoke from the inboard engine, trailing back (left) from it.
if (trouble) {
    move("a-smoke", function (t) { var p = carrier(t); return [p[0], p[1]]; });
    fade("a-smoke", function (t) { return t < SMOKE ? 0 : 0.8 + 0.2 * Math.sin(2 * Math.PI * 3 * t); });
    grow("a-smoke", function (t) { return 0.8 + 0.4 * clamp((t - SMOKE) / 0.6, 0, 1) + 0.1 * Math.sin(2 * Math.PI * 2 * t); });
}
