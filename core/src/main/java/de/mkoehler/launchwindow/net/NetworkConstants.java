package de.mkoehler.launchwindow.net;

/**
 * Fixed values shared by the dedicated server and every client.
 * <p>
 * Launch Window is async/daily-batch, not real-time (design.md 3.1/3.3): a
 * client's "connection" is a short-lived connect-request-disconnect, not a
 * held session, so like Robot Rampage there is no UDP channel and no
 * simulation tick rate - the whole protocol runs over KryoNet's reliable TCP
 * channel.
 *
 * @author Mario Koehler
 */
public final class NetworkConstants {

    /**
     * The default TCP port the dedicated server listens on. Deliberately
     * different from the StarWars (45625/tcp) and Robot Rampage (45725/tcp)
     * projects' ports, so all three servers can run side by side on one
     * machine.
     */
    public static final int TCP_PORT = 45825;

    /**
     * Maximum time, in milliseconds, a client waits for a connection attempt
     * to complete before giving up.
     */
    public static final int CONNECTION_TIMEOUT_MILLIS = 10_000;

    /**
     * Maximum time, in milliseconds, a client waits for a response to one
     * request before giving up. The server side (see {@code GameServer})
     * only drains queued requests once per {@code updatesPerSecond} tick, so
     * a handshake followed by one request can take a couple of seconds even
     * under normal conditions - this leaves generous headroom above that.
     */
    public static final int RESPONSE_TIMEOUT_MILLIS = 8_000;

    /**
     * Size, in bytes, of the buffer used to write queued objects to the
     * network. Several messages can be queued at once, so it is a multiple
     * of {@link #OBJECT_BUFFER_SIZE}.
     */
    public static final int WRITE_BUFFER_SIZE = 262_144;

    /**
     * Size, in bytes, of the buffer used to serialize or deserialize a
     * single object. Must be larger than the biggest single message - a
     * {@code FetchStateResponse} carrying a full personnel roster is the
     * largest today.
     */
    public static final int OBJECT_BUFFER_SIZE = 65_536;

    /**
     * Not instantiable; this class only holds constants.
     */
    private NetworkConstants() {
    }
}
