package de.mkoehler.launchwindow.server;

import de.mkoehler.launchwindow.rules.ReactionPrinter;
import de.mkoehler.launchwindow.model.FrontPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkConstants;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.ArchiveRequest;
import de.mkoehler.launchwindow.server.schedule.GameResult;
import de.mkoehler.launchwindow.net.messages.ArchiveResponse;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;
import de.mkoehler.launchwindow.net.messages.FrontPagesRequest;
import de.mkoehler.launchwindow.net.messages.FrontPagesResponse;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import de.mkoehler.launchwindow.net.messages.SubmitStatementResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.TurnReportRequest;
import de.mkoehler.launchwindow.net.messages.TurnReportResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.PublishPreviewRequest;
import de.mkoehler.launchwindow.net.messages.PublishPreviewResponse;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowRequest;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowResponse;
import de.mkoehler.launchwindow.net.messages.RivalSummary;
import de.mkoehler.launchwindow.net.messages.RivalsRequest;
import de.mkoehler.launchwindow.net.messages.RivalsResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.FindingPublications;
import de.mkoehler.launchwindow.rules.Findings;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.Personality;
import de.mkoehler.launchwindow.rules.TurnReport;
import de.mkoehler.launchwindow.rules.WinCondition;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.accounts.AuthResult;
import de.mkoehler.launchwindow.server.schedule.FrontPageStore;
import de.mkoehler.launchwindow.server.schedule.ReactionStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    /**
     * The largest gzipped turn report sent: a little under Kryo's object buffer, which a bigger
     * message would overflow and drop the connection.
     */
    static final int MAX_REPORT_BYTES = NetworkConstants.OBJECT_BUFFER_SIZE - 1_024;

    private final AccountStore accountStore;
    private final Path dataRoot;
    private final ObjectMapper mapper;
    private final ActionContext context;
    private final GameMeta meta;
    private final GameInfoService gameInfo;
    private final SecureRandom actionSeeds = new SecureRandom();
    private final Map<Integer, String> connectionAccountIds = new HashMap<>();
    private TurnForcer turnForcer;

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
     * @param gameInfo     builds the game info (gap G1)
     */
    public RequestDispatcher(AccountStore accountStore, Path dataRoot, ObjectMapper mapper, ActionContext context,
            GameMeta meta, GameInfoService gameInfo) {
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore must not be null");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.meta = Objects.requireNonNull(meta, "meta must not be null");
        this.gameInfo = Objects.requireNonNull(gameInfo, "gameInfo must not be null");
    }

    /**
     * Returns the connections that have logged in, for notices sent to everyone online.
     *
     * @return their ids
     */
    public java.util.Set<Integer> loggedInConnections() {
        return java.util.Set.copyOf(connectionAccountIds.keySet());
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
        if (message instanceof TurnReportRequest request) {
            return new DispatchResult(handleTurnReport(connectionId, request), false);
        }
        if (message instanceof ResolveTurnNowRequest request) {
            return new DispatchResult(handleResolveTurnNow(connectionId, request), false);
        }
        if (message instanceof RivalsRequest request) {
            return new DispatchResult(handleRivals(connectionId, request), false);
        }
        if (message instanceof GameInfoRequest request) {
            return new DispatchResult(handleGameInfo(connectionId, request), false);
        }
        if (message instanceof FrontPagesRequest request) {
            return new DispatchResult(handleFrontPages(connectionId, request), false);
        }
        if (message instanceof ArchiveRequest request) {
            return new DispatchResult(handleArchive(connectionId, request), false);
        }
        if (message instanceof PublishPreviewRequest request) {
            return new DispatchResult(handlePublishPreview(connectionId, request), false);
        }
        if (message instanceof SubmitStatementRequest request) {
            return new DispatchResult(handleSubmitStatement(connectionId, request), false);
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

        AuthResult authResult = accountStore.login(request.login(), request.password());
        if (!authResult.success()) {
            return HandshakeResponse.reject(authResult.message(), serverVersion);
        }
        connectionAccountIds.put(connectionId, authResult.account().getId());
        return HandshakeResponse.accept(authResult.message(), serverVersion);
    }

    /**
     * Handles a {@link ClaimSlotRequest}: creates the account's slot in this game -
     * the archetype, a unique agency name and director name, and the starting state (the
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
            return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.NOT_AUTHENTICATED, "Not authenticated - send a HandshakeRequest first.");
        }
        if (request.archetype() == null || isBlank(request.agencyName()) || isBlank(request.directorName())) {
            return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.MISSING_FIELDS, "An archetype, an agency name and a director name are all required.");
        }
        if (request.agencyName().strip().length() > ClaimSlotRequest.MAX_NAME_LENGTH
            || request.directorName().strip().length() > ClaimSlotRequest.MAX_NAME_LENGTH) {
            return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.NAME_TOO_LONG,
                "Names can be at most " + ClaimSlotRequest.MAX_NAME_LENGTH + " characters long.");
        }
        try {
            PlayerSnapshotStore store = new PlayerSnapshotStore(playerRoot(accountId), mapper);
            if (store.load().isPresent()) {
                return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.ALREADY_CLAIMED, "This account already has a slot in this game.");
            }
            String agencyName = request.agencyName().strip();
            String directorName = request.directorName().strip();
            Map<String, Integer> publicFindings = new LinkedHashMap<>();
            for (PlayerSnapshot other : everyPlayer()) {
                // A late joiner knows what was published before it joined (design.md section 2.13).
                other.getPublicFindings().forEach((goal, turn) -> publicFindings.merge(goal, turn, Math::min));
                if (agencyName.equalsIgnoreCase(other.getAgencyName())) {
                    return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.AGENCY_NAME_TAKEN, "The agency name \"" + agencyName + "\" is already taken.");
                }
                if (directorName.equalsIgnoreCase(other.getDirectorName())) {
                    return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN, "The director name \"" + directorName + "\" is already taken.");
                }
            }
            BudgetSchedule schedule = context.schedule();
            PlayerSnapshot start = new PlayerSnapshot(agencyName, request.archetype(), schedule.initialFunds(),
                schedule.initialPrestige(), schedule.initialFundingRate(), 1);
            start.setDirectorName(directorName);
            start.setPublicFindings(publicFindings);
            HiringPool.refreshCandidates(start, new SplittableRandom(GameSeeds.forNewPlayer(meta.getGameSeed(),
                accountId)));
            store.save(0, start, null);
            return ClaimSlotResponse.success("Welcome, Director " + directorName + " of " + agencyName + ".");
        } catch (IOException e) {
            return ClaimSlotResponse.failure(ClaimSlotResponse.Reason.SERVER_ERROR, "Failed to create the slot: " + e.getMessage());
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
            // Meant for a turn that resolved while it was on its way: it would land in the next
            // turn, against a state the player hasn't seen.
            if (request.gameTurn() > 0 && request.gameTurn() != gameInfo.openGameTurn()) {
                return SubmitActionResponse.failure(SubmitActionRequest.TURN_RESOLVED);
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
            // Hidden traits, affinities and unseen rapport never leave the server (design.md 2.2(d)).
            return FetchStateResponse.success(Personality.forClient(state, mapper));
        } catch (IOException e) {
            return FetchStateResponse.failure("Failed to fetch state: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link TurnReportRequest}: one of the connection's own player's turn reports, as
     * the JSON it's saved as. The account comes from the connection, never from the request.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response; a missing report is a normal "none"
     */
    public TurnReportResponse handleTurnReport(int connectionId, TurnReportRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return TurnReportResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return TurnReportResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            Optional<TurnReport> report = playerStateService(accountId).turnReport(request.turn());
            if (report.isEmpty()) {
                return TurnReportResponse.none();
            }
            TurnReportResponse response = TurnReportResponse.of(report.get().gameTurn(),
                mapper.writeValueAsString(report.get()));
            if (response.gzippedJson().length > MAX_REPORT_BYTES) {
                return TurnReportResponse.failure("This turn's report is too large to send.");
            }
            return response;
        } catch (IOException e) {
            return TurnReportResponse.failure("Failed to read the turn report: " + e.getMessage());
        }
    }

    /**
     * Resolves the open turn on demand: the scheduler's own pass, for dev mode.
     */
    @FunctionalInterface
    public interface TurnForcer {

        /**
         * Resolves the open turn now.
         *
         * @return the game turn that was resolved
         * @throws IOException if the game's files can't be read or written
         */
        long resolveNow() throws IOException;
    }

    /**
     * Sets what resolves a turn on demand. Without one, every {@link ResolveTurnNowRequest} fails.
     *
     * @param turnForcer the scheduler's resolve-now
     */
    public void setTurnForcer(TurnForcer turnForcer) {
        this.turnForcer = turnForcer;
    }

    /**
     * Handles a {@link ResolveTurnNowRequest}: resolves the open turn for every player at once,
     * for play testing. Refused unless the server runs in dev mode. Runs on the thread that also
     * runs the scheduler, so the two never resolve at the same time.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public ResolveTurnNowResponse handleResolveTurnNow(int connectionId, ResolveTurnNowRequest request) {
        if (connectionAccountIds.get(connectionId) == null) {
            return ResolveTurnNowResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        if (!meta.isDevMode()) {
            return ResolveTurnNowResponse.failure("DEV_MODE_REQUIRED");
        }
        if (turnForcer == null) {
            return ResolveTurnNowResponse.failure("This server can't resolve a turn on demand.");
        }
        try {
            return ResolveTurnNowResponse.resolved(turnForcer.resolveNow());
        } catch (IOException | RuntimeException e) {
            return ResolveTurnNowResponse.failure("Failed to resolve the turn: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link FrontPagesRequest}: every front page printed in the game so far
     * ({@code ui-spec.md} &sect;4.18, G14). The pages carry agency names only.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public FrontPagesResponse handleFrontPages(int connectionId, FrontPagesRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return FrontPagesResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return FrontPagesResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            return FrontPagesResponse.of(FrontPageStore.load(dataRoot, mapper),
                ReactionStore.of(dataRoot, mapper, gameInfo.openGameTurn(), rivalKey(accountId)));
        } catch (IOException | RuntimeException e) {
            return FrontPagesResponse.failure("Failed to read the front pages: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link SubmitStatementRequest} ({@code ui-spec.md} &sect;4.18.1, G17): keeps a statement
     * about a rival's First for the open turn, or withdraws it. Only a rival may give one (not an agency
     * that made the First, nor one that joined after it was printed), only in the turn after the First,
     * and the quote is cleaned and checked: at most {@link FrontPage.Statement#MAX_QUOTE_LENGTH}
     * characters, all {@link FrontPage.Statement#ALLOWED_CHARACTERS}. A No comment keeps no quote.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response, with the player's statements for the open turn
     */
    public SubmitStatementResponse handleSubmitStatement(int connectionId, SubmitStatementRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return SubmitStatementResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return SubmitStatementResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            long open = gameInfo.openGameTurn();
            if (request.gameTurn() != open) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.TURN_RESOLVED);
            }
            Optional<FrontPage> page = FrontPageStore.load(dataRoot, mapper).stream()
                .filter(p -> p.id().equals(request.pageId()) && p.firstPage()).findFirst();
            if (page.isEmpty()) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.UNKNOWN_PAGE);
            }
            if (page.get().gameTurn() != open - 1) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.REACTION_WINDOW_CLOSED);
            }
            PlayerSnapshot state = playerStateService(accountId).currentState();
            if (!ReactionPrinter.mayReact(page.get(), Math.toIntExact(open), state.getAgencyName(),
                    ReactionPrinter.firstTurn(open, state.getSeason()))) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.NOT_A_RIVAL);
            }
            String key = rivalKey(accountId);
            if (request.stance() == null) {
                return SubmitStatementResponse.success(ReactionStore.remove(dataRoot, mapper, open, key,
                    request.pageId()));
            }
            if (!request.stance().givable()) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.INVALID_STANCE);
            }
            String quote = request.stance().quotable() ? FrontPage.Statement.clean(request.quote()) : "";
            if (quote.length() > FrontPage.Statement.MAX_QUOTE_LENGTH) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.QUOTE_TOO_LONG);
            }
            if (!FrontPage.Statement.allowed(quote)) {
                return SubmitStatementResponse.failure(SubmitStatementResponse.QUOTE_BAD_CHARACTERS);
            }
            return SubmitStatementResponse.success(ReactionStore.put(dataRoot, mapper, open, key,
                new FrontPage.Statement(request.pageId(), request.stance(), quote)));
        } catch (ReactionStore.SealedException e) {
            // The turn is resolving and has read its statements: this one would never be printed.
            return SubmitStatementResponse.failure(SubmitStatementResponse.TURN_RESOLVED);
        } catch (IOException | RuntimeException e) {
            return SubmitStatementResponse.failure("Failed to keep the statement: " + e.getMessage());
        }
    }

    private String rivalKey(String accountId) {
        return GameSeeds.rivalKey(meta.getGameSeed(), accountId);
    }

    /**
     * Handles an {@link ArchiveRequest} ({@code design.md} &sect;2.12, G15): for each goal asked about,
     * the first turn any player held it (achieved or credited through a cascade), read from every
     * player's latest resolved snapshot, and whether the asking player was among the first. Once the race
     * is decided ({@code game-result.json}), every goal nobody reached by then opens on the deciding turn,
     * marked {@code atTheEnd}. A finding's page ({@code design.md} &sect;2.13) opens only for the asking
     * player while it knows the finding: from the turn it reached the goal or the finding was published,
     * whichever came first. Only the goals and turns leave the server, never who reached them.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public ArchiveResponse handleArchive(int connectionId, ArchiveRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return ArchiveResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return ArchiveResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            List<String> goals = request.goalIds() == null ? List.of() : List.copyOf(request.goalIds());
            if (meta.isDevMode() && meta.isDevArchiveOpen()) {
                // Every page open, for reviewing them (game-meta.json's devArchiveOpen): nothing saved changes.
                return ArchiveResponse.of(goals.stream().map(goal -> new ArchiveUnlock(goal, 1, true)).toList());
            }
            Map<String, Integer> first = new LinkedHashMap<>();
            Map<String, Integer> mine = Map.of();
            PlayerSnapshot me = null;
            try (Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
                for (Path playerDir : listing.filter(Files::isDirectory).sorted().toList()) {
                    Optional<PlayerSnapshot> snapshot = new PlayerSnapshotStore(playerDir, mapper).load();
                    if (snapshot.isEmpty()) {
                        continue;
                    }
                    Map<String, Integer> times = WinCondition.goalTimes(snapshot.get(), goals, context.catalog());
                    times.forEach((goal, turn) -> first.merge(goal, turn, Math::min));
                    if (playerDir.getFileName().toString().equals(accountId)) {
                        mine = times;
                        me = snapshot.get();
                    }
                }
            }
            // Once the race is decided, every page nobody had reached opens for everyone, on that turn
            // (the user's idea, 2026-10-15): so everyone gets to see the whole archive.
            Path resultFile = dataRoot.resolve(GameResult.FILE_NAME);
            Integer decided = Files.exists(resultFile)
                ? (int) mapper.readValue(resultFile.toFile(), GameResult.class).decidedOnTurn() : null;
            List<ArchiveUnlock> unlocks = new ArrayList<>();
            for (String goal : goals) {
                Integer turn = first.get(goal);
                if (context.catalog().finding(goal).isPresent()) {
                    // A finding's page opens only for the agencies that know it: its discoverers, and
                    // everyone once it's published (design.md section 2.13).
                    Integer own = mine.get(goal);
                    Integer published = me == null ? null : me.getPublicFindings().get(goal);
                    turn = own == null ? published : published == null ? own : Math.min(own, published);
                    if (decided != null && (turn == null || turn > decided)) {
                        unlocks.add(new ArchiveUnlock(goal, decided, false, true));
                    } else if (turn != null) {
                        unlocks.add(new ArchiveUnlock(goal, turn, turn.equals(own)));
                    }
                    continue;
                }
                if (decided != null && (turn == null || turn > decided)) {
                    unlocks.add(new ArchiveUnlock(goal, decided, false, true));
                } else if (turn != null) {
                    unlocks.add(new ArchiveUnlock(goal, turn, turn.equals(mine.get(goal))));
                }
            }
            return ArchiveResponse.of(unlocks);
        } catch (IOException | RuntimeException e) {
            return ArchiveResponse.failure("Failed to read the archive: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link PublishPreviewRequest} ({@code design.md} &sect;2.13): what publishing a finding
     * now would pay the asking player, from every player's latest resolved snapshot (what a rival
     * knows only changes when a turn resolves). Only the amount and the counts leave the server, never
     * which rivals know it.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public PublishPreviewResponse handlePublishPreview(int connectionId, PublishPreviewRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return PublishPreviewResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return PublishPreviewResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            if (request.goalId() == null || context.catalog().finding(request.goalId()).isEmpty()) {
                return PublishPreviewResponse.failure(Findings.PublishOutcome.NOT_A_FINDING.name());
            }
            Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
            try (Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
                for (Path playerDir : listing.filter(Files::isDirectory).sorted().toList()) {
                    new PlayerSnapshotStore(playerDir, mapper).load()
                        .ifPresent(state -> agencies.put(playerDir.getFileName().toString(), state));
                }
            }
            FindingPublications.Preview preview = FindingPublications.preview(accountId, request.goalId(), agencies,
                context.catalog());
            return PublishPreviewResponse.of(preview.prestige(), preview.helped(), preview.rivals());
        } catch (IOException | RuntimeException e) {
            return PublishPreviewResponse.failure("Failed to preview the publication: " + e.getMessage());
        }
    }

    /**
     * Handles a {@link RivalsRequest}: every claimed player's summary for the race, read from
     * each one's latest resolved snapshot. Only the agency, director, archetype and the declared
     * goals' turns leave the server.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public RivalsResponse handleRivals(int connectionId, RivalsRequest request) {
        String accountId = connectionAccountIds.get(connectionId);
        if (accountId == null) {
            return RivalsResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            if (!hasSlot(accountId)) {
                return RivalsResponse.failure(ClaimSlotRequest.SLOT_CLAIM_REQUIRED);
            }
            List<String> declared = meta.getDeclaredGoalIds();
            List<RivalSummary> rivals = new ArrayList<>();
            try (Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
                for (Path playerDir : listing.filter(Files::isDirectory).sorted().toList()) {
                    Optional<PlayerSnapshot> snapshot = new PlayerSnapshotStore(playerDir, mapper).load();
                    if (snapshot.isEmpty()) {
                        continue;
                    }
                    PlayerSnapshot player = snapshot.get();
                    rivals.add(RivalSummary.of(player.getAgencyName(), player.getDirectorName(),
                        player.getArchetype(), playerDir.getFileName().toString().equals(accountId),
                        WinCondition.goalTimes(player, declared, context.catalog()),
                        GameSeeds.rivalKey(meta.getGameSeed(), playerDir.getFileName().toString())));
                }
            }
            return RivalsResponse.of(rivals);
        } catch (IOException | RuntimeException e) {
            return RivalsResponse.failure("Failed to read the race: " + e.getMessage());
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
    /**
     * Handles a {@link GameInfoRequest}: the game's shared facts. Needs an accepted handshake but
     * no slot, so the Join Game screen can show them before the player has joined.
     *
     * @param connectionId the connection the request arrived on
     * @param request      the request
     * @return the response
     */
    public GameInfoResponse handleGameInfo(int connectionId, GameInfoRequest request) {
        if (connectionAccountIds.get(connectionId) == null) {
            return GameInfoResponse.failure("Not authenticated - send a HandshakeRequest first.");
        }
        try {
            return gameInfo.info();
        } catch (IOException | RuntimeException e) {
            return GameInfoResponse.failure("Failed to read the game info: " + e.getMessage());
        }
    }

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
