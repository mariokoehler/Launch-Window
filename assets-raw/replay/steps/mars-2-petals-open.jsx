// Mars 2's lander on the ground (2026-10-21): it comes down with its petals closed round the sphere and opens them
// after touchdown (Mars 3's did, and sent for a few seconds), where mars-landing.jsx's Viking stands on its legs.
var opened = track([[DOWN + 0.3, 0], [DOWN + 1.1, 1, "out"]]);
turn("m-leg-l", function (t) { return rot(t) + 90 * (1 - opened(t)); });
turn("m-leg-r", function (t) { return rot(t) - 90 * (1 - opened(t)); });
