package de.mkoehler.launchwindow.server.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.bot.BotPlans;
import de.mkoehler.launchwindow.bot.FillerBot;
import de.mkoehler.launchwindow.content.AgencyNames;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.SubmitStatementRequest;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.AtomicFiles;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.PlayerSnapshotStore;
import de.mkoehler.launchwindow.persistence.PlayerStateService;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelNames;
import de.mkoehler.launchwindow.server.GameSeeds;
import de.mkoehler.launchwindow.server.PlayerSlots;
import de.mkoehler.launchwindow.server.Statements;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>The filler bots of {@code game-meta.json} ({@code design.md} &sect;5, "Filler bots"): agencies the
 * server plays itself, so a small play test has rivals. Each is a player folder like any other, made
 * through {@link PlayerSlots}, whose actions go through the same validation and journal as a player's
 * and are resolved with everyone's. A bot plays its turn just before the turn resolves, after the turn's
 * backup, so undoing a turn and redoing it plays the bots again with the fixed build
 * ({@code design.md} &sect;3.7). It also gives statements on its rivals' Firsts and on crews lost.</p>
 *
 * <p>A bot's {@code bot.json} in its folder records the last turn it played, so a retry after a crash
 * doesn't play it twice. Rolling back to a turn lowers it to that turn ({@link #rewind}): the turn's own
 * actions go back into the journal as everyone's do, and the turns after it are played afresh.</p>
 *
 * @author Mario Koehler
 */
public final class BotPlayers {

    /** The file in a bot's folder that records the last turn it played. */
    static final String MARKER = "bot.json";
    /** The chance a bot gives a statement on a rival's First, in percent (unconfirmed). */
    static final int REACT_PERCENT = 70;
    /** The chance a bot sends condolences on a rival's lost crew, in percent (unconfirmed). */
    static final int CONDOLENCE_PERCENT = 85;

    private static final System.Logger LOG = System.getLogger(BotPlayers.class.getName());
    private static final FrontPage.Stance[] GIVABLE = Stream.of(FrontPage.Stance.values())
        .filter(FrontPage.Stance::givable).toArray(FrontPage.Stance[]::new);
    private static final FrontPage.Stance[] TONES = Stream.of(FrontPage.Stance.values())
        .filter(FrontPage.Stance::tone).toArray(FrontPage.Stance[]::new);
    private static final FrontPage.Stance[] CONDOLENCES = Stream.of(FrontPage.Stance.values())
        .filter(FrontPage.Stance::condolence).toArray(FrontPage.Stance[]::new);

    /**
     * What {@code bot.json} holds.
     *
     * @param lastPlayedTurn the last turn the bot played
     */
    record Marker(long lastPlayedTurn) {
    }

    private BotPlayers() {
    }

