// Gemini's adapter (2026-10-17): the equipment section (r-trunk, drawn in its parts behind the retrograde
// section, the pack) on the capsule's carrier, turning with it. With ESEP (a time per variant, set by the
// step's own file) its latches fire then and it drifts off past the base, ahead of a capsule flying blunt
// end forward, tumbling and fading out of sight; with none (the suborbital flight), it stays on.
var ESEP = typeof ESEP !== "undefined" ? ESEP : {};
put("r-trunk", "r-carrier");
put("r-trunk-cut-a", "r-carrier");
put("r-trunk-cut-b", "r-carrier");
// Put on a carrier its timeline has already turned, a part keeps a turn against it until it's given one.
turn("r-trunk", function () { return 0; });
turn("r-trunk-cut-a", function () { return 0; });
turn("r-trunk-cut-b", function () { return 0; });
(function () {
    var at = ESEP.hasOwnProperty(VARIANT) ? ESEP[VARIANT] : null;
    if (at === null) {
        hide("r-trunk-cut-a");
        hide("r-trunk-cut-b");
        return;
    }
    var flash = function (t) { return t >= at && t < at + 0.25 ? 1 : 0; };
    fade("r-trunk-cut-a", flash);
    fade("r-trunk-cut-b", flash);
    // In the carrier's frame the base is its -x.
    move("r-trunk", function (t) { var u = Math.max(0, t - at); return [-(70 * u + 40 * u * u), 8 * u]; });
    turn("r-trunk", function (t) { return -30 * Math.max(0, t - at); });
    fade("r-trunk", function (t) { return 1 - clamp((t - at - 0.9) / 0.8, 0, 1); });
})();
