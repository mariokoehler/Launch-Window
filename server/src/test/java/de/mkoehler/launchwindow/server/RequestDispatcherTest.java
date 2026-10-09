package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowRequest;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameResult;
import de.mkoehler.launchwindow.server.schedule.SchedulerState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Tests for {@link RequestDispatcher}, entirely without real sockets -
 * see {@code ServerClientIntegrationTest} for the real-socket proof.</p>
 *
 * @author Mario Koehler
 */
class RequestDispatcherTest {

    private final ObjectMapper mapper = GameServer.createMapper();
    private final List<AccountStore> stores = new ArrayList<>();

    /**
     * Creates an account store and registers it for teardown.
     *
     * @param dataRoot the data root to back it with
     * @return the new store
     */
    private AccountStore createAccountStore(Path dataRoot) {
        AccountStore store = new AccountStore(dataRoot.resolve("accounts.json"));
        stores.add(store);
        return store;
    }

    /**
     * Closes every account store this test created, so no background
     * thread leaks across the test run.
     */
    @AfterEach
    void closeStores() {
        stores.forEach(AccountStore::close);
        stores.clear();
    }

    /**
     * Creates a dispatcher on the early-era pack.
     *
     * @param accountStore the account store
     * @param dataRoot     the data root
     * @param devMode      whether development cheats are accepted
     * @return the dispatcher
     */
    private RequestDispatcher dispatcher(AccountStore accountStore, Path dataRoot, boolean devMode) {
        GameMeta meta = ServerTestGames.meta(devMode);
        return new RequestDispatcher(accountStore, dataRoot, mapper, ServerTestGames.CONTEXT, meta,
            ServerTestGames.gameInfo(dataRoot, mapper, meta));
    }

    /**
     * Verifies that a version mismatch is rejected before the account
     * store is ever touched - no account gets created.
     */
    @Test
    void versionMismatchIsRejectedAndCreatesNoAccount(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);

        HandshakeResponse response =
            dispatcher.handleHandshake(1, new HandshakeRequest("not-a-real-version", "mario", "hunter2"));

