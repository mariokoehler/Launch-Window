package de.mkoehler.launchwindow.net.messages;

/**
 * <p>The server's reply to a {@link HandshakeRequest}: whether the client's
 * version is compatible and its credentials were accepted. Every
 * {@link SubmitActionRequest}/{@link FetchStateRequest} sent on this same
 * connection afterward is only honored once this response has accepted
 * it.</p>
 *
 * <p>This record's fields never change, not even by adding one: it and
 * {@link HandshakeRequest} are registered first in {@code MessageRegistry}
 * specifically so their ids survive drift in every other registered class,
 * but Kryo's record serializer still reads the receiver's own field count
 * to decode one, so two builds with a different field list here could not
 * decode each other's handshake regardless of registration order.</p>
 *
 * @param accepted      whether the client was accepted
 * @param message       a human-readable message - a welcome on success, a
 *                      rejection reason on failure
 * @param serverVersion the version of the answering server, so a client
 *                      rejected for its version can show both
 * @author Mario Koehler
 */
public record HandshakeResponse(boolean accepted, String message, String serverVersion) {

    /**
     * The whole message of a handshake refused because no account has its login: the client offers to
     * create one ({@link CreateAccountRequest}) instead of showing it. A constant, not a field, since this
     * record's shape is pinned.
     */
    public static final String NO_SUCH_ACCOUNT = "NO_SUCH_ACCOUNT";

    /**
     * Creates an accepting response.
     *
     * @param message       a human-readable welcome
     * @param serverVersion the answering server's build version
     * @return the accepting response
     */
    public static HandshakeResponse accept(String message, String serverVersion) {
        return new HandshakeResponse(true, message, serverVersion);
    }

    /**
     * Creates a rejecting response.
     *
     * @param message       a human-readable rejection reason
     * @param serverVersion the answering server's build version
     * @return the rejecting response
     */
    public static HandshakeResponse reject(String message, String serverVersion) {
        return new HandshakeResponse(false, message, serverVersion);
    }
}
