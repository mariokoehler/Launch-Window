// De-orbit burn (2026-10-16): the retrofire for a flight with no reentry preparations before it, so
// nothing has turned the capsule yet. It starts nose first, as it went round the Earth orbit's map,
// the motes streaming past; the attitude jets turn it base first, then the retrofire runs as its
// own clip, 1.2 s later (retrofire.jsx, which build.py appends, reads TURN).
var TURN = true;
