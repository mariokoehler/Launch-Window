package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.client.connect.ClaimResult;
import de.mkoehler.launchwindow.client.connect.Credentials;
import de.mkoehler.launchwindow.client.connect.LoginResult;
import de.mkoehler.launchwindow.client.connect.NetworkGateway;
import de.mkoehler.launchwindow.client.connect.ServerAddress;
import de.mkoehler.launchwindow.client.connect.ServerResult;
import de.mkoehler.launchwindow.actions.AdjustSalariesAction;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.net.NetworkServer;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private final AtomicInteger handshakes = new AtomicInteger();

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
     * The whole first-visit flow over one long-lived connection, a transparent reconnect after the
     * server restarts, then the refusals.
     */
    @Test
    void theGatewayCarriesTheConnectAndJoinFlow(@TempDir Path dataRoot) throws Exception {
        int port = startServer(dataRoot);
        application = new HeadlessApplication(new ApplicationAdapter() {
        });
        gateway = new NetworkGateway();
        ServerAddress address = new ServerAddress("localhost", port);
        Credentials mario = new Credentials(address, "mario", "hunter2");

        LoginResult first = logIn(mario);
        assertEquals(LoginResult.Kind.NEEDS_SLOT, first.kind());
        GameInfoResponse info = first.gameInfo().response();
        assertEquals(1, info.gameTurn());
        assertEquals(new GameDate(1955, Season.WINTER), info.date());
        assertFalse(info.started());
        assertTrue(first.gameInfo().millisUntilNextResolution(System.currentTimeMillis()).isPresent());
        ClaimResult joined = claim(mario, "Baltic Star Agency", "A. Reyes");
        assertEquals(ClaimResult.Kind.JOINED, joined.kind());
        assertEquals("A. Reyes", joined.state().getDirectorName());
        assertEquals(ServerTestGames.PACK_HASH, joined.gameInfo().response().contentPackHash());
        assertEquals(1, handshakes.get(), "the claim reuses the login's connection");

        // Actions go over the same connection: a hire is applied and the refreshed state comes back with it;
        // a refusal comes back as the server's code.
        String candidate = joined.state().getSetSpecialistCandidates().get(0).getId();
        ServerResult hired = submit(mario, new HireAction(PersonnelGroup.SET, candidate));
        assertEquals(ServerResult.Kind.OK, hired.kind(), hired.message());
        assertEquals(1, hired.state().getSetSpecialists().size());
        ServerResult stale = submit(mario, new HireAction(PersonnelGroup.SET, candidate));
        assertEquals(ServerResult.Kind.REFUSED, stale.kind());
        assertEquals("UNKNOWN_CANDIDATE", stale.message());
        double salary = hired.state().getSetSpecialists().get(0).getSalary();
        ServerResult raised = submit(mario, new AdjustSalariesAction(PersonnelGroup.SET, 10));
        assertEquals(ServerResult.Kind.OK, raised.kind(), raised.message());
        assertEquals(salary * 1.1, raised.state().getSetSpecialists().get(0).getSalary(), 1e-9);
        ServerResult refreshed = await(done -> gateway.refresh(mario, done));
        assertEquals(1, refreshed.state().getSetSpecialists().size());
        assertEquals(1, handshakes.get(), "submits and refreshes reuse the connection too");

        // A server restart drops the connection; the next call reconnects and logs in again by itself.
        restartServer(dataRoot, port);
        ClaimResult afterRestart = claim(mario, "Second Agency", "Someone Else");
        assertEquals(ClaimResult.Kind.REFUSED, afterRestart.kind(), afterRestart.message());
        assertEquals(ClaimSlotResponse.Reason.ALREADY_CLAIMED, afterRestart.reason());
        assertEquals(2, handshakes.get(), "one new login after the restart");

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

    /**
     * The session's connection outlives an idle stretch longer than KryoNet's 12 s timeout
     * (keepalives flow), and KryoNet's client thread is a daemon, so a close still running when the
     * client exits can't keep the process alive.
     */
    @Test
    void theConnectionSurvivesIdleStretches(@TempDir Path dataRoot) throws Exception {
        int port = startServer(dataRoot);
        application = new HeadlessApplication(new ApplicationAdapter() {
        });
        gateway = new NetworkGateway();
        Credentials mario = new Credentials(new ServerAddress("localhost", port), "mario", "hunter2");

        assertEquals(LoginResult.Kind.NEEDS_SLOT, logIn(mario).kind());
        Thread.sleep(15_000);
        assertEquals(ClaimResult.Kind.JOINED, claim(mario, "Baltic Star Agency", "A. Reyes").kind());
        assertEquals(1, handshakes.get(), "no reconnect after 15 s idle");

        List<Thread> clientThreads = Thread.getAllStackTraces().keySet().stream()
            .filter(thread -> thread.getName().equals("Client")).toList();
        assertFalse(clientThreads.isEmpty(), "KryoNet's client update thread is running");
        assertTrue(clientThreads.stream().allMatch(Thread::isDaemon), "KryoNet's client thread is a daemon");
    }

    private ServerResult submit(Credentials credentials, PlayerAction action) throws Exception {
        return await(done -> gateway.submit(credentials, action, done));
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
        startServer(dataRoot, port);
        return port;
    }

    /**
     * Stops the server and starts a new one on the same port and data, as a host's restart would,
     * then waits long enough for the client to notice its connection is gone.
     */
    private void restartServer(Path dataRoot, int port) throws Exception {
        pollingThread.shutdownNow();
        server.stop();
        startServer(dataRoot, port);
        Thread.sleep(1_000);
    }

    private void startServer(Path dataRoot, int port) throws IOException {
        ObjectMapper mapper = GameServer.createMapper();
        GameMeta meta = ServerTestGames.meta(false);
        RequestDispatcher dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper, ServerTestGames.CONTEXT,
            meta, ServerTestGames.gameInfo(dataRoot, mapper, meta));
        server = new NetworkServer();
        server.start(port);
        NetworkServer capturedServer = server;
        pollingThread = Executors.newSingleThreadScheduledExecutor();
        pollingThread.scheduleWithFixedDelay(() -> capturedServer.poll(new NetworkServer.Handler() {
            @Override
            public void onMessage(int connectionId, Object message) {
                if (message instanceof HandshakeRequest) {
                    handshakes.incrementAndGet();
                }
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
    }
}
