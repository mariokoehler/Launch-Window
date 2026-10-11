package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * An agency's application this turn ({@code design.md} &sect;2.11): sealed until the turn resolves,
 * and changed or withdrawn until then.
 *
 * @param listingId the listing applied for
 * @param programId the program the contract would fly on
 * @author Mario Koehler
 */
public record ContractApplication(String listingId, String programId) {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public ContractApplication {
        Objects.requireNonNull(listingId, "listingId must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
    }
}
