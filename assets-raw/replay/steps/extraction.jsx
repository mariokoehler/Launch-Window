// Lander extraction: docked to the lander's hatch, the capsule lets the lander go from the stage at 0.6
// (a flash at its feet) and backs away, pulling it out of the adapter; the stage falls behind.
// Glitch: the lander holds fast; at 1.8 the latches let go at the second try and it comes out.
// Failure: the same, and at 1.8 it still holds: the capsule lets go of it and backs away alone, the
// lander left in its stage.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LET = 0.6, AGAIN = 1.8;

apolloScene();
hide("t-panel-top"); hide("t-panel-bottom");

var go = glitch ? AGAIN : LET;
var out = function (t) { return track([[go + 0.1, 0], [go + 1.5, -280, "inout"], [6.0, -280 - 25 * (6.0 - go - 1.5), "lin"]])(t); };
var shake = function (t) { return t > LET + 0.1 && t < LET + 0.5 ? 4 * Math.sin(2 * Math.PI * 9 * (t - LET)) : 0; };
var capsule, lm;
if (failure) {
    // Strains at the first try, again at the second, then lets go of the lander and backs off alone.
    var alone = track([[AGAIN + 0.6, 0], [AGAIN + 2.0, -260, "inout"], [6.0, -320, "lin"]]);
    capsule = function (t) { return t < AGAIN + 0.6 ? shake(t) + (t > AGAIN + 0.1 && t < AGAIN + 0.5 ? 4 * Math.sin(2 * Math.PI * 9 * (t - AGAIN)) : 0) : alone(t); };
    lm = function () { return 0; };
} else {
    capsule = function (t) { return t <= go ? (glitch ? shake(t) : 0) : out(t); };
    lm = function (t) { return t <= go ? 0 : out(t); };
}
move("k-carrier", function (t) { return [DOCKED + capsule(t), 0]; });
turn("k-carrier", function () { return 0; });
lmAt(lm);
// The stage falls behind, slowly, once it's free of the lander.
move("t-stage", function (t) { return [failure ? 0 : 20 * Math.max(0, t - go), 0]; });
flashes("t-flash-lm", glitch || failure ? [LET, AGAIN] : [LET]);
flashes("t-flash-sep", failure ? [AGAIN + 0.5] : []);
puffs("k-brake", glitch || failure ? [LET + 0.1, AGAIN + 0.1] : [LET + 0.1]);
