package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Season;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SpaceRaceNews} and its bundled stories.
 *
 * @author Mario Koehler
 */
class SpaceRaceNewsTest {

    private static final SpaceRaceNews NEWS = SpaceRaceNews.load(Path.of("..", "assets", "content", SpaceRaceNews.FILE));

    /**
     * Every story has a unique id, a date the game can reach, text of a readable length, and only
     * letters the game's fonts can draw.
     */
    @Test
    void theBundledStoriesAreWellFormed() {
        assertFalse(NEWS.stories().isEmpty());
        Set<String> ids = new HashSet<>();
        for (SpaceRaceNews.Story story : NEWS.stories()) {
            assertTrue(ids.add(story.id()), "duplicate id " + story.id());
            assertTrue(story.year() >= GameDate.START_YEAR, story.id());
            assertTrue(story.headline().length() <= 60, story.id() + " headline too long");
            assertTrue(story.text().length() <= 460, story.id() + " text too long");
            Stream.of(story.headline(), story.text(), story.nasa(), story.soviet()).filter(text -> text != null)
                .forEach(text -> text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    story.id() + " uses '" + (char) c + "', which the fonts lack")));
            Stream.of(story.nasa(), story.soviet()).filter(text -> text != null)
                .forEach(text -> assertTrue(text.length() <= 260, story.id() + " view too long"));
        }
    }

    /**
     * Sputnik's season offers several stories; the pick is the same for the same seed, varies with
     * the seed, and a season without stories gives none.
     */
    @Test
    void picksOneStoryPerSeasonAndSeed() {
        GameDate fall1957 = new GameDate(1957, Season.FALL);
        assertTrue(NEWS.storiesFor(fall1957).size() >= 2);
        assertEquals(NEWS.pick(fall1957, 42), NEWS.pick(fall1957, 42));
        Set<String> picked = new HashSet<>();
        for (long seed = 0; seed < 50; seed++) {
            picked.add(NEWS.pick(fall1957, seed));
        }
        assertEquals(NEWS.storiesFor(fall1957).size(), picked.size(), "every story gets picked for some player");
        assertNull(NEWS.pick(new GameDate(1990, Season.WINTER), 42));
    }

    /**
     * A player sees their own side's view of a story, when it has one.
     */
    @Test
    void eachSideSeesItsOwnView() {
        SpaceRaceNews.Story sputnik = NEWS.byId("1957-fall-sputnik-1").orElseThrow();
        assertTrue(sputnik.viewFor(Archetype.NASA).orElseThrow().contains("American"));
        assertTrue(sputnik.viewFor(Archetype.SOVIET).orElseThrow().contains("Pravda"));
        assertTrue(NEWS.byId("no-such-story").isEmpty());
    }
}
