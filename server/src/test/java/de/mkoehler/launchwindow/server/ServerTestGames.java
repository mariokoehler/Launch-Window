package de.mkoehler.launchwindow.server;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentPackHash;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.ScheduleProfile;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Test fixtures for the server: the early-era pack as loaded from the server's own
 * classpath, and game metas with and without dev mode.
 *
 * @author Mario Koehler
 */
public final class ServerTestGames {

    /** The early-era pack, read from the classpath the way the server reads it. */
    public static final ActionContext CONTEXT = new ActionContext(
        ContentPackLoader.loadFromClasspath("content/early-era", ServerTestGames.class.getClassLoader()),
        BudgetSchedule.baspmClose());

    private ServerTestGames() {
    }

    /**
     * Returns a game meta for tests.
     *
     * @param devMode whether development cheats are accepted
     * @return the meta, seeded with 42
     */
    /** The bundled early-era pack's hash, as a frozen copy of it would have. */
    public static final String PACK_HASH =
        ContentPackHash.ofClasspath("content/early-era", ServerTestGames.class.getClassLoader());

    /**
     * Creates the game-info service a test dispatcher uses, on the real clock.
     *
     * @param dataRoot the game's data root
     * @param mapper   the mapper
     * @param meta     the game's configuration
     * @return the service
     */
    public static GameInfoService gameInfo(Path dataRoot, ObjectMapper mapper, GameMeta meta) {
        return new GameInfoService(dataRoot, mapper, "early-era", PACK_HASH, meta, Clock.systemUTC());
    }

    public static GameMeta meta(boolean devMode) {
        GameMeta meta = new GameMeta(ScheduleProfile.daily(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin")), List.of());
        meta.setGameSeed(42);
        meta.setDevMode(devMode);
        meta.setDeclaredGoalIds(GameMeta.EARLY_ERA_DECLARED_GOALS);
        return meta;
    }
}
