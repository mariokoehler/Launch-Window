// Launch preparations: propellant runs up the tower into the two tanks, the oxidizer starts to
// vent as it boils off, and the tanks are topped off. It ends on the countdown's first frame (and
// the launch's): the rocket on the pad, arms on, venting.
// Glitch: a leak at the lower arm sprays from 2.4; the flow stops, the valve shuts at 3.4, the flow
// picks up again and catches up. Failure: the same leak, but at 3.0 it catches fire at the base;
// the fire climbs and at 4.3 the lower tank goes.
padScene();

var LEAK = 2.4;
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";

// How full each tank is. The glitch pauses the lower tank from the leak until 3.6, then catches up.
var lower = glitch ? track([[0.6, 0], [LEAK, 0.5, "lin"], [3.6, 0.5], [4.9, 1, "inout"]])
                   : track([[0.6, 0], [4.2, 1, "inout"]]);
var upper = glitch ? track([[1.4, 0], [LEAK, 0.3, "lin"], [3.6, 0.3], [5.2, 1, "inout"]])
                   : track([[1.4, 0], [4.8, 1, "inout"]]);
// Topped off, the tint goes; on a failure it stays until the blast.
var topped = glitch ? 5.3 : 5.0;
var shown = failure ? function (t) { return t < 4.3 ? 1 : 0; } : track([[topped, 1], [topped + 0.4, 0, "lin"]]);
if (failure) {
    lower = track([[0.6, 0], [LEAK, 0.5, "lin"], [4.3, 0.5]]);
    upper = track([[1.4, 0], [LEAK, 0.3, "lin"], [4.3, 0.3]]);
}
tank("lower", lower, shown);
tank("upper", upper, shown);

// The flow: on at 0.3, stopped from the leak until 3.6 (the glitch) or for good (the failure).
var speed = 140;
var flowOn = glitch ? track([[0.3, 0], [0.6, 1, "lin"], [LEAK, 1], [2.6, 0, "lin"], [3.6, 0], [3.9, 1, "lin"], [topped - 0.3, 1], [topped, 0, "lin"]])
           : failure ? track([[0.3, 0], [0.6, 1, "lin"], [LEAK, 1], [2.6, 0, "lin"]])
           : track([[0.3, 0], [0.6, 1, "lin"], [topped - 0.3, 1], [topped, 0, "lin"]]);
flow(function (t) { return speed * t; }, flowOn);

// The oxidizer starts to vent once it's in (2.2), and keeps venting into the countdown.
wisps(track([[2.0, 0], [2.4, 1, "lin"]]));

if (glitch || failure) {
    var jetUntil = glitch ? 3.4 : 4.3;
    fade("leak-jet", function (t) { return t >= LEAK && t < jetUntil ? 1 : 0; });
    grow("leak-jet", function (t) { return 1 + 0.12 * jitter(t, 5); });
    move("leak-puff", track([[LEAK, [0, 0]], [LEAK + 1.2, [36, 30], "out"]]));
    grow("leak-puff", track([[LEAK, 0.4], [LEAK + 0.6, 1.4, "out"], [LEAK + 1.6, 1.9, "lin"]]));
    fade("leak-puff", track([[LEAK, 0, "snap"], [LEAK + 0.02, 1, "snap"], [LEAK + 1.0, 1], [LEAK + 1.8, 0, "lin"]]));
}

if (failure) {
    // The spill catches at the base, climbs, and the lower tank goes at 4.3.
    fire(track([[3.0, 0, "snap"], [3.02, 0.5, "snap"], [3.6, 1.0, "out"], [4.3, 1.4, "in"], [5.0, 1.2, "lin"], [7.0, 0.9, "lin"]]));
    camera(function () { return 0; }, function (t) { return t >= 4.3 ? 8 * Math.max(0, 1 - (t - 4.3) / 0.6) : t >= 3.0 ? 1 : 0; });
    explode(4.3, 960, 720);
}
