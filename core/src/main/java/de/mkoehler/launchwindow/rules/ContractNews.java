package de.mkoehler.launchwindow.rules;

import java.util.List;
import java.util.Objects;

/**
 * The contract board's public news this turn ({@code design.md} &sect;2.11), the same for every
 * agency: who won a listing, in alphabetical order so the ranking stays sealed, or how many new
 * listings were posted.
 *
 * @param kind      which news
 * @param listingId the listing awarded, or {@code null} for posted listings
 * @param lineId    the line, or {@code null} for posted listings
 * @param clientId  the client, as the reader's side sees it, or {@code null}
 * @param winners   the winning agencies' names, alphabetical; empty for posted listings
 * @param count     the listings posted; 0 for an award
 * @author Mario Koehler
 */
public record ContractNews(Kind kind, String listingId, String lineId, String clientId, List<String> winners,
                           int count) implements ResolutionEvent {

    /** Which news. Turn reports store these by name: constants are only ever appended. */
    public enum Kind {
        /** A listing went to the agencies named. */
        AWARDED,
        /** New listings were posted for the coming turn. */
        POSTED
    }

    /**
     * Checks the kind and copies the names.
     *
     * @throws NullPointerException if {@code kind} is {@code null}
     */
    public ContractNews {
        Objects.requireNonNull(kind, "kind must not be null");
        winners = winners == null ? List.of() : List.copyOf(winners);
    }
}
