// The steps on the pad (launch preparations, countdown): the launch's own scene at rest, with the
// propellant, the leak, the fire, the clock and the explosion on top. Before this: ../motion.jsx,
// setup(...), ../launch-storyboard/lib.jsx.

// The launch's scene, its trail hidden (the launch wipes it away by its first frame), and ours.
function padScene() {
    stack("trail-Success");
    hide("trail-Success");
    // Propellant runs behind the rocket, in front of the tower; its tint and level in front.
    for (var i = 1; i <= 24; i++) { put("dot-" + i).moveAfter(layers.rocket); hide("dot-" + i); }
    var over = ["fill-lower", "fill-upper", "level-lower", "level-upper"];
    for (i = 0; i < over.length; i++) { put(over[i]).moveBefore(layers.rocket); hide(over[i]); }
    var extra = ["leak-jet", "leak-puff", "fire-1", "fire-2", "clock"];
    for (i = 0; i < extra.length; i++) { put(extra[i]); hide(extra[i]); }
    for (i = 0; i <= 9; i++) { put("digit-" + i); hide("digit-" + i); }
    // Nothing moves on the pad unless a step says so: the wisps rest hidden too.
    for (i = 1; i <= 3; i++) hide("wisp-" + i);
}

// A tank's propellant: level(t) in 0..1 raises the tint (a wipe hides its top) and the gold line;
// shown(t) fades both.
function tank(name, level, shown) {
    var d = L["fill-" + name], l = layers["fill-" + name];
    var wipe = l.property("Effects").addProperty("Linear Wipe");
    wipe.property("Wipe Angle").setValue(180);
    bake(wipe.property("Transition Completion"), function (t) { return 100 * (1 - level(t)); });
    fade("fill-" + name, shown);
    move("level-" + name, function (t) { return [0, -(d.h) * level(t)]; });
    fade("level-" + name, function (t) { return level(t) > 0 ? shown(t) : 0; });
}

// The propellant's way, up the tower and along an arm: dots 1-9 to the lower tank, 10-24 to the upper.
// pos(t) is how far the flow has run (px), shown(t) its opacity.
function flow(pos, shown) {
    var lines = [{ from: 1, to: 9, pts: [[1081, 880], [1081, 700], [1011, 700]] },
                 { from: 10, to: 24, pts: [[1081, 880], [1081, 520], [1011, 520]] }];
    for (var k = 0; k < lines.length; k++) {
        (function (line) {
            var len = 0, seg = [];
            for (var i = 1; i < line.pts.length; i++) {
                var a = line.pts[i - 1], b = line.pts[i], n = Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1]);
                seg.push([a, b, n]); len += n;
            }
            var count = line.to - line.from + 1, gap = len / count;
            for (var j = 0; j < count; j++) {
                (function (name, off) {
                    var at = function (t) { var s = (pos(t) + off) % len; return s < 0 ? s + len : s; };
                    move(name, function (t) {
                        var s = at(t);
                        for (var q = 0; q < seg.length; q++) {
                            if (s <= seg[q][2] || q === seg.length - 1) {
                                var u = s / seg[q][2];
                                return [seg[q][0][0] + (seg[q][1][0] - seg[q][0][0]) * u - 1081,
                                        seg[q][0][1] + (seg[q][1][1] - seg[q][0][1]) * u - 880];
                            }
                            s -= seg[q][2];
                        }
                    });
                    // Each dot fades in at the tower's foot and out at the tank.
                    fade(name, function (t) { var s = at(t); return shown(t) * clamp(s / 20, 0, 1) * clamp((len - s) / 20, 0, 1); });
                })("dot-" + (line.from + j), j * gap);
            }
        })(lines[k]);
    }
}

// The clock: shown(t) fades it, digit(t) the number it shows (-1: none), blink(t) true while it's dark.
function clock(shown, digit, blink) {
    fade("clock", shown);
    for (var n = 0; n <= 9; n++) {
        (function (k) {
            fade("digit-" + k, function (t) { return digit(t) === k && !(blink && blink(t)) ? shown(t) : 0; });
        })(n);
    }
}

