// The clips' shared motion helpers (every step's builder puts this first). Before this: DIR (the
// layer PNGs' folder) and L (layers.json); then setup(name, duration) makes the comp.
// Every motion is a function of time, baked into a key on every frame (linear between frames),
// so the eases, overshoots and flickers come out exactly as written.
var FPS = 30, W = 1920, H = 1080;
var comp, world, NFRAMES, layers = {};

// The comp, white, and the camera: the world hangs off a null, which moves down as the camera rises.
function setup(name, dur) {
    comp = app.project.items.addComp(name, W, H, 1, dur, FPS);
    comp.bgColor = [1, 1, 1];
    NFRAMES = Math.round(dur * FPS);
    world = comp.layers.addNull();
    world.name = "World";
    tf(world, "Anchor Point").setValue([0, 0]);
    tf(world, "Position").setValue([0, 0]);
}
// MIRROR (set by the builder): the render is flipped left for right afterwards (build.py), not in
// the comp: EffectCraft drops a large turning layer (the Earth) under a parent scaled by -100.
// A layer that has to come out unflipped is flipped here first (unflip).
var MIRROR = typeof MIRROR !== "undefined" && MIRROR;
function unflip(name) {
    var l = layers[name];
    if (!MIRROR) return;
    var p = tf(l, "Position").value;
    tf(l, "Position").setValue([W - p[0], p[1]]);
    tf(l, "Scale").setValue([-100, 100]);
    l.rest = tf(l, "Position").value;
}

function tf(l, n) { return l.property("Transform").property(n); }

// Adds a part at its world position, pivot as anchor. parent: a part name, "screen" or none (world).
// A step can draw a part as another (2026-10-17): ALIAS maps a part's name to the drawing it uses (build.py
// writes it from a step's "skin", as Mercury's capsule for the generic one). The layer keeps its own name.
function drawn(name) {
    return typeof ALIAS !== "undefined" && ALIAS.hasOwnProperty(name) ? ALIAS[name] : name;
}
function put(name, parent) {
    var d = L[drawn(name)];
    var item = app.project.importFile(new ImportOptions(new File(DIR + drawn(name) + ".png")));
    var l = comp.layers.add(item);
    l.name = name;
    tf(l, "Anchor Point").setValue([d.px - d.x, d.py - d.y]);
    if (parent === "screen") {
        tf(l, "Position").setValue([d.px, d.py]);
    } else if (parent) {
        // Parent first: parenting keeps a layer where it is, and then the position is in the
        // parent's layer space.
        var p = L[drawn(parent)];
        l.parent = layers[parent];
        tf(l, "Position").setValue([d.px - p.x, d.py - p.y]);
    } else {
        tf(l, "Position").setValue([d.px, d.py]);
        l.parent = world;
    }
    l.rest = tf(l, "Position").value;
    layers[name] = l;
    return l;
}

function bake(prop, fn) {
    var ts = [], vs = [];
    for (var f = 0; f <= NFRAMES; f++) { ts.push(f / FPS); vs.push(fn(f / FPS)); }
    if (prop.setValuesAtTimes) prop.setValuesAtTimes(ts, vs);
    else for (var i = 0; i < ts.length; i++) prop.setValueAtTime(ts[i], vs[i]);
}
// Offsets from the part's rest place (world px), degrees, a uniform scale (1 = rest), opacity 0..1.
function move(name, fn) {
    var l = layers[name], r = l.rest;
    bake(tf(l, "Position"), function (t) { var o = fn(t); return [r[0] + o[0], r[1] + o[1]]; });
}
function turn(name, fn) { bake(tf(layers[name], "Rotation"), fn); }
function grow(name, fn) {
    bake(tf(layers[name], "Scale"), function (t) {
        var s = fn(t);
        return s instanceof Array ? [100 * s[0], 100 * s[1]] : [100 * s, 100 * s];
    });
}
function fade(name, fn) { bake(tf(layers[name], "Opacity"), function (t) { return 100 * fn(t); }); }
function hide(name) { tf(layers[name], "Opacity").setValue(0); }

