package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.RandomEventKind;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the random-event stories: the shipped file has every event's sized count, unique ids and
 * only the placeholders its event can fill, and a player sees no story twice before the event's
 * stories start over.
 *
 * @author Mario Koehler
 */
class EventStoriesTest {

    private static final EventStories STORIES = EventStories.load(Path.of("..", "assets", "content",
        EventStories.FILE));

    /** The person-struck events, the only ones that can name someone. */
    private static final Set<RandomEventKind> PERSON = Set.of(RandomEventKind.BASIC_TRAINING_EXTENSION,
        RandomEventKind.EMPLOYEE_DISMISSED, RandomEventKind.EMPLOYEE_FAILED_BASIC_TRAINING,
        RandomEventKind.EMPLOYEE_RESIGNING, RandomEventKind.EMPLOYEE_UNAVAILABLE, RandomEventKind.PERISHED_ASTRONAUT);

    /**
     * Every event kind has at least its sized count of stories (the bot playouts' sizing,
     * 2026-10-10), and every id is unique.
     */
    @Test
    void everyKindHasItsStories() {
        Set<String> ids = new HashSet<>();
        for (EventStories.Story story : STORIES.stories()) {
            assertTrue(ids.add(story.id()), "duplicate id " + story.id());
        }
        for (RandomEventKind kind : RandomEventKind.values()) {
            long count = STORIES.stories().stream().filter(story -> story.kind() == kind).count();
            int wanted = EventStories.SIZED.getOrDefault(kind, EventStories.SIZED_OTHERWISE);
            assertTrue(count >= wanted, kind + " has " + count + " stories, wants " + wanted);
        }
    }

    /**
     * A story uses only the placeholders its event fills, and no other braces; and none carries a
     * figure, which the report's own line gives.
     */
    @Test
    void placeholdersFitTheirEvent() {
        for (EventStories.Story story : STORIES.stories()) {
            String all = story.headline() + " " + story.text();
            String rest = all;
            if (PERSON.contains(story.kind())) {
                rest = rest.replace(EventStories.NAME, "");
            }
            if (story.kind() == RandomEventKind.RELIABILITY_SETBACK) {
                rest = rest.replace(EventStories.COMPONENT, "");
            }
            if (story.kind() == RandomEventKind.MORALE_BOOST || story.kind() == RandomEventKind.MORALE_DROP) {
                rest = rest.replace(EventStories.GROUP, "");
            }
            assertFalse(rest.contains("{") || rest.contains("}"), story.id() + " has a placeholder it can't fill");
            assertFalse(rest.matches(".*\\d.*"), story.id() + " has a figure");
        }
    }

    /**
     * Picking runs through every story of an event once before any repeats, then starts over.
     */
    @Test
    void noRepeatsUntilEveryStoryHasRun() {
        RandomEventKind kind = RandomEventKind.PRESTIGE_POINTS_BOOST;
        int count = (int) STORIES.stories().stream().filter(story -> story.kind() == kind).count();
        List<String> used = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        SplittableRandom random = new SplittableRandom(5);
        for (int i = 0; i < count; i++) {
            assertTrue(seen.add(STORIES.pick(kind, random, used)), "a repeat before every story ran");
        }
        assertEquals(count, used.size());
        String next = STORIES.pick(kind, random, used);
        assertTrue(seen.contains(next));
        assertEquals(1, used.size(), "the event's stories started over");
    }

    /**
     * Every goal a story waits for exists in both packs, since the stories file is outside the pack,
     * and the hardware overhead waits for a satellite in orbit (2026-10-27).
     */
    @Test
    void requiredGoalsExistInEveryPack() {
        for (String pack : List.of("standard", "early-era")) {
            Set<String> goals = ContentPackLoader.load(Path.of("..", "assets", "content", pack)).goals().stream()
                .map(Goal::id).collect(java.util.stream.Collectors.toSet());
            for (EventStories.Story story : STORIES.stories()) {
                if (story.requiresGoal() != null) {
                    assertTrue(goals.contains(story.requiresGoal()), story.id() + "'s goal isn't in " + pack);
                }
            }
        }
        assertEquals("earth-orbit-satellite", STORIES.byId("pp-satellite-sighting").orElseThrow().requiresGoal());
    }

    /**
     * A story that can't run yet is never picked, and keeps its turn: while only it is fresh, the
     * usable story shown longest ago runs again; once it can run, it comes next.
     */
    @Test
    void anUnusableStoryWaitsItsTurn() {
        RandomEventKind kind = RandomEventKind.PRESTIGE_DROP;
        EventStories stories = new EventStories(List.of(
            new EventStories.Story("a", kind, "A", "a", null),
            new EventStories.Story("b", kind, "B", "b", null),
            new EventStories.Story("gated", kind, "G", "g", "goal")));
        List<String> used = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(1);
        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < 6; i++) {
            drawn.add(stories.pick(kind, random, used, story -> story.requiresGoal() == null));
        }
        assertEquals(Set.of("a", "b"), drawn);
        assertFalse(used.contains("gated"));
        assertEquals("gated", stories.pick(kind, random, used, story -> true));
        assertEquals(null, stories.pick(kind, random, used, story -> false));
    }
}
