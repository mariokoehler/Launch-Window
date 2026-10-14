// The lander's tests in Earth orbit (after moon.jsx): over the Earth's limb, the lander (its carrier at
// 0.8, centred on the screen) drifts, turning a little, with the lamp panel at the top right.
var LK = 0.8, LO = [0, -290];

function landerOrbit(lampName) {
    put("paper", "screen");
    put("d-earth");
    put("r-orbit");
    turn("d-earth", function (t) { return -0.25 * t; });
    lander();
    grow("n-carrier", function () { return LK; });
    lamp(lampName, null, null);
}
