// Lunar approach: falling towards the Moon on its open path (dashed), aimed to pass behind it at just
// the right height. The view closes in from far out as it comes, ending where the orbit insertion
// begins. A course correction at 2.0 (a puff); a ring home every 0.75 s.
// Glitch: from 1.6 it drifts off its aim; a correction at 3.0 brings it back by 3.8.
// Failure: the same drift, but the correction at 3.0 misfires: it's aimed wide, and will miss.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DRIFT = 1.6, FIX = glitch || failure ? 3.0 : 2.0;

orbitScene("p-moon");
put("l-shadow");
layers["l-shadow"].moveAfter(layers["p-moon"]);
put("l-open");
layers["l-open"].moveAfter(layers["o-sat"]);
put("l-earth-far", "screen");
put("m-puff");
fade("o-orbit", function () { return 0.6; });

// The open path (loi.jsx's): r = 390 * 2.6 / (1 + 1.6 cos); the craft falls in along it, ending where
// the insertion starts.
function open(th) { return 390 * 2.6 / (1 + 1.6 * Math.cos(th * RAD)); }
var H = 50 * 390 * 390;
// Where the insertion starts: 1.6 s before its burn, on the open path (equal areas, as loi.jsx).
var A0 = (function () { var th = 0; for (var f = 0; f < Math.round(1.6 * FPS); f++) { var r = open(th); th += H / (r * r) / FPS; } return th; })();
// From far out at the bottom (85 degrees), speeding up as it falls in, to A0 (60).
// Most of the way early, so it stays in the frame as the view closes in.
var angle = function (t) { var u = clamp(t / DUR, 0, 1); return 85 + (A0 - 85) * (0.6 * (1 - (1 - u) * (1 - u)) + 0.4 * u); };
// Off its aim: out from the Moon (px), growing, corrected (glitch) or not (failure).
var off = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var d = 60 * clamp((t - DRIFT) / 1.4, 0, 1);
    if (glitch && t > FIX) d *= clamp(1 - (t - FIX) / 0.8, 0, 1);
    if (failure && t > FIX) d += 220 * Math.pow(t - FIX, 1.3);
    return d;
};
function where(t) { var th = angle(t), r = open(th) + off(t); return [OCX + r * Math.cos(th * RAD), OCY + r * Math.sin(th * RAD)]; }
move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
turn("o-sat", function (t) { return angle(t); });

// The view closes in: the world from 0.4 of its size to all of it, about the Moon.
var zoom = function (t) { var u = clamp(t / DUR, 0, 1); return 0.4 + 0.6 * u * u * (3 - 2 * u); };
bake(tf(world, "Scale"), function (t) { return [100 * zoom(t), 100 * zoom(t)]; });
bake(tf(world, "Position"), function (t) { var k = zoom(t); return [OCX * (1 - k), OCY * (1 - k)]; });

// The correction: a puff at the craft.
move("m-puff", function (t) { var p = where(Math.max(t, FIX)); return [p[0] - 960, p[1] - 540]; });
grow("m-puff", track([[FIX, 0.6], [FIX + 0.5, 2.4, "out"], [FIX + 1.1, 3.0, "lin"]]));
fade("m-puff", track([[FIX, 0, "snap"], [FIX + 0.02, 1, "snap"], [FIX + 0.5, 1], [FIX + 1.1, 0, "lin"]]));

radioRingsAt(sendTimes(function (t) { return inShadow(where(t)); }), where);
