package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.net.AppVersion;
import de.mkoehler.launchwindow.net.NetworkClient;
import de.mkoehler.launchwindow.net.messages.ClaimSlotRequest;
import de.mkoehler.launchwindow.net.messages.ClaimSlotResponse;
import de.mkoehler.launchwindow.net.messages.CreateAccountRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoRequest;
import de.mkoehler.launchwindow.net.messages.GameInfoResponse;
import de.mkoehler.launchwindow.net.messages.HandshakeResponse;
import de.mkoehler.launchwindow.net.messages.SaveServerSettingsRequest;
import de.mkoehler.launchwindow.net.messages.ServerSettings;
import de.mkoehler.launchwindow.net.messages.ServerSettingsRequest;
import de.mkoehler.launchwindow.net.messages.ServerSettingsResponse;
import de.mkoehler.launchwindow.net.messages.StartNewGameRequest;
import de.mkoehler.launchwindow.net.messages.NewGameStartedNotice;
import de.mkoehler.launchwindow.net.messages.FetchStateRequest;
import de.mkoehler.launchwindow.net.messages.FetchStateResponse;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole server, as it runs: the admin switches the content pack before anyone has founded an agency, and the
 * server rebuilds its game on it at once, so the next agency is founded on the new pack ({@code ui-spec.md}
 * &sect;4.22, G21); and a new game ends the old one for everyone but keeps the accounts.
 */
class ServerSettingsIntegrationTest {

