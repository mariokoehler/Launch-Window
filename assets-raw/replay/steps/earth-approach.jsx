// Earth approach: home from the Moon, the craft comes in from the left aiming for the entry corridor,
// a narrow band at the top of the Earth's air (gold). Too steep and the heating is too much; too
// shallow and it skips off into space. It reaches the band at 3.4 and runs on into the air, glowing.
// Glitch: from 1.6 its aim drifts shallow; a correction at 2.6 (a puff) puts it back in the band.
// Failure: the same drift, and the correction misfires: it passes above the band and skips away.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DRIFT = 1.6, FIX = 2.6, ARRIVE = 3.4;

orbitScene();
hide("o-orbit");
put("l-corridor");
layers["l-corridor"].moveAfter(layers["o-sat"]);
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
put("m-puff");
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
turn("o-earth", function (t) { return -0.25 * t; });

// The aim: a straight line from the left to the band's left end (angle 250, radius 276), then round
// the top inside the band (clockwise, angles rising) as it slows into the air.
var A = 250 * RAD, T = [OCX + 276 * Math.cos(A), OCY + 276 * Math.sin(A)], P0 = [-80, 160];
var shallow = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var d = 46 * clamp((t - DRIFT) / 1.0, 0, 1);
    if (glitch && t > FIX) d *= clamp(1 - (t - FIX) / 0.6, 0, 1);
    return d;
};
function where(t) {
    if (t <= ARRIVE || failure) {
        // On the line, lifted off it (up, away from the Earth) by the drift.
        var u = t / ARRIVE, x = P0[0] + (T[0] - P0[0]) * u, y = P0[1] + (T[1] - P0[1]) * u;
        return [x, y - shallow(Math.min(t, failure ? 99 : ARRIVE)) * (failure && t > ARRIVE ? 1 + 0.9 * (t - ARRIVE) : 1)];
    }
    // In the band: round the top, slowing.
    var a = A + (0.55 * (t - ARRIVE) - 0.05 * Math.pow(t - ARRIVE, 2)), r = 276 - 6 * (t - ARRIVE);
    return [OCX + r * Math.cos(a), OCY + r * Math.sin(a)];
}
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
// Base first: nose back along its way (the line's heading, then the band's).
var HEAD = Math.atan2(T[1] - P0[1], T[0] - P0[0]) / RAD;
turn("o-sat", function (t) {
    if (t <= ARRIVE || failure) return HEAD - 90 - (failure && t > ARRIVE ? 12 * (t - ARRIVE) : 0);
    return (A / RAD + (0.55 * (t - ARRIVE) - 0.05 * Math.pow(t - ARRIVE, 2)) / RAD) + 0;
});
// The glow of the air once it's in the band (not for the skip).
// Parenting doesn't pass on opacity: the glow fades with the craft.
fade("o-streak", failure ? function () { return 0; } : track([[ARRIVE, 0], [ARRIVE + 0.6, 1, "lin"], [ARRIVE + 1.6, 1], [ARRIVE + 2.4, 0, "lin"]]));
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 10 * t)]; });
fade("o-sat", failure ? function () { return 1; } : track([[ARRIVE + 1.6, 1], [ARRIVE + 2.4, 0.0, "lin"]]));

// The correction: a puff at the craft (the success's at 2.0, where the others drift).
var PUFF = glitch || failure ? FIX : 2.0;
move("m-puff", function (t) { var p = where(Math.max(t, PUFF)); return [p[0] - 960, p[1] - 540]; });
grow("m-puff", track([[PUFF, 0.6], [PUFF + 0.5, 2.2, "out"], [PUFF + 1.1, 2.6, "lin"]]));
fade("m-puff", track([[PUFF, 0, "snap"], [PUFF + 0.02, 1, "snap"], [PUFF + 0.5, 1], [PUFF + 1.1, 0, "lin"]]));
