// The Multiprobe's releases (2026-10-17), after multiprobe.jsx; BIG is set by the step's own file.
// BIG (16 November 1978): the bus turning slowly, the large probe on its forward end; at 0.8 its springs fire (a
// flash at the latches) and it pushes off along the axis, ahead of the bus, which falls slowly behind.
//   Glitch: the latches flash but hold; the second command at 2.0 lets it go. Failure: it holds at the second try too.
// Small (20 November): the bus without the large probe spins up; at 1.6 the clamps' nuts fire (three flashes) and the
// three small probes fly off it at a tangent, each its own way, spreading out ahead.
//   Glitch: North's clamp holds; it goes round once more and lets go at 2.3, off another way.
//   Failure: North's clamp holds for good: two go, and it goes round on the bus.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";

busScene(BIG);
var shift, spin, i;
if (BIG) {
    var LET = 0.8, AGAIN = 2.0;
    var go = failure ? 99 : glitch ? AGAIN : LET;
    spin = spinAngles(10, function () { return 70; });
    // The bus falls behind once the probe's gone.
    shift = function (t) { return t <= go ? 0 : -14 * (t - go); };
    var jolt = function (t, at) { return t > at + 0.05 && t < at + 0.45 ? 4 * Math.sin(2 * Math.PI * 9 * (t - at)) : 0; };
    move("pv-big", function (t) {
        if (t <= go) return [jolt(t, LET) + jolt(t, AGAIN), 0];
        return [190 * (t - go), 0];
    });
    fade("pv-latch", function (t) {
        var times = glitch || failure ? [LET, AGAIN] : [LET];
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.4) return 1 - (t - times[k]) / 0.4;
        return 0;
    });
    move("pv-latch", function (t) { return [jolt(t, LET) + jolt(t, AGAIN), 0]; });
    for (i = 1; i <= 3; i++) {
        (function (k) { placeSmall(k, function (t) { return onBus(spin(t) + 120 * (k - 1), shift(t)); }); })(i);
    }
} else {
    var FIRE = 1.6, LATE = 2.3;
    // Spinning up from the slow turn to the release's rate.
    var rate = track([[0.2, 70], [1.4, 160, "inout"]]);
    spin = spinAngles(10, rate);
    shift = function (t) { return t <= FIRE ? 0 : -14 * (t - FIRE); };
    for (i = 1; i <= 3; i++) {
        (function (k) {
            var free = k === NORTH ? (failure ? 99 : glitch ? LATE : FIRE) : FIRE;
            var phi = function (t) { return spin(t) + 120 * (k - 1); };
            var p0 = onBus(phi(free), shift(free));
            // Off at a tangent to the spin, and on ahead (the bus slows behind them).
            var w = rate(free) * RAD, a = phi(free) * RAD;
            var v = [80, MP.r * w * Math.cos(a), -MP.r * w * Math.sin(a)];
            var p = function (t) {
                if (t <= free) return onBus(phi(t), shift(t));
                var u = t - free;
                return [p0[0] + v[0] * u, p0[1] + v[1] * u, p0[2] + v[2] * u];
            };
            var at = placeSmall(k, p);
            // Its clamp's flash at the firing (each time it fires), on the probe's base.
            var tries = k === NORTH && (glitch || failure) ? [FIRE, LATE] : [FIRE];
            move("pv-clamp-" + k, at);
            grow("pv-clamp-" + k, function (t) { return 1 + p(t)[2] / 2400; });
            fade("pv-clamp-" + k, function (t) {
                for (var j = 0; j < tries.length; j++) if (t >= tries[j] && t < tries[j] + 0.4) return 1 - (t - tries[j]) / 0.4;
                return 0;
            });
        })(i);
    }
}
busSpin(spin, shift);
