// The Moon scenes shared by the lander's steps: the lander (descent and ascent stages on a carrier,
// their engines' plumes) placed by offsets from where it stands on the surface, and a lamp panel.
var LC = [960, 760];

// The lander, its plumes off. The descent's plume is behind its stage, the ascent's between the two.
function lander() {
    put("n-carrier");
    var parts = ["n-plume", "n-descent", "n-flame-up", "n-ascent"];
    for (var i = 0; i < parts.length; i++) put(parts[i], "n-carrier");
    hide("n-plume"); hide("n-flame-up");
}
// A plume burning while on(t), flickering.
function plume(name, on, size) {
    fade(name, function (t) { return on(t) ? 1 : 0; });
    grow(name, function (t) { var k = size ? size(t) : 1; return [k, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t))]; });
}
// The lamp panel (alarm or fuel) and its lamp, lit while lit(t), blinking while blink(t).
function lamp(which, lit, blinking) {
    put("n-panel-" + which, "screen");
    put("n-lamp", "screen");
    fade("n-lamp", function (t) {
        if (blinking && blinking(t)) return Math.floor(t * 5) % 2 === 0 ? 1 : 0.15;
        return lit && lit(t) ? 1 : 0;
    });
}
