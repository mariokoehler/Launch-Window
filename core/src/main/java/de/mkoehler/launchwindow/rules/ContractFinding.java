package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.ContractListing;

import java.util.List;
import java.util.Objects;

/**
 * One agency's part of the turn's contract market ({@code design.md} &sect;2.11), settled once for
 * every agency by {@link ContractMarket} and applied by its own season pass ({@link Contracts}).
 *
 * @param kind            what it is
 * @param listing         the listing
 * @param tier            the tier won, 1 to 3, or 0
 * @param fee             the fee, fixed now, or 0
 * @param programId       the program applied through, or {@code null}
 * @param configurationId the configuration the contract flies, or {@code null}
 * @param winners         for news, the winners' agency names, alphabetical
 * @author Mario Koehler
 */
public record ContractFinding(Kind kind, ContractListing listing, int tier, long fee, String programId,
                              String configurationId, List<String> winners) {

    /** What a finding is. */
    public enum Kind {
        /** The agency won a tier. */
        AWARDED,
        /** The agency applied and missed out. */
        MISSED,
        /** The agency's application came to nothing: its program was closed. */
        VOIDED,
        /** A listing went to the agencies named (news for everyone). */
        NEWS
    }

    /**
     * Checks the kind and listing, and copies the names.
     *
     * @throws NullPointerException if the kind or listing is {@code null}
     */
    public ContractFinding {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(listing, "listing must not be null");
        winners = winners == null ? List.of() : List.copyOf(winners);
    }
}
