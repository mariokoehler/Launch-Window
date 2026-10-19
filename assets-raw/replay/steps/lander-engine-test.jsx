// A lander's engine test in Earth orbit: the descent engine lights at 0.8 and burns steadily, pushing the
// lander up (away from its plume), then shuts down at 3.4.
// Glitch: it sputters from 0.8, the alarm lamp blinking, and only burns steadily from 1.6.
// Failure: the same sputter, and at 1.6 it goes out for good, the alarm lamp lit.
// A step's file can change it (2026-10-19, the crewed LK test; lk-undock-test.jsx, lk-check-burn.jsx): the times
// (LIGHT, STEADY, OFF); FROM, where an earlier burn left it (as lander-staging.jsx's); UNDOCK, the LOK docked on top
// at first, backing off and away from RELEASE before the burn; BOTH, the main engine and the backup lighting
// together, the backup's two nozzles shutting down BACKUP_OFF after it burns steadily (on a glitch the backup
// carries the burn while the main engine sputters; on a failure both go out).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var LIGHT = typeof LIGHT !== "undefined" ? LIGHT : 0.8;
var STEADY = typeof STEADY !== "undefined" ? STEADY : 1.6;
var OFF = typeof OFF !== "undefined" ? OFF : 3.4;
var FROM = typeof FROM !== "undefined" ? FROM : [0, 0, 0];
var UNDOCK = typeof UNDOCK !== "undefined" && UNDOCK;
var RELEASE = UNDOCK ? 0.4 : 0;
var BOTH = typeof BOTH !== "undefined" && BOTH;
var BACKUP_OFF = 0.5;

landerOrbit("alarm");
LO = [LO[0], LO[1] + FROM[0]];
turn("d-earth", function (t) { return FROM[2] - 0.25 * t; });
var rough = glitch || failure;
var main = function (t) {
    if (t < LIGHT || t >= OFF) return 0;
    if (rough && t < STEADY) return Math.floor(t * 9) % 3 === 0 ? 0.5 : 0;
    if (failure) return 0;
    return 1;
};
// The backup's two nozzles: from the light until the main engine has burnt steadily a moment.
var backupOff = (rough ? STEADY : LIGHT) + BACKUP_OFF;
var backup = function (t) { return BOTH && t >= LIGHT && t < backupOff && !(failure && t >= STEADY) ? 1 : 0; };
var burning = function (t) { return Math.max(main(t), backup(t)); };
// The burn pushes it up: speed builds while it burns, and it coasts on after.
var ys = [], v = 0, y = 0;
for (var f = 0; f <= NFRAMES + 1; f++) { ys.push(y); v += 22 * burning(f / FPS) / FPS; y -= v / FPS; }
move("n-carrier", function (t) { return [LO[0], LO[1] + ys[clamp(Math.round(t * FPS), 0, ys.length - 1)]]; });
turn("n-carrier", function (t) { return FROM[1] + 4 * Math.sin(2 * Math.PI * 0.15 * Math.max(0, t - RELEASE)); });
var flicker = function (t, k) { k = Math.max(k, 0.01); return [k, k * (0.9 + 0.12 * Math.sin(2 * Math.PI * 11 * t))]; };
if (BOTH) {
    // n-plume (behind the descent stage) is all three plumes, n-flame-up the main one's alone.
    fade("n-plume", function (t) { return backup(t) > 0 ? 1 : 0; });
    grow("n-plume", function (t) { return flicker(t, 1); });
    fade("n-flame-up", function (t) { return !backup(t) && main(t) > 0 ? 1 : 0; });
    grow("n-flame-up", function (t) { return flicker(t, main(t)); });
} else {
    fade("n-plume", function (t) { return burning(t) > 0 ? 1 : 0; });
    grow("n-plume", function (t) { return flicker(t, burning(t)); });
}
fade("n-lamp", function (t) {
    if (!rough || t < LIGHT) return 0;
    if (t < STEADY) return Math.floor(t * 5) % 2 === 0 ? 1 : 0.15;
    return failure ? 1 : 0;
});
if (UNDOCK) {
    // The LOK, its probe in the LK's plate, lets go at RELEASE: it backs off slowly, then its thrusters take it
    // up and away to the left, clear of the LK's climb.
    put("n-lok");
    move("n-lok", function (t) {
        var u = Math.max(0, t - RELEASE), w = Math.max(0, t - 1.2);
        return [-(w < 1.6 ? 45 * w * w : 115.2 + 144 * (w - 1.6)), FROM[0] - 18 * u];
    });
}
