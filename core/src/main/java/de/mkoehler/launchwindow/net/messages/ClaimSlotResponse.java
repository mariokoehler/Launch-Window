package de.mkoehler.launchwindow.net.messages;

/**
 * The server's reply to a {@link ClaimSlotRequest}.
 *
 * @param success whether the slot was claimed
 * @param message a human-readable result, such as which name is already taken
 * @author Mario Koehler
 */
public record ClaimSlotResponse(boolean success, String message) {

    /**
     * Creates a success.
     *
     * @param message the welcome
     * @return the response
     */
    public static ClaimSlotResponse success(String message) {
        return new ClaimSlotResponse(true, message);
    }

    /**
     * Creates a failure.
     *
     * @param message why the claim failed
     * @return the response
     */
    public static ClaimSlotResponse failure(String message) {
        return new ClaimSlotResponse(false, message);
    }
}
