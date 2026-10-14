// The scene of the transposition and the extraction (with dock.jsx and moon.jsx before it): over the
// Earth's limb after the burn to the Moon, the third stage lies along the way with the lander in its
// adapter, top first (the lander's carrier turned and scaled to fit); the capsule (dock.jsx's chaser)
// is placed by offsets from where its nose meets the lander's hatch, docked.
var LMK = 0.42, LM = [50, -290], DOCKED = 64;

function apolloScene() {
    put("paper", "screen");
    put("d-earth");
    turn("d-earth", function (t) { return -0.25 * t; });
    put("t-stage");
    lander();
    grow("n-carrier", function () { return LMK; });
    turn("n-carrier", function () { return -90; });
    put("t-panel-top");
    put("t-panel-bottom");
    put("k-carrier");
    var parts = ["k-chaser", "k-brake", "k-push", "k-side", "k-stuck"];
    for (var i = 0; i < parts.length; i++) put(parts[i], "k-carrier");
    hide("k-brake"); hide("k-push"); hide("k-side"); hide("k-stuck");
    put("t-flash-sep");
    put("t-flash-lm");
    hide("t-flash-sep"); hide("t-flash-lm");
}
// The lander's offset along the way (it rides out with the capsule in the extraction).
function lmAt(dx) { move("n-carrier", function (t) { return [LM[0] + dx(t), LM[1]]; }); }
// A flash at each time, 0.4 s.
function flashes(name, times) {
    fade(name, function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.4) return 1 - (t - times[k]) / 0.4;
        return 0;
    });
}
