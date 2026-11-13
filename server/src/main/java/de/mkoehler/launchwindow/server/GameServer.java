package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.mkoehler.launchwindow.net.NetworkServer;
import de.mkoehler.launchwindow.net.messages.NewGameStartedNotice;
import de.mkoehler.launchwindow.net.messages.ServerSettingsChangedNotice;
import de.mkoehler.launchwindow.net.messages.TurnResolvedNotice;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.rules.WinCondition;
import de.mkoehler.launchwindow.server.accounts.AccountStore;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.GameMetaStore;
import de.mkoehler.launchwindow.server.schedule.TurnScheduler;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;

/**
 * <p>Headless libGDX application hosting the dedicated server: binds
 * {@link NetworkServer}, and on every {@link #render()} tick drains its
 * queued events and routes them through {@link RequestDispatcher} to
 * {@link AccountStore} and the per-player persistence layer, then gives
 * {@link TurnScheduler} a chance to resolve today's turn.</p>
 *
 * <p>{@link TurnScheduler#checkAndResolveIfDue()} is called from this same
 * {@link #render()} tick, on the same thread that drains
 * {@code NetworkServer.poll()}, rather than from a separate thread - so a
 * turn resolution can never run concurrently with
 * {@link RequestDispatcher} handling a request for the same player.</p>
 *
 * @author Mario Koehler
 */
public class GameServer extends ApplicationAdapter {

    private static final String TAG = "GameServer";

    private final int tcpPort;
    private final Path dataRoot;
    private NetworkServer networkServer;
    private AccountStore accountStore;
    private RequestDispatcher dispatcher;
    private TurnScheduler scheduler;
    private ObjectMapper mapper;
    private ServerSettingsService settings;
    private String lastSchedulerFailure;
    private long nextSchedulerCheckNanos = System.nanoTime();

    /** How often the turn scheduler runs; it re-reads {@code game-meta.json} each time. */
    private static final long SCHEDULER_INTERVAL_NANOS = 1_000_000_000L;

    /**
     * Creates the application.
     *
     * @param tcpPort  the TCP port to listen on
     * @param dataRoot the server's data root directory - accounts and every
     *                 player's own data live under it
     */
    public GameServer(int tcpPort, Path dataRoot) {
        this.tcpPort = tcpPort;
        this.dataRoot = dataRoot;
    }

    /**
     * Builds the single {@link ObjectMapper} configuration every store in
     * this application shares: {@link JavaTimeModule} for the
     * {@code java.time} fields {@link de.mkoehler.launchwindow.server.schedule.GameMeta}
     * and {@link de.mkoehler.launchwindow.server.schedule.SchedulerState}
     * use, with timestamp-array output disabled so the host-edited
     * {@code game-meta.json} reads as {@code "09:00"} rather than
     * {@code [9,0]}. A single shared factory, rather than each caller (and
     * each test) configuring its own mapper, so this configuration can only
     * drift out of sync with itself in one place.
     *
     * @return a newly configured mapper
     */
    public static ObjectMapper createMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public void create() {
        mapper = createMapper();
        accountStore = new AccountStore(dataRoot.resolve("accounts.json"));
        Path metaFile = dataRoot.resolve("game-meta.json");
        GameMeta meta;
        try {
            meta = GameMetaStore.loadOrCreateDefault(metaFile, mapper).meta();
            // At startup only: the scheduler's every-tick read must never write the file (ui-spec.md section 4.22).
            String password = ServerSettingsService.ensureAdminPassword(metaFile, mapper);
            meta.setAdminPassword(password);
            System.out.println("Admin password (game-meta.json, adminPassword): " + password);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read game-meta.json", e);
        }
        settings = new ServerSettingsService(dataRoot, mapper, GameServer.class.getClassLoader(),
            Clock.systemDefaultZone());
        networkServer = new NetworkServer();
        startGame(meta);
        try {
            networkServer.start(tcpPort);
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on TCP port " + tcpPort, e);
        }
        Gdx.app.log(TAG, "Launch Window server listening on TCP " + tcpPort + ", data root " + dataRoot.toAbsolutePath());
    }

