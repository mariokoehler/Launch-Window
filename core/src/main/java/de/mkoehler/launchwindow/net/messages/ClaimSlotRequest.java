package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.Archetype;

/**
 * <p>Claims this account's slot in the game: the agency archetype, the agency's
 * name, and the name of the agency's director, the player's persona ({@code design.md} &sect;3.8). Sent once,
 * after an accepted handshake, by an account that has no slot yet. Until then,
 * every {@link FetchStateRequest} and {@link SubmitActionRequest} fails with
 * {@link #SLOT_CLAIM_REQUIRED}.</p>
 *
 * @param archetype  NASA or SOVIET; several players may share one
 * @param agencyName the agency's name, unique within the game
 * @param directorName the agency director's name, the player's persona, unique within the game
 * @author Mario Koehler
 */
public record ClaimSlotRequest(Archetype archetype, String agencyName, String directorName) {

    /** The failure message a client gets from fetch or submit until it has claimed a slot. */
    public static final String SLOT_CLAIM_REQUIRED = "SLOT_CLAIM_REQUIRED";

    /**
     * The longest agency or director name, so a front page's columns can hold it ({@code ui-spec.md}
     * &sect;4.18, G14; unconfirmed, 2026-10-11).
     */
    public static final int MAX_NAME_LENGTH = 32;
}
