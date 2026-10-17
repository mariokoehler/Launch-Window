// The X-15 at its peak (2026-10-17): above almost all the air, its engine out, it coasts over the top of its arc, nose up
// to nose down, the Earth's limb far below; its fins have nothing to bite on, so it steers with the jets in its nose (a
// puff at 1.2 and 3.8). No air streams past. (Joe Walker took it to 107.8 km on 22 August 1963.)
// Glitch: from 2.0 its nose wanders off; the jets fire again and again from 2.5 and bring it back by 3.2.
// Failure: the same wander and the same jets, and they can't hold it: at 3.0 it departs into a spin and falls away (as
// Mike Adams's X-15 did on 15 November 1967).
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var DRIFT = 2.0, FIX = 2.5, SPIN = 3.0;

x15Scene();
move("d-earth", function () { return [0, 150]; });
turn("d-earth", function (t) { return -0.15 * t; });
var arc = function (t) { var u = (t - 3) / 3; return 110 * u * u - 70; };
var wander = function (t) {
    if (!(glitch || failure) || t < DRIFT) return 0;
    var d = 16 * EASE.out(clamp((t - DRIFT) / 0.8, 0, 1));
    if (glitch) d *= 1 - EASE.inout(clamp((t - FIX) / 0.7, 0, 1));
    return d;
};
var pitch = function (t) {
    var p = -20 + 40 * t / DUR + wander(Math.min(t, failure ? SPIN : 99));
    if (failure && t > SPIN) { var u = t - SPIN; p += 220 * u + 180 * u * u; }
    return p;
};
var off = function (t) { var u = failure ? Math.max(0, t - SPIN) : 0; return [40 * u, arc(t) + 220 * u * u]; };
x15Fly(pitch, off);
ventral(pitch, off, 99, null);
puffs(K.rcsDown, glitch || failure ? [1.2, 2.5, 2.9] : [1.2, 3.8]);
puffs(K.rcsUp, glitch || failure ? [2.7, 3.1] : []);
