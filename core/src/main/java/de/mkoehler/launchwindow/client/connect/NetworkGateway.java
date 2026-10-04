package de.mkoehler.launchwindow.client.connect;

import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkClient;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.net.messages.TurnReportRequest;
import de.mkoehler.launchwindow.net.messages.TurnReportResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The real {@link ServerGateway}: one KryoNet connection for the whole client session
 * ({@code design.md} &sect;3.3, changed 2026-10-04). Connect opens it and logs in once; every later
 * call goes over the same connection; Log out or the client's exit closes it. All calls run one at
 * a time on one background thread, which alone touches the connection, and post their results back
 * to the render thread.
 *
 * <p><b>Reconnecting.</b> A connection held for hours will sometimes drop (Wi-Fi, a sleeping
 * laptop, a server restarted between resolutions). Before each call the gateway checks the
 * connection is still up and, if not, opens a new one and logs in again with the session's
 * credentials. (A connection that died silently, with no TCP close, still looks up until KryoNet's
 * timeout notices; a call in that window fails once, and the retry reconnects.) A call whose connection breaks
 * <em>during</em> the exchange is not repeated: the server may already have acted on it. The
 * connection is dropped and the next call reconnects. (A slot claim repeated by the player is
 * safe: the server answers {@code ALREADY_CLAIMED}, and Join Game then opens the agency.) Any
 * exchange that breaks off also drops the connection, so a late reply to it can never be mistaken
 * for the answer to a later request. Read-only calls added later may reconnect and retry once on
 * such a failure; action submits must never be retried automatically.</p>
 *
 * @author Mario Koehler
 */
public final class NetworkGateway implements ServerGateway {

