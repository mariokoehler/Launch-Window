package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
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

    /**
     * A press story's timed effects, applied where the PR phase applies them, cover the seasons
     * they say in every phase that reads them: two budgets, two research phases, two pool
     * refreshes, then nothing ({@code PressEffects}' class note).
     */
    @Test
    void pressEffectsCoverTheSeasonsTheySay() {
        PrPress.Story story = new PrPress.Story("fx", PrPress.Tone.GOOD, "h", "t", List.of(
            new PrPress.Effect(PrPress.EffectKind.RD_RESULTS, null, 1.1, 2),
            new PrPress.Effect(PrPress.EffectKind.RUNNING_COSTS, null, 0.95, 2),
            new PrPress.Effect(PrPress.EffectKind.HIRING_POOL, "ASTRONAUT", 3, 2)));
        PlayerSnapshot player = newPlayer();
        List<Double> costs = new ArrayList<>();
        List<Double> research = new ArrayList<>();
        List<Integer> pools = new ArrayList<>();
        for (int season = 1; season <= 3; season++) {
            boolean first = season == 1;
            SeasonResolver.advanceSeason(player,
                new SeasonResolver.Context(CATALOG, SCHEDULE, QUIET, new GoalTally(), season, season),
                new Random(season), new SeasonResolver.Observer() {
                    @Override
                    public void before(SeasonResolver.Phase phase, PlayerSnapshot snapshot) {
                        if (phase == SeasonResolver.Phase.BUDGET) {
                            costs.add(snapshot.getCostMultiplier());
                        } else if (phase == SeasonResolver.Phase.RESEARCH) {
                            research.add(snapshot.getRdResultsMultiplier());
                        }
                    }

                    @Override
                    public void after(SeasonResolver.Phase phase, PlayerSnapshot snapshot,
                            List<ResolutionEvent> events) {
                        if (phase == SeasonResolver.Phase.PR_DUTY && first) {
                            PressEffects.apply(story, new Astronaut("a", "A", 30, 60, 50), snapshot, CATALOG,
                                new Random(0));
                        } else if (phase == SeasonResolver.Phase.HIRING_POOL) {
                            pools.add(snapshot.getAstronautCandidates().size());
                        }
                    }
                });
        }
        assertEquals(List.of(0.95, 0.95, 1.0), costs);
        assertEquals(List.of(1.1, 1.1, 1.0), research);
        assertEquals(List.of(13, 13, 10), pools);
    }
}
