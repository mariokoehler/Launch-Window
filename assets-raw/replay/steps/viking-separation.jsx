// Viking's lander separation and deorbit (2026-10-17): in orbit over Mars (its limb below), the orbiter lets its lander
// capsule go at 0.8 (a flash at the latches): the capsule, backshell up and heat shield down, drops away from under it.
// It turns side on (1.5 to 2.1) and fires its deorbit jets from 2.3 (the lander's own hydrazine thrusters) to slow
// down: it falls behind and below. At 3.5 it turns heat shield down again and drops away towards Mars, where its entry
// starts. (Viking 1's lander separated on 20 July 1976 at 08:51 UT.)
// Glitch: the jets cut out at 2.55; a relight at 2.95 finishes the burn.
// Failure: the jets cut out at 2.55 for good: the capsule stays in orbit, side on, drifting with the orbiter.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LET = 0.8, BURN = 2.3, CUT = 2.55, RELIGHT = 2.95;
var DONE = failure ? CUT : glitch ? 3.6 : 3.3, BACK = DONE + 0.2;

put("paper", "screen");
put("e-limb-mars");
put("vk-orbiter");
put("vk-latch");
put("e-shell");
put("vk-jets", "e-shell");
hide("vk-latch");

function burning(t) {
    if (t < BURN || t >= DONE) return false;
    if (glitch && t >= CUT && t < RELIGHT) return false;
    return true;
}
// The burn's push, integrated frame by frame: it slows the capsule, which falls behind (left) and below.
var pushed = (function () {
    var a = [], v = 0, x = 0;
    for (var f = 0; f <= NFRAMES + 1; f++) { a.push(x); if (burning(f / FPS)) v += 110 / FPS; x += v / FPS; }
    return function (t) { return a[clamp(Math.round(t * FPS), 0, a.length - 1)]; };
})();
var jolt = function (t) { return t > LET + 0.05 && t < LET + 0.45 ? 3 * Math.sin(2 * Math.PI * 9 * (t - LET)) : 0; };
var drop = function (t) { return t <= LET ? 0 : 110 * (1 - Math.exp(-(t - LET) / 0.7)); };
// After the burn it turns heat shield down and falls away, smaller as it goes (not the failure's).
var away = function (t) { var u = Math.max(0, t - BACK - 0.6); return failure ? 0 : 120 * (1 - Math.exp(-u / 1.2)); };
var side = failure ? track([[1.5, 0], [2.1, 90, "inout"]]) : track([[1.5, 0], [2.1, 90, "inout"], [BACK, 90], [BACK + 0.6, 0, "inout"]]);

move("e-shell", function (t) { return [-pushed(t) + jolt(t), -85 + drop(t) + 0.45 * pushed(t) + away(t)]; });
turn("e-shell", side);
grow("e-shell", function (t) { return 1 - (failure ? 0 : 0.55 * EASE.inout(clamp((t - BACK - 0.4) / 2.4, 0, 1))); });
// The orbiter gives a little at the release and carries on.
move("vk-orbiter", function (t) { return [jolt(t), t <= LET ? 0 : -14 * (1 - Math.exp(-(t - LET) / 0.7))]; });
move("vk-latch", function (t) { return [jolt(t), 0]; });
fade("vk-latch", function (t) { return t >= LET && t < LET + 0.4 ? 1 - (t - LET) / 0.4 : 0; });
// The jets: on while burning, flickering; the cut-outs sputter first.
fade("vk-jets", function (t) {
    if (!burning(t)) return glitch || failure ? (t >= CUT - 0.15 && t < CUT && jitter(t, 5) > 0 ? 0.4 : 0) : 0;
    return 1;
});
grow("vk-jets", function (t) { return [1, 0.85 + 0.15 * Math.sin(2 * Math.PI * 12 * t)]; });
