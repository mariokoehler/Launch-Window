// The probe landers' scenes (after orbit.jsx, whose radio rings they use, and moon.jsx, whose surface and
// dust they stand on): the lander (sphere, ring, mast, arm) placed by offsets from where it stands on the
// surface, its mast's tip sending the rings.
var VL = [760, 812];

function probeGround() {
    put("paper", "screen");
    put("n-ground");
    put("n-rocks");
    for (var i = 1; i <= 4; i++) put("ring-" + i);
    put("n-dust");
    hide("n-dust");
    put("v-arm");
    put("v-lander");
    put("v-mast", "v-lander");
    put("v-soil");
    hide("v-soil");
}
// The camera in on the lander, 1.6 times, (880 780) at (960 600). After every put(): a part put once the
// world is scaled is placed in its scaled space, off where it belongs.
function probeCamera() {
    tf(world, "Scale").setValue([160, 160]);
    tf(world, "Position").setValue([960 - 1.6 * 880, 600 - 1.6 * 780]);
}
// Where the mast's tip is (world px) with the lander at offset o, turned rot, the mast raised by up degrees
// short of upright.
function mastTip(o, rot, up) {
    var a = rot * RAD, b = (rot + up) * RAD;
    // The mast's foot is 48 above the lander's centre; the tip 80 further along the mast.
    var fx = VL[0] + o[0] + 48 * Math.sin(a), fy = VL[1] + o[1] - 48 * Math.cos(a);
    return [fx + 80 * Math.sin(b), fy - 80 * Math.cos(b)];
}
