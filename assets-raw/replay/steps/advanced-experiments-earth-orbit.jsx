// Extended experiments in orbit: more time in orbit for longer measurements, while the batteries
// last. The satellite goes round sending its readings (a radio ring every 0.75 s), its power gauge
// running down slowly.
// Glitch: at 2.0 the power drops and the readings stop; at 3.2 the backup comes in and they resume.
// Failure: the same drop, but from 3.0 the power runs out: the satellite goes quiet and dark.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DROP = 2.0, BACKUP = 3.2, DRAIN = 3.0;

orbitScene();
put("x-power");
put("x-bar-p", "screen");

var phi = orbitAngles(OSTART, function () { return 55; });
orbitFly(phi);
turn("o-earth", function (t) { return -0.25 * t; });

var level = function (t) {
    var l = 0.85 - 0.03 * t;
    if (!(glitch || failure) || t < DROP) return l;
    var dropped = l - 0.5 * clamp((t - DROP) / 0.3, 0, 1);
    if (glitch && t >= BACKUP) return dropped + 0.3 * clamp((t - BACKUP) / 0.4, 0, 1);
    if (failure && t >= DRAIN) return dropped * (1 - clamp((t - DRAIN) / 0.8, 0, 1));
    return dropped;
};
gauge("x-bar-p", level, function (t) { return (glitch || failure) && t >= DROP && t < (glitch ? BACKUP + 0.4 : 99); });

radioRings(sendTimes(function (t) {
    return (glitch && t >= DROP && t < BACKUP + 0.2) || (failure && t >= DROP);
}), phi);
// Without power, the satellite goes dark.
fade("o-sat", failure ? track([[DRAIN + 0.6, 1], [DRAIN + 1.2, 0.35, "lin"]]) : function () { return 1; });
