// A lander's release (and the probes' release from their bus): nearing its planet, the bus lets its lander
// go at 0.6 (a flash at the latches); the lander drifts off ahead and down, on its own way in.
// Glitch: the latches flash but hold; the second command at 1.8 lets it go.
// Failure: it holds at the second try too: the lander stays on the bus, and goes where the bus goes.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LET = 0.6, AGAIN = 1.8;

put("paper", "screen");
put("v-pod");
put("v-bus");
put("v-latch");
hide("v-latch");

var go = failure ? 99 : glitch ? AGAIN : LET;
var shake = function (t, at) { return t > at + 0.05 && t < at + 0.45 ? 4 * Math.sin(2 * Math.PI * 9 * (t - at)) : 0; };
move("v-pod", function (t) {
    if (t <= go) return [0, shake(t, LET) + shake(t, AGAIN)];
    var u = t - go;
    return [70 * u, 120 * u + 26 * u * u];
});
grow("v-pod", function (t) { return t <= go ? 1 : Math.max(0.45, 1 - 0.12 * (t - go)); });
turn("v-pod", function (t) { return t <= go ? 0 : 14 * (t - go); });
// The bus turns slowly away once it's free of it.
turn("v-bus", function (t) { return t <= go ? 0 : -3 * (t - go); });
fade("v-latch", function (t) {
    var times = glitch || failure ? [LET, AGAIN] : [LET];
    for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.4) return 1 - (t - times[k]) / 0.4;
    return 0;
});
