// Retrofire: in orbit over the Earth's limb, the capsule flies base first (turned round by the
// reentry preparations), its retro pack facing its way. The three motors fire at 1.0 and slow it:
// it sinks below its orbit (dashed), and the Earth comes up, until the burn ends at 3.6.
// Glitch: the middle motor dies at 1.6 and the capsule yaws; the other two hold it, the middle one
// lights again at 2.5, and the burn runs on to 4.2.
// Failure: the same until 2.5, when the other two die as well; a try at 2.9 only sputters, and the
// capsule stays in orbit.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.0, CUT = 1.6, OUT = 2.5, END = glitch ? 4.2 : 3.6;

put("paper", "screen");
put("d-earth");
put("r-orbit");
put("r-carrier");
var parts = ["r-flame-1", "r-flame-2", "r-flame-3", "r-strap-a", "r-strap-b", "r-pack", "r-capsule"];
for (var i = 0; i < parts.length; i++) put(parts[i], "r-carrier");

// Base first: the carrier turned 180 (the preparations' last frame).
// The yaw on two motors, the same in glitch and failure; the failure's dies away once nothing burns.
var yaw = glitch || failure ? function (t) {
    if (t < CUT || t > 3.4) return 0;
    var k = failure ? clamp((3.2 - t) / 0.7, 0, 1) : Math.min(1, (3.4 - t) / 0.5);
    return 7 * Math.sin(2 * Math.PI * 1.1 * (t - CUT)) * k;
} : function () { return 0; };
turn("r-carrier", function (t) { return 180 + yaw(t); });

// How far the capsule has sunk below its orbit (px): the Earth and the orbit come up by as much.
// It keeps sinking after the burn, at the speed the burn left it.
// The same in every variant until the failure's motors die at 2.5: it keeps what it had sunk by
// then; the glitch, on two motors, keeps up by burning longer.
var burning = smooth([[FIRE, 0], [2.4, 30], [3.6, 80], [6.4, 140]]);
var sink = failure ? function (t) { return burning(Math.min(t, OUT)); } : burning;
move("d-earth", function (t) { return [0, -sink(t)]; });
move("r-orbit", function (t) { return [0, -sink(t)]; });
// The Earth turns below; a little slower once the burn has taken speed off.
// Turned on from where the preparations left it (-1.5).
turn("d-earth", function (t) { return -1.5 - 0.25 * t + 0.06 * Math.max(0, (failure ? Math.min(t, OUT) : t) - FIRE); });

// The flames: each lit from FIRE to its own end, flickering.
function lit(n) {
    return function (t) {
        if (t < FIRE) return 0;
        // The middle motor dies at 1.6 (a flicker first), in the glitch and the failure alike.
        if ((glitch || failure) && n === 2 && t >= CUT && t < OUT) return t < CUT + 0.15 && jitter(t, 4) > 0 ? 1 : 0;
        if (failure) {
            // The other two die at 2.5; a try at 2.9 only sputters, a few frames on, a few off.
            if (t >= 2.9 && t < 3.3) return jitter(t, 7 + n) > 0.2 ? 1 : 0;
            return t < OUT ? 1 : 0;
        }
        return t < END ? 1 : 0;
    };
}
for (i = 1; i <= 3; i++) {
    (function (n) {
        fade("r-flame-" + n, lit(n));
        grow("r-flame-" + n, function (t) {
            // Out along the capsule's axis (the carrier's frame: its x), flickering.
            var k = t < FIRE + 0.2 ? 0.4 + 3 * (t - FIRE) : 1;
            return [k * (1 + 0.06 * Math.sin(2 * Math.PI * 12 * t + n)), 1];
        });
    })(i);
}
// A little shake while the motors burn.
camera(function () { return 0; }, function (t) {
    var on = lit(1)(t) + lit(2)(t) + lit(3)(t);
    return on > 0 ? 1.5 : 0;
});
