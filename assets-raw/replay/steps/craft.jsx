// The air launch's craft (2026-10-17): its parts' names and where it hangs under its carrier, read by air.jsx, x15.jsx
// and the air-launch timelines. The X-15 under a B-52's wing unless a step's file names another first (CRAFT, as
// e154.jsx does).
var K = typeof CRAFT !== "undefined" ? CRAFT : {
    plane: "w-plane", skid: "w-skid", flame: "w-flame", carrier: "a-carrier", gear: "a-gear", smoke: "a-smoke",
    ventral: "x15-ventral", shock: "x15-shock", heat: "x15-heat", hot: "x15-hot", rcsUp: "x15-rcs-up", rcsDown: "x15-rcs-down",
    // Where its pivot (WS) hangs, in the carrier's drawing; where its ventral fin's pivot is, in its own.
    hang: [859, 716], V: [788, 858],
    // The X-15 drops its lower fin before landing and lands on skids; the E-154 lowers its wheels instead.
    x15: true
};
