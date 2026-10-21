// Mars 2's lander on its way down (2026-10-21): its petals (m-leg-l and -r, drawn open) stay closed round the sphere,
// where planet-descent.jsx swings Viking's legs out.
turn("m-leg-l", function (t) { return rot(t) + 90; });
turn("m-leg-r", function (t) { return rot(t) - 90; });
