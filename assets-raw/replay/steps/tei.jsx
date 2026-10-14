// Trans-Earth injection: in orbit round the Moon, the craft fires its engine behind it at 1.6, out of
// contact, and comes out on its way home: up and away, towards the Earth on the left.
// Glitch: the burn cuts out at 1.9 and relights at 2.2; it leaves, later.
// Failure: the same cut, and the relight dies at 2.4: it comes out still in orbit.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.6, CUT = 1.9, RELIGHT = 2.2, DIES = 2.4, END = glitch ? 2.9 : 2.4;

orbitScene("p-moon");
put("l-shadow");
layers["l-shadow"].moveAfter(layers["p-moon"]);
put("l-earth-far", "screen");
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];

function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < END;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    return glitch ? t < END : t < DIES && jitter(t, 4) > -0.2;
}
// From the bottom right, behind the Moon at the burn (angle 0 at 1.6): round at 55 a second.
var phi = orbitAngles(88, function () { return 55; });
// Once it's on its way, nearly straight along its heading when the engine stopped, faster and faster.
function where(t) {
    if (failure || t <= END) return orbitWhere(phi, null, t);
    var p = orbitWhere(phi, null, END), a = phi(END) * RAD, u = t - END, d = 260 * u + 90 * u * u;
    return [p[0] + d * Math.sin(a), p[1] - d * Math.cos(a)];
}
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function (t) { return phi(failure ? t : Math.min(t, END)); });
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });

radioRingsAt(sendTimes(function (t) { return inShadow(where(t)); }), where);