    private HeadlessApplication application;
    private NetworkClient client;
    private NetworkClient player;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (player != null) {
            player.close();
        }
        if (application != null) {
            application.exit();
        }
    }

    @Test
    void aPackSwitchRebuildsTheGameInPlace(@TempDir Path dataRoot) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 50;
        application = new HeadlessApplication(new GameServer(port, dataRoot), config);
        Path metaFile = dataRoot.resolve("game-meta.json");
        ObjectMapper mapper = GameServer.createMapper();
        client = new NetworkClient();
        connect(port);
        String password = mapper.readValue(metaFile.toFile(), GameMeta.class).getAdminPassword();
        assertTrue(password != null && password.length() == 19, "the server made a password at startup");

        HandshakeResponse handshake = client.sendAndReceive(
            new CreateAccountRequest(AppVersion.getVersion(), "admin", "secret"), HandshakeResponse.class);
        assertTrue(handshake.accepted());
        assertEquals("standard", client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class).contentPackId());
        ServerSettingsResponse read = client.sendAndReceive(new ServerSettingsRequest(password),
            ServerSettingsResponse.class);
        assertEquals(ServerSettingsResponse.Status.OK, read.status(), read.message());
        ServerSettings s = read.settings();
        ServerSettings edit = new ServerSettings(s.version(), 1, 0, -1, s.activeDays(), s.resolutionTime(),
            s.timezone(), s.breaks(), "early-era", GameMeta.EARLY_ERA_DECLARED_GOALS, 7L, true, false,
            List.of(new ServerSettings.Bot("bot-1", "SOVIET", "", "", 0)), null, null, List.of());

        ServerSettingsResponse saved = client.sendAndReceive(SaveServerSettingsRequest.of(password, edit, ""),
            ServerSettingsResponse.class);

        assertEquals(ServerSettingsResponse.Status.OK, saved.status(), saved.message());
        GameInfoResponse info = awaitPack("early-era");
        assertTrue(info.devMode(), "dev mode applies at once");
        assertTrue(Files.isDirectory(dataRoot.resolve("players").resolve("bot-1")), "the new bot has its agency");
        ClaimSlotResponse claim = client.sendAndReceive(
            new ClaimSlotRequest(Archetype.NASA, "Test Agency", "Tester"), ClaimSlotResponse.class);
        assertTrue(claim.success(), claim.message());
        assertTrue(Files.isDirectory(dataRoot.resolve("content").resolve("early-era")));
    }

    @Test
    void aNewGameWipesTheGameButKeepsTheAccountsAndTellsEveryone(@TempDir Path dataRoot) throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.updatesPerSecond = 50;
        application = new HeadlessApplication(new GameServer(port, dataRoot), config);
        Path metaFile = dataRoot.resolve("game-meta.json");
        ObjectMapper mapper = GameServer.createMapper();
        client = new NetworkClient();
        connect(port);
        String password = mapper.readValue(metaFile.toFile(), GameMeta.class).getAdminPassword();
        long oldSeed = mapper.readValue(metaFile.toFile(), GameMeta.class).getGameSeed();
        assertTrue(client.sendAndReceive(new CreateAccountRequest(AppVersion.getVersion(), "admin", "secret"),
            HandshakeResponse.class).accepted());
        player = new NetworkClient();
        java.util.concurrent.CountDownLatch told = new java.util.concurrent.CountDownLatch(1);
        player.setNoticeListener(notice -> {
            if (notice instanceof NewGameStartedNotice) {
                told.countDown();
            }
        });
        player.connect("localhost", port);
        assertTrue(player.sendAndReceive(new CreateAccountRequest(AppVersion.getVersion(), "player", "secret"),
            HandshakeResponse.class).accepted());
        assertTrue(player.sendAndReceive(new ClaimSlotRequest(Archetype.NASA, "Old Agency", "Old Director"),
            ClaimSlotResponse.class).success());
        String oldGame = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class).gameId();
        ServerSettings s = client.sendAndReceive(new ServerSettingsRequest(password), ServerSettingsResponse.class)
            .settings();
        ServerSettings withBot = new ServerSettings(s.version(), 1, 1, -1, s.activeDays(), s.resolutionTime(),
            s.timezone(), s.breaks(), s.contentPack(), s.declaredGoalIds(), null, false, false,
            List.of(new ServerSettings.Bot("bot-1", "SOVIET", "", "", 0)), null, null, List.of());
        ServerSettingsResponse botSaved = client.sendAndReceive(SaveServerSettingsRequest.of(password, withBot, ""),
            ServerSettingsResponse.class);
        assertEquals(ServerSettingsResponse.Status.OK, botSaved.status(), botSaved.message());
        assertTrue(Files.isDirectory(dataRoot.resolve("players").resolve("bot-1")));
        String version = botSaved.settings().version();

        ServerSettingsResponse unconfirmed = client.sendAndReceive(
            new StartNewGameRequest(password, version, "new game"), ServerSettingsResponse.class);
        assertEquals(ServerSettingsResponse.Status.INVALID_FIELD, unconfirmed.status());
        assertTrue(unconfirmed.fieldErrors().containsKey("confirm"));
        ServerSettingsResponse stale = client.sendAndReceive(
            new StartNewGameRequest(password, "old", StartNewGameRequest.CONFIRMATION), ServerSettingsResponse.class);
        assertEquals(ServerSettingsResponse.Status.STALE_SETTINGS, stale.status());
        assertTrue(Files.isDirectory(dataRoot.resolve("players")), "nothing was deleted on a refusal");

        ServerSettingsResponse started = client.sendAndReceive(
            new StartNewGameRequest(password, version, StartNewGameRequest.CONFIRMATION), ServerSettingsResponse.class);

        assertEquals(ServerSettingsResponse.Status.OK, started.status(), started.message());
        assertTrue(told.await(5, java.util.concurrent.TimeUnit.SECONDS), "the player online heard of it");
        GameMeta meta = mapper.readValue(metaFile.toFile(), GameMeta.class);
        assertTrue(meta.getGameSeed() != oldSeed, "a new seed");
        assertEquals(password, meta.getAdminPassword(), "the settings stay");
        assertTrue(Files.exists(dataRoot.resolve("accounts.json")), "the accounts stay");
        try (java.util.stream.Stream<Path> zips = Files.list(dataRoot.resolve(ServerSettingsService.OLD_GAMES))) {
            assertEquals(1, zips.count(), "the old game is zipped");
        }
        assertTrue(!Files.exists(dataRoot.resolve("players").resolve(accountId(dataRoot, mapper, "player"))),
            "the old agency is gone");
        assertTrue(!oldGame.equals(client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class).gameId()),
            "a new game identity, so clients start unread");
        // The player's connection lives on: its next call hears it has no agency, and it founds a new one.
        FetchStateResponse fetched = player.sendAndReceive(new FetchStateRequest(), FetchStateResponse.class);
        assertEquals(ClaimSlotRequest.SLOT_CLAIM_REQUIRED, fetched.message());
        ClaimSlotResponse again = player.sendAndReceive(new ClaimSlotRequest(Archetype.SOVIET, "New Agency", "New"),
            ClaimSlotResponse.class);
        assertTrue(again.success(), again.message());
        assertTrue(Files.isDirectory(dataRoot.resolve("content").resolve("standard")), "the pack is frozen again");
        assertTrue(Files.isDirectory(dataRoot.resolve("players").resolve("bot-1")), "the bot is in the new game too");
        assertTrue(listCount(dataRoot.resolve("players").resolve("bot-1")) > 0, "with a fresh agency");
    }

    private static long listCount(Path dir) throws IOException {
        try (java.util.stream.Stream<Path> entries = Files.list(dir)) {
            return entries.count();
        }
    }

    private static String accountId(Path dataRoot, ObjectMapper mapper, String login) throws IOException {
        return mapper.readTree(dataRoot.resolve("accounts.json").toFile()).get(login).get("id").asText();
    }

    private void connect(int port) throws InterruptedException, IOException {
        IOException last = null;
        for (int i = 0; i < 100; i++) {
            try {
                client.connect("localhost", port);
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(100);
            }
        }
        throw last;
    }

    private GameInfoResponse awaitPack(String pack) throws IOException, InterruptedException {
        GameInfoResponse info = null;
        for (int i = 0; i < 50; i++) {
            info = client.sendAndReceive(new GameInfoRequest(), GameInfoResponse.class);
            if (pack.equals(info.contentPackId())) {
                return info;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("the game info still says " + (info == null ? null : info.contentPackId()));
    }
}
