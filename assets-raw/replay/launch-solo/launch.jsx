// Launch, success: the rocket vents on the pad, lights, clears the tower and climbs out of frame
// while the camera follows. DIR and L (the layers, from make_launch.py) come before this.
var W = 1920, H = 1080, FPS = 30, DUR = 6.2;
var IGNITE = 1.0, LIFTOFF = 1.7, ACCEL = 150; // px/s² of climb
// The camera lets the rocket rise HOLD px up the frame, keeps it there, then from LET_GO brakes to a
// stop over BRAKE s, so the rocket pulls away out of the top.
var HOLD = 250, LET_GO = 4.8, BRAKE = 0.8;
var comp = app.project.items.addComp("Launch", W, H, 1, DUR, FPS);
comp.bgColor = [1, 1, 1];

function tf(layer, name) { return layer.property("Transform").property(name); }
function add(name, as) {
    var item = app.project.importFile(new ImportOptions(new File(DIR + name + ".png")));
    var layer = comp.layers.add(item);
    layer.name = as || name;
    var d = L[name];
    tf(layer, "Anchor Point").setValue([d.px - d.x, d.py - d.y]);
    tf(layer, "Position").setValue([d.px, d.py]);
    layer.parent = world;
    return layer;
}
function ease(prop, keys) {
    for (var i = 0; i < keys.length; i++) prop.setValueAtTime(keys[i][0], keys[i][1]);
    var dims = prop.isSpatial ? 1 : (prop.value instanceof Array ? prop.value.length : 1);
    for (var k = 1; k <= prop.numKeys; k++) {
        var e = [];
        for (var j = 0; j < dims; j++) e.push(new KeyframeEase(0, 70));
        prop.setTemporalEaseAtKey(k, e, e);
    }
}
// The climb, in px above the pad, as an expression snippet: constant acceleration from liftoff.
var CLIMB = "var c = time > " + LIFTOFF + " ? " + ACCEL + " * Math.pow(time - " + LIFTOFF + ", 2) : 0;";

var paper = app.project.importFile(new ImportOptions(new File(DIR + "paper.png")));
comp.layers.add(paper).name = "paper";

// Everything in the world hangs off one null; the camera moves it down to follow the rocket, and
// it shakes from ignition until the rocket is well clear.
var world = comp.layers.addNull();
world.name = "World";
tf(world, "Anchor Point").setValue([0, 0]);
tf(world, "Position").setValue([0, 0]);
tf(world, "Position").expression =
    "var t = time;" +
    "function c(x) { return x > " + LIFTOFF + " ? " + ACCEL + " * (x - " + LIFTOFF + ") * (x - " + LIFTOFF + ") : 0; }" +
    "function sm(a, b, x) { var u = Math.min(1, Math.max(0, (x - a) / (b - a))); return u * u * (3 - 2 * u); }" +
    "var f;" +
    "if (t < " + LET_GO + ") { f = Math.max(0, c(t) - " + HOLD + " * sm(" + LIFTOFF + ", 3.4, t)); }" +
    "else { var v = " + 2 * ACCEL + " * (" + LET_GO + " - " + LIFTOFF + "); var u = Math.min(t - " + LET_GO + ", " + BRAKE + ");" +
    "  f = c(" + LET_GO + ") - " + HOLD + " + v * (u - u * u / (2 * " + BRAKE + ")); }" +
    "var s = t > " + IGNITE + " && t < 3.4 ? 3.5 * Math.min(1, (t - " + IGNITE + ") / 0.4) * Math.min(1, (3.4 - t) / 0.8) : 0;" +
    "[Math.sin(t * 61) * s, f + Math.cos(t * 47) * s]";

// Bottom to top.
add("cloud1"); add("cloud2"); add("cloud3"); add("cloud4");
add("ruler");
add("tower");
var armUpper = add("armUpper"), armLower = add("armLower");
var trail = add("trail");
var flame = add("flame");
var rocket = add("rocket");
add("ground");
add("pad");

// Propellant boils off before ignition: wisps rise beside the rocket and stop once it lights.
var wispAt = [[1004, 600], [916, 596], [1000, 470]];
for (var i = 0; i < wispAt.length; i++) {
    var w = add("wisp", "wisp" + i);
    tf(w, "Position").setValue(wispAt[i]);
    var off = (i * 0.41).toFixed(2);
    tf(w, "Position").expression = "var ph = ((time + " + off + ") % 1.2) / 1.2; value - [0, 34 * ph]";
    tf(w, "Opacity").expression =
        "var ph = ((time + " + off + ") % 1.2) / 1.2; var a = Math.sin(ph * Math.PI) * 100;" +
        "time > " + IGNITE + " ? a * Math.max(0, 1 - (time - " + IGNITE + ") / 0.3) : a";
}

// The rocket and its fire climb together.
tf(rocket, "Position").expression = CLIMB + "value - [0, c]";
tf(flame, "Position").expression = CLIMB + "value - [0, c]";
// The fire lights in the trench, then flickers; it lengthens as the rocket speeds up.
tf(flame, "Scale").expression =
    "var t = time; var g = t < " + IGNITE + " ? 0 : Math.min(1, (t - " + IGNITE + ") / 0.35);" +
    "var grow = t > " + LIFTOFF + " ? Math.min(1.7, 1 + (t - " + LIFTOFF + ") * 0.3) : 1;" +
    "[100 * g * (1 + 0.05 * Math.sin(t * 53)), 100 * g * grow * (1 + 0.09 * Math.sin(t * 43) + 0.05 * Math.sin(t * 71 + 1))]";

// The trail: revealed from the pad up to the rocket's engine.
var wipe = trail.property("Effects").addProperty("Linear Wipe");
wipe.property("Wipe Angle").setValue(180); // hides from the top down
wipe.property("Transition Completion").expression =
    CLIMB + "Math.max(0, Math.min(100, 100 * (1 - c / " + (L.trail.h - 4) + ")))";

// The arms swing clear just before liftoff.
ease(tf(armUpper, "Rotation"), [[IGNITE + 0.35, 0], [IGNITE + 0.9, 58]]);
ease(tf(armLower, "Rotation"), [[IGNITE + 0.45, 0], [IGNITE + 1.0, 58]]);

// Smoke billows out of the trench along the ground, both ways, and lingers.
var sizes = [42, 58, 76];
for (i = 0; i < 16; i++) {
    var side = i % 2 === 0 ? -1 : 1;
    var t0 = IGNITE + 0.05 + i * 0.06;
    var p = add("puff" + sizes[i % 3], "smoke" + i);
    var reach = 120 + i * 30;
    var lift = (i % 4) * 22;
    ease(tf(p, "Position"), [[t0, [960 + side * 20, 872]], [t0 + 1.8, [960 + side * reach, 868 - lift]]]);
    tf(p, "Scale").setValueAtTime(t0, [0, 0]);
    tf(p, "Scale").setValueAtTime(t0 + 0.9, [100, 100]);
    tf(p, "Scale").setValueAtTime(t0 + 3.5, [125, 125]);
    tf(p, "Opacity").setValueAtTime(0, 0);
    tf(p, "Opacity").setValueAtTime(t0, 100);
}
writeLn("layers: " + comp.numLayers);
