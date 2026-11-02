package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.net.messages.StandingSeries;
import de.mkoehler.launchwindow.persistence.TurnReportStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The few numbers per player and turn the standing charts need ({@code ui-spec.md} component 30, G20),
 * in memory, so a request never reads every turn report. Filled at server start and caught up as each
 * turn resolves. Nothing is saved: the reports stay the record, and a rollback, which runs with the
 * server stopped, is rebuilt from them at the next start.
 *
 * @author Mario Koehler
 */
final class StandingCache {

    private final Path playersRoot;
    private final ObjectMapper mapper;
    private final Map<String, TreeMap<Long, StandingSeries.Turn>> players = new HashMap<>();

    /**
     * Creates an empty cache.
     *
     * @param dataRoot the game's data root
     * @param mapper   the mapper reports are read with
     */
    StandingCache(Path dataRoot, ObjectMapper mapper) {
        this.playersRoot = Objects.requireNonNull(dataRoot, "dataRoot must not be null").resolve("players");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Reads every report not cached yet. The first call reads them all; later ones only list each
     * player's folder, and read what a resolution added.
     *
     * @throws IOException if a report can't be read
     */
    synchronized void catchUp() throws IOException {
        if (!Files.isDirectory(playersRoot)) {
            return;
        }
        try (Stream<Path> listing = Files.list(playersRoot)) {
            for (Path playerDir : listing.filter(Files::isDirectory).toList()) {
                catchUp(playerDir);
            }
        }
    }

    private void catchUp(Path playerDir) throws IOException {
        TurnReportStore reports = new TurnReportStore(playerDir, mapper);
        OptionalLong latest = reports.latestTurn();
        if (latest.isEmpty()) {
            return;
        }
        TreeMap<Long, StandingSeries.Turn> turns =
            players.computeIfAbsent(playerDir.getFileName().toString(), key -> new TreeMap<>());
        long next = turns.isEmpty() ? 1L : turns.lastKey() + 1L;
        for (long turn = next; turn <= latest.getAsLong(); turn++) {
            reports.load(turn).ifPresent(report -> turns.put(report.gameTurn(), StandingSeries.Turn.of(report)));
        }
    }

    /**
     * Returns a player's turns.
     *
     * @param playerId the player's folder name
     * @return the turns, oldest first
     */
    synchronized List<StandingSeries.Turn> turns(String playerId) {
        TreeMap<Long, StandingSeries.Turn> turns = players.get(playerId);
        return turns == null ? List.of() : new ArrayList<>(turns.values());
    }
}
