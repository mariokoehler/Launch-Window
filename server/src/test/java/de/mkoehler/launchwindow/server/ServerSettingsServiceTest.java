package de.mkoehler.launchwindow.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.SaveServerSettingsRequest;
import de.mkoehler.launchwindow.net.messages.ServerSettings;
import de.mkoehler.launchwindow.net.messages.ServerSettingsResponse;
import de.mkoehler.launchwindow.rules.WinCondition;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameMetaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The admin's server settings ({@code ui-spec.md} &sect;4.22, G21): the password and its lockout, the locks, and
 * that a save never writes a file the server can't start from.
 */
class ServerSettingsServiceTest {

    private static final String PASSWORD = "abcd-efgh-jkmn-pqrs";

    private final ObjectMapper mapper = GameServer.createMapper();
    @TempDir
    Path dataRoot;
    private ServerSettingsService service;
    private MutableClock clock;

    @BeforeEach
    void setUp() throws IOException {
        Path file = dataRoot.resolve("game-meta.json");
        GameMeta meta = GameMetaStore.loadOrCreateDefault(file, mapper).meta();
        meta.setGameSeed(1234L);
        meta.setAdminPassword(PASSWORD);
        GameMetaStore.save(file, meta, mapper);
        clock = new MutableClock();
        service = new ServerSettingsService(dataRoot, mapper, getClass().getClassLoader(), clock);
    }

    private static GameInfoResponse info(long turn) {
        return GameInfoResponse.success("standard", "hash", List.of(), turn, turn > 1, -1, 0, null, List.of(),
            List.of(), 0);
    }

    private ServerSettings read(long turn) {
        ServerSettingsResponse response = service.read(PASSWORD, "host", info(turn));
        assertEquals(ServerSettingsResponse.Status.OK, response.status(), response.message());
        return response.settings();
    }

    private ServerSettingsService.SaveOutcome save(ServerSettings edit, long turn) {
        return service.save(SaveServerSettingsRequest.of(PASSWORD, edit, ""), "host", info(turn), () -> info(turn));
    }

    private GameMeta file() throws IOException {
        return mapper.readValue(dataRoot.resolve("game-meta.json").toFile(), GameMeta.class);
    }

    @Test
    void ensureAdminPasswordMakesOneOnlyWhenMissing() throws IOException {
        Path file = dataRoot.resolve("game-meta.json");
        assertEquals(PASSWORD, ServerSettingsService.ensureAdminPassword(file, mapper));
        GameMeta meta = file();
        meta.setAdminPassword(null);
        GameMetaStore.save(file, meta, mapper);

        String made = ServerSettingsService.ensureAdminPassword(file, mapper);

        assertTrue(made.matches("[a-z2-9]{4}(-[a-z2-9]{4}){3}"), made);
        assertEquals(made, file().getAdminPassword());
    }

    @Test
    void aBlankPasswordInTheFileRefusesEveryTry() throws IOException {
        GameMeta meta = file();
        meta.setAdminPassword("");
        GameMetaStore.save(dataRoot.resolve("game-meta.json"), meta, mapper);

        assertEquals(ServerSettingsResponse.Status.WRONG_ADMIN_PASSWORD,
            service.read("", "host", info(1)).status());
    }

    @Test
    void fiveWrongTriesLockTheAddressOutForTenMinutes() {
        for (int i = 0; i < ServerSettingsService.MAX_TRIES - 1; i++) {
            assertEquals(ServerSettingsResponse.Status.WRONG_ADMIN_PASSWORD,
                service.read("wrong", "host", info(1)).status());
        }
        ServerSettingsResponse locked = service.read("wrong", "host", info(1));
        assertEquals(ServerSettingsResponse.Status.ADMIN_LOCKED_OUT, locked.status());
        assertEquals(10, locked.minutesLeft());
        assertEquals(ServerSettingsResponse.Status.ADMIN_LOCKED_OUT, service.read(PASSWORD, "host", info(1)).status());
        assertEquals(ServerSettingsResponse.Status.OK, service.read(PASSWORD, "other", info(1)).status());

        clock.advance(ServerSettingsService.WINDOW_MILLIS + 1);

        assertEquals(ServerSettingsResponse.Status.OK, service.read(PASSWORD, "host", info(1)).status());
    }

