// Explorer 1's deployment (2026-10-20, user): its four whip antennas rode to orbit folded down along it, and its spin
// flung them out once it was free. After deployment.jsx: from 0.3 s after the push they swing out over 1.2 s, one
// drawing of them at a time (parts.py's ex1-d-whips-0 to -12), in every variant.
var WHIPS = 12, OUT = FREE + 0.3, OPEN = 1.2;
for (var w = 0; w <= WHIPS; w++) {
    put("d-whips-" + w, "d-payload");
    (function (n) {
        fade("d-whips-" + n, function (t) {
            return Math.round(WHIPS * clamp((t - OUT) / OPEN, 0, 1)) === n ? 1 : 0;
        });
    })(w);
}
