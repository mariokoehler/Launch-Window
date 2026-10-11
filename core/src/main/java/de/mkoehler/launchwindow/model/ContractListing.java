package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * One listing on the contract board ({@code design.md} &sect;2.11): a line's client with an offer,
 * the same for every agency. The client is named per side, since the Soviet side's clients are
 * ministries and institutes; the text is in {@code commercial-clients.json}.
 *
 * @param id             a stable id, unique within the game
 * @param lineId         the line, a program category
 * @param nasaClientId   the client NASA agencies see, or {@code null} without client text
 * @param sovietClientId the client Soviet agencies see, or {@code null} without client text
 * @param postedTurn     the first turn it is on the board
 * @param lastTurn       the last turn it takes applications; it leaves the board after it
 * @author Mario Koehler
 */
public record ContractListing(String id, String lineId, String nasaClientId, String sovietClientId, int postedTurn,
                              int lastTurn) {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if the id or line is {@code null}
     */
    public ContractListing {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(lineId, "lineId must not be null");
    }

    /**
     * Returns the client one side sees.
     *
     * @param archetype the side
     * @return the client's id, or {@code null}
     */
    public String clientFor(Archetype archetype) {
        return archetype == Archetype.SOVIET ? sovietClientId : nasaClientId;
    }
}
