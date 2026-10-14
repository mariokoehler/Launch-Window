// The spacewalk scene, shared by the spacewalk steps: over the Earth's limb, the way home's capsule
// (nose right) with a hatch on its top, and a suited figure on a tether. The figure is placed by a
// pose [dx, dy, turn] from where it stands in the open hatch (EP); deep below it is inside, hidden by
// the capsule.
var EP = [870, 300], EW = [850, 300], EH = [916, 296], ANCHOR = [884, 380];
var INSIDE = 150;

// transfer: a second capsule docked nose to nose, with its own hatch and a handrail between them.
function spacewalkScene(transfer) {
    put("paper", "screen");
    put("d-earth");
    put("r-orbit");
    put("e-box");
    put("e-suit");
    put("e-fog", "e-suit");
    put("e-gun", "e-suit");
    put("e-tether");
    put("r-capsule");
    put("e-hole");
    put("e-hatch");
    if (transfer) {
        put("e-capsule-b");
        put("e-collar");
        put("e-hole-b");
        put("e-hatch-b");
        put("e-rail");
    }
    turn("d-earth", function (t) { return -0.25 * t; });
    hide("e-fog"); hide("e-gun");
    // The camera in close, so the figure reads on the replay's small monitor: 1.35 times, the
    // capsule's top (900 400) at (960 430); for a transfer 1.2 times, the stack's middle (1128 400) at
    // (960 470). The Earth's limb stays at the bottom.
    var k = transfer ? 1.2 : 1.35, c = transfer ? [1128, 400, 470] : [900, 400, 430];
    tf(world, "Scale").setValue([100 * k, 100 * k]);
    tf(world, "Position").setValue([960 - k * c[0], c[2] - k * c[1]]);
}
// A point of the figure at rest (world px), where the pose puts it.
function evaAt(pose, p) {
    var a = pose[2] * RAD, x = p[0] - EP[0], y = p[1] - EP[1];
    return [EP[0] + pose[0] + x * Math.cos(a) - y * Math.sin(a), EP[1] + pose[1] + x * Math.sin(a) + y * Math.cos(a)];
}
// The pose that puts the hand on a point, at a turn.
function handOn(p, rot) {
    var h = evaAt([0, 0, rot], EH);
    return [p[0] - h[0], p[1] - h[1], rot];
}
// Poses through keys [[t, [dx, dy, turn]], ...], eased in and out.
function poses(keys) {
    var f = [];
    for (var i = 0; i < 3; i++) {
        var ks = [];
        for (var j = 0; j < keys.length; j++) ks.push([keys[j][0], keys[j][1][i], "inout"]);
        f.push(track(ks));
    }
    return function (t) { return [f[0](t), f[1](t), f[2](t)]; };
}
// Moves the figure by pose(t), with its tether from the hatch to its backpack (slack inside).
function walk(pose) {
    move("e-suit", function (t) { var p = pose(t); return [p[0], p[1]]; });
    turn("e-suit", function (t) { return pose(t)[2]; });
    move("e-tether", function () { return [ANCHOR[0] - EW[0], ANCHOR[1] - EW[1]]; });
    turn("e-tether", function (t) { var w = evaAt(pose(t), EW); return Math.atan2(w[1] - ANCHOR[1], w[0] - ANCHOR[0]) / RAD; });
    grow("e-tether", function (t) {
        var w = evaAt(pose(t), EW);
        return [Math.max(0.01, Math.sqrt(Math.pow(w[0] - ANCHOR[0], 2) + Math.pow(w[1] - ANCHOR[1], 2)) / 100), 1];
    });
    fade("e-tether", function (t) { return pose(t)[1] > 90 ? 0 : 1; });
}
// The hatch opens over [a, a + 0.6] and closes over [c, c + 0.6] (the second capsule's: "e-hatch-b").
function hatch(a, c, name) {
    var s = name === "e-hatch-b" ? -1 : 1;
    turn(name || "e-hatch", track([[a, 0], [a + 0.6, s * 110, "out"], [c, s * 110], [c + 0.6, 0, "in"]]));
}
// The package rides in the hand from the grab on.
function carry(pose, grab) {
    move("e-box", function (t) {
        if (t < grab) return [0, 0];
        var h = evaAt(pose(t), EH);
        return [h[0] - 975, h[1] - 360];
    });
    turn("e-box", function (t) { return t < grab ? 0 : pose(t)[2]; });
}
