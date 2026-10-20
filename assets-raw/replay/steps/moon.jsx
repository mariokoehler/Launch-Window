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
// Apollo's flag (2026-10-20), planted on the first moonwalk (flag.jsx), standing beside the lander in the
// surface scenes after it: when the builder sets FLAG. Put it before the figure, so the figure passes in front.
function flagPlanted() {
    if (typeof FLAG === "undefined" || !FLAG) return false;
    put("e-flag-pole");
    put("e-flag-cloth", "e-flag-pole");
    return true;
}
// The experiment package left on the ground by a moonwalk (2026-10-20, the LK-700's landing): where moonwalk.jsx sets
// it down (its REST, at its BOXK), when the builder sets PACKAGE. Put it before the lander, as the moonwalk does.
function packageLeft() {
    if (typeof PACKAGE === "undefined" || !PACKAGE) return false;
    put("e-box");
    grow("e-box", function () { return 0.6; });
    move("e-box", function () { return [1236 + 34 - 975, 880 - 0.6 * 20 - 360]; });
    return true;
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
