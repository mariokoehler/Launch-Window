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
 * &sect;4.22, G21).
 */
class ServerSettingsIntegrationTest {

    private HeadlessApplication application;
    private NetworkClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
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
