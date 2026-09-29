package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.net.NetworkServer;
import de.mkoehler.launchwindow.server.accounts.AccountStore;

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>Headless libGDX application hosting the dedicated server: binds
 * {@link NetworkServer}, and on every {@link #render()} tick drains its
 * queued events and routes them through {@link RequestDispatcher} to
 * {@link AccountStore} and the per-player persistence layer.</p>
 *
 * @author Mario Koehler
 */
public class GameServer extends ApplicationAdapter {

    private static final String TAG = "GameServer";

    private final int tcpPort;
    private final Path dataRoot;
    private NetworkServer networkServer;
    private AccountStore accountStore;
    private RequestDispatcher dispatcher;

    /**
     * Creates the application.
     *
     * @param tcpPort  the TCP port to listen on
     * @param dataRoot the server's data root directory - accounts and every
     *                 player's own data live under it
     */
    public GameServer(int tcpPort, Path dataRoot) {
        this.tcpPort = tcpPort;
        this.dataRoot = dataRoot;
    }

    @Override
    public void create() {
        accountStore = new AccountStore(dataRoot.resolve("accounts.json"));
        dispatcher = new RequestDispatcher(accountStore, dataRoot, new ObjectMapper());
        networkServer = new NetworkServer();
        try {
            networkServer.start(tcpPort);
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on TCP port " + tcpPort, e);
        }
        Gdx.app.log(TAG, "Launch Window server listening on TCP " + tcpPort + ", data root " + dataRoot.toAbsolutePath());
    }

    @Override
    public void render() {
        networkServer.poll(new NetworkServer.Handler() {
            @Override
            public void onMessage(int connectionId, Object message) {
                dispatch(connectionId, message);
            }

            @Override
            public void onDisconnect(int connectionId) {
                dispatcher.handleDisconnect(connectionId);
            }
        });
    }

    /**
     * Routes one received message through {@link RequestDispatcher} and
     * sends its response back on the same connection, closing the
     * connection afterward if the dispatcher says to (a rejected
     * handshake).
     *
     * @param connectionId the connection the message arrived on
     * @param message      the message
     */
    private void dispatch(int connectionId, Object message) {
        RequestDispatcher.DispatchResult result;
        try {
            result = dispatcher.handle(connectionId, message);
        } catch (IllegalArgumentException e) {
            Gdx.app.error(TAG, e.getMessage());
            return;
        }
        if (result.closeConnection()) {
            networkServer.sendThenClose(connectionId, result.response());
        } else {
            networkServer.send(connectionId, result.response());
        }
    }

    @Override
    public void dispose() {
        networkServer.stop();
        accountStore.close();
    }
}
