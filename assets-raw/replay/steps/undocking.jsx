// Undocking: docked, the light steady. At 0.8 the latches let go (a flash), and the puffs ahead of the
// chaser's shoulders back it away from the target's collar.
// Glitch: the latches flash but hold: the puffs only shake the two. At 2.2 they let go at the second
// try, and it backs away.
// Failure: the same, and when it comes free at 2.2 a jet on its top sticks open (as on Gemini 8): it
// tumbles away backwards, faster and faster.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LET = 0.8, AGAIN = 2.2;

dockScene(false);
blink(function () { return true; });

var go = glitch || failure ? AGAIN : LET;
var away = track([[go + 0.1, 0], [go + 1.4, -160, "inout"], [6.4, -160 - 30 * (6.4 - go - 1.4), "lin"]]);
// The hold: a shudder from the first puffs, 0.4 s.
var shake = function (t) { return t > LET + 0.1 && t < LET + 0.5 ? 4 * Math.sin(2 * Math.PI * 9 * (t - LET)) : 0; };
var x, y = function () { return 0; }, spin = function () { return 0; };
if (failure) {
    x = function (t) { if (t <= go) return shake(t); var u = t - go; return -60 * u - 22 * u * u; };
    y = function (t) { var u = Math.max(0, t - go); return 18 * u * u; };
    spin = function (t) { var u = Math.max(0, t - go); return -(40 * u + 34 * u * u); };
    fade("k-stuck", function (t) { return t >= go + 0.1 ? 1 : 0; });
    grow("k-stuck", function (t) { return 1 + 0.08 * jitter(t, 5); });
} else {
    x = function (t) { return t <= go ? shake(t) : away(t); };
}
chaser(function (t) { return [x(t), y(t)]; }, spin);
if (glitch || failure) move("k-target", function (t) { return [-shake(t) * 0.5, 0]; });
latches(glitch || failure ? [LET, AGAIN] : [LET]);
puffs("k-brake", glitch || failure ? [LET + 0.1, AGAIN + 0.1] : [LET + 0.1]);
