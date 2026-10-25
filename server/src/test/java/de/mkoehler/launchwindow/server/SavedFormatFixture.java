package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mkoehler.launchwindow.bot.BotPlans;
import de.mkoehler.launchwindow.bot.FillerBot;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.accounts.PlayerAccount;
import de.mkoehler.launchwindow.server.schedule.BotPlayer;
import de.mkoehler.launchwindow.server.schedule.BotPlayers;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.TurnScheduler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * <p>Writes a saved-format fixture for {@link SavedFormatTest} (review.md 2.1): a small early-era game as
 * the server leaves it on disk, with every kind of saved file a running game has. Two filler bots and a
 * player play {@value #TURNS} turns; then the player and the bots queue the next turn's actions, so the
 * journal holds pending actions of several types. The accounts file holds the player's account and one
 * still on the old SHA-256 hash.</p>
 *
 * <p>Run it from the repository root with the server's test classpath:
 * {@code mvn -q -pl server test-compile exec:java -Dexec.classpathScope=test
 * -Dexec.mainClass=de.mkoehler.launchwindow.server.SavedFormatFixture -Dexec.args=dev}. The folder
 * {@code dev} may be written again whenever a saved format changes before the first live game; at the
 * release that starts it, write {@code v1} and never write it again (CLAUDE.md).</p>
 *
 * @author Mario Koehler
 */
public final class SavedFormatFixture {

    /** Where the fixtures live, from the repository root. */
    static final Path FIXTURES = Path.of("server", "src", "test", "resources", "saved-format");
    /** The player's login and password. */
    static final String LOGIN = "pilot";
    static final String PASSWORD = "fixture-pass";
    /** The account still on the old hash, and its password. */
    static final String LEGACY_LOGIN = "veteran";
    static final String LEGACY_PASSWORD = "old-pass";

    private static final int TURNS = 12;

    private SavedFormatFixture() {
    }

    /**
     * Writes the fixture.
     *
     * @param args the fixture's folder name, {@code dev} by default
     * @throws IOException if a file can't be written
     */
    public static void main(String[] args) throws IOException {
        Path target = FIXTURES.resolve(args.length > 0 ? args[0] : "dev");
        if (Files.exists(target)) {
            deleteTree(target);
        }
        write(target);
        System.out.println("Wrote " + target.toAbsolutePath());
    }

    static void write(Path dataRoot) throws IOException {
        ObjectMapper mapper = GameServer.createMapper();
        Files.createDirectories(dataRoot);
        GameMeta meta = ServerTestGames.meta(false);
        meta.setContentPack("early-era");
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        meta.setBots(List.of(new BotPlayer("bot-1", Archetype.SOVIET, null, null),
            new BotPlayer("bot-2", Archetype.NASA, null, null)));
        mapper.writeValue(dataRoot.resolve("game-meta.json").toFile(), meta);
        FrozenContentPack pack = FrozenContentPack.freeze(dataRoot, "early-era", SavedFormatFixture.class.getClassLoader());
        ActionContext context = new ActionContext(ContentPackLoader.load(pack.directory()), BudgetSchedule.baspmClose());

        String playerId;
        AccountStore accounts = new AccountStore(dataRoot.resolve("accounts.json"));
        try {
            playerId = accounts.create(LOGIN, PASSWORD).account().getId();
        } finally {
            accounts.close();
        }
        addLegacyAccount(dataRoot.resolve("accounts.json"), mapper);
        PlayerSlots.claim(dataRoot, mapper, context.schedule(), meta.getGameSeed(), playerId, Archetype.NASA,
            "Fixture Agency", "Pat Fixture");

        TurnScheduler scheduler = new TurnScheduler(dataRoot, mapper, Clock.fixed(Instant.parse("2026-09-29T06:00:00Z"),
            ZoneId.of("Europe/Berlin")), context, RandomEventRoster.baspmClose());
        for (int turn = 1; turn <= TURNS; turn++) {
            // The player plays like a bot, so their journal and snapshots hold every kind of action.
            queue(dataRoot, mapper, context, playerId, turn);
            scheduler.resolveNow();
        }
        // The open turn: the player's and the bots' actions wait in their journals, the bots' statements too.
        queue(dataRoot, mapper, context, playerId, TURNS + 1);
        BotPlayers.play(dataRoot, mapper, context, meta, TURNS + 1);
        Path backups = dataRoot.resolve("backups");
        if (Files.exists(backups)) {
            deleteTree(backups);
        }
    }

    private static void queue(Path dataRoot, ObjectMapper mapper, ActionContext context, String playerId, long turn)
            throws IOException {
        PlayerStateService service = new PlayerStateService(dataRoot.resolve("players").resolve(playerId), mapper,
            context);
        var state = service.currentState();
        FillerBot.Turn plan = new FillerBot(context.catalog(), BotPlans.forPack(context.catalog(), state.getArchetype()))
            .turn(turn * 7_919L, List.of());
        for (int i = 0; i < FillerBot.MAX_ACTIONS_PER_TURN; i++) {
            PlayerAction action = plan.next(state);
            if (action == null) {
                return;
            }
            if (service.submitAction(action).applied()) {
                state = service.currentState();
            }
        }
    }

    private static void addLegacyAccount(Path file, ObjectMapper mapper) throws IOException {
        ObjectNode accounts = (ObjectNode) mapper.readTree(file.toFile());
        String salt = "00112233445566778899aabbccddeeff";
        // One SHA-256 round over salt and password, as accounts were hashed before 2026-10-25.
        java.security.MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        digest.update(java.util.HexFormat.of().parseHex(salt));
        digest.update(LEGACY_PASSWORD.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        PlayerAccount legacy = new PlayerAccount("legacy-account", LEGACY_LOGIN,
            java.util.HexFormat.of().formatHex(digest.digest()), salt, null);
        ObjectNode node = mapper.valueToTree(legacy);
        node.remove("passwordScheme");
        accounts.set(LEGACY_LOGIN, node);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), accounts);
    }

    static void deleteTree(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }
}
