package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SeasonResolver}: the phase order, the observer hooks, and that a
 * review follows the player's own seasons, not the game turn.
 *
 * @author Mario Koehler
 */
class SeasonResolverTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final BudgetSchedule SCHEDULE = BudgetSchedule.baspmClose();
    private static final RandomEventRoster QUIET =
        new RandomEventRoster(0, 0, RandomEventRoster.baspmClose().events());

    private static PlayerSnapshot newPlayer() {
        return new PlayerSnapshot("Agency", Archetype.NASA, SCHEDULE.initialFunds(), SCHEDULE.initialPrestige(),
            SCHEDULE.initialFundingRate(), 1);
    }

    @Test
    void phasesRunInTheDocumentedOrder() {
        List<SeasonResolver.Phase> seen = new ArrayList<>();
        SeasonResolver.advanceSeason(newPlayer(),
            new SeasonResolver.Context(CATALOG, SCHEDULE, QUIET, new GoalTally(), 1, 1), new Random(1),
            new SeasonResolver.Observer() {
                @Override
                public void before(SeasonResolver.Phase phase, PlayerSnapshot snapshot) {
                    seen.add(phase);
                }
            });

        assertEquals(List.of(SeasonResolver.Phase.values()), seen);
    }

    @Test
    void reviewsFollowThePlayersOwnSeasonsNotTheGameTurn() {
        // A late joiner on game turn 40, in their own 16th season: the review is due.
        List<ResolutionEvent> due = SeasonResolver.advanceSeason(newPlayer(),
            new SeasonResolver.Context(CATALOG, SCHEDULE, QUIET, new GoalTally(), 40, 16), new Random(1));
        assertTrue(due.stream().anyMatch(BudgetReviewed.class::isInstance));

        // Game turn 32 but the player's own 3rd season: no review.
        List<ResolutionEvent> notDue = SeasonResolver.advanceSeason(newPlayer(),
            new SeasonResolver.Context(CATALOG, SCHEDULE, QUIET, new GoalTally(), 32, 3), new Random(1));
        assertTrue(notDue.stream().noneMatch(BudgetReviewed.class::isInstance));
    }

    @Test
    void theSameSeedResolvesTheSameWay() {
        PlayerSnapshot first = newPlayer();
        PlayerSnapshot second = newPlayer();
        SeasonResolver.Context context = new SeasonResolver.Context(CATALOG, SCHEDULE,
            RandomEventRoster.baspmClose(), new GoalTally(), 3, 3);

        List<ResolutionEvent> a = SeasonResolver.advanceSeason(first, context, new Random(99));
        List<ResolutionEvent> b = SeasonResolver.advanceSeason(second, context, new Random(99));

        assertEquals(a, b);
        assertEquals(first.getFunds(), second.getFunds());
        assertEquals(first.getAstronautCandidates().size(), second.getAstronautCandidates().size());
    }
}