// Eases for the segment that ends at a key.
var EASE = {
    lin: function (u) { return u; },
    "in": function (u) { return u * u; },
    out: function (u) { return 1 - (1 - u) * (1 - u); },
    inout: function (u) { return u * u * (3 - 2 * u); },
    snap: function (u) { return u >= 1 ? 1 : 0; }
};
function mix(a, b, u) {
    if (a instanceof Array) { var r = []; for (var i = 0; i < a.length; i++) r.push(a[i] + (b[i] - a[i]) * u); return r; }
    return a + (b - a) * u;
}
// keys: [[t, value, ease], ...]; holds the first value before and the last after.
function track(keys) {
    return function (t) {
        if (t <= keys[0][0]) return keys[0][1];
        for (var i = 1; i < keys.length; i++) {
            if (t <= keys[i][0]) {
                var u = (t - keys[i - 1][0]) / (keys[i][0] - keys[i - 1][0]);
                return mix(keys[i - 1][1], keys[i][1], EASE[keys[i][2] || "inout"](u));
            }
        }
        return keys[keys.length - 1][1];
    };
}
// A smooth curve through [[t, v], ...] (monotone cubic, so it never overshoots a key); it starts
// at rest and leaves the last key at the last segment's speed.
function smooth(pts) {
    var n = pts.length, d = [], m = [];
    for (var i = 0; i < n - 1; i++) d.push((pts[i + 1][1] - pts[i][1]) / (pts[i + 1][0] - pts[i][0]));
    m.push(0);
    for (i = 1; i < n - 1; i++) m.push(d[i - 1] * d[i] <= 0 ? 0 : 2 / (1 / d[i - 1] + 1 / d[i]));
    m.push(d[n - 2]);
    return function (t) {
        if (t <= pts[0][0]) return pts[0][1];
        if (t >= pts[n - 1][0]) return pts[n - 1][1] + m[n - 1] * (t - pts[n - 1][0]);
        for (var k = 0; k < n - 1; k++) {
            if (t <= pts[k + 1][0]) {
                var h = pts[k + 1][0] - pts[k][0], s = (t - pts[k][0]) / h;
                var h00 = 2 * s * s * s - 3 * s * s + 1, h10 = s * s * s - 2 * s * s + s;
                var h01 = -2 * s * s * s + 3 * s * s, h11 = s * s * s - s * s;
                return h00 * pts[k][1] + h10 * h * m[k] + h01 * pts[k + 1][1] + h11 * h * m[k + 1];
            }
        }
    };
}
function clamp(x, a, b) { return Math.max(a, Math.min(b, x)); }
// A new random value every frame, in -1..1, the same on every run.
function jitter(t, seed) {
    var f = Math.round(t * FPS);
    var x = Math.sin(f * 12.9898 + seed * 78.233) * 43758.5453;
    return 2 * (x - Math.floor(x)) - 1;
}
var RAD = Math.PI / 180;
// Which one of several states shows: fn(t) returns its name, every other is hidden.
function states(names, fn) {
    for (var i = 0; i < names.length; i++) {
        (function (n) { fade(n, function (t) { return fn(t) === n ? 1 : 0; }); })(names[i]);
    }
}
// Shake: amplitude envelope (px) times a new random offset every frame.
function camera(offset, amplitude) {
    bake(tf(world, "Position"), function (t) {
        var a = amplitude(t);
        return [a * jitter(t, 1), offset(t) + a * jitter(t, 2)];
    });
}
// A flame's flicker: its length ±3 % at 12 Hz, about its pivot.
function flicker(names, extra) {
    for (var i = 0; i < names.length; i++) {
        grow(names[i], function (t) {
            var k = extra ? extra(t) : 1;
            return [k, k * (1 + 0.03 * Math.sin(2 * Math.PI * 12 * t))];
        });
    }
}
