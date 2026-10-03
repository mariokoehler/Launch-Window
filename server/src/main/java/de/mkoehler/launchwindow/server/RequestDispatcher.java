package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.accounts.AuthResult;
import de.mkoehler.launchwindow.server.schedule.GameMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SplittableRandom;
import java.util.stream.Stream;

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
 * and {@link #handleFetchState} reject anything sent before that, and answer
 * {@link ClaimSlotRequest#SLOT_CLAIM_REQUIRED} until the account has claimed its slot.</p>
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
    private final ActionContext context;
    private final GameMeta meta;
    private final SecureRandom actionSeeds = new SecureRandom();
    private final Map<Integer, String> connectionAccountIds = new HashMap<>();

    /**
     * Creates a new dispatcher.
     *
     * @param accountStore the account store to authenticate against
     * @param dataRoot     the server's data root directory - each player's
     *                     own data lives under {@code dataRoot/players/<accountId>/}
     * @param mapper       the Jackson mapper used by every underlying
     *                     persistence store
     * @param context      the game's content pack and budget schedule
     * @param meta         the game's configuration (seed, dev mode)
     */
    public RequestDispatcher(AccountStore accountStore, Path dataRoot, ObjectMapper mapper, ActionContext context,
            GameMeta meta) {
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.meta = Objects.requireNonNull(meta, "meta must not be null");
    }

    /**
     * Dispatches one received message by type and reports whether the
     * connection should be closed after the response is sent.
     *
     * @param connectionId the connection the message arrived on
     * @param message      the message, one of {@link HandshakeRequest},
     *                     {@link ClaimSlotRequest}, {@link SubmitActionRequest} or
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
        if (message instanceof ClaimSlotRequest request) {
            return new DispatchResult(handleClaimSlot(connectionId, request), false);
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
     * authenticates the connection for every subsequent request on it. An
     * account without a slot in this game must then send a {@link ClaimSlotRequest}.
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
        connectionAccountIds.put(connectionId, authResult.account().getId());
        return HandshakeResponse.accept(authResult.message(), serverVersion);
    }

    /**
     * Handles a {@link ClaimSlotRequest}: creates the account's slot in this game -
     * the archetype, a unique agency name and callsign, and the starting state (the
     * budget schedule's starting values, every building at level 1, empty rosters,
     * and fresh candidate pools; user decision, 2026-10-03). A late joiner starts at
     * their own season 1 ({@code design.md} &sect;3.8).
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public ClaimSlotResponse handleClaimSlot(int connectionId, ClaimSlotRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return ClaimSlotResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        if (request.archetype() == null || isBlank(request.agencyName()) || isBlank(request.callsign())) {
            return ClaimSlotResponse.failure("An archetype, an agency name and a callsign are all required.");
        }
        try {
            PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot(accountId), mapper);
            if (store.load().isPresent()) {
                return ClaimSlotResponse.failure("This account already has a slot in this game.");
            }
            String agencyName = request.agencyName().strip();
            String callsign = request.callsign().strip();
            for (PlayerSnapshot other : everyPlayer()) {
                if (agencyName.equalsIgnoreCase(other.getAgencyName())) {
                    return ClaimSlotResponse.failure("The agency name \"" + agencyName + "\" is already taken.");
                }
                if (callsign.equalsIgnoreCase(other.getCallsign())) {
                    return ClaimSlotResponse.failure("The callsign \"" + callsign + "\" is already taken.");
                }
            }
            BudgetSchedule schedule = context.schedule();
            PlayerSnapshot start = new PlayerSnapshot(agencyName, request.archetype(), schedule.initialFunds(),
                schedule.initialPrestige(), schedule.initialFundingRate(), 1);
            start.setCallsign(callsign);
            HiringPool.refreshCandidates(start, new SplittableRandom(GameSeeds.forNewPlayer(meta.getGameSeed(),
                accountId)));
            store.save(0, start, null);
            return ClaimSlotResponse.success("Welcome, " + callsign + " of " + agencyName + ".");
        } catch (IOException e) {
            return ClaimSlotResponse.failure("Failed to create the slot: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link SubmitActionRequest}: stamps the action with a server-chosen
     * seed (so a client can't pick its own rolls), refuses development cheats outside
     * dev mode, and validates the action against the player's current preview. Only an
     * action that takes effect is journaled; the response names the rules outcome.
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
        PlayerAction action = request.action();
        if (action == null) {
            return SubmitActionResponse.failure("No action given.");
        }
        if (action instanceof SetFundsAction && !meta.isDevMode()) {
            return SubmitActionResponse.failure("Setting funds is a development cheat; this server isn't in dev mode.");
        }
        try {
            if (!hasSlot(accountId)) {
                return SubmitActionResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            action.setSeed(actionSeeds.nextLong());
            ActionResult result = playerStateService(accountId).submitAction(action);
            return result.applied() ? SubmitActionResponse.success(result.outcome())
                : SubmitActionResponse.failure(result.outcome());
        } catch (IOException e) {
            return SubmitActionResponse.failure("Failed to submit action: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link FetchStateRequest} on an already-authenticated
     * connection: the player's preview, with today's actions applied.
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
            if (!hasSlot(accountId)) {
                return FetchStateResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
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

    private boolean hasSlot(String accountId) throws IOException {
        return new PlayerSnapshotStore(playerRoot(accountId), mapper).latestTurnNumber().isPresent();
    }

    /**
     * Loads every claimed player's latest snapshot, for the uniqueness checks.
     *
     * @return the snapshots
     * @throws IOException if a player's files can't be read
     */
    private List<PlayerSnapshot> everyPlayer() throws IOException {
        Path players = dataRoot.resolve("players");
        List<PlayerSnapshot> result = new ArrayList<>();
        if (!Files.exists(players)) {
            return result;
        }
        try (Stream<Path> listing = Files.list(players)) {
            for (Path playerDir : listing.filter(Files::isDirectory).toList()) {
                new PlayerSnapshotStore(playerDir, mapper).load().ifPresent(result::add);
            }
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Creates a state service for the given account.
     *
     * @param accountId the account to create it for
     * @return the state service
     */
    private PlayerStateService playerStateService(String accountId) {
        return new PlayerStateService(playerRoot(accountId), mapper, context);
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
