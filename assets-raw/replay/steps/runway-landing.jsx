// Runway landing (a spaceplane's landing after its descent): the rocket plane glides in over a dry lake,
// flares, puts its skid down at 2.4 in a puff of dust, lowers its nose and rolls out to a stop.
// Glitch: it comes down hard at 2.4 and bounces; the second touch at 3.0 holds.
// Failure: the same bounce, and at 3.0 the skid gives way (as on an X-15 landing in 1962): the tail
// drops onto the lakebed and scrapes along in sparks and dust to a stop, tilted.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var TOUCH = 2.4, AGAIN = 3.0, V = 360, X0 = -230;

put("paper", "screen");
put("w-ground");
put("n-dust");
put("w-sparks");
put(K.plane);
put(K.skid, K.plane);
hide("w-sparks");
// The camera in on the touchdown, 1.35 times, (800 800) at (960 640).
tf(world, "Scale").setValue([135, 135]);
tf(world, "Position").setValue([960 - 1.35 * 800, 640 - 1.35 * 800]);

var x, y, pitch, down = glitch || failure ? AGAIN : TOUCH, stopK = failure ? 260 : glitch ? 100 : 70;
// The glide and flare: level by touchdown.
var glideX = function (t) { return X0 - V * (TOUCH - t); };
var glideY = function (t) { return -150 * Math.pow(Math.max(0, TOUCH - t), 1.5); };
var flare = track([[0, -4], [1.6, -4], [2.2, -10, "inout"]]);
// On the ground from `down`: slowing to a stop.
var roll = function (t) {
    var u = Math.max(0, t - down), stop = (V - (down - TOUCH) * 30) / (2 * stopK);
    var xu = Math.min(u, stop);
    return X0 + V * (down - TOUCH) + (V - (down - TOUCH) * 30) * xu - stopK * xu * xu;
};
x = function (t) { return t <= TOUCH ? glideX(t) : t <= down ? X0 + V * (t - TOUCH) : roll(t); };
y = function (t) {
    if (t <= TOUCH) return glideY(t);
    if (t <= down) return -60 * Math.sin(Math.PI * (t - TOUCH) / (AGAIN - TOUCH));
    return failure ? 14 * clamp((t - AGAIN) / 0.2, 0, 1) : 0;
};
pitch = function (t) {
    if (t <= TOUCH) return flare(t);
    if (t <= down) return -10 + 2 * Math.sin(Math.PI * (t - TOUCH) / (AGAIN - TOUCH));
    // Nose down onto its wheel; the failure sits tail down instead, nose up.
    return failure ? -10 - 4 * clamp((t - AGAIN) / 0.3, 0, 1) : track([[down, -10], [down + 0.4, 0, "in"]])(t);
};
move(K.plane, function (t) { return [x(t), y(t)]; });
turn(K.plane, pitch);
// The failure's skid folds up under the body.
// The X-15's skid swings up as it gives way; the E-154's wheels fold up into it (2026-10-17), leaving it on its belly.
if (K.x15) turn(K.skid, function (t) { return failure && t > AGAIN ? 60 * clamp((t - AGAIN) / 0.2, 0, 1) : 0; });
else grow(K.skid, function (t) { return [1, failure && t > AGAIN ? Math.max(0.05, 1 - clamp((t - AGAIN) / 0.2, 0, 1)) : 1]; });
// Dust where the skid (or the tail) meets the ground while it's moving fast on it, and at each touch.
var speed = function (t) { return (x(t + 0.05) - x(t)) / 0.05; };
move("n-dust", function (t) { return [x(t) + WSX() - 960 - 40, 0]; });
function WSX() { return 830; }
fade("n-dust", function (t) {
    var on = 0;
    if (t >= TOUCH && t < TOUCH + 0.5) on = 1 - (t - TOUCH) / 0.5;
    if (t >= down) on = Math.max(on, clamp(speed(t) / V, 0, 1) * (failure ? 1 : 0.7));
    return on;
});
grow("n-dust", function (t) { return (failure && t > AGAIN ? 1.3 : 0.8) + 0.1 * Math.sin(2 * Math.PI * 4 * t); });
if (failure) {
    move("w-sparks", function (t) { return [x(t), 0]; });
    fade("w-sparks", function (t) { return t > AGAIN && speed(t) > 20 ? (Math.floor(t * 12) % 2 ? 1 : 0.5) : 0; });
}