    @Test
    void theReadNeverCarriesThePassword() {
        ServerSettingsResponse response = service.read(PASSWORD, "host", info(1));
        assertFalse(new String(ServerSettings.gzip(response.settings())).contains(PASSWORD));
        assertFalse(mapper.valueToTree(response.settings()).toString().contains(PASSWORD));
    }

    @Test
    void aSaveWithoutChangesKeepsTheFile() throws IOException {
        GameMeta before = file();

        ServerSettingsService.SaveOutcome outcome = save(read(1), 1);

        assertEquals(ServerSettingsResponse.Status.OK, outcome.response().status(), outcome.response().message());
        assertEquals(mapper.valueToTree(before), mapper.valueToTree(file()));
        assertFalse(outcome.packChanged());
    }

    @Test
    void aSaveOverAHandEditIsStale() throws IOException {
        ServerSettings settings = read(1);
        GameMeta meta = file();
        meta.setDevMode(true);
        GameMetaStore.save(dataRoot.resolve("game-meta.json"), meta, mapper);

        assertEquals(ServerSettingsResponse.Status.STALE_SETTINGS, save(settings, 1).response().status());
        assertTrue(file().isDevMode());
    }

    @Test
    void refusedFieldsSaveNothing() throws IOException {
        ServerSettings s = read(1);
        ServerSettings edit = new ServerSettings(s.version(), 1, 0, -1, List.of(), "25:00", "Mars/Olympus",
            List.of(new ServerSettings.Break("2027-02-10", "2027-02-01")), "nowhere", List.of(), 5L, true, false,
            List.of(), null, null, List.of());

        ServerSettingsService.SaveOutcome outcome = save(edit, 1);

        assertEquals(ServerSettingsResponse.Status.INVALID_FIELD, outcome.response().status());
        Map<String, String> errors = outcome.response().fieldErrors();
        assertEquals("Pick at least one day.", errors.get("days"));
        assertTrue(errors.containsKey("time"));
        assertEquals("Not a time zone.", errors.get("timezone"));
        assertEquals("A break ends before it starts.", errors.get("breaks.0"));
        assertEquals("That pack isn't on this server.", errors.get("pack"));
        assertEquals("At least one goal.", errors.get("goals"));
        assertFalse(file().isDevMode());
        assertEquals(1234L, file().getGameSeed());
    }

    @Test
    void theSeedHidesOnceAnAgencyExistsAndSurvivesASave() throws IOException {
        Files.createDirectories(dataRoot.resolve("players").resolve("someone"));
        ServerSettings settings = read(1);
        assertNull(settings.gameSeed());
        assertEquals(ServerSettingsService.LOCK_AGENCY, settings.packLock());
        ServerSettings edit = new ServerSettings(settings.version(), 1, 0, -1, settings.activeDays(),
            settings.resolutionTime(), settings.timezone(), settings.breaks(), "early-era",
            GameMeta.EARLY_ERA_DECLARED_GOALS, null, false, false, List.of(), null, null, List.of());

        ServerSettingsService.SaveOutcome outcome = save(edit, 1);

        assertEquals(ServerSettingsResponse.Status.OK, outcome.response().status(), outcome.response().message());
        assertEquals(1234L, file().getGameSeed());
        assertEquals("standard", file().getContentPack());
        assertEquals(PASSWORD, file().getAdminPassword());
    }

    @Test
    void theGoalsLockOnceATurnResolved() throws IOException {
        ServerSettings settings = read(3);
        assertEquals(ServerSettingsService.LOCK_TURN, settings.goalsLock());
        ServerSettings edit = new ServerSettings(settings.version(), 3, 0, -1, settings.activeDays(),
            settings.resolutionTime(), settings.timezone(), settings.breaks(), settings.contentPack(),
            List.of("earth-orbit-satellite"), null, false, false, List.of(), null, null, List.of());

        assertEquals(ServerSettingsResponse.Status.OK, save(edit, 3).response().status());
        assertEquals(GameMeta.STANDARD_DECLARED_GOALS, file().getDeclaredGoalIds());
    }

