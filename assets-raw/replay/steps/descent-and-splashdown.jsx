// Descent and splashdown: base down under the clouds (reentry's last frame), the capsule falls
// fast; the drogue opens at 0.8 and slows it, the main at 1.6, and it drifts down to the sea, which
// comes up from below; splashdown at 4.4, a splash and rings, and it floats under its chute.
// Glitch: the main comes out at 1.6 but streams, then fills halfway at 2.0 and the capsule swings
// under it, until it fills at 3.0; a later, harder splash (4.7).
// Failure: the same streamer at 1.6, but it never fills; the capsule keeps falling fast, hits the
// sea at 3.6 in a big splash and goes under.
// The body is descent.jsx, shared with the descent and landing.
var GROUND = false;
