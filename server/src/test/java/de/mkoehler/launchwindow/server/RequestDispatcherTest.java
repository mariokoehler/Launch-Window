package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Season;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.messages.ArchiveRequest;
import de.mkoehler.launchwindow.net.messages.ArchiveResponse;
import de.mkoehler.launchwindow.net.messages.ArchiveUnlock;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.CreateAccountRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.net.messages.FrontPagesRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeRequest;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowRequest;
import de.mkoehler.launchwindow.net.messages.ResolveTurnNowResponse;
import de.mkoehler.launchwindow.net.messages.SubmitActionRequest;
import de.mkoehler.launchwindow.net.messages.SubmitActionResponse;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import de.mkoehler.launchwindow.net.messages.SubmitStatementResponse;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.schedule.FrontPageStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameResult;
import de.mkoehler.launchwindow.server.schedule.LossStore;
import de.mkoehler.launchwindow.server.schedule.ReactionStore;
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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));

        HandshakeResponse response =
            dispatcher.handleHandshake(2, new HandshakeRequest(AppVersion.getVersion(), "mario", "wrong"));

        assertFalse(response.accepted());
        assertFalse(dispatcher.isAuthenticated(2));
    }

    /**
     * Verifies that a handshake with a login nobody has is refused with the code the client offers to
     * create an account on, closes the connection and makes no account (review.md 2.3).
     */
    @Test
    void anUnknownLoginIsRefusedWithoutMakingAnAccount(@TempDir Path dataRoot) {
        AccountStore accountStore = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(accountStore, dataRoot, true);

        RequestDispatcher.DispatchResult result =
            dispatcher.handle(1, new HandshakeRequest(AppVersion.getVersion(), "mairo", "hunter2"));

        HandshakeResponse response = (HandshakeResponse) result.response();
        assertFalse(response.accepted());
        assertEquals(HandshakeResponse.NO_SUCH_ACCOUNT, response.message());
        assertTrue(result.closeConnection());
        assertTrue(accountStore.findByLogin("mairo").isEmpty());

        assertTrue(dispatcher.handleCreateAccount(2, new CreateAccountRequest(AppVersion.getVersion(), "mairo",
            "hunter2")).accepted());
        assertTrue(dispatcher.isAuthenticated(2));
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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));

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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleCreateAccount(2, new CreateAccountRequest(AppVersion.getVersion(), "anna", "secret"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.NASA, "Hanseatic Space Agency", "Mario"));

        assertEquals(ClaimSlotResponse.Reason.AGENCY_NAME_TAKEN, dispatcher.handleClaimSlot(2,
            new ClaimSlotRequest(Archetype.NASA, "hanseatic space agency", "Anna")).reason());
        assertEquals(ClaimSlotResponse.Reason.DIRECTOR_NAME_TAKEN,
            dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", "MARIO")).reason());
        assertEquals(ClaimSlotResponse.Reason.MISSING_FIELDS,
            dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", " ")).reason());
        assertEquals(ClaimSlotResponse.Reason.NAME_TOO_LONG, dispatcher.handleClaimSlot(2,
            new ClaimSlotRequest(Archetype.NASA, "B".repeat(ClaimSlotRequest.MAX_NAME_LENGTH + 1), "Anna")).reason(),
            "a front page's columns hold 32 characters (ui-spec.md 4.18)");
        assertTrue(dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Bavaria", "Anna")).success(),
            "the same archetype is fine");
    }

    /**
     * Verifies the front pages go to a player with a slot, and only to one (ui-spec.md 4.18, G14).
     */
    @Test
    void frontPagesNeedASlot(@TempDir Path dataRoot) throws Exception {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        assertFalse(dispatcher.handleFrontPages(1, new FrontPagesRequest()).success());
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));
        assertTrue(dispatcher.handleFrontPages(1, new FrontPagesRequest()).pages().isEmpty());
        FrontPage page = new FrontPage("3-earth-orbit-satellite", "earth-orbit-satellite", 3, "sat-signal", "",
            List.of(), List.of(new FrontPage.Achiever("OKB-1", "Chief", List.of())), List.of());
        FrontPageStore.keep(dataRoot, GameServer.createMapper(), 3, List.of(page));
        assertEquals(List.of(page), dispatcher.handleFrontPages(1, new FrontPagesRequest()).pages());
    }

    /**
     * Verifies a statement on a rival's First (ui-spec.md 4.18.1, G17): refused for the agency that made
     * it, a late joiner, a page from another turn, a stale turn, an unknown page, the server's own stance
     * and a bad quote; kept, changed and withdrawn for a rival, and sent back with the front pages, keyed
     * by the rival key in the turn's file.
     */
    @Test
    void aRivalGivesChangesAndWithdrawsAStatement(@TempDir Path dataRoot) throws Exception {
        AccountStore store = createAccountStore(dataRoot);
        RequestDispatcher dispatcher = dispatcher(store, dataRoot, false);
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));
        dispatcher.handleCreateAccount(2, new CreateAccountRequest(AppVersion.getVersion(), "anna", "hunter2"));
        dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "Kestrel", "Anna"));
        dispatcher.handleCreateAccount(3, new CreateAccountRequest(AppVersion.getVersion(), "late", "hunter2"));
        dispatcher.handleClaimSlot(3, new ClaimSlotRequest(Archetype.NASA, "Latecomer", "Lou"));
        for (String login : List.of("mario", "anna")) {
            // Playing since turn 1: three turns resolved before the open turn 4.
            PlayerSnapshotStore snapshots = new PlayerSnapshotStore(
                dataRoot.resolve("players").resolve(store.findByLogin(login).orElseThrow().getId()), mapper);
            PlayerSnapshot state = snapshots.load().orElseThrow();
            state.setSeason(4);
            snapshots.save(3, state, null);
        }
        SchedulerState open = new SchedulerState();
        open.setNextGameTurn(4);
        open.setLastResolvedDate(LocalDate.of(2026, 10, 9));
        mapper.writeValue(dataRoot.resolve("scheduler-state.json").toFile(), open);
        FrontPage first = new FrontPage("3-earth-orbit-satellite", "earth-orbit-satellite", 3, "sat-signal", "",
            List.of(), List.of(new FrontPage.Achiever("OKB-1", "Chief", List.of())), List.of());
        FrontPage older = new FrontPage("2-human-in-space", "human-in-space", 2, "space-first-flight", "",
            List.of(), List.of(new FrontPage.Achiever("OKB-1", "Chief", List.of())), List.of());
        FrontPageStore.keep(dataRoot, mapper, 2, List.of(older));
        FrontPageStore.keep(dataRoot, mapper, 3, List.of(first));

        assertEquals(SubmitStatementResponse.NOT_A_RIVAL, dispatcher.handleSubmitStatement(1,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, "")).message());
        assertEquals(SubmitStatementResponse.NOT_A_RIVAL, dispatcher.handleSubmitStatement(3,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, "")).message());
        assertEquals(SubmitStatementResponse.TURN_RESOLVED, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(3, first.id(), FrontPage.Stance.POLITE, "")).message());
        assertEquals(SubmitStatementResponse.UNKNOWN_PAGE, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, "3-nothing", FrontPage.Stance.POLITE, "")).message());
        assertEquals(SubmitStatementResponse.REACTION_WINDOW_CLOSED, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, older.id(), FrontPage.Stance.POLITE, "")).message());
        assertEquals(SubmitStatementResponse.INVALID_STANCE, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.DECLINED, "")).message());
        assertEquals(SubmitStatementResponse.QUOTE_TOO_LONG, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, "a".repeat(141))).message());
        assertEquals(SubmitStatementResponse.QUOTE_BAD_CHARACTERS, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, "Hi {quote}")).message());

        SubmitStatementResponse kept = dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, "  Well   flown.  "));
        assertTrue(kept.success(), kept.message());
        assertEquals(List.of(new FrontPage.Statement(first.id(), FrontPage.Stance.POLITE, "Well flown.")), kept.yours());
        SubmitStatementResponse changed = dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.NO_COMMENT, "dropped"));
        assertEquals(List.of(new FrontPage.Statement(first.id(), FrontPage.Stance.NO_COMMENT, "")), changed.yours());
        assertEquals(changed.yours(), dispatcher.handleFrontPages(2, new FrontPagesRequest()).yours());
        assertTrue(dispatcher.handleFrontPages(1, new FrontPagesRequest()).yours().isEmpty(), "never another's");
        String anna = store.findByLogin("anna").orElseThrow().getId();
        assertEquals(List.of(GameSeeds.rivalKey(ServerTestGames.meta(false).getGameSeed(), anna)),
            List.copyOf(ReactionStore.load(dataRoot, mapper, 4).keySet()));
        assertFalse(Files.readString(dataRoot.resolve("reactions").resolve("turn-4.json")).contains(anna));

        SubmitStatementResponse withdrawn = dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), null, ""));
        assertTrue(withdrawn.success());
        assertTrue(withdrawn.yours().isEmpty());

        // The resolution reads the turn's statements: one arriving after that is refused, not lost.
        dispatcher.handleSubmitStatement(2, new SubmitStatementRequest(4, first.id(), FrontPage.Stance.POLITE, ""));
        assertEquals(1, ReactionStore.seal(dataRoot, mapper, 4).size());
        assertEquals(SubmitStatementResponse.TURN_RESOLVED, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.NEXT, "")).message());
        assertEquals(SubmitStatementResponse.TURN_RESOLVED, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), null, "")).message());
        assertEquals(1, ReactionStore.seal(dataRoot, mapper, 4).size(), "a retry reads the same statements");
        ReactionStore.dropAfter(dataRoot, 4);
        assertFalse(Files.exists(dataRoot.resolve("reactions").resolve("turn-4.sealed")), "a rollback reopens it");
        assertEquals(1, ReactionStore.load(dataRoot, mapper, 4).size(), "and keeps its statements");
        assertTrue(dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.NEXT, "")).success());

        // A crew lost in turn 3 (ui-spec.md 4.18.3): its agency gives a tone, every other agency condolences.
        String mario = store.findByLogin("mario").orElseThrow().getId();
        FrontPage.Loss loss = new FrontPage.Loss("3-loss-1", 3, GameSeeds.rivalKey(ServerTestGames.meta(false)
            .getGameSeed(), mario), "OKB-1", "Chief", "vostok-orbital", "10-reentry", List.of(), List.of(), List.of());
        LossStore.keep(dataRoot, mapper, 3, List.of(loss));
        assertEquals(List.of(loss), dispatcher.handleFrontPages(2, new FrontPagesRequest()).losses());
        assertEquals(SubmitStatementResponse.INVALID_STANCE, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, loss.id(), FrontPage.Stance.PEOPLE, "")).message());
        assertEquals(SubmitStatementResponse.INVALID_STANCE, dispatcher.handleSubmitStatement(1,
            new SubmitStatementRequest(4, loss.id(), FrontPage.Stance.CONDOLENCES, "")).message());
        assertTrue(dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, loss.id(), FrontPage.Stance.TRIBUTE, "  They went first. ")).success());
        SubmitStatementResponse eulogy = dispatcher.handleSubmitStatement(1,
            new SubmitStatementRequest(4, loss.id(), FrontPage.Stance.COURAGE, ""));
        assertEquals(List.of(new FrontPage.Statement(loss.id(), FrontPage.Stance.COURAGE, "")), eulogy.yours());
        assertEquals(SubmitStatementResponse.INVALID_STANCE, dispatcher.handleSubmitStatement(2,
            new SubmitStatementRequest(4, first.id(), FrontPage.Stance.TRIBUTE, "")).message(),
            "a First takes none of the new stances");
    }

    /**
     * Verifies the archive's answer (design.md 2.12, G15): per goal asked about, the first turn any
     * player held it and whether the asker was among the first; a goal nobody holds isn't listed.
     */
    @Test
    void archiveAnswersTheFirstTurnAnyoneHeldEachGoal(@TempDir Path dataRoot) throws Exception {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleCreateAccount(2, new CreateAccountRequest(AppVersion.getVersion(), "anna", "secret"));
        assertFalse(dispatcher.handleArchive(1, ArchiveRequest.of(List.of("earth-orbit-satellite"))).success(),
            "a slot is needed");
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));
        dispatcher.handleClaimSlot(2, new ClaimSlotRequest(Archetype.NASA, "NACA", "Anna"));
        List<Path> players;
        try (java.util.stream.Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
            players = listing.filter(Files::isDirectory).toList();
        }
        for (Path player : players) {
            de.mkoehler.launchwindow.persistence.PlayerSnapshotStore store =
                new de.mkoehler.launchwindow.persistence.PlayerSnapshotStore(player, mapper);
            PlayerSnapshot snapshot = store.load().orElseThrow();
            boolean soviet = snapshot.getArchetype() == Archetype.SOVIET;
            // The Soviets reach the satellite in turn 3, NASA in turn 5; both reach radio from orbit in turn 6.
            snapshot.getAchievedGoals().put("earth-orbit-satellite", soviet ? 3 : 5);
            snapshot.getAchievedGoals().put("radio-signals-from-orbit", 6);
            store.save(store.latestTurnNumber().orElse(0) + 1, snapshot, null);
        }

        ArchiveResponse response = dispatcher.handleArchive(2,
            ArchiveRequest.of(List.of("earth-orbit-satellite", "radio-signals-from-orbit", "lunar-flyby")));

        assertTrue(response.success());
        assertEquals(List.of(new ArchiveUnlock("earth-orbit-satellite", 3, false),
            new ArchiveUnlock("radio-signals-from-orbit", 6, true)), response.unlocks(),
            "NASA was second to the satellite, tied first to radio from orbit; nobody flew past the Moon");

        // The race decided in turn 8: what nobody reached opens then, for everyone; what was reached keeps its turn.
        java.nio.file.Files.writeString(dataRoot.resolve(GameResult.FILE_NAME), mapper.writeValueAsString(
            new GameResult("WON", List.of("x"), List.of("OKB-1"), 8)));
        assertEquals(List.of(new ArchiveUnlock("earth-orbit-satellite", 3, false),
            new ArchiveUnlock("radio-signals-from-orbit", 6, true), new ArchiveUnlock("lunar-flyby", 8, false, true)),
            dispatcher.handleArchive(2, ArchiveRequest.of(
                List.of("earth-orbit-satellite", "radio-signals-from-orbit", "lunar-flyby"))).unlocks());
    }

    /**
     * Verifies that game-meta.json's devArchiveOpen opens every page asked about, in dev mode only: a
     * finding's page too, which otherwise opens only for the agencies that know it (design.md 2.13).
     */
    @Test
    void devArchiveOpenOpensEveryPageInDevModeOnly(@TempDir Path dataRoot) {
        for (boolean dev : new boolean[]{true, false}) {
            GameMeta meta = ServerTestGames.meta(dev);
            meta.setDevArchiveOpen(true);
            Path root = dataRoot.resolve(String.valueOf(dev));
            RequestDispatcher dispatcher = new RequestDispatcher(createAccountStore(root), root, mapper,
                ServerTestGames.CONTEXT, meta, ServerTestGames.gameInfo(root, mapper, meta));
            dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
            dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));
            List<ArchiveUnlock> unlocks = dispatcher.handleArchive(1, ArchiveRequest.of(
                List.of("lunar-flyby", "magnetic-field-study"))).unlocks();
            assertEquals(dev ? List.of(new ArchiveUnlock("lunar-flyby", 1, true),
                new ArchiveUnlock("magnetic-field-study", 1, true)) : List.of(), unlocks);
        }
    }

    /**
     * Verifies the whole authenticated flow with a real action: the response names the
     * rules outcome, the preview reflects it, and a rejected action changes nothing.
     */
    @Test
    void submitThenFetchReturnsTheSubmittedAction(@TempDir Path dataRoot) {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
     * An action names the open turn its screen showed. Once that turn has resolved the action is
     * refused and nothing changes; the open turn's own actions, and unchecked ones, go through.
     */
    @Test
    void anActionMeantForAResolvedTurnChangesNothing(@TempDir Path dataRoot) throws java.io.IOException {
        RequestDispatcher dispatcher = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        dispatcher.handleClaimSlot(1, new ClaimSlotRequest(Archetype.SOVIET, "OKB-1", "Chief"));
        de.mkoehler.launchwindow.server.schedule.SchedulerState moved =
            new de.mkoehler.launchwindow.server.schedule.SchedulerState();
        moved.setNextGameTurn(2);
        moved.setLastResolvedDate(java.time.LocalDate.of(2026, 10, 9));
        new de.mkoehler.launchwindow.server.schedule.SchedulerStateStore(dataRoot.resolve("scheduler-state.json"),
            GameServer.createMapper()).save(moved);

        assertEquals(SubmitActionResponse.failure(SubmitActionRequest.TURN_RESOLVED), dispatcher.handleSubmitAction(1,
            new SubmitActionRequest(new OpenProgramAction("sputnik-1"), 1)));
        assertEquals(List.of(), dispatcher.handleFetchState(1, new FetchStateRequest()).state().getOpenedProgramIds());
        assertEquals(SubmitActionResponse.success("OPENED"), dispatcher.handleSubmitAction(1,
            new SubmitActionRequest(new OpenProgramAction("sputnik-1"), 2)));
        assertEquals(SubmitActionResponse.success("OPENED"), dispatcher.handleSubmitAction(1,
            new SubmitActionRequest(new OpenProgramAction("vostok"))), "an unchecked request skips the turn check");
    }

    /**
     * Resolving the turn now needs dev mode; in dev mode it runs the scheduler's resolve-now.
     */
    @Test
    void resolvingTheTurnNowNeedsDevMode(@TempDir Path dataRoot) {
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        RequestDispatcher normal = dispatcher(createAccountStore(dataRoot), dataRoot, false);
        normal.setTurnForcer(() -> calls.incrementAndGet());
        normal.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
        ResolveTurnNowResponse refused = normal.handleResolveTurnNow(1, new ResolveTurnNowRequest());
        assertFalse(refused.success());
        assertEquals("DEV_MODE_REQUIRED", refused.message());
        assertEquals(0, calls.get());

        RequestDispatcher dev = dispatcher(createAccountStore(dataRoot), dataRoot, true);
        dev.setTurnForcer(() -> 7L);
        assertFalse(dev.handleResolveTurnNow(2, new ResolveTurnNowRequest()).success(), "needs a login");
        dev.handleCreateAccount(2, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
            dispatcher.handle(2, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
        dispatcher.handle(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));
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
        dispatcher.handleCreateAccount(1, new CreateAccountRequest(AppVersion.getVersion(), "mario", "hunter2"));

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
