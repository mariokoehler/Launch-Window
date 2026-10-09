package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.persistence.PlayerAction;

/**
 * <p>Sent by a client to submit one action to its own not-yet-resolved
 * journal (see {@code design.md} &sect;3.7). Only honored on a connection
 * that has already completed a successful {@link HandshakeRequest} - no
 * credentials travel with this message, since the connection is already
 * authenticated.</p>
 *
 * <p>The client names the open game turn its state belongs to. If the turn has
 * resolved since (the player acted on the old turn's screen while it resolved),
 * the server refuses with {@link #TURN_RESOLVED} and applies nothing, so an
 * action meant for one turn never lands in the next.</p>
 *
 * @param action   the action to submit
 * @param gameTurn the open game turn the client's state belongs to, or 0 to skip
 *                 the check (the developer client and tests)
 * @author Mario Koehler
 */
public record SubmitActionRequest(PlayerAction action, long gameTurn) {

    /** The refusal when the turn the client named has resolved since. */
    public static final String TURN_RESOLVED = "TURN_RESOLVED";

    /**
     * A request that skips the turn check.
     *
     * @param action the action to submit
     */
    public SubmitActionRequest(PlayerAction action) {
        this(action, 0);
    }
}
