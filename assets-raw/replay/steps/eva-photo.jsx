// A photo, and back in (2026-10-18; the second of two spacewalk steps): floating where eva-out ends, the
// figure brings up its camera, turns it on the Earth and takes two pictures, then pulls itself in along the
// tether, camera in hand; the hatch closes.
// Glitch: after the first picture the camera slips from the glove at 1.5 and drifts; the hand goes after
// it and has it back at 2.4, and the second picture is never taken. Failure: the fingers touch it at 2.2,
// just short, which sends it off faster, and it's gone (Gemini 10's camera was lost the same way).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var FLOAT = [-60, -110, -345], AIM = [-60, -104, -305];
var SLIP = 1.5, CATCH = 2.4, MISS = 2.2;

spacewalkScene(false, true);

var start = [[0, FLOAT], [0.4, FLOAT], [1.0, AIM]];
var home = function (t0) { return [[t0 + 0.6, [-30, -60, -340]], [t0 + 1.2, [0, 0, -360]], [t0 + 2.0, [0, INSIDE, -360]]]; };
var loose = function () { return [0, 0, 0]; }, pose, grabbed = function () { return true; }, done;
if (glitch || failure) {
    // Loose: it drifts on from where the hand let it go, turning; the failure's after the miss faster.
    var let_ = evaAt(AIM, EH);
    var drift = function (t) {
        var u = t - SLIP, x = 40 * u, y = -25 * u, r = 70 * u;
        if (failure && t > MISS) { var w = t - MISS; x += 70 * w; y -= 40 * w; r += 120 * w; }
        return [let_[0] - EH[0] + x, let_[1] - EH[1] + y, AIM[2] + r];
    };
    loose = drift;
    // The hand goes after it the same way in both, to where it would be at the catch; the failure's
    // fingers touch it at MISS, just short, and send it off.
    var c = drift(Math.min(CATCH, MISS)), u = CATCH - MISS;
    var reach = handOn([EH[0] + c[0] + 40 * u, EH[1] + c[1] - 25 * u], AIM[2] - 10);
    if (glitch) {
        pose = poses(start.concat([[SLIP, AIM], [CATCH, reach]], home(CATCH + 0.3)));
        grabbed = function (t) { return t < SLIP || t >= CATCH; };
        done = CATCH + 2.3;
    } else {
        pose = poses(start.concat([[SLIP, AIM], [CATCH, reach], [CATCH + 0.2, reach]], home(CATCH + 0.4)));
        grabbed = function (t) { return t < SLIP; };
        done = CATCH + 2.6;
        fade("e-camera", track([[0, 0], [0.3, 0], [0.5, 1], [3.6, 1], [4.6, 0]]));
    }
} else {
    pose = poses(start.concat([[1.9, AIM]], home(2.1)));
    done = 4.1;
}
walk(pose);
carry(pose, 99);
var cam = handCam(pose, grabbed, loose);
if (glitch) {
    // Caught, it settles into the glove's grip over 0.4 s rather than snapping to it.
    var off = loose(CATCH)[2] - pose(CATCH)[2];
    turn("e-camera", function (t) { return cam(t)[2] + (t >= CATCH ? off * (1 - EASE.inout(clamp((t - CATCH) / 0.4, 0, 1))) : 0); });
}
if (!failure) fade("e-camera", track([[0, 0], [0.3, 0], [0.5, 1]]));
flashes(cam, glitch || failure ? [1.2] : [1.2, 1.7]);
hatch(-1, done);
