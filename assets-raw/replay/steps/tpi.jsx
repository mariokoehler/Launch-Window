// Trans-planetary injection: in the Earth orbit scene (the camera up and left, to give it room), the
// probe fires its engine at 1.0 and the burn sends it out of the Earth's grip: it climbs, and once
// the engine stops it leaves on a nearly straight line, off to the right.
// Glitch: the burn cuts out at 1.6 and relights at 2.2; it leaves, later.
// Failure: the same cut, but the relight dies at 2.6: it's left on a long ellipse, out and back round.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FIRE = 1.0, CUT = 1.6, RELIGHT = 2.2, DIES = 2.6;

orbitScene();
put("o-streak");
layers["o-streak"].parent = layers["o-sat"];
hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
turn("o-earth", function (t) { return -0.25 * t; });

// When the engine burns.
function burning(t) {
    if (t < FIRE) return false;
    if (!(glitch || failure)) return t < 2.6;
    if (t < CUT) return true;
    if (t < RELIGHT) return false;
    return glitch ? t < 3.4 : t < DIES;
}
// The thrust so far (burn seconds) raises the orbit; a full burn (1.6 s) sends it away, faster and
// faster once the engine stops; the failure's short one (1.0 s) leaves it on a long ellipse, out to
// 640 and back round.
var spent = [], b = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { spent.push(b); if (burning(f / FPS)) b += 1 / FPS; }
function burnt(t) { return spent[clamp(Math.round(t * FPS), 0, spent.length - 1)]; }
var END = glitch ? 3.4 : 2.6;
var radius = function (t) {
    var r = 390 + 60 * burnt(t);
    if (failure && t > DIES) {
        var u = t - DIES, top = 640, at = 390 + 60 * burnt(DIES);
        return u < 1.4 ? at + (top - at) * Math.sin(Math.PI / 2 * u / 1.4) : top - (top - 390) * Math.sin(Math.PI / 2 * clamp((u - 1.4) / 2.2, 0, 1));
    }
    if (!failure && t > END) r += 90 * Math.pow(t - END, 2);
    return r;
};
var phi = orbitAngles(OSTART, function (t) { return 55 * Math.pow(390 / radius(t), 1.5); });
// Out of the Earth's grip it goes nearly straight, along its heading when the engine stopped.
function where(t) {
    if (failure || t <= END) return orbitWhere(phi, radius, t);
    var p = orbitWhere(phi, radius, END), a = phi(END) * RAD, u = t - END, d = 150 * u + 70 * u * u;
    return [p[0] + d * Math.sin(a), p[1] - d * Math.cos(a)];
}
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function (t) { return phi(failure ? t : Math.min(t, END)); });
bake(tf(world, "Position"), function () { return [-300, -150]; });
// The flame behind it while the engine burns, flickering.
fade("o-streak", function (t) { return burning(t) ? 1 : 0; });
grow("o-streak", function (t) { return [1, 0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t)]; });
