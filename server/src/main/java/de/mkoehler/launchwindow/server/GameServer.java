package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;

/**
 * Headless libGDX application hosting the dedicated server.
 * <p>
 * Deliberately minimal for now: just proves KryoNet actually binds and
 * accepts/logs connections. No message protocol exists yet (design.md 3.3's
 * "connect, send today's actions, get current state, disconnect" RPC-style
 * usage isn't designed in detail yet), so there is no message registry and
 * no queued-event handling here yet - see Robot Rampage's own
 * {@code NetworkServer}/{@code MessageRegistry} for the pattern to follow
 * once real message types exist; building that machinery before there's
 * anything to register would be premature.
 *
 * @author Mario Koehler
 */
public class GameServer extends ApplicationAdapter {

    private static final String TAG = "GameServer";

    private final int tcpPort;
    private Server server;

    /**
     * Creates the application.
     *
     * @param tcpPort the TCP port to listen on
     */
    public GameServer(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    @Override
    public void create() {
        server = new Server();
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Gdx.app.log(TAG, "Connection " + connection.getID() + " opened");
            }

            @Override
            public void disconnected(Connection connection) {
                Gdx.app.log(TAG, "Connection " + connection.getID() + " closed");
            }
        });
        server.start();
        try {
            server.bind(tcpPort);
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on TCP port " + tcpPort, e);
        }
        Gdx.app.log(TAG, "Launch Window server listening on TCP " + tcpPort);
    }

    @Override
    public void dispose() {
        server.stop();
    }
}
