package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameMetaStore;
import de.mkoehler.launchwindow.server.schedule.GameResult;
import de.mkoehler.launchwindow.server.schedule.ResolutionSchedule;
import de.mkoehler.launchwindow.server.schedule.SchedulerState;
import de.mkoehler.launchwindow.server.schedule.SchedulerStateStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the game info (gap G1) from the game's own files, read fresh on every request the way the
 * scheduler reads them every tick: {@code game-meta.json} (so a host's edit to the schedule or
 * blackouts shows at once), {@code scheduler-state.json} and {@code game-result.json}. Only the
 * content pack's id and hash are fixed at startup, since they describe what was actually loaded.
 *
 * <p>Never writes anything: before the scheduler's first tick has bootstrapped its state, the
 * state is computed the way that bootstrap would, not saved.</p>
 *
 * @author Mario Koehler
 */
public final class GameInfoService {

    private final Path dataRoot;
    private final ObjectMapper mapper;
    private final String contentPackId;
    private final String contentPackHash;
    private final GameMeta startupMeta;
    private final Clock clock;
    private final BudgetSchedule schedule;

    /**
     * Creates the service.
     *
     * @param dataRoot        the game's data root
     * @param mapper          the server's shared mapper (with {@code java.time} support)
     * @param contentPackId   the loaded content pack's id
     * @param contentPackHash the loaded content pack's hash
     * @param startupMeta     the configuration read at startup, used only if {@code game-meta.json} is gone
     * @param clock           the clock for "now"
     */
    public GameInfoService(Path dataRoot, ObjectMapper mapper, String contentPackId, String contentPackHash,
                           GameMeta startupMeta, Clock clock) {
        this(dataRoot, mapper, contentPackId, contentPackHash, startupMeta, clock, BudgetSchedule.baspmClose());
    }

    /**
     * Creates the service for a game with its own budget schedule, which the game info carries to the
     * client.
     *
     * @param dataRoot        the game's data root
     * @param mapper          the server's shared mapper (with {@code java.time} support)
     * @param contentPackId   the loaded content pack's id
     * @param contentPackHash the loaded content pack's hash
     * @param startupMeta     the configuration read at startup, used only if {@code game-meta.json} is gone
     * @param clock           the clock for "now"
     * @param schedule        the game's budget schedule
     */
    public GameInfoService(Path dataRoot, ObjectMapper mapper, String contentPackId, String contentPackHash,
                           GameMeta startupMeta, Clock clock, BudgetSchedule schedule) {
        this.dataRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.contentPackId = Objects.requireNonNull(contentPackId, "contentPackId must not be null");
        this.contentPackHash = Objects.requireNonNull(contentPackHash, "contentPackHash must not be null");
        this.startupMeta = Objects.requireNonNull(startupMeta, "startupMeta must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.schedule = Objects.requireNonNull(schedule, "schedule must not be null");
    }

    /**
     * Reads the open game turn: the one actions go into.
     *
     * @return the open game turn, 1 before the first resolution
     * @throws IOException if the scheduler state exists but can't be read
     */
    public long openGameTurn() throws IOException {
        return new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load()
            .map(SchedulerState::getNextGameTurn).orElse(1L);
    }

    /**
     * Reads the game's current facts.
     *
     * @return the game info
     * @throws IOException if a file exists but can't be read
     */
    public GameInfoResponse info() throws IOException {
        Path metaFile = dataRoot.resolve("game-meta.json");
        GameMeta meta = Files.exists(metaFile) ? GameMetaStore.loadOrCreateDefault(metaFile, mapper).meta() : startupMeta;
        ZonedDateTime now = ZonedDateTime.now(clock);
        Optional<SchedulerState> saved = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load();
        long gameTurn = saved.map(SchedulerState::getNextGameTurn).orElse(1L);
        LocalDate lastResolved = saved.map(SchedulerState::getLastResolvedDate)
            .orElseGet(() -> ResolutionSchedule.bootstrapLastResolvedDate(meta, now));
        long nextMillis = ResolutionSchedule.next(meta, lastResolved, now)
            .map(next -> next.toInstant().toEpochMilli()).orElse(-1L);

        String status = null;
        List<String> agencies = new ArrayList<>();
        List<String> directors = new ArrayList<>();
        long resultTurn = 0;
        Path resultFile = dataRoot.resolve(GameResult.FILE_NAME);
        if (Files.exists(resultFile)) {
            GameResult result = mapper.readValue(resultFile.toFile(), GameResult.class);
            status = result.status();
            agencies.addAll(result.winnerAgencies());
            for (String accountId : result.winnerIds()) {
                directors.add(new PlayerSnapshotStore(dataRoot.resolve("players").resolve(accountId), mapper).load()
                    .map(PlayerSnapshot::getDirectorName).orElse(""));
            }
            resultTurn = result.decidedOnTurn();
        }
        return GameInfoResponse.success(contentPackId, contentPackHash, meta.getDeclaredGoalIds(), gameTurn,
            gameTurn > 1, nextMillis, clock.millis(), status, agencies, directors, resultTurn, meta.isDevMode(),
            schedule);
    }
}
