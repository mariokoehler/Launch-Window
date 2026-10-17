// A rover on Mars (2026-10-17): PrOP-M, which Mars 2 and Mars 3 carried, on the lander mars-surface shows (Mars 2's
// lands as Viking's in the replay), closer (2.2 times) and a little to the right. The arm swings out with the rover hanging from
// its tip and sets it down at 1.6; the arm folds back, and the rover walks off on its skis (a step every 0.7 s, its
// body rising and sliding forward), its tether paying out behind it. The real ones never got to: Mars 2 crashed, and
// Mars 3 fell silent seconds after landing.
// Glitch: on its second step a front bar meets a rock (3.25): it stops, steps back, sidesteps and walks on round it.
// Failure: the same bump, and it stays there: the lander falls silent at 3.6.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SET = 1.6, BUMP = 3.25, QUIET = 3.6, FOLD = -178, OUT = 18, TIP = [186, 40];
var C = [960, 827], ARM = [1034, 808];

put("paper", "screen");
put("m-ground");
put("m-stones");
put("m-rock");
for (var i = 1; i <= 4; i++) put("ring-" + i);
put("pr-cable");
put("m-leg-l");
put("m-leg-r");
put("m-lander");
put("v-arm");
put("pr-rover");
put("pr-ski", "pr-rover");
put("pr-bump", "pr-rover");
hide("pr-bump");
tf(world, "Scale").setValue([220, 220]);
tf(world, "Position").setValue([960 - 2.2 * 1150, 540 - 2.2 * 800]);

move("m-ground", function () { return [0, -200]; });
move("m-stones", function () { return [0, -200]; });
var down = [0, C[1] - 440];
move("m-lander", function () { return down; });
move("m-leg-l", function () { return down; });
move("m-leg-r", function () { return down; });
move("v-arm", function () { return [ARM[0] - 800, ARM[1] - 832]; });

// The arm: out with the rover, back once it's down.
var arm = track([[0.3, FOLD], [SET, OUT, "inout"], [SET + 0.3, OUT], [SET + 1.1, FOLD, "inout"]]);
turn("v-arm", arm);
function tip(t) {
    var a = arm(t) * RAD;
    return [ARM[0] + TIP[0] * Math.cos(a) - TIP[1] * Math.sin(a), ARM[1] + TIP[0] * Math.sin(a) + TIP[1] * Math.cos(a)];
}
var X0 = tip(SET)[0];
// The rock its second step meets (m-rock is drawn from x 1056, on y 1100).
var ROCK = X0 + 91;
move("m-rock", function () { return [ROCK - 1056, -200]; });

// Its steps: [start, dx, dy (towards us), size] each, 0.35 s long.
var steps;
if (glitch) steps = [[2.4, 34, 0], [3.1, 17, 0], [3.5, -24, 0], [3.95, 0, 16], [4.5, 34, 0], [5.2, 34, 0]];
else if (failure) steps = [[2.4, 34, 0], [3.1, 17, 0]];
else steps = [[2.4, 34, 0], [3.1, 34, 0], [3.8, 34, 0], [4.5, 34, 0], [5.2, 34, 0]];
// The bumped step stops short, at the rock.
var LONG = function (k) { return (glitch || failure) && k === 1 ? 0.175 : 0.35; };
function walked(t) {
    var x = 0, y = 0, lift = 0;
    for (var k = 0; k < steps.length; k++) {
        var s = steps[k], u = clamp((t - s[0]) / LONG(k), 0, 1), e = EASE.inout(u);
        x += s[1] * e;
        y += s[2] * e;
        if (u > 0 && u < 1) lift = 5 * Math.sin(Math.PI * u);
    }
    return [x, y, lift];
}
var rover = function (t) {
    if (t <= SET) { var p = tip(t); return [p[0], p[1] - 3.5]; }
    var w = walked(t);
    return [X0 + w[0], 900 + w[1] - w[2]];
};
move("pr-rover", function (t) { var p = rover(t); return [p[0] - 960, p[1] - 900]; });
// Sidestepping towards us, it's a little bigger.
grow("pr-rover", function (t) { return 1 + 0.08 * walked(t)[1] / 16; });

// The tether, from under the lander's body to the rover's back, once it's down.
var ANCHOR = [1028, 840];
move("pr-cable", function (t) { return [ANCHOR[0] - 960, ANCHOR[1] - 900]; });
turn("pr-cable", function (t) { var p = rover(t); return Math.atan2(p[1] - 14 - ANCHOR[1], p[0] - 24 - ANCHOR[0]) / RAD; });
grow("pr-cable", function (t) { var p = rover(t), dx = p[0] - 24 - ANCHOR[0], dy = p[1] - 14 - ANCHOR[1]; return [Math.sqrt(dx * dx + dy * dy) / 100, 1]; });
fade("pr-cable", function (t) { return t >= SET ? 1 : 0; });

if (glitch || failure) fade("pr-bump", track([[BUMP, 0, "snap"], [BUMP + 0.02, 1, "snap"], [BUMP + 0.3, 1], [BUMP + 0.6, 0, "lin"]]));

// Sending from the dish all along, until the failure's lander goes quiet.
var sends = [];
for (var s = 0.3; s < DUR; s += 0.75) if (!(failure && s >= QUIET)) sends.push(s);
radioRingsAt(sends, function () { return [C[0] + 30, C[1] - 58]; });
