// The spacewalk scene, shared by the spacewalk steps: over the Earth's limb, the way home's capsule
// (nose right) with a hatch on its top, and a suited figure on a tether. The figure is placed by a
// pose [dx, dy, turn] from where it stands in the open hatch (EP); deep below it is inside, hidden by
// the capsule.
var EP = [870, 300], EW = [850, 300], EH = [916, 296], ANCHOR = [884, 380];
var INSIDE = 150;

// transfer: a second capsule docked nose to nose, with its own hatch and a handrail between them.
// kit (2026-10-18, Gemini's spacewalks in steps): the package's mount, boom, lamp and spark, and the
// camera and its flash, all hidden until a timeline shows them.
// pair (2026-10-18, a spacewalk on a rendezvous): the other capsule a few metres off, nose to nose (the
// first mirrored about XP), its hatch shut and its crewman inside with a camera ("e-suit-b").
// DOCKED (a switch set before the timeline, eva-docked.jsx; 2026-10-18): the Agena docked at the nose.
var XP = 1199;
function spacewalkScene(transfer, kit, pair) {
    var docked = typeof DOCKED !== "undefined" && DOCKED;
    put("paper", "screen");
    put("d-earth");
    put("r-orbit");
    if (kit) put("e-mount");
    put("e-box");
    if (kit) {
        put("e-boom", "e-box");
        put("e-lamp", "e-box");
        put("e-spark");
    }
    put("e-suit");
    put("e-fog", "e-suit");
    put("e-gun", "e-suit");
    if (kit) {
        put("e-camera");
        put("e-flash");
        hide("e-lamp"); hide("e-spark"); hide("e-camera"); hide("e-flash");
        tf(layers["e-boom"], "Scale").setValue([100, 1]);
    }
    put("e-tether");
    put("r-capsule");
    put("e-hole");
    put("e-hatch");
    if (docked) put("e-agena");
    if (pair) {
        put("e-suit-b");
        put("e-camera-b", "e-suit-b");
        put("e-flash-b", "e-suit-b");
        put("e-capsule-pair");
        put("e-hole-pair");
        put("e-hatch-pair");
        hide("e-flash-b");
    }
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
    // (960 470); for a pair or a docked Agena 1.3 times, the two craft's middle at (960 440). The
    // Earth's limb stays at the bottom.
    // A craft's own transfer framing (TRANSFER_VIEW, lok-transfer.jsx: [x, y, at y, scale]; 2026-10-19).
    var tv = transfer && typeof TRANSFER_VIEW !== "undefined" ? TRANSFER_VIEW : null;
    var k = tv ? tv[3] : transfer ? 1.2 : pair || docked ? 1.3 : 1.35;
    var c = tv ? tv : transfer ? [1128, 400, 470] : pair ? [XP, 400, 440] : docked ? [1153, 400, 440] : [900, 400, 430];
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
    // All the way in, it's gone (a foot would show under the hull, 2026-10-18).
    fade("e-suit", function (t) { return pose(t)[1] > INSIDE - 10 ? 0 : 1; });
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
// The kit's motion (2026-10-18). The boom out of the package, 0 (in) to 1 (up); it shows only once out.
function boom(fn) {
    grow("e-boom", function (t) { return [1, Math.max(0.01, fn(t))]; });
    fade("e-boom", function (t) { return fn(t) > 0.04 ? 1 : 0; });
}
// The camera in the hand (pose) while held(t), else where loose(t) puts it: [dx, dy, turn] from rest.
function handCam(pose, held, loose) {
    var at = function (t) {
        if (held(t)) { var h = evaAt(pose(t), EH); return [h[0] - EH[0], h[1] - EH[1], pose(t)[2]]; }
        return loose(t);
    };
    move("e-camera", function (t) { var a = at(t); return [a[0], a[1]]; });
    turn("e-camera", function (t) { return at(t)[2]; });
    return at;
}
// A flash at the lens at each of times, a tenth of a second long, the camera where cam(t) puts it.
function flashes(cam, times) {
    var on = function (t) { for (var i = 0; i < times.length; i++) if (t >= times[i] && t < times[i] + 0.1) return true; return false; };
    move("e-flash", function (t) {
        var c = cam(t), a = c[2] * RAD, x = 944 - EH[0], y = 294 - EH[1];
        return [EH[0] + c[0] + x * Math.cos(a) - y * Math.sin(a) - 944, EH[1] + c[1] + x * Math.sin(a) + y * Math.cos(a) - 294];
    });
    fade("e-flash", function (t) { return on(t) ? 1 : 0; });
}
// Micrometeoroid hits on the plate: [[t, dx, dy], ...] from its middle, each a spark for 0.2 s.
function sparks(hits) {
    var at = function (t) { for (var i = 0; i < hits.length; i++) if (t >= hits[i][0] && t < hits[i][0] + 0.2) return hits[i]; return null; };
    move("e-spark", function (t) { var h = at(t); return h ? [h[1], h[2]] : [0, 0]; });
    grow("e-spark", function (t) { var h = at(t); return h ? 1 - 2 * (t - h[0]) : 1; });
    fade("e-spark", function (t) { return at(t) ? 1 : 0; });
}
// Where the figure works at the package (a hand on its corner) and where it takes it.
var WORK = handOn([957, 354], 25), TAKE = handOn([975, 360], 60);
