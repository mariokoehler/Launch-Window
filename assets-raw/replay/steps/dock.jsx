// The close-up shared by the rendezvous and docking steps: over the Earth's limb (the way home's,
// turning as it does in the reentry preparations), the chaser (the capsule, nose right, on a carrier
// with its jets) and ahead of it the docking target, or another capsule. At rest the chaser is
// docked: a step moves it by offsets from there. A step that carries on from another passes that
// one's length as t0, so the Earth's turn and the light's blinking carry on too.
var T0 = 0;
function dockScene(other, t0) {
    T0 = t0 || 0;
    put("paper", "screen");
    put("d-earth");
    put("r-orbit");
    put("k-carrier");
    var parts = ["k-chaser", "k-brake", "k-push", "k-side", "k-stuck"];
    for (var i = 0; i < parts.length; i++) put(parts[i], "k-carrier");
    if (other) {
        put("k-other");
    } else {
        // The target over the chaser, so a docked nose sits inside its collar.
        put("k-target");
        put("k-light", "k-target");
        put("k-latch", "k-target");
        hide("k-latch");
    }
    turn("d-earth", function (t) { return -0.25 * (t + T0); });
    // Every jet off until a step fires it.
    hide("k-brake"); hide("k-push"); hide("k-side"); hide("k-stuck");
}
// The chaser's offset from docked, [x, y], and its turn.
function chaser(where, spin) {
    move("k-carrier", where);
    turn("k-carrier", spin || function () { return 0; });
}
// A jet's puffs at the given times: each grows and fades over 0.6 s.
function puffs(name, times) {
    grow(name, function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.6) return 0.5 + (t - times[k]) / 0.6;
        return 0.5;
    });
    fade(name, function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.6) return 1 - (t - times[k]) / 0.6;
        return 0;
    });
}
// The target's light blinks (twice a second) until steady(t).
function blink(steady) {
    fade("k-light", function (t) { return steady && steady(t) ? 1 : (Math.floor((t + T0) * 4) % 2 === 0 ? 1 : 0.15); });
}
// The latches' flash at each time, 0.4 s.
function latches(times) {
    fade("k-latch", function (t) {
        for (var k = 0; k < times.length; k++) if (t >= times[k] && t < times[k] + 0.4) return 1 - (t - times[k]) / 0.4;
        return 0;
    });
}
