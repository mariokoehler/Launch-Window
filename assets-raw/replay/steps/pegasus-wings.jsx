// Pegasus's deployment (2026-10-21, user): after deployment.jsx, whose payload here is the boilerplate CSM and whose
// stage keeps Pegasus (it stayed on the S-IV). Once the boilerplate is clear, 0.9 s after it's free, a motor unfolds
// the two wings over 2.2 s, one drawing of each at a time (parts.py's pg-d-wing-l- and -r-0 to -12). The glitch's left
// wing stalls half open for 0.8 s; the failure's never open (the stage runs into the boilerplate). The radio rings
// come from Pegasus, not the boilerplate, once its wings are out.
var WING = 12, OPEN = FREE + 0.9, SPAN = 2.2, STALL = 0.8;
function unfolded(side) {
    return function (t) {
        var u = t - OPEN;
        if (glitch && side === "l" && u > SPAN / 2) u = Math.max(SPAN / 2, u - STALL);
        return clamp(u / SPAN, 0, 1);
    };
}
var sides = ["l", "r"];
for (var si = 0; si < sides.length; si++) {
    for (var w = 0; w <= WING; w++) {
        put("d-wing-" + sides[si] + "-" + w, "d-stage");
        (function (n, open) {
            fade("d-wing-" + sides[si] + "-" + n, function (t) {
                return !failure && t >= OPEN && Math.round(WING * open(t)) === n ? 1 : 0;
            });
        })(w, unfolded(sides[si]));
    }
}

// The rings, from Pegasus's middle on the stage (728 at rest; the rings rest at 960), once both wings are out.
var wingSends = [];
if (!failure) for (var ws = OPEN + SPAN + (glitch ? STALL : 0); ws < DUR; ws += 0.8) wingSends.push(ws);
for (i = 1; i <= 4; i++) {
    (function (n, k) {
        var mine = function (t) {
            var last = -1;
            for (var j = k; j < wingSends.length; j += 4) if (wingSends[j] <= t) last = wingSends[j];
            return last;
        };
        move(n, function (t) { var te = mine(t); return [728 - 960 + stage(te < 0 ? 0 : te), 0]; });
        grow(n, function (t) { var te = mine(t); return te < 0 ? 1.0 : 1.0 + 2.4 * clamp((t - te) / 1.0, 0, 1); });
        fade(n, function (t) { var te = mine(t); return te < 0 ? 0 : 1 - clamp((t - te) / 1.0, 0, 1); });
    })("ring-" + i, i - 1);
}