    @Test
    void aPackSwitchSavesAFileTheServerStartsFrom() throws IOException {
        ServerSettings settings = read(1);
        List<ServerSettings.Bot> bots = new ArrayList<>(List.of(new ServerSettings.Bot("bot-1", "SOVIET", "", "", 0)));
        ServerSettings edit = new ServerSettings(settings.version(), 1, 0, -1, List.of("MONDAY", "FRIDAY"), "18:30",
            "America/New_York", List.of(new ServerSettings.Break("2026-12-24", "2027-01-02")), "early-era",
            GameMeta.EARLY_ERA_DECLARED_GOALS, 99L, true, true, bots, null, null, List.of());

        ServerSettingsService.SaveOutcome outcome = save(edit, 1);

        assertEquals(ServerSettingsResponse.Status.OK, outcome.response().status(), outcome.response().message());
        assertTrue(outcome.packChanged());
        GameMeta saved = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        assertEquals("early-era", saved.getContentPack());
        assertEquals(99L, saved.getGameSeed());
        assertEquals("18:30", saved.getSchedule().getResolutionTime().toString());
        assertEquals(1, saved.getBlackouts().size());
        assertTrue(saved.isDevArchiveOpen());
        assertEquals(PASSWORD, saved.getAdminPassword());
        // What GameServer.startGame checks before it runs.
        FrozenContentPack pack = FrozenContentPack.freeze(dataRoot, saved.getContentPack(),
            getClass().getClassLoader());
        ContentCatalog catalog = ContentPackLoader.load(pack.directory());
        WinCondition.evaluate(Map.of(), saved.getDeclaredGoalIds(), catalog);
        assertNotNull(outcome.response().settings());
    }

    @Test
    void aGoalTheChosenPackLacksIsRefused() throws IOException {
        ServerSettings s = read(1);
        ServerSettings edit = new ServerSettings(s.version(), 1, 0, -1, s.activeDays(), s.resolutionTime(),
            s.timezone(), s.breaks(), "early-era", List.of("crewed-lunar-landing"), null, false, false, List.of(),
            null, null, List.of());

        assertTrue(save(edit, 1).response().fieldErrors().get("goals").startsWith("That goal isn't in this pack"));
    }

    @Test
    void aNewPasswordNeedsEightCharacters() throws IOException {
        ServerSettings s = read(1);
        ServerSettingsService.SaveOutcome shortOne = service.save(SaveServerSettingsRequest.of(PASSWORD, s, "short"),
            "host", info(1), () -> info(1));
        assertEquals("At least 8 characters.", shortOne.response().fieldErrors().get("password"));

        service.save(SaveServerSettingsRequest.of(PASSWORD, s, "a-longer-one"), "host", info(1), () -> info(1));

        assertEquals("a-longer-one", file().getAdminPassword());
    }

    @Test
    void aBotInTheRaceStaysAsTheFileHasIt() throws IOException {
        GameMeta meta = file();
        meta.setBots(List.of(new de.mkoehler.launchwindow.server.schedule.BotPlayer("bot-1",
            de.mkoehler.launchwindow.model.Archetype.NASA, "Old Name", null)));
        GameMetaStore.save(dataRoot.resolve("game-meta.json"), meta, mapper);
        Files.createDirectories(dataRoot.resolve("players").resolve("bot-1"));
        ServerSettings s = read(1);
        ServerSettings edit = new ServerSettings(s.version(), 1, 0, -1, s.activeDays(), s.resolutionTime(),
            s.timezone(), s.breaks(), s.contentPack(), s.declaredGoalIds(), null, false, false,
            List.of(new ServerSettings.Bot("bot-2", "SOVIET", "Taken Name", "", 0)), null, null, List.of());

        assertEquals(ServerSettingsResponse.Status.OK, save(edit, 1).response().status());

        List<de.mkoehler.launchwindow.server.schedule.BotPlayer> bots = file().getBots();
        assertEquals(2, bots.size());
        assertEquals("Old Name", bots.get(0).agencyName());
        assertEquals("bot-2", bots.get(1).id());
    }

    /** A clock the test moves. */
    private static final class MutableClock extends Clock {
        private long millis = 1_800_000_000_000L;

        void advance(long by) {
            millis += by;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }
}
