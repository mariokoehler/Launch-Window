package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Something that happened to the agency's own contract or application this turn
 * ({@code design.md} &sect;2.11).
 *
 * @param kind            what happened
 * @param listingId       the listing
 * @param lineId          the line
 * @param clientId        the client, as the agency's side sees it, or {@code null}
 * @param programId       the program, or {@code null}
 * @param configurationId the configuration, or {@code null}
 * @param tier            the tier won, 1 to 3, or 0
 * @param fee             the fee the contract pays, or 0 for an application that won nothing
 * @param funds           the money paid in now: the fee, or the insurer's refund; 0 otherwise
 * @param prestige        the prestige this cost, as a positive number; 0 for none
 * @param missionId       the mission that flew it, or {@code null}
 * @param launchCost      what the flight's launch cost, for the profit; 0 before a flight
 * @param recordChange    how the line's record points changed, for a flight's end or a lapse
 * @author Mario Koehler
 */
public record ContractSettled(Kind kind, String listingId, String lineId, String clientId, String programId,
                              String configurationId, int tier, long fee, long funds, long prestige,
                              String missionId, long launchCost, int recordChange) implements ResolutionEvent {

    /**
     * What happened. Turn reports store these by name: constants are only ever appended.
     */
    public enum Kind {
        /** The application won a tier. */
        AWARDED,
        /** Three agencies with a better record won it. */
        MISSED,
        /** The application came to nothing: its program was closed before the turn resolved. */
        VOIDED,
        /** The contract flight succeeded and the client paid the fee. */
        PAID,
        /** The contract flight failed; the insurer refunded the launch cost. */
        REFUNDED,
        /** The contract wasn't launched in its window. */
        LAPSED
    }

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if the kind, listing or line is {@code null}
     */
    public ContractSettled {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(listingId, "listingId must not be null");
        Objects.requireNonNull(lineId, "lineId must not be null");
    }
}
