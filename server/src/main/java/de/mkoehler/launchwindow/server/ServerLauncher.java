package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import de.mkoehler.launchwindow.net.NetworkConstants;

/**
 * Entry point for the dedicated server process.
 *
 * @author Mario Koehler
 */
public final class ServerLauncher {

    /**
     * Not instantiable; this class only holds the process entry point.
     */
    private ServerLauncher() {
    }

    /**
     * Boots the headless libGDX application hosting {@link GameServer}.
     *
     * @param args optionally the TCP port to listen on as the first argument
     *             (the default is {@link NetworkConstants#TCP_PORT})
     */
    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : NetworkConstants.TCP_PORT;
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Async/daily-batch (design.md 3.1): there is no continuous simulation to tick, just
        // short-lived client connections plus one scheduled turn-resolution job (not implemented
        // yet). A modest rate is plenty for now.
        config.updatesPerSecond = 1;
        new HeadlessApplication(new GameServer(port), config);
    }
}
