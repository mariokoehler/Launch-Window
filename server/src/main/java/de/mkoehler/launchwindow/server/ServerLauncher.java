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
     * from. (Unconfirmed default: revisit once design.md &sect;5's
     * scheduler-and-server-config-file slice defines a real one.)
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
        // Async/daily-batch (design.md 3.1): there is no continuous simulation to tick, just
        // short-lived client connections plus one scheduled turn-resolution job (not implemented
        // yet). A modest rate is plenty for now.
        config.updatesPerSecond = 1;
        new HeadlessApplication(new GameServer(port, dataRoot), config);
    }
}