    /**
     * Makes the slot of every bot that has none yet, drawing the names it lacks.
     *
     * @param dataRoot the data root
     * @param mapper   the persistence mapper
     * @param context  the game's pack and budget schedule
     * @param meta     the game's configuration
     * @throws IOException if a player's files can't be read or a slot can't be written
     */
    public static void ensureSlots(Path dataRoot, ObjectMapper mapper, ActionContext context, GameMeta meta)
            throws IOException {
        for (BotPlayer bot : meta.getBots()) {
            if (new PlayerSnapshotStore(dataRoot.resolve("players").resolve(bot.id()), mapper).load().isPresent()) {
                continue;
            }
            List<PlayerSnapshot> others = PlayerSlots.everyPlayer(dataRoot, mapper);
            Set<String> agencies = others.stream().map(p -> p.getAgencyName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
            Set<String> directors = others.stream().map(PlayerSnapshot::getDirectorName).collect(Collectors.toSet());
            SplittableRandom random = new SplittableRandom(GameSeeds.forNewPlayer(meta.getGameSeed(), bot.id()));
            String agency = bot.agencyName();
            if (agency == null || agency.isBlank()) {
                List<String> free = AgencyNames.standard().forArchetype(bot.archetype()).stream()
                    .filter(name -> !agencies.contains(name.toLowerCase(Locale.ROOT))).toList();
                agency = free.isEmpty() ? "Agency " + bot.id() : free.get(random.nextInt(free.size()));
            }
            String director = bot.directorName();
            if (director == null || director.isBlank()) {
                director = PersonnelNames.forCandidate(bot.id(), bot.archetype(), agency, PersonnelGroup.ASTRONAUT,
                    Set.of(), Set.of(), directors).name();
            }
            var result = PlayerSlots.claim(dataRoot, mapper, context.schedule(), meta.getGameSeed(), bot.id(),
                bot.archetype(), clip(agency.strip()), clip(director.strip()));
            if (!result.success()) {
                LOG.log(System.Logger.Level.WARNING, "Bot " + bot.id() + " has no slot: " + result.message());
            }
        }
    }

    /**
     * Plays the open turn for every bot that hasn't played it yet. A bot that fails is logged and skipped,
     * so it never holds up the players' turn.
     *
     * @param dataRoot the data root
     * @param mapper   the persistence mapper
     * @param context  the game's pack and budget schedule
     * @param meta     the game's configuration
     * @param gameTurn the turn about to resolve
     */
    public static void play(Path dataRoot, ObjectMapper mapper, ActionContext context, GameMeta meta,
            long gameTurn) {
        for (BotPlayer bot : meta.getBots()) {
            try {
                playOne(dataRoot, mapper, context, meta, bot, gameTurn);
            } catch (IOException | RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Bot " + bot.id() + " couldn't play turn " + gameTurn, e);
            }
        }
    }

    private static void playOne(Path dataRoot, ObjectMapper mapper, ActionContext context, GameMeta meta,
            BotPlayer bot, long gameTurn) throws IOException {
        Path folder = dataRoot.resolve("players").resolve(bot.id());
        if (new PlayerSnapshotStore(folder, mapper).latestTurnNumber().isEmpty()) {
            return;
        }
        Path marker = folder.resolve(MARKER);
        if (Files.exists(marker) && mapper.readValue(marker.toFile(), Marker.class).lastPlayedTurn() >= gameTurn) {
            return;
        }
        String key = GameSeeds.rivalKey(meta.getGameSeed(), bot.id());
        List<String> rivals = new ArrayList<>();
        try (Stream<Path> listing = Files.list(dataRoot.resolve("players"))) {
            for (Path other : listing.filter(Files::isDirectory).sorted().toList()) {
                String id = other.getFileName().toString();
                if (!id.equals(bot.id()) && new PlayerSnapshotStore(other, mapper).latestTurnNumber().isPresent()) {
                    rivals.add(GameSeeds.rivalKey(meta.getGameSeed(), id));
                }
            }
        }
        PlayerStateService service = new PlayerStateService(folder, mapper, context);
        // Its own stream, off the season's, so a bot's choices never move anyone's rolls.
        long seed = GameSeeds.forSeason(meta.getGameSeed(), gameTurn, bot.id()) ^ 0xB07B07L;
        PlayerSnapshot state = service.currentState();
        FillerBot.Turn turn = new FillerBot(context.catalog(), BotPlans.forPack(context.catalog(), state.getArchetype()))
            .turn(seed, rivals);
        for (int i = 0; i < FillerBot.MAX_ACTIONS_PER_TURN; i++) {
            PlayerAction action = turn.next(state);
            if (action == null) {
                break;
            }
            if (service.submitAction(action).applied()) {
                state = service.currentState();
            }
        }
        if (!turn.idle()) {
            speak(dataRoot, mapper, gameTurn, key, state, new SplittableRandom(seed ^ 0x5EECL));
        }
        writeMarker(mapper, marker, gameTurn);
    }

    /**
     * Lowers every bot's record of the last turn it played to {@code gameTurn}, for a rollback to that
     * turn ({@link TurnRollback}). The turn's own actions are back in the journal, so it counts as played;
     * every later turn is played again.
     *
     * @param dataRoot the data root
     * @param mapper   the persistence mapper
     * @param gameTurn the turn rolled back to
     * @throws IOException if a marker can't be read or written
     */
    static void rewind(Path dataRoot, ObjectMapper mapper, long gameTurn) throws IOException {
        Path playersDir = dataRoot.resolve("players");
        if (!Files.isDirectory(playersDir)) {
            return;
        }
        try (Stream<Path> listing = Files.list(playersDir)) {
            for (Path folder : listing.filter(Files::isDirectory).sorted().toList()) {
                Path marker = folder.resolve(MARKER);
                if (Files.exists(marker) && mapper.readValue(marker.toFile(), Marker.class).lastPlayedTurn() > gameTurn) {
                    writeMarker(mapper, marker, gameTurn);
                }
            }
        }
    }

    /** Written beside and moved into place, so a crash never leaves a torn marker. */
    private static void writeMarker(ObjectMapper mapper, Path marker, long gameTurn) throws IOException {
        Path temp = Files.createTempFile(marker.getParent(), "bot-", ".json.tmp");
        mapper.writeValue(temp.toFile(), new Marker(gameTurn));
        AtomicFiles.moveReplacingWithRetry(temp, marker);
    }

    /** Statements on the turn before's Firsts and lost crews, some of them, a stance at random. */
    private static void speak(Path dataRoot, ObjectMapper mapper, long gameTurn, String key, PlayerSnapshot state,
            SplittableRandom random) throws IOException {
        for (FrontPage.Loss loss : LossStore.load(dataRoot, mapper, gameTurn - 1)) {
            boolean own = loss.rivalKey().equals(key);
            if (own || random.nextInt(100) < CONDOLENCE_PERCENT) {
                FrontPage.Stance[] stances = own ? TONES : CONDOLENCES;
                Statements.give(dataRoot, mapper, gameTurn, key, state, new SubmitStatementRequest(
                    Math.toIntExact(gameTurn), loss.id(), stances[random.nextInt(stances.length)], ""));
            }
        }
        for (FrontPage page : FrontPageStore.load(dataRoot, mapper)) {
            if (page.firstPage() && page.gameTurn() == gameTurn - 1 && random.nextInt(100) < REACT_PERCENT) {
                // Refused when it's the bot's own First: that's fine.
                Statements.give(dataRoot, mapper, gameTurn, key, state, new SubmitStatementRequest(
                    Math.toIntExact(gameTurn), page.id(), GIVABLE[random.nextInt(GIVABLE.length)], ""));
            }
        }
    }

    private static String clip(String name) {
        return name.length() <= ClaimSlotRequest.MAX_NAME_LENGTH ? name
            : name.substring(0, ClaimSlotRequest.MAX_NAME_LENGTH).strip();
    }
}
