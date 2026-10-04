package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.NetworkGateway;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.NetworkServer;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives the client's real {@link NetworkGateway}, the one the Connect and Join Game screens
 * use, against a real server over real sockets: a new login needs a slot, the claim joins, the
 * next login lands in the game, and each refusal comes back as the screen expects it. Results
 * are delivered through a headless libGDX application's render thread, as in the client.
 *
 * @author Mario Koehler
 */
class ClientGatewayIntegrationTest {

    private static final long TIMEOUT_SECONDS = 15;

    private NetworkServer server;
    private ScheduledExecutorService pollingThread;
    private HeadlessApplication application;
    private NetworkGateway gateway;
    private AccountStore accountStore;

    @AfterEach
    void tearDown() {
        if (gateway != null) {
            gateway.dispose();
        }
        if (application != null) {
            application.exit();
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
     * The whole first-visit flow, then the refusals.
     */
    @Test
    void theGatewayCarriesTheConnectAndJoinFlow(@TempDir Path dataRoot) throws Exception {
        int port = startServer(dataRoot);
        application = new HeadlessApplication(new ApplicationAdapter() {
        });
        gateway = new NetworkGateway();
        ServerAddress address = new ServerAddress("localhost", port);
        Credentials mario = new Credentials(address, "mario", "hunter2");

        assertEquals(LoginResult.Kind.NEEDS_SLOT, logIn(mario).kind());
        ClaimResult joined = claim(mario, "Baltic Star Agency", "A. Reyes");
        assertEquals(ClaimResult.Kind.JOINED, joined.kind());
        assertEquals("A. Reyes", joined.state().getDirectorName());

        LoginResult again = logIn(mario);
        assertEquals(LoginResult.Kind.IN_GAME, again.kind());
        assertEquals("Baltic Star Agency", again.state().getAgencyName());

        assertEquals(LoginResult.Kind.REJECTED, logIn(new Credentials(address, "mario", "wrong")).kind());

        Credentials anna = new Credentials(address, "anna", "secret");
        assertEquals(LoginResult.Kind.NEEDS_SLOT, logIn(anna).kind());
        ClaimResult taken = claim(anna, "Orion Spaceworks", "a. reyes");
        assertEquals(ClaimResult.Kind.REFUSED, taken.kind());
        assertEquals(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN, taken.reason());

        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        assertEquals(LoginResult.Kind.UNREACHABLE,
            logIn(new Credentials(new ServerAddress("localhost", closedPort), "mario", "hunter2")).kind());
    }

    private LoginResult logIn(Credentials credentials) throws Exception {
        return await(done -> gateway.logIn(credentials, done));
    }

    private ClaimResult claim(Credentials credentials, String agency, String director) throws Exception {
        return await(done -> gateway.claimSlot(credentials, Archetype.NASA, agency, director, done));
    }

    private static <T> T await(Consumer<Consumer<T>> call) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        call.accept(result::complete);
        return result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private int startServer(Path dataRoot) throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        accountStore = new AccountStore(dataRoot.resolve("accounts.json"));
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, new ObjectMapper(),
            ServerTestGames.CONTEXT, ServerTestGames.meta(false));
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
}
