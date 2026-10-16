// Lunokhod deploy (2026-10-16): Luna 17's first surface operations. On the Moon, the descent stage lowers
// its two ramps (by 1.2), and the rover rolls off the deck, down the front ramp and onto the surface (on
// it by 3.4), drives clear and stops, then opens its lid to the Sun (4.6 to 5.6: solar cells on its
// underside) and calls home. Lunokhod 1 rolled down Luna 17's ramps on 17 November 1970.
// Glitch: halfway down the ramp its wheels slip (2.6): it slides back a little, wheels spinning, and waits
// out the drivers' delay on Earth; at 3.9 it tries again and makes it down.
// Failure: at the ramp's foot its front wheels dig into the soft soil (3.0) and it pitches over onto its
// nose in a cloud of dust, and lies there.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";

lunokhodScene(false);
lookAt(function () { return 1130; });

turn("u-ramp-f", track([[0.2, -90], [1.2, 30, "inout"]]));
turn("u-ramp-r", track([[0.2, 90], [1.2, -30, "inout"]]));

var x, extra = null, pivot = null, spin = null;
if (failure) {
    x = track([[1.4, 960], [3.0, 1215, "in"]]);
    extra = track([[3.0, 0], [3.5, 62, "in"], [3.7, 54, "out"], [3.9, 58, "inout"]]);
    pivot = [HALF, 0];
} else if (glitch) {
    x = track([[1.4, 960], [2.6, 1110, "in"], [3.6, 1096, "out"], [3.9, 1096], [5.4, 1420, "inout"]]);
    spin = function (t) { return 720 * clamp(t - 2.6, 0, 1.0); };
} else {
    x = track([[1.4, 960], [4.4, 1420, "inout"]]);
}
var at = drive(x, extra, pivot, spin);

// The dust: at the front wheels as they slip (glitch) or dig in (failure).
if (glitch || failure) {
    var from = glitch ? 2.6 : 3.0, to = glitch ? 3.6 : 3.9;
    move("u-kick", function (t) { var p = onRover(at, Math.min(Math.max(t, from), to), [HALF, 0]); return [p[0] - 870, p[1] - 874]; });
    fade("u-kick", function (t) { return t < from ? 0 : t < to ? 1 : clamp(1 - (t - to) / 0.6, 0, 1); });
    grow("u-kick", function (t) { return failure ? 1.2 + 0.8 * clamp(t - from, 0, 1) : 0.8 + 0.2 * Math.sin(2 * Math.PI * 4 * t); });
}

// The lid opens to the Sun once it's down, and it calls home.
if (!failure) {
    lid(glitch ? track([[5.5, 0], [6.3, -115, "inout"]]) : track([[4.6, 0], [5.6, -115, "inout"]]));
    sending(at, glitch ? [6.0] : [5.4, 6.0]);
} else {
    sending(at, []);
}
