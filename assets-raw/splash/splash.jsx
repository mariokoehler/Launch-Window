// Launch Window splash: the mission-patch logo assembling itself. 800 x 800 (4x the SVG), 30 fps.
// Run through make_splash.py, which first writes the layers with split_logo.py.
var DIR = new File($.fileName).parent.fsName + "/build/layers/";
var FPS = 30, SIZE = 800, DUR = 4.0, C = SIZE / 2;

var comp = app.project.items.addComp("Splash", SIZE, SIZE, 1, DUR, FPS);
comp.bgColor = [0x16 / 255, 0x30 / 255, 0x5c / 255];

function img(name) {
    var item = app.project.importFile(new ImportOptions(new File(DIR + name + ".png")));
    var layer = comp.layers.add(item);
    layer.name = name;
    return layer;
}
function tf(layer, name) { return layer.property("Transform").property(name); }
// keys: [[t, value], ...]; ease: "out" (fast start, soft land), "inout", "linear"
function anim(prop, keys, ease) {
    for (var i = 0; i < keys.length; i++) prop.setValueAtTime(keys[i][0], keys[i][1]);
    if (ease === "linear") return;
    var dims = prop.value instanceof Array ? prop.value.length : 1;
    if (prop.isSpatial) dims = 1;
    for (var k = 1; k <= prop.numKeys; k++) {
        var inInf = 85, outInf = ease === "out" ? 15 : 60;
        var e = [], o = [];
        for (var d = 0; d < dims; d++) { e.push(new KeyframeEase(0, inInf)); o.push(new KeyframeEase(0, outInf)); }
        prop.setTemporalEaseAtKey(k, e, o);
    }
}
function scaleIn(layer, t0, t1, overshoot) {
    anim(tf(layer, "Scale"), [[t0, [0, 0]], [t0 + (t1 - t0) * 0.65, [overshoot, overshoot]], [t1, [100, 100]]], "inout");
}
function fade(layer, t0, t1) { anim(tf(layer, "Opacity"), [[t0, 0], [t1, 100]], "inout"); }
// layers sit at the comp centre; a pivot at an SVG point (200 units -> 800 px)
function pivot(layer, x, y) {
    tf(layer, "Anchor Point").setValue([x * 4, y * 4]);
    tf(layer, "Position").setValue([x * 4, y * 4]);
}

// Bottom to top: comp.layers.add puts each new layer on top.
var ring = img("ring"), disc = img("disc");
var stars = [];
for (var i = 0; i < 9; i++) stars.push(img("star" + i));
var arc = img("arc"), earth = img("earth"), rocket = img("rocket"), flame = img("flame");
var frame = img("frame"), accent = img("accent");
var rules = img("rules"), wordmark = img("wordmark"), tagline = img("tagline");

// The patch: everything hangs off one null that pops in.
var patch = comp.layers.addNull();
patch.name = "Patch";
// Anchor and position both at the centre: at rest its layer space is comp space, and it scales about the centre.
tf(patch, "Anchor Point").setValue([C, C]);
tf(patch, "Position").setValue([C, C]);
var all = [ring, disc, arc, rocket, flame, earth, frame, accent, rules, wordmark, tagline].concat(stars);
for (i = 0; i < all.length; i++) all[i].parent = patch;
anim(tf(patch, "Scale"), [[0, [0, 0]], [0.35, [108, 108]], [0.55, [100, 100]]], "inout");
anim(tf(patch, "Rotation"), [[0, -40], [0.55, 0]], "out");

// The field fills behind the ring a beat later.
pivot(disc, 100, 100);
scaleIn(disc, 0.1, 0.5, 104);
// A copy of the disc clips what moves in from outside the field (the Earth, the rocket).
var field = disc.duplicate();
field.name = "Field matte";

// Stars twinkle on, scattered in time; each from its own centre.
var starAt = [[56, 55], [137, 44], [72, 94], [127, 78], [142, 108], [35, 75], [165, 62], [48, 112], [160, 105]];
var starT = [0.45, 0.62, 0.52, 0.75, 0.58, 0.68, 0.49, 0.8, 0.71];
for (i = 0; i < 9; i++) {
    pivot(stars[i], starAt[i][0], starAt[i][1]);
    scaleIn(stars[i], starT[i], starT[i] + 0.3, 170);
    anim(tf(stars[i], "Rotation"), [[starT[i], -90], [starT[i] + 0.3, 0]], "out");
}

// Earth rises into the bottom of the field.
anim(tf(earth, "Position"), [[0.4, [C, C + 160]], [1.1, [C, C]]], "out");
fade(earth, 0.4, 0.7);
earth.setTrackMatte(field, TrackMatteType.ALPHA);

// The rocket climbs up over the Earth from below the field.
var RISE = 600;
anim(tf(rocket, "Position"), [[0.9, [C, C + RISE]], [2.0, [C, C]]], "out");
pivot(flame, 100, 122);
anim(tf(flame, "Position"), [[0.9, [400, 488 + RISE]], [2.0, [400, 488]]], "out");
rocket.setTrackMatte(field, TrackMatteType.ALPHA);
flame.setTrackMatte(field, TrackMatteType.ALPHA);
// The flame flickers for the whole clip, longer while the rocket is still climbing.
tf(flame, "Scale").expression =
    "var climb = time < 2.0 ? 1.0 + 0.6 * (1 - time / 2.0) : 1.0;" +
    "var f = climb * (100 + 14 * Math.sin(time * 41) + 8 * Math.sin(time * 67 + 1.3));" +
    "[100 + 4 * Math.sin(time * 53), f]";

// The window's arc draws itself over the nose (its x only grows, so a wipe draws it on).
var wipe = arc.property("Effects").addProperty("Linear Wipe");
wipe.property("Wipe Angle").setValue(270);
wipe.property("Feather").setValue(24);
anim(wipe.property("Transition Completion"), [[1.6, 100], [2.35, 0]], "inout");

// The accent star flares where the arc ends.
pivot(accent, 156, 84);
scaleIn(accent, 2.3, 2.7, 190);
anim(tf(accent, "Rotation"), [[2.3, -180], [2.7, 0]], "out");
var glow = accent.property("Effects").addProperty("Glow");

// The ribbon: rules stretch out, the wordmark rises, the tagline follows.
pivot(rules, 100, 154);
anim(tf(rules, "Scale"), [[2.4, [0, 100]], [2.85, [100, 100]]], "out");
fade(rules, 2.4, 2.6);
anim(tf(wordmark, "Position"), [[2.55, [C, C + 24]], [3.0, [C, C]]], "out");
fade(wordmark, 2.55, 2.9);
anim(tf(tagline, "Position"), [[2.85, [C, C + 16]], [3.3, [C, C]]], "out");
fade(tagline, 2.85, 3.2);

writeLn("layers: " + comp.numLayers + ", glow params: " + glow.numProperties);
