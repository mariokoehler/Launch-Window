// Ingress after a crossing (2026-10-20, Soyuz 4 and 5's crew transfer; spacewalk.jsx's transfer scene, framed as the
// crossing): standing in the second craft's open hatch, where the crossing leaves it, the figure sinks in out of sight,
// and the hatch swings shut over it.
// Glitch: the hatch stops part shut at 2.0 and won't latch; a second pull at 2.6 seats it.
// Failure: the same stop, and neither pull seats it: at 3.2 the hatch is still open, and stays so.
var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
var OVER = typeof TRANSFER_OVER !== "undefined" ? TRANSFER_OVER : 494;
var DY = typeof TRANSFER_DY !== "undefined" ? TRANSFER_DY : 0;
var STOP = 2.0, PULL = 2.6, AGAIN = 3.2;

spacewalkScene(true);

var pose = poses([[0.4, [OVER, DY, 0]], [1.3, [OVER, INSIDE, 0]]]);
walk(pose);
hide("e-box");
// The first hatch shut already, the second open (spacewalk.jsx's hatch: the second's turns the other way).
turn("e-hatch", function () { return 0; });
var shutting = glitch
    ? track([[1.4, -110], [STOP, -40, "in"], [PULL, -40], [PULL + 0.4, 0, "in"]])
    : failure
        ? track([[1.4, -110], [STOP, -40, "in"], [PULL, -40], [PULL + 0.15, -30, "out"], [PULL + 0.4, -40], [AGAIN, -40],
                 [AGAIN + 0.15, -30, "out"], [AGAIN + 0.4, -40]])
        : track([[1.4, -110], [2.0, 0, "in"]]);
turn("e-hatch-b", shutting);
