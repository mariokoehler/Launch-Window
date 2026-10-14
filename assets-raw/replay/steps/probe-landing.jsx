// A probe lander's landing on another world: it comes down under its parachute onto a rocky plain,
// touching down at 2.2 in a puff of dust; the chute falls away, the mast extends and it starts sending.
// Glitch: it comes down hard and tips onto its side; the mast still extends, sideways, and it sends, weakly
// (Venera 7 landed on its side, and its faint signal was found in the recordings afterwards).
// Failure: the same hard landing, and nothing: no mast, no signal.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DOWN = 2.2, RAISE = 2.8;

probeGround();
put("r-main");
probeCamera();
// The chute's lines meet at its pivot (960 322); on the sphere's top it's 0.6 of its size.
var CK = 0.6;
grow("r-main", function () { return CK; });

var hard = glitch || failure;
var y = function (t) { return t <= DOWN ? -520 * Math.pow((DOWN - t) / DOWN, 1.3) : 0; };
var tip = hard ? track([[DOWN, 0], [DOWN + 0.4, -78, "out"]]) : function () { return 0; };
// Tipped over, its centre settles to lie a sphere's radius off the ground.
var o = function (t) { return [0, y(t) + (hard ? 22 * clamp((t - DOWN) / 0.4, 0, 1) : 0)]; };
move("v-lander", o);
turn("v-lander", tip);
// The mast is stowed until it's down, then extends out of the sphere's top (never, lost).
var ext = function (t) { return failure ? 0 : EASE.out(clamp((t - RAISE) / 0.5, 0, 1)); };
grow("v-mast", function (t) { return [1, Math.max(0.01, ext(t))]; });
fade("v-mast", function (t) { return ext(t) > 0 ? 1 : 0; });
// The chute: on the sphere's top while it comes down, then off downwind, collapsing.
move("r-main", function (t) {
    var top = [VL[0], VL[1] + y(t) - 48], u = Math.max(0, t - DOWN);
    return [top[0] - 960 + 160 * u, top[1] - 322 + 50 * u * u];
});
turn("r-main", function (t) { return 40 * clamp((t - DOWN) / 1.0, 0, 1); });
fade("r-main", function (t) { return 1 - clamp((t - DOWN - 0.6) / 0.8, 0, 1); });
// Dust at the touchdown.
move("n-dust", function () { return [VL[0] - 960, 0]; });
fade("n-dust", function (t) { return t >= DOWN && t < DOWN + 1.0 ? 1 - (t - DOWN) : 0; });
grow("n-dust", function (t) { return (hard ? 1.0 : 0.7) + 0.4 * clamp((t - DOWN) / 0.6, 0, 1); });
// The signal from the mast's tip once it's up: weak (every other one) when tipped over, none when lost.
if (!failure) {
    var sends = [];
    for (var s = RAISE + 0.5; s < DUR; s += glitch ? 1.5 : 0.75) sends.push(s);
    radioRingsAt(sends, function (t) { return mastTip(o(t), tip(t), 0); });
} else {
    for (var j = 1; j <= 4; j++) hide("ring-" + j);
}
hide("v-arm");
