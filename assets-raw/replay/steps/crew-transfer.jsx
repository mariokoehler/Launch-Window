// Crew transfer (and the spacewalks to a lander): two craft docked nose to nose. Both hatches open;
// the figure rises out of the first, takes the handrail and goes along it hand over hand, then climbs
// into the second, and the hatches close.
// Glitch: at 2.2 its hand slips off the rail and it drifts away, turning; at 2.8 puffs from its gun
// bring it back to the rail, and it goes on across, late.
// Failure: the same slip, and nothing brings it back: at 2.8 it's hauled in on its tether to the hatch
// it came from, and the crossing is off.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var SLIP = 2.2, ANSWER = 2.8;
// The second hatch's distance and the rail's end (a craft's own, lok-transfer.jsx; 2026-10-19).
var OVER = typeof TRANSFER_OVER !== "undefined" ? TRANSFER_OVER : 494;
var RAIL_END = typeof TRANSFER_RAIL_END !== "undefined" ? TRANSFER_RAIL_END : 1290;
// The rail's height and where the figure stands up at each end, from the first hatch's (Gemini EOR's, 2026-10-19,
// gemini-eor-transfer.jsx: the lander sits in a frame that stands higher than the hatch).
var RAIL = typeof TRANSFER_RAIL !== "undefined" ? TRANSFER_RAIL : 352;
var UP = typeof TRANSFER_UP !== "undefined" ? TRANSFER_UP : 0;
var DY = typeof TRANSFER_DY !== "undefined" ? TRANSFER_DY : 0;

spacewalkScene(true);

var along = function (x) { return handOn([x, RAIL], 80); };
// Hand over hand: a small bob along the rail.
function hands(p, t, from) { return [p[0], p[1] + 4 * Math.sin(2 * Math.PI * 2 * (t - from)), p[2]]; }
// FROM_STANDING and TO_STANDING (2026-10-20, Soyuz 4 and 5's egress, crossing and ingress in three steps): it starts
// standing in the first hatch, its hatch open, and ends standing in the second's, which stays open.
var FROM_STANDING = typeof FROM_STANDING !== "undefined" && FROM_STANDING;
var TO_STANDING = typeof TO_STANDING !== "undefined" && TO_STANDING;
var IN0 = FROM_STANDING ? 0 : INSIDE;
var start = [[0, [0, IN0, 0]], [0.8, [0, IN0, 0]], [1.4, [0, UP, 0]], [1.8, along(960)], [3.2, along(RAIL_END)]];
var pose;
if (glitch || failure) {
    var before = poses(start), at = hands(before(SLIP), SLIP, 1.8), grip = evaAt(at, EH)[0];
    var adrift = function (t) { var u = t - SLIP; return [at[0] + 14 * u, at[1] - 70 * u, at[2] + 70 * u]; };
    var lost = adrift(ANSWER);
    var after = glitch
        ? poses([[ANSWER, lost], [3.4, along(grip + 40)], [4.4, along(RAIL_END)], [4.9, [OVER, DY, 0]], [5.5, [OVER, TO_STANDING ? DY : INSIDE, 0]]])
        : poses([[ANSWER, lost], [4.2, [0, UP, 0]], [4.8, [0, INSIDE, 0]]]);
    pose = function (t) {
        if (t <= SLIP) return t > 1.8 ? hands(before(t), t, 1.8) : before(t);
        if (t <= ANSWER) return adrift(t);
        return glitch && t > 3.4 && t < 4.4 ? hands(after(t), t, 3.4) : after(t);
    };
} else {
    var all = poses(start.concat([[3.7, [OVER, DY, 0]], [4.3, [OVER, TO_STANDING ? DY : INSIDE, 0]]]));
    pose = function (t) { return t > 1.8 && t < 3.2 ? hands(all(t), t, 1.8) : all(t); };
}
walk(pose);
hide("e-box");
var shut = glitch ? 5.8 : failure ? 5.0 : 4.6;
hatch(FROM_STANDING ? -1 : 0.3, shut);
hatch(0.6, TO_STANDING && !failure ? 99 : shut, "e-hatch-b");
if (glitch) fade("e-gun", function (t) { return t >= ANSWER && t < 3.4 && Math.floor(t * 10) % 2 === 0 ? 1 : 0; });
