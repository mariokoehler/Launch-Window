package de.mkoehler.launchwindow.net.messages;

import de.mkoehler.launchwindow.persistence.PlayerAction;

/**
 * <p>Sent by a client to submit one action to its own not-yet-resolved
 * journal (see {@code design.md} &sect;3.7). Only honored on a connection
 * that has already completed a successful {@link HandshakeRequest} - no
 * credentials travel with this message, since the connection is already
 * authenticated.</p>
 *
 * @param action the action to submit
 * @author Mario Koehler
 */
public record SubmitActionRequest(PlayerAction action) {
}