        assertFalse(response.accepted());
        assertTrue(accountStore.findByLogin("mario").isEmpty());
        assertFalse(dispatcher.isAuthenticated(1));
    }

    /**
     * Verifies that the wrong password for an existing account is
     * rejected.
     */
    @Test
    void wrongPasswordIsRejected(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));

        HandshakeResponse response =
            dispatcher.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "wrong"));

        assertFalse(response.accepted());
        assertFalse(dispatcher.isAuthenticated(2));
    }

    /**
     * Verifies that submitting an action on a connection that never
     * completed a handshake is rejected.
     */
    @Test
    void submitBeforeHandshakeIsRejected(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, true);

        SubmitActionResponse response = dispatcher.handleSubmitAction(1, new SubmitActionRequest(new SetFundsAction(1_000)));

        assertFalse(response.success());
    }

    /**
     * Verifies that fetching state on a connection that never completed a
     * handshake is rejected.
     */
    @Test
    void fetchBeforeHandshakeIsRejected(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, true);

        FetchStateResponse response = dispatcher.handleFetchState(1, new FetchStateRequest());

        assertFalse(response.success());
    }

    /**
     * Verifies that an account with no slot gets the claim-required answer from fetch
     * and submit, and that claiming creates the starting state exactly once.
     */
    @Test
    void claimSlotCreatesTheStartingStateExactlyOnce(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));

        assertEquals(ClaimSlotRequest.SLOT_CLAIM_REQUIRED,
            dispatcher.handleFetchState(1, new FetchStateRequest()).message());
        assertEquals(ClaimSlotRequest.SLOT_CLAIM_REQUIRED,
            dispatcher.handleSubmitAction(1, new SubmitActionRequest(new OpenProgramAction("sputnik-1"))).message());

        assertTrue(dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief")).success());
        assertEquals(ClaimSlotResponse.Reason.ALREADY_CLAIMED,
            dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.NASA, "Other", "Other")).reason(),
            "a second claim is refused");

        PlayerSnapshot start = dispatcher.handleFetchState(1, new FetchStateRequest()).state();
        assertEquals(Archetype.SOVIET, start.getArchetype());
        assertEquals("OKB-1", start.getAgencyName());
        assertEquals("Chief", start.getDirectorName());
        assertEquals(25_000, start.getFunds());
        assertEquals(250, start.getPrestige());
        assertEquals(3_000, start.getFundingRatePerSeason());
        assertTrue(start.getAstronauts().isEmpty() && start.getSetSpecialists().isEmpty()
            && start.getMissionControlOfficers().isEmpty(), "rosters start empty");
        assertEquals(HiringPool.HIRING_BATCH_SIZE, start.getSetSpecialistCandidates().size());
    }

    /**
     * Verifies that agency names and director names are unique within the game, ignoring case.
     */
    @Test
    void agencyNamesAndDirectorNamesAreUnique(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "anna", "secret"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.NASA, "Hanseatic Space Agency", "Mario"));

        assertEquals(ClaimSlotResponse.Reason.AGENCY_NAME_TAKEN, dispatcher.handleClaimSlot(2,
            new ClaimSlotRequest(Archetype.NASA, "hanseatic space agency", "Anna")).reason());
        assertEquals(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN,
            dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", "MARIO")).reason());
        assertEquals(ClaimSlotResponse.Reason.MISSING_FIELDS,
            dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", " ")).reason());
        assertTrue(dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", "Anna")).success(),
            "the same archetype is fine");
    }

    /**
     * Verifies the whole authenticated flow with a real action: the response names the
     * rules outcome, the preview reflects it, and a rejected action changes nothing.
     */
    @Test
    void submitThenFetchReturnsTheSubmittedAction(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));

        SubmitActionResponse wrongAgency =
            dispatcher.handleSubmitAction(1, new SubmitActionRequest(new OpenProgramAction("mercury")));
        SubmitActionResponse opened =
            dispatcher.handleSubmitAction(1, new SubmitActionRequest(new OpenProgramAction("sputnik-1")));
        FetchStateResponse fetchResponse = dispatcher.handleFetchState(1, new FetchStateRequest());

        assertEquals(SubmitActionResponse.failure("WRONG_AGENCY"), wrongAgency);
        assertEquals(SubmitActionResponse.success("OPENED"), opened);
        assertEquals(25_000 - 876, fetchResponse.state().getFunds());
        assertEquals(List.of("sputnik-1"), fetchResponse.state().getOpenedProgramIds());
    }

    /**
     * Resolving the turn now needs dev mode; in dev mode it runs the scheduler's resolve-now.
     */
    @Test
    void resolvingTheTurnNowNeedsDevMode(@TempDir Path dataRoot) {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        RequestDispatcher normal = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        normal.setTurnForcer(() -> calls.incrementAndGet());
        normal.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        ResolveTurnNowResponse refused = normal.handleResolveTurnNow(1, new ResolveTurnNowRequest());
        assertFalse(refused.success());
        assertEquals("DEV_MODE_REQUIRED", refused.message());
        assertEquals(0, calls.get());

        RequestDispatcher dev = dispatcher(createAccountStore(dataRoot), dataRoot, true);
        dev.setTurnForcer(() -> 7L);
        assertFalse(dev.handleResolveTurnNow(2, new ResolveTurnNowRequest()).success(), "needs a login");
        dev.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        ResolveTurnNowResponse resolved = dev.handleResolveTurnNow(2, new ResolveTurnNowRequest());
        assertTrue(resolved.success(), resolved.message());
        assertEquals(7L, resolved.resolvedTurn());
    }

    /**
     * Verifies that the funds cheat is refused outside dev mode, and that the server
     * overwrites whatever seed a client sent.
     */
    @Test
    void cheatsNeedDevModeAndSeedsComeFromTheServer(@TempDir Path dataRoot) throws Exception {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));

        assertFalse(dispatcher.handleSubmitAction(1, new SubmitActionRequest(new SetFundsAction(1_000_000))).success());

        PlayerSnapshot preview = dispatcher.handleFetchState(1, new FetchStateRequest()).state();
        HireAction hire = new HireAction(PersonnelGroup.SET, preview.getSetSpecialistCandidates().get(0).getId());
        hire.setSeed(12345);
        assertTrue(dispatcher.handleSubmitAction(1, new SubmitActionRequest(hire)).success());
        try (var accountDirs = Files.list(dataRoot.resolve("players"))) {
            Path journal = accountDirs.findFirst().orElseThrow().resolve("journal");
            try (var files = Files.list(journal)) {
                PlayerAction journaled = mapper.readValue(files.findFirst().orElseThrow().toFile(), PlayerAction.class);
                assertNotEquals(12345, journaled.getSeed(), "the client's seed is replaced");
            }
        }
    }
    /**
     * Verifies that a disconnect clears a connection's authentication, so
     * a later request reusing that connection id is rejected again.
     */
    @Test
    void disconnectClearsTheConnectionsAuthentication(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        assertTrue(dispatcher.isAuthenticated(1));

        dispatcher.handleDisconnect(1);

        assertFalse(dispatcher.isAuthenticated(1));
        assertFalse(dispatcher.handleFetchState(1, new FetchStateRequest()).success());
    }

    /**
     * Verifies that {@code handle} reports a rejected handshake as one to
     * close the connection over, and an accepted one as one to keep open.
     */
    @Test
    void handleReportsCloseConnectionOnlyForARejectedHandshake(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);

        RequestDispatcher.DispatchResult rejected =
            dispatcher.handle(1, new HandshakeRequest("not-a-real-version", "mario", "hunter2"));
        assertTrue(rejected.closeConnection());
        assertFalse(((HandshakeResponse) rejected.response()).accepted());

        RequestDispatcher.DispatchResult accepted =
            dispatcher.handle(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        assertFalse(accepted.closeConnection());
        assertTrue(((HandshakeResponse) accepted.response()).accepted());
    }

    /**
     * Verifies that {@code handle} routes {@link SubmitActionRequest} and
     * {@link FetchStateRequest} the same way their own dedicated methods
     * do, and rejects any other message type.
     */
    @Test
    void handleRoutesKnownMessageTypesAndRejectsUnknownOnes(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);
        dispatcher.handle(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handle(1, new ClaimSlotRequest(Archetype.NASA, "Agency", "Mario"));

        RequestDispatcher.DispatchResult submitResult = dispatcher.handle(1, new SubmitActionRequest(new SetFundsAction(1_500)));
        assertFalse(submitResult.closeConnection());
        assertTrue(((SubmitActionResponse) submitResult.response()).success());

        RequestDispatcher.DispatchResult fetchResult = dispatcher.handle(1, new FetchStateRequest());
        assertFalse(fetchResult.closeConnection());
        assertEquals(1_500, ((FetchStateResponse) fetchResult.response()).state().getFunds());

        assertThrows(IllegalArgumentException.class, () -> dispatcher.handle(1, "not a real message"));
    }

    /**
     * The game info needs a login but no slot, reports a fresh game as turn 1 (winter 1955, not
     * started) with a next resolution ahead, and picks up the scheduler's progress and the result
     * from their files, naming the winner's director.
     */
    @Test
    void gameInfoNeedsALoginButNoSlot(@TempDir Path dataRoot) throws Exception {
        AccountStore store = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(store, dataRoot, false);
        assertFalse(dispatcher.handleGameInfo(1, new GameInfoRequest()).success());
        dispatcher.handleHandshake(1, new HandshakeRequest(AppVersion.getVersion(), "mario", "hunter2"));

        GameInfoResponse fresh = dispatcher.handleGameInfo(1, new GameInfoRequest());
        assertTrue(fresh.success(), fresh.message());
        assertEquals("early-era", fresh.contentPackId());
        assertEquals(ServerTestGames.PACK_HASH, fresh.contentPackHash());
        assertEquals(GameMeta.EARLY_ERA_DECLARED_GOALS, fresh.declaredGoalIds());
        assertEquals(1, fresh.gameTurn());
        assertEquals(new GameDate(1955, Season.WINTER), fresh.date());
        assertFalse(fresh.started());
        assertTrue(fresh.nextResolutionMillis() > fresh.serverNowMillis(), "a fresh game's first turn is still ahead");
        assertFalse(fresh.decided());

        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.NASA, "Orion Spaceworks", "A. Reyes"));
        String accountId = store.findByLogin("mario").orElseThrow().getId();
        SchedulerState state = new SchedulerState();
        state.setNextGameTurn(6);
        state.setLastResolvedDate(LocalDate.now(ZoneId.of("Europe/Berlin")));
        mapper.writeValue(dataRoot.resolve("scheduler-state.json").toFile(), state);
        mapper.writeValue(dataRoot.resolve(GameResult.FILE_NAME).toFile(),
            new GameResult("WINNER", List.of(accountId), List.of("Orion Spaceworks"), 5));

        GameInfoResponse later = dispatcher.handleGameInfo(1, new GameInfoRequest());
        assertEquals(new GameDate(1956, Season.SPRING), later.date());
        assertTrue(later.started());
        assertEquals("WINNER", later.resultStatus());
        assertEquals(List.of("Orion Spaceworks"), later.winnerAgencies());
        assertEquals(List.of("A. Reyes"), later.winnerDirectors());
        assertEquals(5, later.resultTurn());
    }
}
