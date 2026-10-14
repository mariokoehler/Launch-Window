// Rendezvous: two capsules, nose to nose. The chaser comes in from the left, brakes at 1.4 and holds
// a few metres off the other by 2.0; the two then fly in formation, bobbing together.
// Glitch: holding, the chaser starts drifting back and down at 2.2; a puff at 3.0 brings it back by 4.0.
// Failure: the same drift, and at 3.0 the puff is a sputter (its fuel spent, as Gemini 4's was trying to
// keep station with its spent stage): it drifts on, out of reach.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var V = 400 / 1.7, BRAKE = 1.4, DRIFT = 2.2, FIX = 3.0, HOLD = 40;

dockScene(true);

var bob = function (t) { return 6 * Math.sin(2 * Math.PI * 0.35 * Math.max(0, t - 2.0)); };
var come = function (t) {
    if (t <= BRAKE) return -360 + V * t;
    var u = Math.min(t - BRAKE, 0.6);
    return -360 + V * BRAKE + V * u - V / 1.2 * u * u;
};
var drift = function (t) { return [-40 * (t - DRIFT), 22 * (t - DRIFT)]; };
var x, y;
if (glitch) {
    var back = track([[FIX, 1], [4.0, 0, "inout"]]);
    x = function (t) { return come(t) + (t > DRIFT ? drift(Math.min(t, FIX))[0] * back(t) : 0); };
    y = function (t) { return bob(t) + (t > DRIFT ? drift(Math.min(t, FIX))[1] * back(t) : 0); };
} else if (failure) {
    // The glitch's drift until the fix, then faster.
    var d = function (t) { var u = t - DRIFT, w = Math.max(0, t - FIX); return u <= 0 ? [0, 0] : [-40 * u - 18 * w * w, 22 * u + 8 * w * w]; };
    x = function (t) { return come(t) + d(t)[0]; };
    y = function (t) { return bob(t) + d(t)[1]; };
} else {
    x = come;
    y = bob;
}
chaser(function (t) { return [x(t), y(t)]; }, failure ? track([[FIX, 0], [6.0, -14, "in"]]) : null);
move("k-other", function (t) { return [0, bob(t)]; });
puffs("k-brake", [BRAKE]);
// The fix: a full puff behind the base, or the failure's sputter, small and short.
if (failure) {
    grow("k-push", function (t) { return t >= 0.2 && t < 0.8 ? 0.5 + (t - 0.2) / 0.6 : 0.3; });
    fade("k-push", function (t) { return t >= 0.2 && t < 0.8 ? 1 - (t - 0.2) / 0.6 : t >= FIX && t < FIX + 0.2 ? 0.7 : 0; });
} else {
    puffs("k-push", glitch ? [0.2, FIX] : [0.2]);
}
