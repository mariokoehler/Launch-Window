// Docking: from the holding point (where the target approach ends) a puff behind the chaser sends it
// in; its nose goes into the target's collar at 1.6, the latches catch (a flash) and the light stops
// blinking: one craft.
// Glitch: at 1.6 the nose bounces off the collar, nudging the target on; the chaser stops, closes in
// again at 2.4 and the latches catch at 3.0.
// Failure: the same bounce, and at 3.0 a second one: the latches never catch, and the two drift apart,
// the target turning slowly away.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var HOLD = -150, IN = 0.4, TOUCH = 1.6, AGAIN = 2.4, TOUCH2 = 3.0, NUDGE = 14;

// It carries on from the target approach (6.4 s).
dockScene(false, 6.4);

// The first approach: from rest at IN to contact at TOUCH, at 145 px/s at the end.
var first = track([[IN, HOLD], [0.7, -131, "in"], [TOUCH, 0, "lin"]]);
var second = track([[AGAIN, -50], [2.6, -44, "in"], [TOUCH2, NUDGE, "lin"]]);
var x;
if (glitch || failure) {
    var back = track([[TOUCH, 0], [AGAIN, -50, "out"]]);
    x = function (t) {
        if (t <= TOUCH) return first(t);
        if (t <= AGAIN) return back(t);
        if (t <= TOUCH2) return second(t);
        if (glitch) return NUDGE;
        return track([[TOUCH2, NUDGE], [3.6, -60, "out"], [6.4, -150, "lin"]])(t);
    };
} else {
    x = function (t) { return Math.min(first(t), 0); };
}
chaser(function (t) { return [x(t), 0]; });
// The target: still, or nudged on by the bounce (and docked, the stack drifts on as one).
if (glitch || failure) {
    var tx = failure ? track([[TOUCH, 0], [AGAIN, NUDGE, "out"], [TOUCH2, NUDGE], [6.4, NUDGE + 160, "out"]])
                     : track([[TOUCH, 0], [AGAIN, NUDGE, "out"]]);
    move("k-target", function (t) { return [tx(t), 0]; });
    if (failure) turn("k-target", track([[TOUCH2, 0], [6.4, 10, "out"]]));
}
var caught = failure ? 99 : glitch ? TOUCH2 : TOUCH;
latches(failure ? [] : [caught]);
blink(function (t) { return t >= caught; });
puffs("k-push", glitch || failure ? [IN, AGAIN] : [IN]);
puffs("k-brake", failure ? [3.2] : []);
