package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import de.mkoehler.launchwindow.net.NetworkConstants;

import java.nio.file.Path;

/**
 * Entry point for the dedicated server process.
 *
 * @author Mario Koehler
 */
public final class ServerLauncher {

    /**
     * The data root used when none is given on the command line - a plain
     * relative "data" directory next to wherever the process is started
     * from. (Unconfirmed default.)
     */
    private static final Path DEFAULT_DATA_ROOT = Path.of("data");

    /**
     * Not instantiable; this class only holds the process entry point.
     */
    private ServerLauncher() {
    }

    /**
     * Boots the headless libGDX application hosting {@link GameServer}.
     *
     * @param args optionally the TCP port to listen on as the first
     *             argument (the default is {@link NetworkConstants#TCP_PORT})
     *             and the data root directory as the second
     *             (see {@link #DEFAULT_DATA_ROOT})
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : NetworkConstants.TCP_PORT;
        Path dataRoot = args.length > 1 ? Path.of(args[1]) : DEFAULT_DATA_ROOT;
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Requests are handled on the render() tick, so the tick rate is the server's response
        // latency: at 1 per second every call waited up to a second, and hiring five people (a
        // submit and a fetch each) took several seconds. 50 ticks a second answers within 20 ms;
        // GameServer runs the turn scheduler only once a second.
        config.updatesPerSecond = 50;
        new HeadlessApplication(new GameServer(port, dataRoot), config);
    }
}
