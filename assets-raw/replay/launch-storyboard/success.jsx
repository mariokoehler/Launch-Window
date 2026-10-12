// Variant 1, success (storyboard row 1): 6.0 s, then a 0.3 s hold.
stack("trail-Success");

wisps(track([[1.2, 1], [1.6, 0, "lin"]]));

// Ignition at 1.0, full thrust at 1.6, flickering to the end.
states(["flame-ignite", "flame-full"], function (t) {
    return t < 1.0 ? "" : t < 1.6 ? "flame-ignite" : "flame-full";
});
flicker(["flame-ignite", "flame-full"]);

// The arms swing clear at 1.7 in 0.4 s, with a 3° overshoot.
turn("armUpper", track([[1.7, 0], [2.0, 83, "out"], [2.1, 80, "inout"]]));
turn("armLower", track([[1.7, 0], [2.0, -83, "out"], [2.1, -80, "inout"]]));

// The climb: held down until 2.2, then accelerating through the frames' heights.
var rise = smooth([[2.2, 0], [2.8, 60], [4.2, 340], [6.0, 1220]]);
move("rocket", function (t) { return [0, -rise(t)]; });
trailTo("trail-Success", function (t) { return 840 - rise(t); });

// Ground smoke: [t, offset out, scale, opacity, ease].
var s12 = [[1.0, -12, 0.2, 0, "snap"], [1.02, -12, 0.2, 1, "snap"], [1.2, -8, 0.5, 1, "out"], [2.0, 60, 1.0, 1, "out"],
           [2.8, 140, 1.4, 1, "out"], [4.2, 180, 1.5, 0.5, "out"], [6.0, 210, 1.5, 0.2, "lin"]];
var s34 = [[1.5, 30, 0.4, 0, "lin"], [2.0, 70, 1.0, 1, "out"], [2.8, 150, 1.4, 1, "out"],
           [4.2, 190, 1.5, 0.5, "out"], [6.0, 220, 1.5, 0.2, "lin"]];
var s56 = [[2.2, 100, 0.4, 0, "lin"], [2.8, 184, 1.4, 1, "out"], [4.2, 236, 1.5, 0.5, "out"], [6.0, 275, 1.5, 0.2, "lin"]];
groundSmoke({"smoke-1": s12, "smoke-2": s12, "smoke-3": s34, "smoke-4": s34, "smoke-5": s56, "smoke-6": s56});

// The camera follows from 3.2, lagging, so the rocket leaves the top at about 5.6.
camera(smooth([[3.2, 0], [4.2, 120], [6.0, 300], [6.3, 300]]),
       track([[1.6, 0, "snap"], [1.62, 2, "snap"], [2.0, 2], [2.2, 3, "lin"], [2.8, 1, "lin"], [3.2, 0, "lin"]]));
