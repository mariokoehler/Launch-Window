// Along the handrail (2026-10-19; the LOK's lone spacewalk, the middle of three): the crossing to the LK
// rehearsed. From where the spacewalk out ends (FLOAT) the figure pulls itself in along the tether, takes the
// handrail by the hatch and goes hand over hand to its end by the docking probe, where the LK would be; it
// stays there a moment, comes back along it, lets go and drifts out to FLOAT again, where the photo begins.
// Glitch: at 1.9 its hand slips off the rail and it drifts away, turning; at 2.5 puffs from its gun bring it
// back to the rail, and it goes on, late.
// Failure: the same slip, and at 2.5 the visor fogs over (the Krechet suit overheating): it's hauled in to
// the hatch and goes in.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FLOAT = [-60, -110, -345], RAIL = 352, NEAR = 960, FAR = 1004, ROT = -280;
var SLIP = 1.9, ANSWER = 2.5;

spacewalkScene(false);
put("e-rail");

var along = function (x) { return handOn([x, RAIL], ROT); };
// Hand over hand: a small bob along the rail.
function hands(p, t, from) { return [p[0], p[1] + 4 * Math.sin(2 * Math.PI * 2 * (t - from)), p[2]]; }
var keys = [[0, FLOAT], [0.3, FLOAT], [1.2, along(NEAR)], [2.4, along(FAR)], [3.0, along(FAR)], [4.2, along(NEAR)],
            [5.4, FLOAT]];
var pose;
if (glitch || failure) {
    var before = poses(keys), at = hands(before(SLIP), SLIP, 1.2);
    var adrift = function (t) { var u = t - SLIP; return [at[0] + 10 * u, at[1] - 60 * u, at[2] + 60 * u]; };
    var lost = adrift(ANSWER);
    var after = glitch
        ? poses([[ANSWER, lost], [3.0, along(975)], [3.6, along(FAR)], [3.9, along(FAR)], [4.8, along(NEAR)], [5.9, FLOAT]])
        : poses([[ANSWER, lost], [4.0, [0, 0, -360]], [4.6, [0, INSIDE, -360]]]);
    pose = function (t) {
        if (t <= SLIP) return t > 1.2 ? hands(before(t), t, 1.2) : before(t);
        if (t <= ANSWER) return adrift(t);
        return glitch && t > 3.0 && t < 4.8 && !(t > 3.6 && t < 3.9) ? hands(after(t), t, 3.0) : after(t);
    };
} else {
    var all = poses(keys);
    pose = function (t) { return (t > 1.2 && t < 2.4) || (t > 3.0 && t < 4.2) ? hands(all(t), t, 1.2) : all(t); };
}
walk(pose);
hide("e-box");
// The hatch stays open (it was left open), and closes behind the failure's figure.
turn("e-hatch", failure ? track([[0, 110], [4.8, 110], [5.4, 0, "in"]]) : function () { return 110; });
if (glitch) fade("e-gun", function (t) { return t >= ANSWER && t < 3.0 && Math.floor(t * 10) % 2 === 0 ? 1 : 0; });
if (failure) fade("e-fog", track([[ANSWER, 0], [ANSWER + 0.3, 1, "out"]]));
