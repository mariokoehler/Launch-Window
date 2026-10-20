// Egress, a stand-up spacewalk (2026-10-18; Gemini 10, 11 and 12 began theirs so): the cabin vents, the
// hatch opens, and the figure rises to stand in it and looks round, ending as the spacewalk out begins.
// Glitch: the hatch stops part open at 0.8; at 1.4 a shove from inside frees it (Gemini 4's hatch was
// stiff), and the figure stands up late. Failure: the shove doesn't move it, nor a second; it's pulled shut.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var STUCK = 0.8, SHOVE = 1.4;

spacewalkScene(typeof STANDUP_TRANSFER !== "undefined" && STANDUP_TRANSFER, true);

// The hatch opens the same way in every variant until it sticks (or doesn't) at 40 degrees.
var hatchAt;
if (glitch) {
    hatchAt = track([[0.3, 0], [STUCK, 40], [SHOVE, 40], [SHOVE + 0.1, 46, "out"], [SHOVE + 0.7, 110, "out"]]);
} else if (failure) {
    hatchAt = track([[0.3, 0], [STUCK, 40], [SHOVE, 40], [SHOVE + 0.1, 46, "out"], [SHOVE + 0.3, 40],
                     [2.0, 40], [2.1, 46, "out"], [2.3, 40], [3.0, 40], [3.6, 0, "in"]]);
} else {
    hatchAt = track([[0.3, 0], [STUCK, 40], [1.3, 110, "out"]]);
}
turn("e-hatch", hatchAt);
var up = glitch ? SHOVE + 0.6 : 1.0;
var pose = failure ? function () { return [0, INSIDE, 0]; }
    : poses([[up, [0, INSIDE, 0]], [up + 0.7, [0, 0, 0]], [up + 1.3, [0, 0, -8]], [up + 2.3, [0, 0, 6]], [up + 3.0, [0, 0, 0]]]);
walk(pose);
carry(pose, 99);
