// Apollo's rover drives (user, 2026-10-20): the rover parked beside the lander, the flag standing, close in as the
// moonwalks. The crew drive off right, the wheels throwing a little dust, and the camera rides along; a rock in the
// way at 2.1 bumps the rover gently, and it pulls up at the next site at 4.8.
// Glitch: the rock gives it a hard jolt and the rear fender's extension snaps off (as Apollo 17's did): from then
// on the wheel throws a rooster tail of dust over the crew, all the way to the site.
// Failure: the same jolt, and the rover dies, stopping dead. The crew climb off and hop
// back towards the lander (the reason no drive went farther than they could walk back).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure", trouble = glitch || failure;
var LRV = [1400, 846], WHEELS = [1354, 1446], R = 16, V = 220, BUMP = 1730, LET = 2.25;

put("paper", "screen");
put("lrv-ridge");
put("n-ground");
put("lrv-terrain");
put("lrv-bump");
lander();
flagPlanted();
put("lrv-body");
put("lrv-wheel-rear", "lrv-body");
put("lrv-wheel-front", "lrv-body");
put("lrv-dust", "lrv-body");
put("lrv-spray", "lrv-body");
put("lrv-crew", "lrv-body");
put("lrv-fender");
put("lrv-walker");
put("e-suit");
tf(world, "Scale").setValue([200, 200]);

// The speed: up to V by 1.1, down to a stop at the site by 4.8 (or dead by 2.75); the distance summed frame by frame.
function speed(t) {
    if (t < 0.5) return 0;
    if (t < 1.1) return V * (t - 0.5) / 0.6;
    if (failure && t > 2.45) return V * clamp(1 - (t - 2.45) / 0.3, 0, 1);
    if (t > 4.2) return V * clamp(1 - (t - 4.2) / 0.6, 0, 1);
    return V;
}
var xs = [LRV[0]];
for (var f = 1; f <= NFRAMES; f++) xs.push(xs[f - 1] + speed((f - 0.5) / FPS) / FPS);
function x(t) { return xs[clamp(Math.round(t * FPS), 0, NFRAMES)]; }
// Going over the rock: each wheel lifts as it crosses it, gently, or hard in trouble (the front first); the body
// rides up and pitches with them, and bobs a little all the drive.
var LIFT = trouble ? 13 : 3;
function over(wx) { var d = (wx - BUMP) / 14; return Math.abs(d) < 1 ? LIFT * Math.cos(d * Math.PI / 2) : 0; }
function bob(t) { return speed(t) > 1 ? 1.2 * Math.sin(2 * Math.PI * 3 * t) * speed(t) / V : 0; }
function lifts(t) { var o = x(t) - LRV[0]; return [over(WHEELS[1] + o), over(WHEELS[0] + o)]; }
move("lrv-body", function (t) { var l = lifts(t); return [x(t) - LRV[0], -(l[0] + l[1]) / 2 - bob(t)]; });
turn("lrv-body", function (t) { var l = lifts(t); return Math.atan2(l[1] - l[0], WHEELS[1] - WHEELS[0]) / RAD; });
turn("lrv-wheel-front", function (t) { return (x(t) - LRV[0]) / R / RAD; });
turn("lrv-wheel-rear", function (t) { return (x(t) - LRV[0]) / R / RAD; });
fade("lrv-dust", function (t) { return speed(t) > 20 ? 0.6 + 0.4 * Math.abs(jitter(t, 3)) : 0; });
fade("lrv-spray", glitch ? function (t) { return t > LET + 0.1 && speed(t) > 20 ? 0.7 + 0.3 * Math.abs(jitter(t, 4)) : 0; }
                         : function () { return 0; });

// The fender's extension: on the rover, or (the glitch) knocked off at the jolt, tumbling back to the ground.
var FD = L["lrv-fender"], off = x(LET) - LRV[0], lying = [FD.px + off - 50, 878];
move("lrv-fender", function (t) {
    if (!glitch || t < LET) { var l = lifts(t); return [x(t) - LRV[0], -(l[0] + l[1]) / 2 - bob(t)]; }
    var u = clamp((t - LET) / 0.5, 0, 1), from = [FD.px + off, FD.py];
    return [mix(from[0], lying[0], u) - FD.px, mix(from[1], lying[1], u) - 30 * Math.sin(Math.PI * u) - FD.py];
});
turn("lrv-fender", function (t) { return glitch && t >= LET ? 300 * clamp((t - LET) / 0.5, 0, 1) : 0; });

// The failure's walk back: both off the rover at 3.3, turned to face the lander, hopping left.
var OFF = 3.3, stop = x(3.0);
var WALK = [stop - 20, stop + 14];
function feet(i, t) {
    if (t < OFF + 0.3) return [WALK[i], 880];
    var u = t - OFF - 0.3, start = 0.3 * i;
    var wx = WALK[i] - 210 * Math.max(0, u - start);
    var hop = u > start ? 16 * Math.abs(Math.sin(Math.PI * 2.2 * (u - start))) : 0;
    return [wx, 880 - hop];
}
var SUITS = ["lrv-walker", "e-suit"], EP = [870, 300], FP = [876, 366], S = 0.45;
for (var i = 0; i < 2; i++) {
    (function (i, name) {
        grow(name, function () { return [-S, S]; });
        move(name, function (t) { var p = feet(i, t); return [p[0] + S * (FP[0] - EP[0]) - EP[0], p[1] - S * (FP[1] - EP[1]) - EP[1]]; });
        fade(name, function (t) { return failure && t >= OFF ? 1 : 0; });
    })(i, SUITS[i]);
}
fade("lrv-crew", function (t) { return failure && t >= OFF ? 0 : 1; });

// The camera rides along a little behind the rover (the lander's right half in view at the start); in the failure it
// follows the crew back.
function cam(t) {
    var c = x(t);
    if (failure && t > OFF) c = Math.min(c, feet(0, t)[0] + 120);
    return Math.max(1300, c - 100);
}
bake(tf(world, "Position"), function (t) { return [960 - 2 * cam(t), 600 - 2 * 800]; });
move("lrv-ridge", function (t) { return [0.5 * (cam(t) - 1300), 0]; });
