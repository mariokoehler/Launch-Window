// Filming (2026-10-18; Voskhod 2's middle spacewalk step): floating where the spacewalk out ends (FLOAT), the
// figure turns slowly on its tether; it brings its camera up and takes two pictures, then lets the camera hang
// and drifts back to FLOAT. (The cine camera on the airlock is left out: it popped in at this clip, user, 2026-10-19.)
// Glitch: the suit has stiffened in the vacuum (Leonov's did): at 1.15 the hand slips off the shutter; it tries
// again and gets one picture at 2.2. Failure: the second try slips too: no pictures.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FLOAT = [-60, -110, -345], AIM = [-60, -104, -305], SLIPPED = [-58, -106, -322];
var SLIP = 1.15, AGAIN = 2.2;

spacewalkScene(false, true);

var start = [[0, FLOAT], [0.4, FLOAT], [1.0, AIM]];
var pose, shots;
if (glitch || failure) {
    pose = poses(start.concat([[SLIP, AIM], [1.35, SLIPPED], [1.8, SLIPPED], [AGAIN - 0.1, AIM], [AGAIN + 0.05, AIM],
                               [AGAIN + 0.25, SLIPPED], [3.6, SLIPPED], [4.5, FLOAT]]));
    shots = glitch ? [AGAIN] : [];
} else {
    pose = poses(start.concat([[2.0, AIM], [3.6, [-56, -112, -330]], [4.5, FLOAT]]));
    shots = [1.2, 1.7];
}
walk(pose);
carry(pose, 99);
var cam = handCam(pose, function () { return true; }, function () { return [0, 0, 0]; });
fade("e-camera", track([[0, 0], [0.3, 0], [0.5, 1]]));
flashes(cam, shots);
hatch(-1, 99);
