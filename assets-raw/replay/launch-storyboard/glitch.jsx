// Variant 2, glitch then saved (storyboard row 2): 6.4 s, then a 0.3 s hold.
stack("trail-Glitch");

wisps(track([[0.8, 1], [1.2, 0, "lin"]]));

// Ignition 0.8, full 1.0; the stutter flips sputter and ignite every 0.1 s from 2.6, and at 3.4
// the flame catches with one bright frame.
states(["flame-ignite", "flame-full", "flame-sputter"], function (t) {
    if (t < 0.8) return "";
    if (t < 1.0) return "flame-ignite";
    if (t >= 2.6 && t < 3.4) return Math.floor((t - 2.6) / 0.1) % 2 === 0 ? "flame-sputter" : "flame-ignite";
    return "flame-full";
});
flicker(["flame-ignite", "flame-full", "flame-sputter"], function (t) { return t >= 3.4 && t < 3.4 + 1 / FPS ? 1.15 : 1; });

turn("armUpper", track([[1.1, 0], [1.33, 83, "out"], [1.4, 80, "inout"]]));
turn("armLower", track([[1.1, 0], [1.33, -83, "out"], [1.4, -80, "inout"]]));

// The bell's height above the pad: liftoff 1.6, the stall from 2.6, climbing again from 3.4.
var rise = smooth([[1.6, 0], [2.2, 80], [2.6, 135], [3.0, 170], [3.4, 185], [4.0, 360], [6.4, 1240]]);
// The lean and the damped swing back.
var lean = track([[2.6, 0], [3.0, -7, "out"], [3.4, -7.5, "lin"], [4.0, 3, "inout"], [4.3, -1, "inout"], [4.6, 0, "inout"]]);
// The bell follows the kinked trail: its x at the bell's height.
function kinkX(y) {
    for (var i = 1; i < KINK.length; i++) {
        if (y >= KINK[i][0]) {
            var u = (KINK[i - 1][0] - y) / (KINK[i - 1][0] - KINK[i][0]);
            return KINK[i - 1][1] + (KINK[i][1] - KINK[i - 1][1]) * u;
        }
    }
    return KINK[KINK.length - 1][1];
}
var bellY = function (t) { return 840 - rise(t); };
move("rocket", function (t) { return poseFor(kinkX(bellY(t)), bellY(t), lean(t)); });
turn("rocket", lean);
trailTo("trail-Glitch", bellY);

// The puff from the bell at the stutter stays where it was born.
move("smoke-7", function () { return [-12.6, -180]; });
grow("smoke-7", track([[2.6, 0.3], [2.9, 1.2, "out"], [4.0, 1.4, "lin"]]));
fade("smoke-7", track([[2.6, 0, "snap"], [2.62, 1, "snap"], [3.0, 1], [4.0, 0.4, "lin"], [4.2, 0, "lin"]]));

var s12 = [[0.8, -12, 0.2, 0, "snap"], [0.82, -12, 0.2, 1, "snap"], [1.0, -8, 0.5, 1, "out"], [1.4, 60, 1.0, 1, "out"],
           [2.2, 140, 1.4, 1, "out"], [3.0, 170, 1.45, 0.8, "out"], [4.0, 190, 1.5, 0.5, "out"], [6.4, 210, 1.5, 0.2, "lin"]];
var s34 = [[1.0, 30, 0.4, 0, "lin"], [1.4, 70, 1.0, 1, "out"], [2.2, 150, 1.4, 1, "out"], [3.0, 180, 1.45, 0.8, "out"],
           [4.0, 200, 1.5, 0.5, "out"], [6.4, 220, 1.5, 0.2, "lin"]];
var s56 = [[1.6, 100, 0.4, 0, "lin"], [2.2, 184, 1.4, 1, "out"], [3.0, 223, 1.45, 0.8, "out"], [4.0, 249, 1.5, 0.5, "out"],
           [6.4, 275, 1.5, 0.2, "lin"]];
groundSmoke({"smoke-1": s12, "smoke-2": s12, "smoke-3": s34, "smoke-4": s34, "smoke-5": s56, "smoke-6": s56});

// The camera waits out the scare, then follows from 3.6, lagging.
var stutterShake = function (t) { return t >= 2.6 ? 6 * Math.max(0, 1 - (t - 2.6) / 0.4) : 0; };
camera(smooth([[3.6, 0], [4.0, 60], [6.4, 320], [6.7, 320]]),
       function (t) {
           var base = t < 1.0 ? 0 : t < 1.6 ? 2 : 3 * Math.max(0, 1 - (t - 1.6) / 0.6);
           return Math.max(base, stutterShake(t));
       });
