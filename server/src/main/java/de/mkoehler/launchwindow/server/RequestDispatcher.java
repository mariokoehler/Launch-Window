package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.accounts.AuthResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Turns the wire messages in {@code de.mkoehler.launchwindow.net.messages}
 * into calls against {@link AccountStore} and {@link PlayerStateService}, and
 * their results back into responses. Deliberately has no dependency on
 * KryoNet or libGDX, so it is testable without real sockets - the owner of
 * {@link de.mkoehler.launchwindow.net.NetworkServer} wires this class's
 * methods to that server's {@code Handler} callbacks.</p>
 *
 * <p><b>Threading contract:</b> not thread-safe on its own - every method
 * here must be called from the single thread that drains
 * {@code NetworkServer.poll(Handler)}, the same contract that class already
 * documents. {@link #connectionAccountIds} is a plain, unsynchronized map on
 * that assumption.</p>
 *
 * <p>A connection is only authenticated after a successful
 * {@link #handleHandshake(int, HandshakeRequest)} - {@link #handleSubmitAction}
 * and {@link #handleFetchState} reject anything sent before that.</p>
 *
 * <p>{@link #handle(int, Object)} is the single entry point the owner of
 * {@code NetworkServer} should actually call - it dispatches by message
 * type and reports whether the connection should be closed after the
 * response is sent (a rejected handshake), so that decision lives in one
 * place rather than being duplicated by every caller.</p>
 *
 * @author Mario Koehler
 */
public class RequestDispatcher {

    private final AccountStore accountStore;
    private final Path dataRoot;
    private final ObjectMapper mapper;
    private final Map<Integer, String> connectionAccountIds = new HashMap<>();

    /**
     * Creates a new dispatcher.
     *
     * @param accountStore the account store to authenticate against
     * @param dataRoot     the server's data root directory - each player's
     *                     own data lives under {@code dataRoot/players/<accountId>/}
     * @param mapper       the Jackson mapper used by every underlying
     *                     persistence store
     */
    public RequestDispatcher(AccountStore accountStore, Path dataRoot, ObjectMapper mapper) {
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Dispatches one received message by type and reports whether the
     * connection should be closed after the response is sent.
     *
     * @param connectionId the connection the message arrived on
     * @param message      the message, one of {@link HandshakeRequest},
     *                     {@link SubmitActionRequest} or
     *                     {@link FetchStateRequest}
     * @return the response to send, paired with whether to close the
     *     connection afterward
     * @throws IllegalArgumentException if {@code message} is none of the
     *                                  expected request types
     */
    public DispatchResult handle(int connectionId, Object message) {
        if (message instanceof HandshakeRequest request) {
            HandshakeResponse response = handleHandshake(connectionId, request);
            return new DispatchResult(response, !response.accepted());
        }
        if (message instanceof SubmitActionRequest request) {
            return new DispatchResult(handleSubmitAction(connectionId, request), false);
        }
        if (message instanceof FetchStateRequest request) {
            return new DispatchResult(handleFetchState(connectionId, request), false);
        }
        throw new IllegalArgumentException("Unexpected message type: " + message.getClass().getName());
    }

    /**
     * Handles a {@link HandshakeRequest}: checks the client's version first
     * - an incompatible client never reaches {@link AccountStore} at all -
     * then authenticates (or auto-creates) the account, and on success,
     * authenticates the connection for every subsequent request on it and
     * bootstraps a placeholder turn-0 snapshot if this account has none yet.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public HandshakeResponse handleHandshake(int connectionId, HandshakeRequest request) {
        String serverVersion = AppVersion.getVersion();
        if (!serverVersion.equals(request.clientVersion())) {
            return HandshakeResponse.reject(
                "Version mismatch: server is " + serverVersion + ", client is " + request.clientVersion() + ".",
                serverVersion);
        }

        AuthResult authResult = accountStore.login(request.login(), request.password(), request.displayName());
        if (!authResult.success()) {
            return HandshakeResponse.reject(authResult.message(), serverVersion);
        }

        String accountId = authResult.account().getId();
        try {
            bootstrapSnapshotIfMissing(accountId, request.displayName());
        } catch (IOException e) {
            return HandshakeResponse.reject("Failed to prepare player data: " + e.getMessage(), serverVersion);
        }
        connectionAccountIds.put(connectionId, accountId);
        return HandshakeResponse.accept(authResult.message(), serverVersion);
    }

    /**
     * Handles a {@link SubmitActionRequest} on an already-authenticated
     * connection.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public SubmitActionResponse handleSubmitAction(int connectionId, SubmitActionRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return SubmitActionResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            playerStateService(accountId).submitAction(request.action());
            return SubmitActionResponse.success("Accepted.");
        } catch (IOException e) {
            return SubmitActionResponse.failure("Failed to submit action: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link FetchStateRequest} on an already-authenticated
     * connection.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public FetchStateResponse handleFetchState(int connectionId, FetchStateRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return FetchStateResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            PlayerSnapshot state = playerStateService(accountId).currentState();
            return FetchStateResponse.success(state);
        } catch (IOException e) {
            return FetchStateResponse.failure("Failed to fetch state: " + e.getMessage());
        }
    }

    /**
     * Forgets a connection's authentication, so a later request on the same
     * connection id (KryoNet does not reuse ids within one server process)
     * cannot be mistaken for the player who previously held it.
     *
     * @param connectionId the connection that ended
     */
    public void handleDisconnect(int connectionId) {
        connectionAccountIds.remove(connectionId);
    }

    /**
     * Returns whether the given connection has completed a successful
     * handshake.
     *
     * @param connectionId the connection to check
     * @return {@code true} if authenticated
     */
    public boolean isAuthenticated(int connectionId) {
        return connectionAccountIds.containsKey(connectionId);
    }

    /**
     * Creates a placeholder turn-0 snapshot for an account that has none
     * yet. This is an M1 stand-in only - it is replaced by the real
     * slot-claim flow (archetype choice, agency name, persona) once that
     * exists.
     *
     * @param accountId   the account to bootstrap
     * @param displayName the account's display name, used as a placeholder
     *                    agency name
     * @throws IOException if the snapshot cannot be read or written
     */
    private void bootstrapSnapshotIfMissing(String accountId, String displayName) throws IOException {
        PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot(accountId), mapper);
        if (store.load().isEmpty()) {
            store.save(0, new PlayerSnapshot(displayName, Archetype.NASA, 0, 0, 0, 1), null);
        }
    }

    /**
     * Creates a state service for the given account.
     *
     * @param accountId the account to create it for
     * @return the state service
     */
    private PlayerStateService playerStateService(String accountId) {
        return new PlayerStateService(playerRoot(accountId), mapper);
    }

    /**
     * Returns the data directory for one account's player data.
     *
     * @param accountId the account
     * @return the account's data directory
     */
    private Path playerRoot(String accountId) {
        return dataRoot.resolve("players").resolve(accountId);
    }

    /**
     * The result of {@link #handle(int, Object)}: the response to send,
     * paired with whether the connection should be closed once it has been
     * sent.
     *
     * @param response        the response to send
     * @param closeConnection whether to close the connection after sending
     *                        it
     */
    public record DispatchResult(Object response, boolean closeConnection) {

        /**
         * Creates a result.
         *
         * @param response        the response to send
         * @param closeConnection whether to close the connection after
         *                        sending it
         */
        public DispatchResult {
            Objects.requireNonNull(response, "response must not be null");
        }
    }
}
