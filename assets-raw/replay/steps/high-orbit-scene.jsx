// A long elliptical Earth orbit (2026-10-18; circumlunar Gemini's heat-shield test): the burn that stretches the
// orbit (high-orbit-injection.jsx) and the coast out along it (high-orbit.jsx). The Earth orbit scene with the
// camera pulled back (0.6) so the whole ellipse fits, its high point straight up; the planned ellipse dashed
// (o-ellipse, drawn with its low point at the bottom, turned to where the burn ends). The craft goes round
// anticlockwise, as everywhere (angles falling).
var HO_RP = 390, HO_RA = 1100, HO_END = 2.6;

// zoom: pull the camera back over the first second, from the Earth orbit's framing (the burn, which cuts on from
// it); otherwise back from the start.
function hoScene(zoom) {
    orbitScene();
    put("o-ellipse");
    layers["o-ellipse"].moveAfter(layers["o-orbit"]);
    hide("ring-1"); hide("ring-2"); hide("ring-3"); hide("ring-4");
    turn("o-earth", function (t) { return -0.25 * t; });
    // The camera back: the ellipse's middle (the Earth at 960 560, its high point 1100 above) on the screen's.
    var w = zoom ? function (t) { return EASE.inout(clamp(t / 1.0, 0, 1)); } : function () { return 1; };
    bake(tf(world, "Scale"), function (t) { var k = mix(1, 0.6, w(t)); return [100 * k, 100 * k]; });
    bake(tf(world, "Position"), function (t) { return [mix(0, 960 - 0.6 * 960, w(t)), mix(0, 540 - 0.6 * 205, w(t))]; });
}

// An ellipse with the Earth at its focus, low point rp and high point ra (px): where the craft is tau seconds
// after the low point, as [radius, angle past the low point in degrees], at the pace the circular orbit
// (55 degrees a second at 390) sets.
function kepler(rp, ra) {
    var a = (rp + ra) / 2, e = (ra - rp) / (ra + rp);
    var period = 360 / 55 * Math.pow(a / HO_RP, 1.5);
    return function (tau) {
        var m = 2 * Math.PI * tau / period, big = m;
        for (var i = 0; i < 12; i++) big = big - (big - e * Math.sin(big) - m) / (1 - e * Math.cos(big));
        var nu = 2 * Math.atan(Math.sqrt((1 + e) / (1 - e)) * Math.tan(big / 2));
        return [a * (1 - e * Math.cos(big)), nu / RAD];
    };
}
// The point tau seconds past the low point at angle peri (degrees) on that ellipse.
function onEllipse(k, peri, tau) {
    var s = k(tau), p = (peri - s[1]) * RAD;
    return [OCX + s[0] * Math.cos(p), OCY + s[0] * Math.sin(p)];
}
// Moves the craft along where(t), nose along its way (from the way it moves).
function flyAlong(where) {
    move("o-sat", function (t) { var p = where(t); return [p[0] - OCX, p[1] - OCY]; });
    turn("o-sat", function (t) {
        var a = where(Math.max(0, t - 0.02)), b = where(t + 0.02);
        return Math.atan2(b[1] - a[1], b[0] - a[0]) / RAD + 90;
    });
}
