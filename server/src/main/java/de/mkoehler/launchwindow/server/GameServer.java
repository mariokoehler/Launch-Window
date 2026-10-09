package de.mkoehler.launchwindow.server;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.mkoehler.launchwindow.net.NetworkServer;
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
        ObjectMapper mapper = createMapper();
        accountStore = new AccountStore(dataRoot.resolve("accounts.json"));
        GameMeta meta;
        try {
            meta = GameMetaStore.loadOrCreateDefault(dataRoot.resolve("game-meta.json"), mapper).meta();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read game-meta.json", e);
        }
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
        GameInfoService gameInfo = new GameInfoService(dataRoot, mapper, pack.packId(), pack.hash(), meta,
            Clock.systemDefaultZone(), context.schedule());
        dispatcher = new RequestDispatcher(accountStore, dataRoot, mapper, context, meta, gameInfo);
        scheduler = new TurnScheduler(dataRoot, mapper, Clock.systemDefaultZone(), context,
            RandomEventRoster.baspmClose());
        dispatcher.setTurnForcer(scheduler::resolveNow);
        networkServer = new NetworkServer();
        // Everyone online hears at once that the turn resolved (on this thread, after it's saved).
        scheduler.setOnResolved(turn -> {
            for (int connectionId : dispatcher.loggedInConnections()) {
                networkServer.send(connectionId, new TurnResolvedNotice(turn));
            }
        });
        try {
            networkServer.start(tcpPort);
        } catch (IOException e) {
            throw new IllegalStateException("Could not listen on TCP port " + tcpPort, e);
        }
        Gdx.app.log(TAG, "Launch Window server listening on TCP " + tcpPort + ", data root " + dataRoot.toAbsolutePath());
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
     * handshake).
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
        }
        if (result.closeConnection()) {
            networkServer.sendThenClose(connectionId, result.response());
        } else {
            networkServer.send(connectionId, result.response());
        }
    }

    @Override
    public void dispose() {
        networkServer.stop();
        accountStore.close();
    }
}
