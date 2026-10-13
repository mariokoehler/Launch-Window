// The launch scene, shared by its three variants and by the steps on the pad (launch preparations,
// countdown). Before this: ../motion.jsx (the helpers), L, VARIANT, DUR, and setup(...) called.
// Where the rocket's bell exit (960, 840) is, for a rocket pose: pivot (960, 600).
function bellOf(tx, ty, r) {
    return [960 + tx - 240 * Math.sin(r * RAD), 600 + ty + 240 * Math.cos(r * RAD)];
}

// The pose that puts the bell at (bx, by) with the rocket turned r degrees.
function poseFor(bx, by, r) {
    return [bx - 960 + 240 * Math.sin(r * RAD), by - 600 - 240 * Math.cos(r * RAD)];
}

// The trail shows from the pad up to the bell: a wipe hides the part above it.
function trailTo(name, bellY) {
    var d = L[name], l = layers[name];
    var wipe = l.property("Effects").addProperty("Linear Wipe");
    wipe.property("Wipe Angle").setValue(180);
    bake(wipe.property("Transition Completion"), function (t) {
        return clamp(100 * (bellY(t) - d.y) / d.h, 0, 100);
    });
}

// Pre-launch venting: each wisp drifts up 40 px and fades, 0.8 s a loop, until the envelope ends.
function wisps(envelope) {
    for (var i = 1; i <= 3; i++) {
        (function (n, off) {
            var ph = function (t) { return ((t + off) % 0.8) / 0.8; };
            move(n, function (t) { return [0, -40 * ph(t)]; });
            fade(n, function (t) { var p = ph(t); return 0.7 * Math.min(1, p / 0.12) * (1 - p) * envelope(t); });
        })("wisp-" + i, i * 0.27);
    }
}

// The ground smoke, smoke-1 to smoke-6: each side's puff moves out by -side * its offset.
// keys: [[t, offset, scale, opacity, ease], ...] per puff.
function groundSmoke(perPuff) {
    for (var name in perPuff) {
        (function (n, keys) {
            var side = (n === "smoke-1" || n === "smoke-3" || n === "smoke-5") ? -1 : 1;
            var off = track(keys.map(function (k) { return [k[0], k[1], k[4]]; }));
            var sc = track(keys.map(function (k) { return [k[0], k[2], k[4]]; }));
            var op = track(keys.map(function (k) { return [k[0], k[3], k[4] === "snap" ? "snap" : "lin"]; }));
            move(n, function (t) { return [side * off(t), 0]; });
            grow(n, sc);
            fade(n, op);
        })(name, perPuff[name]);
    }
}

// The parts in the storyboard's back-to-front order.
function stack(trail) {
    put("paper", "screen");
    put("ruler");
    for (var i = 1; i <= 4; i++) put("cloud-" + i);
    put("trailPlanned");
    put(trail);
    for (i = 1; i <= 3; i++) put("wisp-" + i);
    put("tower"); put("armUpper"); put("armLower");
    put("rocket");
    // The flame hangs off the rocket but draws behind it.
    var flames = ["flame-ignite", "flame-full", "flame-sputter"];
    for (i = 0; i < flames.length; i++) { put(flames[i], "rocket"); layers[flames[i]].moveAfter(layers.rocket); }
    for (i = 1; i <= 6; i++) put("debris-" + i);
    put("ground"); put("pad");
    for (i = 1; i <= 12; i++) put("smoke-" + i);
    put("burst");
    for (i = 1; i <= 6; i++) put("fallPath-" + i);
    // Hidden unless a variant shows them.
    var hidden = ["trailPlanned", "burst"];
    for (i = 1; i <= 6; i++) hidden.push("debris-" + i, "fallPath-" + i);
    for (i = 1; i <= 12; i++) hidden.push("smoke-" + i);
    for (i = 0; i < hidden.length; i++) tf(layers[hidden[i]], "Opacity").setValue(0);
    for (i = 0; i < flames.length; i++) tf(layers[flames[i]], "Opacity").setValue(0);
}
