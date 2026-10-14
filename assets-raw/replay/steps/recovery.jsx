// Recovery: the capsule floats where the splashdown left it (the camera 200 px down, the sea line at
// its base). A helicopter comes in from the right, the capsule's flotation collar fills at 1.6, the
// hoist comes down to its nose and hooks it at 2.8, and it's lifted out of the sea from 3.2.
// Glitch: from 1.4 it lists in the swell; the collar fills late (2.6) and rights it, and the lift
// comes later (3.6).
// Failure (Liberty Bell 7): the same list, but no collar: it floods and sits lower and lower. The hook
// catches it at 3.0, the helicopter strains, and at 4.0 has to let it go: it sinks.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIST = 1.4, COLLAR = glitch ? 2.6 : 1.6, DOWN = failure ? 2.6 : glitch ? 3.0 : 2.2, HOOK = DOWN + 0.6;
var LIFT = glitch ? 3.6 : 3.2, STRAIN = 3.0, LETGO = 4.0;

put("paper", "screen");
put("r-carrier");
put("y-collar");
put("r-capsule-up", "r-carrier");
put("y-sea-front");
put("y-cable");
put("y-hook");
put("y-heli");
put("y-rotor", "y-heli");
for (var i = 1; i <= 3; i++) put("r-ring-" + i);

// The camera and the sea as the descent ends.
camera(function () { return 200; }, function () { return 0; });
move("y-sea-front", function () { return [0, -482]; });
fade("y-sea-front", function () { return 1; });

// The capsule: bobbing; listing in the glitch and the failure; sinking as the failure floods; lifted
// with the helicopter otherwise.
var bob = function (t) { return 5 * Math.sin(2 * Math.PI * 0.6 * t); };
var list = glitch || failure ? function (t) {
    if (t < LIST) return 0;
    var a = 24 * clamp((t - LIST) / 0.8, 0, 1) + 5 * Math.sin(2 * Math.PI * 0.7 * (t - LIST));
    if (glitch && t > COLLAR) a *= clamp(1 - (t - COLLAR) / 0.6, 0, 1);
    if (failure) a += 12 * clamp((t - 2.6) / 1.2, 0, 1);
    return a;
} : function () { return 0; };
var flood = failure ? function (t) {
    if (t < LIST) return 0;
    // It floods from 2.6, when a collar would have filled (the glitch's does).
    var low = 74 * clamp((t - 2.6) / 1.4, 0, 1);
    return t < LETGO ? low : low + 520 * (1 - Math.exp(-(t - LETGO) * 0.8));
} : function () { return 0; };

// The helicopter: in from the right, hovering 150 px above its rest; it rises with the capsule, or
// (failure) dips as it strains and then climbs away empty.
var heliX = track([[0.4, 1400], [2.0, 0, "out"]]);
var heliY = function (t) {
    var y = -150 + 4 * Math.sin(2 * Math.PI * 0.8 * t);
    if (!failure && t > LIFT) y -= 150 * Math.pow(t - LIFT, 1.5);
    if (failure && t > STRAIN && t < LETGO) y += 30 * clamp((t - STRAIN) / 0.4, 0, 1) + 4 * jitter(t, 9);
    if (failure && t >= LETGO) y += 30 * Math.max(0, 1 - (t - LETGO) / 0.5) - 260 * Math.pow(t - LETGO, 1.5);
    return y;
};
var heliDx = function (t) { return heliX(t) + (failure && t > LETGO ? 160 * Math.pow(t - LETGO, 1.4) : 0); };
move("y-heli", function (t) { return [heliDx(t), heliY(t)]; });
turn("y-heli", function (t) { return failure && t > STRAIN && t < LETGO ? 3 * Math.sin(2 * Math.PI * 2 * t) : 0; });
grow("y-rotor", function (t) { return [0.25 + 0.75 * Math.abs(Math.cos(2 * Math.PI * 7 * t)), 1]; });

// Lifted: the capsule follows the helicopter up from the hook.
var lifted = function (t) { return failure || t < LIFT ? 0 : heliY(t) - heliY(LIFT); };
var capY = function (t) { return bob(t) * (1 - clamp((t - LIFT) / 0.3, 0, 1) * (failure ? 0 : 1)) + flood(t) + lifted(t); };
move("r-carrier", function (t) { return [0, capY(t)]; });
turn("r-carrier", list);
fade("r-carrier", function () { return 1; });
// Going down, the failure's capsule fades into the water.
fade("r-capsule-up", failure ? track([[LETGO + 0.15, 1], [LETGO + 0.9, 0.4, "lin"], [LETGO + 1.6, 0, "lin"]]) : function () { return 1; });
// The collar fills at the sea line round the base, and goes with the capsule.
grow("y-collar", function (t) { return t < COLLAR || failure ? 0.01 : 0.3 + 0.7 * clamp((t - COLLAR) / 0.3, 0, 1); });
move("y-collar", function (t) { return [0, capY(t)]; });
turn("y-collar", list);
if (failure) hide("y-collar");

// The hoist: from the winch (under the cabin, at 234 at rest) down to the capsule's nose (148 px above
// its centre), let go in the failure.
var WINCH = 234, NOSE = 470 - 148;
var length = function (t) {
    var full = NOSE + capY(Math.min(t, failure ? LETGO : 99)) - (WINCH + heliY(t));
    if (t < DOWN) return 0.02;
    if (t < HOOK) return Math.max(0.02, full * (t - DOWN) / 0.6);
    if (failure && t >= LETGO) return Math.max(0.02, full * Math.max(0, 1 - (t - LETGO) / 0.5));
    return full;
};
function winch(t) { return [heliDx(t), heliY(t)]; }
move("y-cable", function (t) { return winch(t); });
grow("y-cable", function (t) { return [1, Math.max(0.01, length(t) / 100)]; });
move("y-hook", function (t) { var w = winch(t); return [w[0], w[1] + length(t) - 100]; });
fade("y-cable", function (t) { return t >= DOWN ? 1 : 0; });
fade("y-hook", function (t) { return t >= DOWN ? 1 : 0; });

// Where the failure's capsule goes under, rings on the water.
for (i = 1; i <= 3; i++) {
    (function (n, k) {
        var t0 = LETGO + 0.6 + 0.5 * k;
        move(n, function () { return [0, -482]; });
        grow(n, function (t) { return !failure || t < t0 ? 0.2 : 0.2 + 1.2 * clamp((t - t0) / 1.6, 0, 1); });
        fade(n, function (t) { return !failure || t < t0 ? 0 : 1 - clamp((t - t0) / 1.6, 0, 1); });
    })("r-ring-" + i, i - 1);
}
