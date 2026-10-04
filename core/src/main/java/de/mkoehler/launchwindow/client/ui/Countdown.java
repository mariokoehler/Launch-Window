package de.mkoehler.launchwindow.client.ui;

import java.util.OptionalLong;

/**
 * Words the time until the next turn resolves, as the header's next-resolution chip shows it
 * ({@code ui-spec.md} &sect;2: "Next turn in 6 h 12 m").
 *
 * @author Mario Koehler
 */
public final class Countdown {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    /**
     * Not instantiable; this class only holds the wording.
     */
    private Countdown() {
    }

    /**
     * Words the time left.
     *
     * @param millisLeft the time left, or empty if no resolution is scheduled
     * @return for example "Next turn in 6 h 12 m", "Next turn in 2 d 4 h" or "Turn resolving now"
     */
    public static String nextTurn(OptionalLong millisLeft) {
        if (millisLeft.isEmpty()) {
            return "No turn scheduled";
        }
        long left = millisLeft.getAsLong();
        if (left <= 0) {
            return "Turn resolving now";
        }
        if (left < MINUTE) {
            return "Next turn in under a minute";
        }
        if (left >= DAY) {
            return "Next turn in " + left / DAY + " d " + (left % DAY) / HOUR + " h";
        }
        if (left >= HOUR) {
            return "Next turn in " + left / HOUR + " h " + (left % HOUR) / MINUTE + " m";
        }
        return "Next turn in " + left / MINUTE + " m";
    }
}
