// The carrier's entry (Venera 4 to 8: the carrier was never meant to survive). The lander gone, the
// carrier falls base first towards Venus, whose limb comes up, sending what its instruments measure (a
// radio ring every 0.75 s). It meets the air at 1.2: a bow of hot air builds under it, the air streams
// past and it shakes. At 3.3 its panels tear off and tumble away; at 3.9 the body bursts, the pieces
// stream back and fade, and only the limb is left.
// The burn-up is the plan, the same in every variant; what differs is the data it sends home.
// Glitch: the signal drops out at 1.6 and is back at 2.75, in time to send the rest.
// Failure: the signal drops out at 1.6 for good: it burns up with its measurements unsent.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var AIR = 1.2, QUIET = 1.6, BACK = 2.75, PANELS = 3.3, BREAK = 3.9;
// Where the carrier hangs on screen: its parts are drawn about (960, 380).
var BX = 0, BY = 60;

put("paper", "screen");
put("b-limb");
for (var i = 1; i <= 8; i++) put("r-streak-" + i);
put("b-glow");
put("b-panel-l");
put("b-panel-r");
put("b-body");
put("b-hot");
for (i = 1; i <= 4; i++) put("ring-" + i);
for (i = 1; i <= 4; i++) put("r-chip-" + i);
put("burst");

// How hot: from the air at 1.2, full by 2.4, until the body goes.
var heat = track([[AIR, 0], [2.4, 1], [BREAK, 1], [BREAK + 0.6, 0]]);
// The shake grows with the heat; it stops when there's nothing left to shake.
function shake(t, seed) { return t >= BREAK ? 0 : 5 * heat(t) * Math.sin(2 * Math.PI * 11 * t + seed); }

// The limb comes up from below, slowing as the carrier slows, and settles just under it: the carrier
// burns up in the air above the clouds (the two dashed shells), never reaching them.
move("b-limb", function (t) { return [0, -260 * (1 - Math.exp(-t / 1.2))]; });

// The body: it holds its place, shaking, until it bursts.
move("b-body", function (t) { return [BX + shake(t, 0), BY + shake(t, 1.3)]; });
fade("b-body", function (t) { return t < BREAK ? 1 : 0; });
move("b-hot", function (t) { return [BX + shake(t, 0), BY + shake(t, 1.3)]; });
fade("b-hot", function (t) { return t < BREAK ? clamp((heat(t) - 0.3) / 0.7, 0, 1) * (0.75 + 0.25 * Math.sin(2 * Math.PI * 7 * t)) : 0; });
// The panels tear off at 3.3 and tumble away up and out (the air pushes them back), fading as they burn.
var panels = {"b-panel-l": [-1, 260], "b-panel-r": [1, -300]};
for (var p in panels) {
    (function (n, v) {
        move(n, function (t) {
            var u = Math.max(0, t - PANELS);
            return [BX + shake(t, 0) + v[0] * 220 * u, BY + shake(t, 1.3) - 420 * u - 160 * u * u];
        });
        turn(n, function (t) { return v[1] * Math.max(0, t - PANELS); });
        fade(n, function (t) { return t < PANELS + 0.5 ? 1 : Math.max(0, 1 - (t - PANELS - 0.5) / 0.6); });
    })(p, panels[p]);
}
// The bow of hot air under it, flickering, as wide as the carrier, narrower once the panels are gone.
move("b-glow", function (t) { return [BX + shake(t, 0), BY + shake(t, 1.3)]; });
fade("b-glow", heat);
grow("b-glow", function (t) {
    var k = 0.7 + 0.3 * heat(t), narrow = t < PANELS ? 1 : Math.max(0.45, 1 - 1.2 * (t - PANELS));
    return [k * narrow * (1 + 0.05 * Math.sin(2 * Math.PI * 9 * t)), k];
});

// The burst, and the small pieces streaming back up, tumbling, fading as they burn.
move("burst", function () { return [BX, BY + 380 - 600]; });
grow("burst", track([[BREAK, 0.3], [BREAK + 0.35, 2.2, "out"], [BREAK + 1.0, 2.6, "lin"]]));
fade("burst", track([[BREAK, 0, "snap"], [BREAK + 0.02, 1, "snap"], [BREAK + 0.5, 1], [BREAK + 1.1, 0, "lin"]]));
var flings = [[-260, -520, 300], [180, -620, -380], [-120, -760, 240], [300, -460, -260]];
for (i = 1; i <= 4; i++) {
    (function (n, v) {
        fade(n, function (t) { return t >= BREAK && t < BREAK + 1.6 ? 1 - Math.max(0, t - BREAK - 0.9) / 0.7 : 0; });
        move(n, function (t) { var u = Math.max(0, t - BREAK); return [BX - 2 + v[0] * u, BY - 86 + v[1] * u]; });
        turn(n, function (t) { return v[2] * Math.max(0, t - BREAK); });
    })("r-chip-" + i, flings[i - 1]);
}

// The air streaming up past it, each streak looping every 0.5 s in its own lane across the flow.
for (i = 1; i <= 8; i++) {
    (function (n, k) {
        var lane = (k % 2 ? 1 : -1) * (30 + 40 * k), phase = k * 0.13;
        move(n, function (t) { var u = ((t + phase) % 0.5) / 0.5; return [lane, BY + 120 - 520 * u]; });
        turn(n, function () { return -90; });
        fade(n, function (t) { var u = ((t + phase) % 0.5) / 0.5; return 0.9 * heat(t) * Math.sin(Math.PI * u); });
    })("r-streak-" + i, i);
}

// What it sends home: a ring every 0.75 s from 0.5 while it's alive; the glitch's signal is gone from
// 1.6 until 2.75, the failure's from 1.6 on.
var sends = sendTimes(function (t) {
    if (t >= BREAK) return true;
    if (failure) return t >= QUIET;
    if (glitch) return t >= QUIET && t < BACK;
    return false;
});
radioRingsAt(sends, function () { return [960 + BX, 380 + BY]; });

camera(function () { return 0; }, function (t) { return 3 * heat(t) * (t >= BREAK ? 0 : 1) + (t >= BREAK && t < BREAK + 0.4 ? 8 : 0); });
