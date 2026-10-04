package de.mkoehler.launchwindow.client.connect;

import de.mkoehler.launchwindow.rules.TurnReport;

/**
 * What a turn report fetch came to.
 *
 * @param kind    how it went
 * @param message the server's or connection's reason, or {@code ""}
 * @param report  the report, when {@link Kind#FOUND}
 * @author Mario Koehler
 */
public record TurnReportResult(Kind kind, String message, TurnReport report) {

    /**
     * How a fetch went.
     */
    public enum Kind {
        /** A report came back. */
        FOUND,
        /** There's no report for that turn (none resolved yet, or the player joined later). */
        NONE,
        /** The server refused, or the connection failed. */
        FAILED
    }
}
