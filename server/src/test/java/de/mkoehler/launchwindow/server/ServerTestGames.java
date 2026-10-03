package de.mkoehler.launchwindow.server;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.server.schedule.GameMeta;
import de.mkoehler.launchwindow.server.schedule.ScheduleProfile;

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
    public static GameMeta meta(boolean devMode) {
        GameMeta meta = new GameMeta(ScheduleProfile.daily(LocalTime.of(9, 0), ZoneId.of("Europe/Berlin")), List.of());
        meta.setGameSeed(42);
        meta.setDevMode(devMode);
        return meta;
    }
}
