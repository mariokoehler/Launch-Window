package de.mkoehler.launchwindow.net.messages;

/**
 * The server's reply to a {@link ClaimSlotRequest}.
 *
 * @param success whether the slot was claimed
 * @param reason  why the claim failed, so a client can show the right inline error without
 *                parsing {@code message}; {@code null} on success
 * @param message a human-readable result, such as which name is already taken
 * @author Mario Koehler
 */
public record ClaimSlotResponse(boolean success, Reason reason, String message) {

    /**
     * Why a slot claim was refused ({@code ui-spec.md} &sect;5).
     */
    public enum Reason {
        /** The connection never sent an accepted handshake. */
        NOT_AUTHENTICATED,
        /** The archetype, agency name or director name is missing. */
        MISSING_FIELDS,
        /** This account already has a slot in this game. */
        ALREADY_CLAIMED,
        /** Another player's agency already has this name (ignoring case). */
        AGENCY_NAME_TAKEN,
        /** Another player's director already has this name (ignoring case). */
        DIRECTOR_NAME_TAKEN,
        /** The server could not write the new slot. */
        SERVER_ERROR
    }

    /**
     * Creates a success.
     *
     * @param message the welcome
     * @return the response
     */
    public static ClaimSlotResponse success(String message) {
        return new ClaimSlotResponse(true, null, message);
    }

    /**
     * Creates a failure.
     *
     * @param reason  why the claim failed
     * @param message the same, in words
     * @return the response
     */
    public static ClaimSlotResponse failure(Reason reason, String message) {
        return new ClaimSlotResponse(false, reason, message);
    }
}
