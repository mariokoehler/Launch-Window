// The Luna landers' scene (2026-10-16), after orbit.jsx (its radio rings) and lunokhod.jsx (the rover, the
// camera): the Ye-8 descent stage that Luna 15, 16 and 17 shared, on a carrier so it can come down and
// lean as one, with what it carries on its deck: PAYLOAD "rover" (Lunokhod, ramps folded up) or "sampler"
// (the return rocket with its capsule on top, and the drill arm folded up at the deck's front).
function lunaScene(payload) {
    put("paper", "screen");
    put("n-ground");
    put("n-rocks");
    for (var i = 1; i <= 4; i++) { put("ring-" + i); hide("ring-" + i); }
    put("n-dust");
    hide("n-dust");
    put("u-kick");
    hide("u-kick");
    put("u-lander");
    var parts = ["u-plume", "u-ramp-r", "u-stage", "u-ramp-f"];
    if (payload === "sampler") parts = ["u-plume", "u-stage", "s-rocket", "s-capsule", "s-arm"];
    for (i = 0; i < parts.length; i++) put(parts[i], "u-lander");
    hide("u-plume");
    if (payload === "rover") {
        turn("u-ramp-f", function () { return -90; });
        turn("u-ramp-r", function () { return 90; });
        put("u-rover", "u-lander");
        var rover = ["u-ant", "u-lid", "u-tub", "u-wheel-1", "u-wheel-2", "u-wheel-3", "u-wheel-4"];
        for (i = 0; i < rover.length; i++) put(rover[i], "u-rover");
    } else {
        put("s-core", "s-arm");
        hide("s-core");
        put("s-flash");
        hide("s-flash");
        turn("s-arm", function () { return -90; });
    }
    put("burst");
    hide("burst");
}