    /**
     * Builds the game from its settings: the frozen pack, the rules' context, the bots' slots, the game info, the
     * dispatcher and the scheduler. Run at startup, and again when the admin switches the content pack (possible
     * only while no agency exists and no turn has resolved, ui-spec.md section 4.22), so nobody can found an agency
     * against the old pack. The logged-in connections carry over.
     *
     * @param meta the settings
     */
    private void startGame(GameMeta meta) {
        // The game loads only its own frozen copy of the pack, made on the first start, and pins it for the
        // server's life (design.md section 5, M5 "Content packs").
        FrozenContentPack pack;
        try {
            pack = FrozenContentPack.freeze(dataRoot, meta.getContentPack(), GameServer.class.getClassLoader());
        } catch (IOException e) {
            throw new IllegalStateException("Could not freeze content pack " + meta.getContentPack(), e);
        }
        ContentCatalog catalog = ContentPackLoader.load(pack.directory());
        // A bad declared goal id must fail here, at startup, not stall every turn's win check mid-game.
        WinCondition.evaluate(java.util.Map.of(), meta.getDeclaredGoalIds(), catalog);
        ActionContext context = new ActionContext(catalog, BudgetSchedule.baspmClose());
        // The filler bots' slots, so they're in the race before the first turn (design.md section 5).
        try {
            de.mkoehler.launchwindow.server.schedule.BotPlayers.ensureSlots(dataRoot, mapper, context, meta);
        } catch (IOException e) {
            throw new IllegalStateException("Could not make the bots' slots", e);
        }
        GameInfoService gameInfo = new GameInfoService(dataRoot, mapper, pack.packId(), pack.hash(), meta,
            Clock.systemDefaultZone(), context.schedule());
        RequestDispatcher previous = dispatcher;
        dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper, context, meta, gameInfo);
        if (previous != null) {
            dispatcher.adoptConnections(previous);
        }
        dispatcher.setServerSettings(settings, networkServer::remoteHost);
        scheduler = new TurnScheduler(dataRoot, mapper, Clock.systemDefaultZone(), context,
            RandomEventRoster.baspmClose());
        dispatcher.setTurnForcer(scheduler::resolveNow);
        refreshStanding();
        // Everyone online hears at once that the turn resolved (on this thread, after it's saved).
        scheduler.setOnResolved(turn -> {
            refreshStanding();
            for (int connectionId : dispatcher.loggedInConnections()) {
                networkServer.send(connectionId, new TurnResolvedNotice(turn));
            }
        });
    }

    @Override
    public void render() {
        networkServer.poll(new NetworkServer.Handler() {
            @Override
            public void onMessage(int connectionId, Object message) {
                dispatch(connectionId, message);
            }

            @Override
            public void onDisconnect(int connectionId) {
                dispatcher.handleDisconnect(connectionId);
            }
        });
        long now = System.nanoTime();
        if (now - nextSchedulerCheckNanos < 0) {
            return;
        }
        nextSchedulerCheckNanos = now + SCHEDULER_INTERVAL_NANOS;
        try {
            scheduler.checkAndResolveIfDue();
            lastSchedulerFailure = null;
        } catch (IOException | RuntimeException e) {
            // Anything thrown here would end the render thread, and with it all NetworkServer
            // polling - caught broadly, not just IOException, since a host's typo in
            // game-meta.json (re-read every tick) surfaces as IllegalStateException/
            // NullPointerException, not an I/O failure. Keyed on toString(), never null, so a
            // persistent error logs once rather than every tick forever.
            String failure = e.toString();
            if (!Objects.equals(failure, lastSchedulerFailure)) {
                lastSchedulerFailure = failure;
                Gdx.app.error(TAG, "Turn resolution failed, will retry next tick: " + failure, e);
            }
        }
    }

    /**
     * Routes one received message through {@link RequestDispatcher} and
     * sends its response back on the same connection, closing the
     * connection afterward if the dispatcher says to (a rejected
     * handshake). A handler that throws is logged and its connection
     * closed, so a bug in one request never stops the server.
     *
     * @param connectionId the connection the message arrived on
     * @param message      the message
     */
    private void dispatch(int connectionId, Object message) {
        RequestDispatcher.DispatchResult result;
        try {
            result = dispatcher.handle(connectionId, message);
        } catch (IllegalArgumentException e) {
            Gdx.app.error(TAG, e.getMessage());
            return;
        } catch (RuntimeException e) {
            // A handler's bug must never end the render thread: the server would keep its port open
            // and answer nothing. The client hears the connection close instead of waiting it out.
            Gdx.app.error(TAG, "Request " + message.getClass().getSimpleName() + " failed; connection "
                + connectionId + " closed", e);
            networkServer.close(connectionId);
            return;
        }
        if (result.closeConnection()) {
            networkServer.sendThenClose(connectionId, result.response());
        } else {
            networkServer.send(connectionId, result.response());
        }
        // Taken before a rebuild, which replaces the dispatcher that holds them.
        boolean settingsChanged = dispatcher.takeSettingsChanged();
        boolean newGame = dispatcher.takeNewGameStarted();
        if (dispatcher.takePackChanged() || newGame) {
            rebuildGame();
        }
        if (newGame) {
            // Every agency is gone: everyone online goes back to Connect and founds a new one (ui-spec.md 4.22).
            for (int id : dispatcher.loggedInConnections()) {
                networkServer.send(id, new NewGameStartedNotice());
            }
        } else if (settingsChanged) {
            // Everyone online fetches the game info again: the countdown, the Dev button (ui-spec.md section 4.22).
            for (int id : dispatcher.loggedInConnections()) {
                networkServer.send(id, new ServerSettingsChangedNotice());
            }
        }
    }

    /** Rebuilds the game from the saved settings, after the admin switched the pack or started a new game. */
    private void rebuildGame() {
        RequestDispatcher current = dispatcher;
        try {
            GameMeta meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
            startGame(meta);
            Gdx.app.log(TAG, "Rebuilt the game on content pack " + meta.getContentPack());
        } catch (IOException | RuntimeException e) {
            dispatcher = current;
            Gdx.app.error(TAG, "Couldn't rebuild the game on the new pack; restart the server", e);
        }
    }

    @Override
    public void dispose() {
        networkServer.stop();
        accountStore.close();
    }

    /** Reads what the standing charts need; a failure only costs the first request the same read. */
    private void refreshStanding() {
        try {
            dispatcher.refreshStanding();
        } catch (java.io.IOException | RuntimeException e) {
            Gdx.app.error(TAG, "Couldn't read the standing: " + e.getMessage());
        }
    }
}
