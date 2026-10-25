package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.server.FrozenContentPack;
import de.mkoehler.launchwindow.server.GameServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * <p>Undoes resolved turns, for when a bug made a turn come out wrong ({@code design.md} &sect;3.7):
 * the game goes back to the start of a turn, the server gets the fix, and the turn resolves again.
 * Each player's season is seeded from the game seed and the turn, so the redone turn rolls the same
 * dice and only what the bug touched comes out differently.</p>
 *
 * <p>Rolling back to turn N undoes N and every turn after it: each player's snapshots and reports
 * from N on are deleted, the actions N resolved go back into the journal, and the actions queued
 * later are set aside in {@code journal-rolled-back/} (they were planned against the wrong
 * results; players queue again). The turns' intelligence, contract market and publications files go too, the
 * statements on the Firsts given after turn N (turn N's own stay, so a redo prints the same follow-ups), the
 * game result if one of the turns decided it, the bots' record of the turns they played, and the scheduler's next turn becomes N. The whole data root is zipped
 * into {@code backups/} first ({@link TurnBackups}).</p>
 *
 * <p>Run it with the server stopped, as {@link #main}. With {@code --redo} it resolves turn N again
 * at once, before anyone reconnects and changes their queued actions; the schedule is left alone,
 * so the next turn still resolves on its usual day.</p>
 *
 * @author Mario Koehler
 */
public final class TurnRollback {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private TurnRollback() {
    }

    /**
     * Rolls the game back to the start of a turn.
     *
     * @param dataRoot the data root
     * @param mapper   the server's mapper
     * @param context  the game's content and budget schedule
     * @param gameTurn the turn to undo, with every turn after it
     * @param now      the local time, for the backup's name
     * @return the zip of the data root as it was before the rollback
     * @throws IOException              if a file can't be read, moved or deleted
     * @throws IllegalArgumentException if the turn hasn't resolved
     */
    public static Path rollBackTo(Path dataRoot, ObjectMapper mapper, ActionContext context, long gameTurn,
            LocalDateTime now) throws IOException {
        SchedulerStateStore stateStore = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper);
        SchedulerState state = stateStore.load()
            .orElseThrow(() -> new IllegalArgumentException("no turn has resolved yet"));
        long lastResolved = lastResolved(dataRoot, mapper);
        if (gameTurn < 1 || gameTurn > lastResolved) {
            throw new IllegalArgumentException("turn " + gameTurn + " hasn't resolved; the last one was "
                + lastResolved);
        }
        Path backup = TurnBackups.beforeRollback(dataRoot, gameTurn, now);
        List<Path> players = players(dataRoot);
        for (long turn = lastResolved; turn >= gameTurn; turn--) {
            for (Path player : players) {
                new PlayerStateService(player, mapper, context).rollBack(turn);
            }
            Files.deleteIfExists(dataRoot.resolve("intelligence").resolve("turn-" + turn + ".json"));
            Files.deleteIfExists(TurnScheduler.marketFile(dataRoot, turn));
            Files.deleteIfExists(TurnScheduler.publicationsFile(dataRoot, turn));
        }
        FrontPageStore.dropFrom(dataRoot, mapper, gameTurn);
        // Statements given in later turns answered pages that are being undone; the turn's own stay.
        ReactionStore.dropAfter(dataRoot, gameTurn);
        // The crews lost in the undone turns are their results; the turn before's stay for its statements to answer.
        LossStore.dropFrom(dataRoot, gameTurn);
        Path resultFile = dataRoot.resolve(GameResult.FILE_NAME);
        if (Files.exists(resultFile) && mapper.readValue(resultFile.toFile(), GameResult.class).decidedOnTurn() >= gameTurn) {
            Files.delete(resultFile);
        }
        BotPlayers.rewind(dataRoot, mapper, gameTurn);
        TurnBackups.dropAfter(dataRoot, gameTurn);
        state.setNextGameTurn(gameTurn);
        stateStore.save(state);
        return backup;
    }

    /**
     * Returns the last turn resolved for anyone: usually the scheduler's, but a resolution that
     * failed halfway leaves some players a turn ahead of it, and that turn must be undoable too.
     *
     * @param dataRoot the data root
     * @param mapper   the server's mapper
     * @return the turn, or 0 if none has resolved
     * @throws IOException if a file can't be read
     */
    static long lastResolved(Path dataRoot, ObjectMapper mapper) throws IOException {
        long last = new SchedulerStateStore(dataRoot.resolve("scheduler-state.json"), mapper).load()
            .map(state -> state.getNextGameTurn() - 1).orElse(0L);
        for (Path player : players(dataRoot)) {
            last = Math.max(last, new PlayerSnapshotStore(player, mapper).latestTurnNumber().orElse(0));
        }
        return last;
    }

    private static List<Path> players(Path dataRoot) throws IOException {
        Path playersDir = dataRoot.resolve("players");
        if (!Files.exists(playersDir)) {
            return List.of();
        }
        try (Stream<Path> listing = Files.list(playersDir)) {
            return listing.filter(Files::isDirectory).sorted().toList();
        }
    }

    /**
     * Rolls a game back from the command line, with the server stopped:
     * {@code TurnRollback <dataRoot> <turn> [--redo] [--yes]}. Without {@code --yes} it only says
     * what it would do.
     *
     * @param args the data root, the turn to undo (with every turn after it), and the flags
     * @throws IOException if a file can't be read or written
     */
    public static void main(String[] args) throws IOException {
        List<String> flags = Stream.of(args).filter(arg -> arg.startsWith("--")).toList();
        List<String> positional = Stream.of(args).filter(arg -> !arg.startsWith("--")).toList();
        if (positional.size() != 2 || !List.of("--redo", "--yes").containsAll(flags)) {
            System.err.println("Usage: TurnRollback <dataRoot> <turn> [--redo] [--yes]");
            System.err.println("  Undoes <turn> and every turn after it. Stop the server first.");
            System.err.println("  --redo  resolve <turn> again right away, with this build's code");
            System.err.println("  --yes   do it; without it, only says what it would do");
            System.exit(2);
            return;
        }
        Path dataRoot = Path.of(positional.get(0));
        long gameTurn = Long.parseLong(positional.get(1));
        boolean redo = flags.contains("--redo");
        ObjectMapper mapper = GameServer.createMapper();
        long lastResolved = lastResolved(dataRoot, mapper);
        if (gameTurn < 1 || gameTurn > lastResolved) {
            System.err.println("Turn " + gameTurn + " hasn't resolved; the last one was " + lastResolved + ".");
            System.exit(2);
            return;
        }
        String undone = gameTurn == lastResolved ? "turn " + gameTurn : "turns " + gameTurn + " to " + lastResolved;
        if (!flags.contains("--yes")) {
            System.out.println("Would undo " + undone + " in " + dataRoot.toAbsolutePath()
                + (redo ? ", then resolve turn " + gameTurn + " again." : "; the server resolves turn " + gameTurn
                + " again at its next scheduled time.") + " Make sure the server is stopped, then add --yes.");
            return;
        }
        GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        FrozenContentPack pack = FrozenContentPack.freeze(dataRoot, meta.getContentPack(),
            TurnRollback.class.getClassLoader());
        ActionContext context = new ActionContext(ContentPackLoader.load(pack.directory()), BudgetSchedule.baspmClose());
        Path backup = rollBackTo(dataRoot, mapper, context, gameTurn, LocalDateTime.now());
        System.out.println("Undid " + undone + ". The data root before the rollback is in " + backup + ".");
        if (redo) {
            long resolved = new TurnScheduler(dataRoot, mapper, Clock.systemDefaultZone(), context,
                RandomEventRoster.baspmClose()).resolveNow();
            System.out.println("Resolved turn " + resolved + " again.");
        }
    }
}
