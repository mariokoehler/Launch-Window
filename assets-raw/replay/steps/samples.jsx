// Apollo's last moonwalk (user, 2026-10-20): the lander on the Moon, close in, the flag standing beside it. The
// suited figure climbs down with the rock box, hops out past the boulders, opens it and stoops to scoop up a rock,
// then a second a step further on, shuts the lid, and carries the box back up the ladder and in: the samples go
// home in the lander.
// Glitch: the second rock bounces off the box's rim and rolls away; the figure hops after it and gets it.
// Failure: the same bounce, and hopping after it the figure trips and falls flat, the box thrown and its rock
// spilled. At 4.5 the visor fogs; it gets up at last and goes back in, the box left lying.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LEAN = 35, REACH = 33, BOXK = 0.6;
var P1 = [1630, 875.5], P2 = [1670, 876], ROLLED = [1720, 876];
var A = [P1[0] - REACH, 880, 0], B = [P2[0] - REACH, 880, 0], C = [ROLLED[0] - REACH, 880, 0];
function bent(p) { return [p[0], p[1], LEAN]; }

put("paper", "screen");
put("n-ground");
put("n-rocks");
flagPlanted();
put("e-pebble-1");
put("e-pebble-2");
put("e-rockbox");
put("e-rockbox-lid", "e-rockbox");
put("e-suit");
put("e-fog", "e-suit");
hide("e-fog");
lander();
put("n-ladder");
// The camera in close, as the moonwalk's ((1130 800) at (960 600), twice), following the figure out past the
// boulders and back.
tf(world, "Scale").setValue([200, 200]);

// The walk, and when each rock goes into the box (SCOOP), the lid shuts (SHUT) and the box is let go (DROP).
var keys = [[0, INSIDE], [0.6, PORCH], [1.4, FOOT], [2.6, A], [2.8, bent(A)], [3.05, A], [3.25, B], [3.45, bent(B)], [3.7, B]];
var spans = [[1.4, 2.6, 5], [3.05, 3.25, 1]];
var SCOOP = [2.85, 3.5], SHUT = 3.75, DROP = 99, TRIP = 4.05, home, back = 1.4;
if (glitch) {
    keys = keys.concat([[3.9, B], [4.2, C], [4.4, bent(C)], [4.65, C]]);
    spans.push([3.9, 4.2, 1]);
    SCOOP = [2.85, 4.45]; SHUT = 4.7; home = 4.9; back = 1.5;
} else if (failure) {
    var going = hopping(keys3(keys.concat([[3.9, B], [4.2, C]])), spans.concat([[3.9, 4.2, 1]])), down = going(TRIP);
    var fallen = [down[0] + 20, 880, 84], up = 5.1;
    keys = keys.concat([[3.9, B], [TRIP, down], [TRIP + 0.3, fallen, "in"], [up, fallen], [up + 0.4, [fallen[0], 880, 0]]]);
    spans.push([3.9, TRIP, 1]);
    SCOOP = [2.85, 99]; SHUT = 99; DROP = TRIP; home = 5.6;
} else {
    home = 3.9;
}
var from = failure ? [fallen[0], 880, 0] : keys[keys.length - 1][1];
keys = keys.concat([[home, from], [home + back, FOOT], [home + back + 0.8, PORCH], [home + back + 1.2, INSIDE]]);
spans.push([home, home + back, 5]);
var walk = failure ? (function () {
    var rest = hopping(keys3(keys), spans);
    return function (t) { return t <= TRIP ? going(t) : rest(t); };
})() : hopping(keys3(keys), spans);
walker(walk);
var cam = function (t) { return clamp(walk(t)[0] - 270, 1130, 1300); };
bake(tf(world, "Position"), function (t) { return [960 - 2 * cam(t), 600 - 2 * 800]; });

// The box: in the hand, hanging from its handle; falling, thrown ahead and left lying on its side.
var hand = function (t) { return at(walk(t), EH); };
var THROWN = [fallen ? fallen[0] + 60 : 0, 880 - BOXK * 22];
var box = function (t) {
    if (t < DROP) return hand(t);
    var h = hand(DROP), w = EASE.out(clamp((t - DROP) / 0.4, 0, 1));
    return [mix(h[0], THROWN[0], w), mix(h[1], THROWN[1], w) - 30 * Math.sin(Math.PI * w)];
};
var BX = L["e-rockbox"];
grow("e-rockbox", function () { return BOXK; });
move("e-rockbox", function (t) { var p = box(t); return [p[0] - BX.px, p[1] - BX.py]; });
turn("e-rockbox", function (t) { return t < DROP ? 0 : 90 * EASE.out(clamp((t - DROP) / 0.4, 0, 1)); });
// The lid: shut on the way out, opened as the figure stops by the first rock, shut on the last.
turn("e-rockbox-lid", function (t) {
    if (t < 2.6) return 0;
    if (t < 2.75) return -110 * (t - 2.6) / 0.15;
    if (t < SHUT) return -110;
    return -110 * (1 - clamp((t - SHUT) / 0.12, 0, 1));
});

// The rocks: on the ground until scooped, then into the box's mouth and gone. The second, in trouble, bounces off
// the rim and rolls on (the glitch scoops it up there). The first spills out of the thrown box.
function into(p, t0) {
    return function (t) {
        if (t < t0) return p;
        var m = box(t0 + 0.15), u = clamp((t - t0) / 0.15, 0, 1);
        return [mix(p[0], m[0], u), mix(p[1], m[1] + 10, u)];
    };
}
var trouble = glitch || failure;
var p1 = into(P1, SCOOP[0]);
var p2 = trouble ? (function () {
    var BOUNCE = 3.5, rim = box(BOUNCE + 0.1), later = into(ROLLED, SCOOP[1]);
    return function (t) {
        if (t < BOUNCE) return P2;
        if (t < BOUNCE + 0.12) { var u = (t - BOUNCE) / 0.12; return [mix(P2[0], rim[0] + 6, u), mix(P2[1], rim[1] - 4, u)]; }
        if (t < 3.9) { var v = (t - BOUNCE - 0.12) / (3.9 - BOUNCE - 0.12); return [mix(rim[0] + 6, ROLLED[0], v), mix(rim[1] - 4, ROLLED[1], v) - 26 * Math.sin(Math.PI * v)]; }
        return later(t);
    };
})() : into(P2, SCOOP[1]);
var SPILL = [THROWN[0] + 24, 876.5];
var P1X = L["e-pebble-1"], P2X = L["e-pebble-2"];
move("e-pebble-1", function (t) {
    var p = t < DROP ? p1(t) : [mix(box(DROP)[0], SPILL[0], clamp((t - DROP) / 0.5, 0, 1)),
                               mix(box(DROP)[1], SPILL[1], clamp((t - DROP) / 0.5, 0, 1)) - 24 * Math.sin(Math.PI * clamp((t - DROP) / 0.5, 0, 1))];
    return [p[0] - P1X.px, p[1] - P1X.py];
});
fade("e-pebble-1", function (t) { return t >= SCOOP[0] + 0.15 && t < DROP ? 0 : 1; });
move("e-pebble-2", function (t) { var p = p2(t); return [p[0] - P2X.px, p[1] - P2X.py]; });
fade("e-pebble-2", function (t) { return t >= SCOOP[1] + 0.15 ? 0 : 1; });
if (failure) fade("e-fog", track([[4.5, 0], [4.8, 1, "out"]]));