// The base fire, two shapes alternating every 0.08 s; size(t) scales it, 0 hides it.
function fire(size) {
    states(["fire-1", "fire-2"], function (t) {
        if (size(t) <= 0) return "";
        return Math.floor(t / 0.08) % 2 === 0 ? "fire-1" : "fire-2";
    });
    grow("fire-1", function (t) { var s = Math.max(size(t), 0.01); return [s, s * (1 + 0.05 * Math.sin(2 * Math.PI * 9 * t))]; });
    grow("fire-2", function (t) { var s = Math.max(size(t), 0.01); return [s, s * (1 + 0.05 * Math.sin(2 * Math.PI * 7 * t))]; });
}

// The rocket blows up at tb, from (ox, oy): the burst, the pieces thrown and landing, smoke rising.
function explode(tb, ox, oy) {
    var G = 1400, GROUND = 872;
    fade("rocket", function (t) { return t < tb ? 1 : 0; });
    var pieces = { "debris-1": [-260, -760, -220], "debris-2": [330, -560, 260], "debris-3": [-170, -340, 140],
                   "debris-4": [140, -160, -70], "debris-5": [-420, -620, 400], "debris-6": [400, -660, -380] };
    for (var name in pieces) {
        (function (n, v) {
            var py = L[n].py, half = Math.min(L[n].w, L[n].h) / 2;
            // When it lands: its pivot comes down to the ground less half its size.
            var floor = GROUND - half - py;
            var tl = (-v[1] + Math.sqrt(v[1] * v[1] + 2 * G * floor)) / G;
            var at = function (t) { return clamp(t - tb, 0, tl); };
            move(n, function (t) { var u = at(t); return [v[0] * u, v[1] * u + 0.5 * G * u * u]; });
            turn(n, function (t) { return v[2] * at(t); });
            fade(n, function (t) { return t < tb ? 0 : 1; });
        })(name, pieces[name]);
    }
    move("burst", function () { return [ox - 960, oy - 600]; });
    grow("burst", track([[tb, 0.3], [tb + 0.35, 2.4, "out"], [tb + 1.0, 2.8, "lin"]]));
    fade("burst", track([[tb, 0, "snap"], [tb + 0.02, 1, "snap"], [tb + 0.5, 1], [tb + 1.0, 0, "lin"]]));
    // Smoke: the ground puffs roll out, the rest climb from the blast.
    groundSmoke({
        "smoke-1": [[tb, 0, 0.6, 0, "snap"], [tb + 0.02, 0, 0.6, 1, "snap"], [tb + 0.6, 120, 1.6, 1, "out"], [tb + 2.5, 200, 2.0, 0.5, "out"]],
        "smoke-2": [[tb, 0, 0.6, 0, "snap"], [tb + 0.02, 0, 0.6, 1, "snap"], [tb + 0.6, 120, 1.6, 1, "out"], [tb + 2.5, 200, 2.0, 0.5, "out"]],
        "smoke-3": [[tb + 0.1, 20, 0.5, 0, "lin"], [tb + 0.7, 160, 1.5, 1, "out"], [tb + 2.5, 260, 1.9, 0.5, "out"]],
        "smoke-4": [[tb + 0.1, 20, 0.5, 0, "lin"], [tb + 0.7, 160, 1.5, 1, "out"], [tb + 2.5, 260, 1.9, 0.5, "out"]]
    });
    var climb = { "smoke-8": [0, -260, 2.6], "smoke-9": [-90, -200, 2.0], "smoke-10": [80, -170, 2.2], "smoke-11": [-40, -320, 2.4] };
    for (name in climb) {
        (function (n, c) {
            var from = [ox - L[n].px, oy - L[n].py];
            var u = track([[tb, 0], [tb + 2.4, 1, "out"]]);
            move(n, function (t) { var k = u(t); return [from[0] + c[0] * k, from[1] + c[1] * k]; });
            grow(n, function (t) { return 0.5 + (c[2] - 0.5) * u(t); });
            fade(n, track([[tb + 0.05, 0, "snap"], [tb + 0.07, 1, "snap"], [tb + 1.6, 1], [tb + 3.0, 0.45, "lin"]]));
        })(name, climb[name]);
    }
}
