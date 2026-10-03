package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.model.Archetype;

/**
 * <p>Claims this account's slot in the game: the agency archetype, the agency's
 * name, and the player's own callsign ({@code design.md} &sect;3.8). Sent once,
 * after an accepted handshake, by an account that has no slot yet. Until then,
 * every {@link FetchStateRequest} and {@link SubmitActionRequest} fails with
 * {@link #SLOT_CLAIM_REQUIRED}.</p>
 *
 * @param archetype  NASA or SOVIET; several players may share one
 * @param agencyName the agency's name, unique within the game
 * @param callsign   the player's persona, unique within the game
 * @author Mario Koehler
 */
public record ClaimSlotRequest(Archetype archetype, String agencyName, String callsign) {

    /** The failure message a client gets from fetch or submit until it has claimed a slot. */
    public static final String SLOT_CLAIM_REQUIRED = "SLOT_CLAIM_REQUIRED";
}
