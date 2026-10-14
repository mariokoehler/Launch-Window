// Working on the surface (Mars's): the lander stands with its mast up, sending; its sampler arm swings out,
// scoops the soil at 1.8 and brings it back.
// Glitch: the arm stalls short of the ground at 1.4 (Viking 1's arm stuck on a pin it should have
// dropped); freed at 2.2, it scoops at 2.6.
// Failure: the same stall, and at 2.2 the lander falls silent for good (Mars 3 went quiet seconds after
// landing).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var STALL = 1.4, FREE = 2.2, FOLD = -100, TIP = [186, 40];

probeGround();
probeCamera();

// The success's swing out; the glitch and the failure follow it until the stall, then hold there.
var swing = track([[0.4, FOLD], [1.6, 0, "inout"], [2.0, 0], [2.8, FOLD, "inout"]]);
var arm;
if (glitch || failure) {
    var held = swing(STALL);
    var shake = function (t) { return (t > STALL && t < STALL + 0.4) || (t > 1.8 && t < 2.1) ? 3 * Math.sin(2 * Math.PI * 7 * (t - STALL)) : 0; };
    var rest = glitch ? track([[FREE, held], [2.5, 0, "inout"], [2.8, 0], [3.6, FOLD, "inout"]]) : function () { return held; };
    arm = function (t) { return t <= STALL ? swing(t) : t <= FREE ? held + shake(t) : rest(t); };
} else {
    arm = swing;
}
var scoop = glitch ? 2.6 : failure ? 99 : 1.8;
turn("v-arm", arm);
// The soil: lifted at the scoop, riding in it back to the lander, fading.
function tip(t) {
    var a = arm(t) * RAD;
    return [800 + TIP[0] * Math.cos(a) - TIP[1] * Math.sin(a), 832 + TIP[0] * Math.sin(a) + TIP[1] * Math.cos(a)];
}
move("v-soil", function (t) { var p = tip(t); return [p[0] - 990, p[1] - 878]; });
fade("v-soil", function (t) { return t >= scoop && t < scoop + 1.2 ? 1 - Math.max(0, t - scoop - 0.6) / 0.6 : 0; });
grow("v-soil", function (t) { return 0.8 + 0.4 * clamp((t - scoop) / 0.3, 0, 1); });
// Sending all along, from the mast's tip, until the failure goes quiet.
var sends = [];
for (var s = 0.3; s < DUR; s += 0.75) if (!(failure && s >= FREE)) sends.push(s);
radioRingsAt(sends, function () { return mastTip([0, 0], 0, 0); });
