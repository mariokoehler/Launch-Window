package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkClient;
import de.mkoehler.launchwindow.net.NetworkServer;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Exercises {@link NetworkServer}, {@link NetworkClient} and
 * {@link RequestDispatcher} together over real loopback sockets - M1's
 * walking-skeleton exit criterion (see {@code design.md} &sect;5): a
 * client can log in, submit an action, and read it back over real
 * sockets.</p>
 *
 * <p>Stands in for what {@link GameServer}'s {@code render()} loop does in
 * the real application: a background thread polls {@link NetworkServer} on
 * a fixed short interval and routes each event through
 * {@link RequestDispatcher#handle(int, Object)}, the exact same call
 * {@code GameServer.dispatch} makes - not a second copy of the routing
 * logic, so this test actually exercises what {@code GameServer} runs.</p>
 *
 * @author Mario Koehler
 */
class ServerClientIntegrationTest {

    private static final long TIMEOUT_MILLIS = 5_000;

    private final ObjectMapper mapper = new ObjectMapper();

    private NetworkServer server;
    private NetworkClient client;
    private AccountStore accountStore;
    private ScheduledExecutorService pollingThread;

    /**
     * Finds a TCP port nobody is listening on.
     *
     * @return a free port
     * @throws IOException if no socket could be opened
     */
    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Polls until a condition holds or a timeout passes.
     *
     * @param condition what to wait for
     * @return whether the condition became true in time
     * @throws InterruptedException if the wait is interrupted
     */
    private static boolean waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(5);
        }
        return false;
    }

    /**
     * Starts a server, a dispatcher wired to it via
     * {@link RequestDispatcher#handle(int, Object)}, and a background
     * thread polling the server every 10ms - standing in for
     * {@code GameServer}'s own render loop.
     *
     * @param dataRoot the data root to back the account store and every
     *                 player's own data with
     * @return the port the server is listening on
     * @throws IOException if the port cannot be bound
     */
    private int startServer(Path dataRoot) throws IOException {
        int port = freePort();
        accountStore = new AccountStore(dataRoot.resolve("accounts.json"));
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper);
        server = new NetworkServer();
        server.start(port);
        NetworkServer capturedServer = server;
        pollingThread = Executors.newSingleThreadScheduledExecutor();
        pollingThread.scheduleWithFixedDelay(() -> capturedServer.poll(new NetworkServer.Handler() {
            @Override
            public void onMessage(int connectionId, Object message) {
                RequestDispatcher.DispatchResult result = dispatcher.handle(connectionId, message);
                if (result.closeConnection()) {
                    capturedServer.sendThenClose(connectionId, result.response());
                } else {
                    capturedServer.send(connectionId, result.response());
                }
            }

            @Override
            public void onDisconnect(int connectionId) {
                dispatcher.handleDisconnect(connectionId);
            }
        }), 0, 10, TimeUnit.MILLISECONDS);
        return port;
    }

    /**
     * Shuts down whatever a test started.
     */
    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (pollingThread != null) {
            pollingThread.shutdownNow();
        }
        if (server != null) {
            server.stop();
        }
        if (accountStore != null) {
            accountStore.close();
        }
    }

    /**
     * The full authenticated flow over real sockets: handshake, submit one
     * action, fetch state back and see it reflected.
     */
    @Test
    void handshakeSubmitAndFetchWorkOverRealSockets(@TempDir Path dataRoot) throws IOException {
        int port = startServer(dataRoot);
        client = new NetworkClient();
        client.connect("localhost", port);

        HandshakeResponse handshake = client.sendAndReceive(
            new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2", "Hanseatic Space Agency"),
            HandshakeResponse.class);
        assertTrue(handshake.accepted());

        SubmitActionResponse submitResponse =
            client.sendAndReceive(new SubmitActionRequest(new SetFundsAction(5_000)), SubmitActionResponse.class);
        assertTrue(submitResponse.success());

        FetchStateResponse fetchResponse = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
        assertTrue(fetchResponse.success());
        assertEquals(5_000, fetchResponse.state().getFunds());
    }

    /**
     * A version mismatch is rejected, and the server actually closes the
     * connection afterward - not just refuses later requests on it.
     */
    @Test
    void versionMismatchIsRejectedAndDisconnectsOverRealSockets(@TempDir Path dataRoot) throws IOException,
        InterruptedException {
        int port = startServer(dataRoot);
        client = new NetworkClient();
        client.connect("localhost", port);

        HandshakeResponse handshake = client.sendAndReceive(
            new HandshakeRequest("not-a-real-version", "mario", "hunter2", "Hanseatic Space Agency"),
            HandshakeResponse.class);
        assertFalse(handshake.accepted());

        assertTrue(waitFor(() -> !client.isConnected()), "the server must close the connection after rejecting it");
        assertThrows(IOException.class,
            () -> client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class));
    }
}
