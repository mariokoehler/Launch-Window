// Luna sampler (2026-10-16): a Luna sample return's surface operations (Luna 16 brought 101 g of soil home
// in September 1970). On from the landing's last frame: the drill arm swings down to the surface (by 1.4),
// drills (1.4 to 3.0, dust at the bit), lifts the core up and over into the return capsule on top (4.4,
// a flash as it's sealed in), and folds back. The lander calls home every 1.2 s.
// Glitch: the bit hits rock at 2.0 and stalls; the arm lifts a little and drills again beside it (2.9),
// then carries the core up (loaded at 5.0).
// Failure: the bit jams at 2.0: the arm strains against it, can't pull free, and the lander falls silent.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DOWN = 46, OVER = -137, STOW = -90, REACH = 154, PIVOT = [1060, 770];
var LOAD = glitch ? 5.0 : 4.4;

lunaScene("sampler");
lookAt(function () { return 1130; });

var arm;
if (failure) {
    var strain = function (t) { return t < 2.0 ? 0 : 3 * Math.sin(2 * Math.PI * 9 * t) * clamp((3.4 - t) / 1.4, 0, 1) - 2 * clamp((t - 2.0) / 0.4, 0, 1); };
    arm = function (t) {
        var base = track([[0.4, STOW], [1.4, DOWN, "inout"], [2.0, DOWN + 1.2, "lin"]])(t);
        return base + (t > 1.4 && t < 2.0 ? 1.2 * Math.sin(2 * Math.PI * 18 * t) : 0) + strain(t);
    };
} else if (glitch) {
    var g = track([[0.4, STOW], [1.4, DOWN, "inout"], [2.0, DOWN + 1.2, "lin"], [2.6, DOWN + 1.2], [2.9, DOWN - 8, "inout"],
                   [3.3, DOWN + 1, "inout"], [3.6, DOWN + 2, "lin"], [LOAD, OVER, "inout"], [LOAD + 0.4, OVER], [6.2, STOW, "inout"]]);
    arm = function (t) {
        var drilling = (t > 1.4 && t < 2.0) || (t > 3.3 && t < 3.6);
        var stalled = t >= 2.0 && t < 2.6;
        return g(t) + (drilling ? 1.2 * Math.sin(2 * Math.PI * 18 * t) : 0) + (stalled ? 2.5 * Math.sin(2 * Math.PI * 7 * t) : 0);
    };
} else {
    var s = track([[0.4, STOW], [1.4, DOWN, "inout"], [3.0, DOWN + 2, "lin"], [LOAD, OVER, "inout"], [LOAD + 0.4, OVER],
                   [5.6, STOW, "inout"]]);
    arm = function (t) { return s(t) + (t > 1.4 && t < 3.0 ? 1.2 * Math.sin(2 * Math.PI * 18 * t) : 0); };
}
turn("s-arm", arm);

// The bit's tip in the world.
function tip(t) { var a = arm(t) * RAD; return [PIVOT[0] + REACH * Math.cos(a), PIVOT[1] + REACH * Math.sin(a)]; }
// The dust at the bit while it drills (and while the failure's strains).
var drillFrom = 1.4, drillTo = glitch ? 3.6 : failure ? 3.4 : 3.0;
move("u-kick", function (t) { var p = tip(Math.min(Math.max(t, drillFrom), drillTo)); return [p[0] - 870, 878 - 874]; });
fade("u-kick", function (t) {
    if (glitch && t >= 2.6 && t < 3.3) return 0;
    return t >= drillFrom && t < drillTo ? 1 : t >= drillTo ? clamp(1 - (t - drillTo) / 0.5, 0, 1) : 0;
});
grow("u-kick", function (t) { return 0.7 + 0.2 * Math.sin(2 * Math.PI * 6 * t); });

// The core rides up in the bit once it's drilled, and goes into the capsule with a flash.
if (!failure) {
    var lifted = glitch ? 3.6 : 3.0;
    fade("s-core", function (t) { return t >= lifted && t < LOAD + 0.1 ? 1 : 0; });
    fade("s-flash", track([[LOAD + 0.05, 0, "snap"], [LOAD + 0.1, 1, "snap"], [LOAD + 0.6, 0, "lin"]]));
    grow("s-flash", track([[LOAD + 0.05, 0.6], [LOAD + 0.6, 1.3, "out"]]));
}
// It calls home from the rocket's antenna; the failure's calls stop once the bit jams.
radioRingsAt(failure ? [0.4, 1.6] : [0.4, 1.6, 2.8, 4.0, 5.2], function () { return [1012, 700]; });
