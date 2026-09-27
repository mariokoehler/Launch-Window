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
     * Not instantiable; this class only holds constants.
     */
    private NetworkConstants() {
    }
}
