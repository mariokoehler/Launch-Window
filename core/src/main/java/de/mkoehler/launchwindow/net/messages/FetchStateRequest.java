package de.mkoehler.launchwindow.net.messages;

/**
 * <p>Sent by a client to fetch its own current state: its last resolved
 * snapshot with today's not-yet-resolved actions replayed on top (see
 * {@code design.md} &sect;3.7's login flow). Only honored on a connection
 * that has already completed a successful {@link HandshakeRequest} - no
 * credentials travel with this message, since the connection is already
 * authenticated. Carries no fields of its own; sending it is the whole
 * request.</p>
 *
 * @author Mario Koehler
 */
public record FetchStateRequest() {
}
