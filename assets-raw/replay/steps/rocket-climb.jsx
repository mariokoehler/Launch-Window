// The rocket plane's powered climb: alone now, nose high, its engine burning, it climbs steeply; the camera
// rides with it, so the Earth's limb below drops away and the air streams past along its way.
// Glitch: at 2.0 the engine runs rough (the flame stutters); it steadies at 2.8.
// Failure: the same rough running, and at 2.8 the engine shuts down for good: the climb runs out, the
// nose falls through and it heads back down.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var ROUGH = 2.0, STEADY = 2.8, NOSE = -38;

put("paper", "screen");
put("d-earth");
for (var i = 1; i <= 8; i++) put("a-flow-" + i);
put("w-plane");
put("w-skid", "w-plane");
put("x15-ventral", "w-plane");
put("w-flame", "w-plane");
hide("w-skid");
// Both rough alike from 2.0; at 2.8 the glitch's steadies, the failure's stops.
var on = function (t) {
    if ((glitch || failure) && t >= ROUGH && t < STEADY) return Math.floor(t * 10) % 3 !== 0;
    return !(failure && t >= STEADY);
};
// Height gained (the limb falls away as it climbs) and the pitch: steady, or falling through.
var hs = [], h = 0, v = 260;
for (var f = 0; f <= NFRAMES + 1; f++) { hs.push(h); var t = f / FPS; v += (on(t) ? 40 : -260) / FPS; h += v / FPS; }
var H = function (t) { return hs[clamp(Math.round(t * FPS), 0, hs.length - 1)]; };
var pitch = failure ? function (t) { return t < STEADY ? NOSE : NOSE + 50 * EASE.inout(clamp((t - STEADY - 0.2) / 2.0, 0, 1)); } : function () { return NOSE; };
// The plane centred on the screen; the limb, below, moves down with height gained.
move("w-plane", function () { return [960 - 965, 560 - 820]; });
turn("w-plane", pitch);
move("d-earth", function (t) { return [0, 0.4 * H(t)]; });
turn("d-earth", function (t) { return -0.25 * t; });
fade("w-flame", function (t) { return on(t) ? 1 : 0; });
grow("w-flame", function (t) { return [2 + 0.15 * Math.sin(2 * Math.PI * 11 * t), 1]; });
// Speed lines along its way (opposite its nose), fewer as it slows.
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        move(n, function (t) {
            var a = pitch(t) * RAD, s = (k * 251 + 900 * t) % 1400 - 700, side = -240 + 60 * k;
            return [-(s) * Math.cos(a) - side * Math.sin(a), -(s) * Math.sin(a) + side * Math.cos(a) + 20];
        });
        turn(n, function (t) { return pitch(t); });
        fade(n, function (t) { return failure && t > STEADY ? clamp(1 - (t - STEADY) / 1.5, 0, 1) : 1; });
    })("a-flow-" + i, i);
}
