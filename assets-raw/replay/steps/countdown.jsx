// Countdown: the clock by the pad counts down from 5 while the oxidizer vents; at 0 it goes dark,
// and the launch takes over from the same frame. (Its length is a multiple of the wisps' 0.8 s
// loop, so the venting runs on across the cut.)
// Glitch: the count holds at 3, the digit blinking, and picks up again at 3.6.
// Failure: the count reaches 1, and at 4.3 the engine lights early, sputters and blows the base out.
padScene();
wisps(function () { return 1; });

var glitch = VARIANT === "glitch", failure = VARIANT === "failure";
// The second each number shows, 5 down to 0.
var at = glitch ? [0.3, 1.2, 2.1, 3.6, 4.5, 5.4] : [0.3, 1.2, 2.1, 3.0, 3.9, 4.8];
// The clock stays on "0" a while, so the result can be read before the launch cuts in.
var off = glitch ? 7.2 : 6.4;
var shown = failure ? track([[0.0, 0], [0.3, 1, "lin"], [4.6, 1], [5.2, 0, "lin"]])
                    : track([[0.0, 0], [0.3, 1, "lin"], [off - 0.5, 1], [off - 0.1, 0, "lin"]]);
clock(shown,
      function (t) {
          var n = -1;
          for (var i = 0; i < at.length; i++) if (t >= at[i]) n = 5 - i;
          if (failure && n < 1) n = 1;
          return n;
      },
      // The hold: the 3 blinks, 0.3 s on and off, from 2.4 until the count resumes.
      glitch ? function (t) { return t >= 2.4 && t < 3.6 && Math.floor((t - 2.4) / 0.3) % 2 === 1; } : null);

if (failure) {
    // The engine lights at 4.3 before its time, sputters, and the base goes at 4.6.
    states(["flame-ignite", "flame-sputter"], function (t) {
        if (t < 4.3 || t >= 4.6) return "";
        return Math.floor((t - 4.3) / 0.06) % 2 === 0 ? "flame-sputter" : "flame-ignite";
    });
    flicker(["flame-ignite", "flame-sputter"]);
    fire(track([[4.6, 0, "snap"], [4.62, 1.2, "snap"], [5.2, 1.4, "out"], [7.0, 1.0, "lin"]]));
    camera(function () { return 0; }, function (t) { return t >= 4.6 ? 8 * Math.max(0, 1 - (t - 4.6) / 0.6) : t >= 4.3 ? 2 : 0; });
    explode(4.6, 960, 800);
}