    /** Reads turn reports as the server saves them: strict, unknown fields are errors. */
    private static final ObjectMapper REPORTS = new ObjectMapper();

    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "server-gateway");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Closes dropped connections off the worker thread. KryoNet's {@code Client.stop()} sometimes
     * takes several seconds on Windows (seen up to ~11 s, 2026-10-04); closing inline would hold
     * back whatever the worker does next by that long. It was the likely cause of the "weird
     * timeouts" the earlier connection-per-call design ran into.
     */
    private static final ExecutorService CLOSER = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "server-gateway-close");
        thread.setDaemon(true);
        return thread;
    });

    /** The session's connection; only the worker thread touches it. {@code null} when logged out. */
    private NetworkClient client;
    /** Who the connection is logged in as; only the worker thread touches it. */
    private Credentials session;

    /**
     * Creates the gateway and its background thread. No connection is opened until {@link #logIn}.
     */
    public NetworkGateway() {
    }

    @Override
    public void logIn(Credentials credentials, Consumer<LoginResult> done) {
        run(() -> {
            // Pressing Connect is always a fresh login, even with the same credentials.
            dropConnection();
            Optional<LoginResult> refusal = openSession(credentials);
            if (refusal.isPresent()) {
                return refusal.get();
            }
            try {
                GameInfoResponse info = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class);
                if (!info.success()) {
                    return LoginResult.failure(LoginResult.Kind.FAILED, info.message());
                }
                GameInfo gameInfo = new GameInfo(info, System.currentTimeMillis());
                return LoginResult.fromFetch(client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class))
                    .withGameInfo(gameInfo);
            } catch (IOException | RuntimeException e) {
                dropConnection();
                return LoginResult.failure(LoginResult.Kind.FAILED, e.getMessage());
            }
        }, message -> LoginResult.failure(LoginResult.Kind.FAILED, message), done);
    }

    @Override
    public void claimSlot(Credentials credentials, Archetype archetype, String agencyName, String directorName,
                          Consumer<ClaimResult> done) {
        run(() -> {
            Optional<LoginResult> refusal = ensureSession(credentials);
            if (refusal.isPresent()) {
                return claimFailure(refusal.get());
            }
            try {
                ClaimSlotResponse claim = client.sendAndReceive(
                    new ClaimSlotRequest(archetype, agencyName, directorName), ClaimSlotResponse.class);
                if (!claim.success()) {
                    return ClaimResult.refused(claim);
                }
                GameInfoResponse info = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class);
                FetchStateResponse fetch = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
                if (!info.success() || !fetch.success()) {
                    return ClaimResult.failure(ClaimResult.Kind.FAILED,
                        info.success() ? fetch.message() : info.message());
                }
                return ClaimResult.joined(fetch.state(), new GameInfo(info, System.currentTimeMillis()));
            } catch (IOException | RuntimeException e) {
                dropConnection();
                return ClaimResult.failure(ClaimResult.Kind.FAILED, e.getMessage());
            }
        }, message -> ClaimResult.failure(ClaimResult.Kind.FAILED, message), done);
    }

    @Override
    public void submit(Credentials credentials, PlayerAction action, Consumer<ServerResult> done) {
        run(() -> {
            Optional<LoginResult> refusal = ensureSession(credentials);
            if (refusal.isPresent()) {
                return sessionFailure(refusal.get());
            }
            SubmitActionResponse response;
            try {
                response = client.sendAndReceive(new SubmitActionRequest(action), SubmitActionResponse.class);
            } catch (IOException | RuntimeException e) {
                dropConnection();
                return ServerResult.of(ServerResult.Kind.FAILED, e.getMessage());
            }
            if (!response.success()) {
                return ServerResult.of(ServerResult.Kind.REFUSED, response.message());
            }
            ServerResult fetched = readWithRetry(credentials);
            // The action is applied either way; without a fresh state the screen refreshes later.
            return fetched.kind() == ServerResult.Kind.OK
                ? new ServerResult(ServerResult.Kind.OK, response.message(), fetched.state(), fetched.gameInfo())
                : ServerResult.of(ServerResult.Kind.OK, response.message());
        }, message -> ServerResult.of(ServerResult.Kind.FAILED, message), done);
    }

    @Override
    public void refresh(Credentials credentials, Consumer<ServerResult> done) {
        run(() -> readWithRetry(credentials), message -> ServerResult.of(ServerResult.Kind.FAILED, message), done);
    }

    @Override
    public void fetchTurnReport(Credentials credentials, long turn, Consumer<TurnReportResult> done) {
        run(() -> {
            TurnReportResult first = fetchReportOnce(credentials, turn);
            return first.kind() == TurnReportResult.Kind.FAILED && first.report() == null && client == null
                ? fetchReportOnce(credentials, turn) : first;
        }, message -> new TurnReportResult(TurnReportResult.Kind.FAILED, message, null), done);
    }

    private TurnReportResult fetchReportOnce(Credentials credentials, long turn) {
        Optional<LoginResult> refusal = ensureSession(credentials);
        if (refusal.isPresent()) {
            return new TurnReportResult(TurnReportResult.Kind.FAILED, refusal.get().message(), null);
        }
        try {
            TurnReportResponse response = client.sendAndReceive(new TurnReportRequest(turn), TurnReportResponse.class);
            if (!response.success()) {
                return new TurnReportResult(TurnReportResult.Kind.FAILED, response.message(), null);
            }
            if (!response.found()) {
                return new TurnReportResult(TurnReportResult.Kind.NONE, "", null);
            }
            return new TurnReportResult(TurnReportResult.Kind.FOUND, "", REPORTS.readValue(response.json(),
                TurnReport.class));
        } catch (IOException | RuntimeException e) {
            dropConnection();
            return new TurnReportResult(TurnReportResult.Kind.FAILED, e.getMessage(), null);
        }
    }

    @Override
    public void logOut() {
        worker.execute(this::dropConnection);
    }

    @Override
    public void dispose() {
        worker.execute(this::dropConnection);
        worker.shutdown();
    }

    /**
     * Makes sure the connection is up and logged in as these credentials, reconnecting if it has
     * dropped or belongs to someone else.
     *
     * @param credentials who and where
     * @return the login's refusal, or empty if the session is ready
     */
    private Optional<LoginResult> ensureSession(Credentials credentials) {
        if (client != null && client.isConnected() && credentials.equals(session)) {
            return Optional.empty();
        }
        dropConnection();
        return openSession(credentials);
    }

    /**
     * Opens a connection and logs in. On success it becomes the session's connection.
     *
     * @param credentials who and where
     * @return the refusal, or empty on success
     */
    private Optional<LoginResult> openSession(Credentials credentials) {
        NetworkClient fresh = new NetworkClient();
        try {
            fresh.connect(credentials.address().host(), credentials.address().port());
        } catch (IOException | RuntimeException e) {
            closeLater(fresh);
            return Optional.of(LoginResult.failure(LoginResult.Kind.UNREACHABLE, e.getMessage()));
        }
        try {
            HandshakeResponse response = fresh.sendAndReceive(
                new HandshakeRequest(AppVersion.getVersion(), credentials.login(), credentials.password()),
                HandshakeResponse.class);
            Optional<LoginResult> refusal = LoginResult.fromHandshake(response, AppVersion.getVersion());
            if (refusal.isPresent()) {
                closeLater(fresh);
                return refusal;
            }
        } catch (IOException | RuntimeException e) {
            closeLater(fresh);
            return Optional.of(LoginResult.failure(LoginResult.Kind.FAILED, e.getMessage()));
        }
        client = fresh;
        session = credentials;
        return Optional.empty();
    }

    /**
     * Fetches the game info and the state; on a broken connection, reconnects and tries once more.
     *
     * @param credentials who and where
     * @return the result
     */
    private ServerResult readWithRetry(Credentials credentials) {
        ServerResult first = readOnce(credentials);
        if (first.kind() != ServerResult.Kind.FAILED) {
            return first;
        }
        return readOnce(credentials);
    }

    private ServerResult readOnce(Credentials credentials) {
        Optional<LoginResult> refusal = ensureSession(credentials);
        if (refusal.isPresent()) {
            return sessionFailure(refusal.get());
        }
        try {
            GameInfoResponse info = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class);
            FetchStateResponse fetch = client.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
            if (!info.success() || !fetch.success()) {
                return ServerResult.of(ServerResult.Kind.REFUSED, info.success() ? fetch.message() : info.message());
            }
            return new ServerResult(ServerResult.Kind.OK, "", fetch.state(),
                new GameInfo(info, System.currentTimeMillis()));
        } catch (IOException | RuntimeException e) {
            dropConnection();
            return ServerResult.of(ServerResult.Kind.FAILED, e.getMessage());
        }
    }

    private static ServerResult sessionFailure(LoginResult refusal) {
        return switch (refusal.kind()) {
            case UNREACHABLE -> ServerResult.of(ServerResult.Kind.UNREACHABLE, refusal.message());
            case FAILED -> ServerResult.of(ServerResult.Kind.FAILED, refusal.message());
            case VERSION_MISMATCH -> ServerResult.of(ServerResult.Kind.LOGIN_FAILED, "The server is now on version "
                + refusal.serverVersion() + ". Update the client and connect again.");
            default -> ServerResult.of(ServerResult.Kind.LOGIN_FAILED, refusal.message());
        };
    }

    private static ClaimResult claimFailure(LoginResult refusal) {
        return switch (refusal.kind()) {
            case UNREACHABLE -> ClaimResult.failure(ClaimResult.Kind.UNREACHABLE, refusal.message());
            case FAILED -> ClaimResult.failure(ClaimResult.Kind.FAILED, refusal.message());
            case VERSION_MISMATCH -> ClaimResult.failure(ClaimResult.Kind.LOGIN_FAILED, "The server is now on version "
                + refusal.serverVersion() + ". Update the client and connect again.");
            default -> ClaimResult.failure(ClaimResult.Kind.LOGIN_FAILED, refusal.message());
        };
    }

    private void dropConnection() {
        if (client != null) {
            closeLater(client);
            client = null;
            session = null;
        }
    }

    private static void closeLater(NetworkClient connection) {
        CLOSER.execute(connection::close);
    }

    /**
     * Runs work on the background thread and always delivers a result, even if the work throws
     * something unexpected: a screen waiting on a call must never be left disabled for good.
     */
    private <T> void run(Supplier<T> work, Function<String, T> ifCrashed, Consumer<T> done) {
        worker.execute(() -> {
            T result;
            try {
                result = work.get();
            } catch (Throwable e) {
                Gdx.app.error("NetworkGateway", "Server call failed unexpectedly", e);
                dropConnection();
                result = ifCrashed.apply(e.toString());
            }
            T delivered = result;
            Gdx.app.postRunnable(() -> done.accept(delivered));
        });
    }
}
