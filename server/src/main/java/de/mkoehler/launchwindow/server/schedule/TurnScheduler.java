package de.mkoehler.launchwindow.server.schedule;

import java.util.Set;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.rules.ReactionPrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.CommercialClients;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.AtomicFiles;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.ContractMarket;
import de.mkoehler.launchwindow.rules.FindingPublications;
import de.mkoehler.launchwindow.rules.MemorialPrinter;
import de.mkoehler.launchwindow.rules.SciencePrinter;
import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.content.Theories;
import de.mkoehler.launchwindow.rules.FrontPagePrinter;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.IntelligenceBoard;
import de.mkoehler.launchwindow.rules.RivalFirsts;
import de.mkoehler.launchwindow.rules.WinCondition;
import de.mkoehler.launchwindow.server.GameSeeds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * <p>Decides, on every call to {@link #checkAndResolveIfDue()}, whether
 * today's turn should resolve for this server's one game (see
 * {@code design.md} &sect;3.6, &sect;3.9), and if so resolves it for every
 * player under this server's data root.</p>
 *
 * <p>Re-reads {@code game-meta.json} on every call rather than caching it at
 * construction time, so a blackout the host adds mid-day - before the
 * configured resolution time - still suppresses that day's resolution, as
 * &sect;3.6 requires.</p>
 *
 * <p>Only ever resolves the single next calendar day it is behind by, never
 * a backlog of several missed days at once: if the server was down across
 * more than one scheduled resolution day, the days in between are skipped
 * rather than caught up on the next boot.</p>
 *
 * <p>Intended to be called from the same thread that drains
 * {@code NetworkServer.poll()} - e.g. inside {@code GameServer.render()} -
 * so a resolution never runs concurrently with {@code RequestDispatcher}
 * handling a request for the same player (see that slice's threading
 * constraint note in {@code design.md} &sect;5).</p>
 *
 * <p>If resolving any one player throws, the exception propagates and this
 * call's scheduler state is not advanced, so the next call retries the same
 * game turn for every player - already-resolved players are unaffected,
 * since {@link PlayerStateService#resolveTurn} is itself idempotent.
 * A player whose resolution keeps failing therefore blocks every other
 * player's turn from ever advancing past it, for as long as it keeps
 * failing; this is accepted for now and would need addressing before this
 * scales past a small group of trusted players.</p>
 *
 * @author Mario Koehler
 */
public class TurnScheduler {

    private final Path dataRoot;
    private final Path gameMetaFile;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final SchedulerStateStore stateStore;
    private final ActionContext context;
    private final RandomEventRoster roster;
    private java.util.function.LongConsumer onResolved = turn -> { };

    /**
     * Creates a new scheduler for the game rooted at the given data
     * directory.
     *
     * @param dataRoot the server's data root - {@code game-meta.json},
     *                 {@code scheduler-state.json} and every player's own
     *                 directory live directly under it
     * @param mapper   the Jackson mapper used to read/write every file this
     *                 class touches
     * @param clock    the clock used for "now"
     * @param context  the game's content pack and budget schedule
     * @param roster   the random-event content
     */
    public TurnScheduler(Path dataRoot, ObjectMapper mapper, Clock clock, ActionContext context,
            RandomEventRoster roster) {
        this.context = Objects.requireNonNull(context, "context must not be null");
        this.roster = Objects.requireNonNull(roster, "roster must not be null");
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.gameMetaFile = dataRoot.resolve("game-meta.json");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.stateStore = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);
    }

    /**
     * Checks whether today's turn is due, and resolves it for every player
     * if so. A no-op if today's turn was already resolved, if today is not
     * one of the schedule's active weekdays, if today falls in a blackout,
     * or if the configured resolution time has not yet passed today.
     *
     * <p>On the very first call ever for this data root, the scheduler
     * never treats a day already past its resolution time as due just
     * because the server happens to be booting late in the day: if "now" is
     * already past today's resolution time, today is bootstrapped as
     * already handled, and the first real resolution happens on the next
     * active, non-blacked-out day after that. If "now" is still before
     * today's resolution time, today itself remains eligible, so a server
     * started fresh earlier in the day can still resolve its first turn
     * later that same day.</p>
     *
     * @throws IOException if {@code game-meta.json}, the scheduler state,
     *                      or any player's own data cannot be read or
     *                      written
     */
    public void checkAndResolveIfDue() throws IOException {
        GameMetaStore.Result metaResult = GameMetaStore.loadOrCreateDefault(gameMetaFile, mapper);
        GameMeta meta = metaResult.meta();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(meta.getSchedule().getTimezone());
        LocalDate today = now.toLocalDate();

        SchedulerState state = loadOrBootstrapState(now, meta);
        if (!ResolutionSchedule.isDue(meta, state.getLastResolvedDate(), now)) {
            return;
        }

        long turn = state.getNextGameTurn();
        resolveAllPlayers(turn, meta);

        state.setLastResolvedDate(today);
        state.setNextGameTurn(turn + 1);
        stateStore.save(state);
        onResolved.accept(turn);
    }

    /**
     * Resolves the open turn right away, for play testing in dev mode: the same pass as a
     * scheduled resolution, but it leaves the schedule alone, so today's scheduled turn still
     * resolves at its time.
     *
     * @return the game turn that was resolved
     * @throws IOException if {@code game-meta.json}, the scheduler state, or any player's own data
     *                     cannot be read or written
     */
    public long resolveNow() throws IOException {
        GameMeta meta = GameMetaStore.loadOrCreateDefault(gameMetaFile, mapper).meta();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(meta.getSchedule().getTimezone());
        SchedulerState state = loadOrBootstrapState(now, meta);
        long turn = state.getNextGameTurn();
        resolveAllPlayers(turn, meta);
        state.setNextGameTurn(turn + 1);
        stateStore.save(state);
        onResolved.accept(turn);
        return turn;
    }

    /**
     * Sets who hears that a turn has resolved: called with the turn once its resolution is saved,
     * scheduled or forced.
     *
     * @param listener the listener
     */
    public void setOnResolved(java.util.function.LongConsumer listener) {
        this.onResolved = Objects.requireNonNull(listener, "listener must not be null");
    }

    /**
     * Loads the persisted scheduler state, or bootstraps and saves a fresh
     * one on the very first call: {@code nextGameTurn} 1, and
     * {@code lastResolvedDate} set to yesterday if today's resolution time
     * has not passed yet (today stays eligible later that same day),
     * or to today if it already has (today is treated as already handled,
     * so a late-in-the-day first boot never fires immediately).
     *
     * @param now  the current moment, in the schedule's own timezone
     * @param meta the game meta, for its schedule's resolution time
     * @return the loaded or newly bootstrapped state
     * @throws IOException if the state file exists but cannot be read, or
     *                      a freshly bootstrapped one cannot be written
     */
    private SchedulerState loadOrBootstrapState(ZonedDateTime now, GameMeta meta) throws IOException {
        Optional<SchedulerState> existing = stateStore.load();
        if (existing.isPresent()) {
            return existing.get();
        }
        SchedulerState fresh = new SchedulerState();
        fresh.setNextGameTurn(1);
        fresh.setLastResolvedDate(ResolutionSchedule.bootstrapLastResolvedDate(meta, now));
        stateStore.save(fresh);
        return fresh;
    }

    /**
     * Resolves one game turn for every claimed player, then checks for a winner.
     *
     * <p>The goal tally is built from every player's state at the <em>start</em> of
     * the turn, so a retry after a crash halfway through prices same-turn goals the
     * same way as the first attempt; already-resolved players are skipped by
     * {@link PlayerStateService#resolveTurn}'s own idempotence. Each player's season
     * pass is seeded from the game seed, the turn, and the player, so it always comes
     * out the same. The win check runs once after everyone has resolved, and the
     * first decided result is recorded and never overwritten
     * ({@code design.md} &sect;5, M4 decision 4).</p>
     *
     * @param gameTurn the turn to resolve
     * @param meta     the game's configuration
     * @throws IOException if a player's files can't be read or written
     */
    private void resolveAllPlayers(long gameTurn, GameMeta meta) throws IOException {
        BotPlayers.ensureSlots(dataRoot, mapper, context, meta);
        Path playersDir = dataRoot.resolve("players");
        if (!Files.exists(playersDir)) {
            return;
        }
        // Before anything is written, so a bad resolution can be undone (design.md section 3.7).
        TurnBackups.beforeTurn(dataRoot, gameTurn);
        // The filler bots play their turn last thing before it resolves, after the backup (design.md section 5).
        BotPlayers.play(dataRoot, mapper, context, meta, gameTurn);
        Map<String, PlayerStateService> services = new LinkedHashMap<>();
        List<PlayerSnapshot> startOfTurn = new ArrayList<>();
        Map<String, PlayerSnapshot> startByKey = new LinkedHashMap<>();
        Map<String, PlayerSnapshot> withActions = new LinkedHashMap<>();
        try (Stream<Path> listing = Files.list(playersDir)) {
            for (Path playerDir : listing.filter(Files::isDirectory).sorted().toList()) {
                if (new PlayerSnapshotStore(playerDir, mapper).latestTurnNumber().isEmpty()) {
                    continue;
                }
                PlayerStateService service = new PlayerStateService(playerDir, mapper, context);
                services.put(playerDir.getFileName().toString(), service);
                String key = GameSeeds.rivalKey(meta.getGameSeed(), playerDir.getFileName().toString());
                service.stateBefore(gameTurn).ifPresent(state -> {
                    startOfTurn.add(state);
                    startByKey.put(key, state);
                });
                withActions.put(key, service.currentState());
            }
        }
        GoalTally tally = GoalTally.fromPlayers(startOfTurn);
        // Last turn's Firsts, read from the start of this turn, so a retry answers the same ones (design.md 2.1(e)).
        List<String> rivalFirsts = RivalFirsts.settledFirstOn(startOfTurn, Math.toIntExact(gameTurn - 1));
        Map<String, List<IntelFinding>> intelligence = intelligence(gameTurn, meta, withActions);
        ContractMarket.Settlement market = market(gameTurn, meta, withActions, startByKey);
        FindingPublications.Settlement publications = publications(gameTurn, withActions);
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            String key = GameSeeds.rivalKey(meta.getGameSeed(), player.getKey());
            player.getValue().resolveTurn(gameTurn, tally, roster,
                GameSeeds.forSeason(meta.getGameSeed(), gameTurn, player.getKey()),
                intelligence.getOrDefault(key, List.of()), market.turnFor(key), rivalFirsts,
                publications.turnFor(key));
        }
        printFrontPages(gameTurn, meta, services);
        recordResultIfDecided(gameTurn, meta, services);
    }

    /**
     * Prints the turn's front pages for the great Firsts ({@code ui-spec.md} &sect;4.18, G14) from
     * every player's turn report, which survives a retry after a crash halfway through (a player
     * already resolved returns no events, but their report is on disk), and the follow-ups with the
     * rivals' statements on the turn before's Firsts (&sect;4.18.1, G17) and the science pages of the theories
     * settled this turn (&sect;4.18.2, G18), and keeps them all in {@code frontpages.json} at once. The crews lost
     * this turn are recorded in {@code losses/turn-N.json} for the next turn to answer, and the memorial pages of
     * those lost in the turn before are printed, first of all (&sect;4.18.3, G19).
     *
     * @param gameTurn the turn just resolved
     * @param meta     the game's configuration, for its seed
     * @param services every player's state service
     * @throws IOException if a report or the pages file can't be read or written
     */
    private void printFrontPages(long gameTurn, GameMeta meta, Map<String, PlayerStateService> services)
            throws IOException {
        // The crews lost this turn (ui-spec.md 4.18.3): public from the next turn, printed at its end.
        List<MemorialPrinter.Agency> named = new ArrayList<>();
        List<MemorialPrinter.Turn> turns = new ArrayList<>();
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            PlayerSnapshot state = player.getValue().stateBefore(gameTurn + 1).orElseThrow();
            MemorialPrinter.Agency agency = new MemorialPrinter.Agency(
                GameSeeds.rivalKey(meta.getGameSeed(), player.getKey()), state.getAgencyName(), state.getDirectorName());
            named.add(agency);
            turns.add(new MemorialPrinter.Turn(agency, player.getValue().turnReport(gameTurn).orElse(null),
                state.getMemorial()));
        }
        LossStore.keep(dataRoot, mapper, gameTurn, MemorialPrinter.losses(Math.toIntExact(gameTurn), turns));
        FrontPages words = FrontPages.standard();
        if (words.firsts().isEmpty()) {
            return;
        }
        // Sealed as they are read: a statement arriving later is refused, not kept unprinted.
        Map<String, List<FrontPage.Statement>> statements = ReactionStore.seal(dataRoot, mapper, gameTurn);
        List<FrontPagePrinter.Agency> agencies = new ArrayList<>();
        for (PlayerStateService service : services.values()) {
            PlayerSnapshot state = service.stateBefore(gameTurn + 1).orElseThrow();
            agencies.add(new FrontPagePrinter.Agency(state.getAgencyName(), state.getDirectorName(),
                service.turnReport(gameTurn).orElse(null)));
        }
        List<FrontPage> pages = new ArrayList<>(MemorialPrinter.print(Math.toIntExact(gameTurn), meta.getGameSeed(),
            LossStore.load(dataRoot, mapper, gameTurn - 1), statements, named, MemorialPages.standard(), words));
        pages.addAll(followUps(gameTurn, meta, services, words, statements));
        pages.addAll(FrontPagePrinter.print(Math.toIntExact(gameTurn), meta.getGameSeed(), agencies, words,
            goal -> context.catalog().goal(goal).isPresent()));
        // Theories of the day settled this turn (ui-spec.md 4.18.2): each once, so not those settled before.
        Set<String> settled = FrontPageStore.load(dataRoot, mapper).stream()
            .filter(p -> p.science() && p.gameTurn() < gameTurn).map(FrontPage::storyId)
            .collect(java.util.stream.Collectors.toSet());
        pages.addAll(SciencePrinter.print(Math.toIntExact(gameTurn), meta.getGameSeed(), agencies,
            Theories.standard(), settled, words, goal -> context.catalog().goal(goal).isPresent()));
        FrontPageStore.keep(dataRoot, mapper, gameTurn, pages);
    }

    /**
     * Prints the follow-ups of the turn before's Firsts from the statements given in this turn
     * ({@code reactions/turn-N.json}), which a retry reads again, so it prints the same pages.
     *
     * @param gameTurn the turn just resolved
     * @param meta     the game's configuration, for its seed
     * @param services every player's state service, by account
     * @param words      the Firsts' words, for the weather
     * @param statements this turn's statements, by agency key, sealed
     * @return the follow-ups, maybe none
     * @throws IOException if a file can't be read
     */
    private List<FrontPage> followUps(long gameTurn, GameMeta meta, Map<String, PlayerStateService> services,
            FrontPages words, Map<String, List<FrontPage.Statement>> statements) throws IOException {
        List<FrontPage> firsts = FrontPageStore.load(dataRoot, mapper).stream()
            .filter(p -> p.firstPage() && p.gameTurn() == gameTurn - 1).toList();
        if (firsts.isEmpty()) {
            return List.of();
        }
        List<ReactionPrinter.Agency> agencies = new ArrayList<>();
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            PlayerSnapshot state = player.getValue().stateBefore(gameTurn + 1).orElseThrow();
            Map<String, FrontPage.Statement> given = new LinkedHashMap<>();
            for (FrontPage.Statement statement : statements.getOrDefault(
                    GameSeeds.rivalKey(meta.getGameSeed(), player.getKey()), List.of())) {
                given.put(statement.pageId(), statement);
            }
            agencies.add(new ReactionPrinter.Agency(state.getAgencyName(), state.getDirectorName(),
                ReactionPrinter.firstTurn(gameTurn + 1, state.getSeason()), Set.copyOf(state.getAchievedGoals().keySet()),
                given));
        }
        return ReactionPrinter.print(Math.toIntExact(gameTurn), meta.getGameSeed(), firsts, agencies,
            FrontPageReactions.standard(), words);
    }

    /**
     * Settles the turn's intelligence for every player once ({@code design.md} &sect;2.9) and keeps
     * it in {@code intelligence/turn-N.json}, so a retry after a crash halfway through applies the
     * same findings, even to players whose actions were already archived. Settled from each player's
     * state with the turn's actions applied, keyed by the opaque key operations name their target by.
     *
     * @param gameTurn    the turn
     * @param meta        the game's configuration, for its seed
     * @param withActions every player's state with the turn's actions applied, by key
     * @return each player's findings, by key
     * @throws IOException if the file can't be read or written
     */
    private Map<String, List<IntelFinding>> intelligence(long gameTurn, GameMeta meta,
            Map<String, PlayerSnapshot> withActions) throws IOException {
        Path file = dataRoot.resolve("intelligence").resolve("turn-" + gameTurn + ".json");
        TypeReference<Map<String, List<IntelFinding>>> type = new TypeReference<>() { };
        if (Files.exists(file)) {
            return mapper.readValue(file.toFile(), type);
        }
        Map<String, List<IntelFinding>> findings = IntelligenceBoard.settle(withActions, context.catalog(),
            meta.getGameSeed(), gameTurn);
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "turn-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), findings);
        AtomicFiles.moveReplacingWithRetry(temp, file);
        return findings;
    }

    /**
     * Settles the turn's contract market once for every player ({@code design.md} &sect;2.11) and
     * keeps it in {@code market/turn-N.json}, like the intelligence: the awards from the turn's
     * applications and the board for the coming turn, against the board the previous turn's file
     * left. A retry after a crash halfway through re-reads the file, so every player gets the same.
     *
     * @param gameTurn    the turn
     * @param meta        the game's configuration, for its seed
     * @param withActions every player's state with the turn's actions applied, by key
     * @param startOfTurn every player's state at the start of the turn, by key, for the scores
     * @return the settlement
     * @throws IOException if a file can't be read or written
     */
    private ContractMarket.Settlement market(long gameTurn, GameMeta meta, Map<String, PlayerSnapshot> withActions,
            Map<String, PlayerSnapshot> startOfTurn) throws IOException {
        Path file = marketFile(dataRoot, gameTurn);
        if (Files.exists(file)) {
            return mapper.readValue(file.toFile(), ContractMarket.Settlement.class);
        }
        Path previous = marketFile(dataRoot, gameTurn - 1);
        ContractMarket.Settlement last = Files.exists(previous)
            ? mapper.readValue(previous.toFile(), ContractMarket.Settlement.class) : null;
        ContractMarket.Settlement settlement = ContractMarket.settle(withActions, startOfTurn,
            last == null ? List.of() : last.board(), last == null ? List.of() : last.usedClientIds(),
            context.catalog(), CommercialClients.standard(), meta.getGameSeed(), Math.toIntExact(gameTurn));
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "turn-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), settlement);
        AtomicFiles.moveReplacingWithRetry(temp, file);
        return settlement;
    }

    /**
     * Settles the turn's publications of findings once for every player ({@code design.md} &sect;2.13)
     * and keeps them in {@code publications/turn-N.json}, like the market, so a retry after a crash
     * halfway through applies the same ones. Settled from each player's state with the turn's actions
     * applied, before any flight of the turn.
     *
     * @param gameTurn    the turn
     * @param withActions every player's state with the turn's actions applied, by key
     * @return the settlement
     * @throws IOException if the file can't be read or written
     */
    private FindingPublications.Settlement publications(long gameTurn, Map<String, PlayerSnapshot> withActions)
            throws IOException {
        Path file = publicationsFile(dataRoot, gameTurn);
        if (Files.exists(file)) {
            return mapper.readValue(file.toFile(), FindingPublications.Settlement.class);
        }
        FindingPublications.Settlement settlement = FindingPublications.settle(withActions, context.catalog());
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "turn-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), settlement);
        AtomicFiles.moveReplacingWithRetry(temp, file);
        return settlement;
    }

    /**
     * Returns the file a turn's publications of findings are kept in.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn
     * @return the file
     */
    static Path publicationsFile(Path dataRoot, long gameTurn) {
        return dataRoot.resolve("publications").resolve("turn-" + gameTurn + ".json");
    }

    /**
     * Returns the file a turn's contract market is kept in.
     *
     * @param dataRoot the data root
     * @param gameTurn the turn
     * @return the file
     */
    static Path marketFile(Path dataRoot, long gameTurn) {
        return dataRoot.resolve("market").resolve("turn-" + gameTurn + ".json");
    }

    private void recordResultIfDecided(long gameTurn, GameMeta meta, Map<String, PlayerStateService> services)
            throws IOException {
        Path resultFile = dataRoot.resolve(GameResult.FILE_NAME);
        if (Files.exists(resultFile)) {
            return;
        }
        Map<String, PlayerSnapshot> players = new LinkedHashMap<>();
        for (Map.Entry<String, PlayerStateService> player : services.entrySet()) {
            players.put(player.getKey(), player.getValue().stateBefore(gameTurn + 1).orElseThrow());
        }
        WinCondition.Outcome outcome = WinCondition.evaluate(players, meta.getDeclaredGoalIds(), context.catalog());
        if (outcome.status() == WinCondition.Status.NO_WINNER_YET) {
            return;
        }
        List<String> agencies = outcome.winners().stream().map(id -> players.get(id).getAgencyName()).toList();
        GameResult result = new GameResult(outcome.status().name(), outcome.winners(), agencies, gameTurn);
        Path temp = Files.createTempFile(dataRoot, "game-result-", ".json.tmp");
        mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), result);
        AtomicFiles.moveReplacingWithRetry(temp, resultFile);
    }
}