package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.ResolutionEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Test fixtures for the persistence layer: the early-era pack as an action
 * context, and a turn resolution with random events switched off.
 *
 * @author Mario Koehler
 */
final class TestContexts {

    /** The early-era pack with the BASPM-close budget. */
    static final ActionContext EARLY_ERA = new ActionContext(
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era")), BudgetSchedule.baspmClose());

    /** Random events that never fire, so a resolved season is predictable. */
    static final RandomEventRoster QUIET = new RandomEventRoster(0, 0, RandomEventRoster.baspmClose().events());

    private TestContexts() {
    }

    /**
     * Resolves a turn with no goals settled yet and no random events.
     *
     * @param service  the player's service
     * @param gameTurn the turn
     * @return the season's events
     * @throws IOException if the player's files can't be read or written
     */
    static List<ResolutionEvent> resolveQuietly(PlayerStateService service, long gameTurn) throws IOException {
        return service.resolveTurn(gameTurn, new GoalTally(), QUIET, 42L);
    }
}
