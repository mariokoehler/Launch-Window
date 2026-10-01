package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * <p>Tests for {@link MissionConfiguration}.</p>
 *
 * @author Mario Koehler
 */
class MissionConfigurationTest {

    /**
     * Builds a minimal, valid single-season configuration with the given
     * number of seasons, each holding one trivial step.
     *
     * @param seasonCount how many identical seasons to build
     * @return the built season list
     */
    private static List<MissionSeason> seasons(int seasonCount) {
        MissionStep step = new MissionStep("step-1",
            List.of(new MissionStep.ComponentInvolvement("rocket-1", 100)), List.of(), List.of(), false);
        List<MissionSeason> result = new ArrayList<>();
        for (int i = 0; i < seasonCount; i++) {
            result.add(new MissionSeason(List.of(step)));
        }
        return result;
    }

    /**
     * Verifies that a well-formed configuration keeps every field as given.
     */
    @Test
    void storesEveryField() {
        List<MissionSeason> seasons = seasons(1);
        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-std", "sputnik-1", seasons);

        assertEquals("sputnik-1-std", configuration.id());
        assertEquals("sputnik-1", configuration.programId());
        assertEquals(seasons, configuration.seasons());
    }

    /**
     * Verifies that {@code durationInSeasons} is derived from the season
     * list's own size.
     */
    @Test
    void durationInSeasonsMatchesTheSeasonListSize() {
        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-ext", "sputnik-1", seasons(3));

        assertEquals(3, configuration.durationInSeasons());
    }

    /**
     * Verifies that an empty season list is rejected.
     */
    @Test
    void rejectsEmptySeasons() {
        assertThrows(IllegalArgumentException.class,
            () -> new MissionConfiguration("sputnik-1-std", "sputnik-1", List.of()));
    }

    /**
     * Verifies that a blank id or program id is rejected.
     */
    @Test
    void rejectsBlankIds() {
        List<MissionSeason> seasons = seasons(1);
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration(" ", "sputnik-1", seasons));
        assertThrows(IllegalArgumentException.class, () -> new MissionConfiguration("sputnik-1-std", " ", seasons));
    }

    /**
     * Verifies the constructor defensively copies the season list: a
     * mutation to the original list after construction does not affect the
     * stored configuration.
     */
    @Test
    void defensivelyCopiesSeasons() {
        List<MissionSeason> mutable = new ArrayList<>(seasons(1));
        MissionConfiguration configuration = new MissionConfiguration("sputnik-1-std", "sputnik-1", mutable);

        mutable.add(new MissionSeason(seasons(1).get(0).steps()));

        assertEquals(1, configuration.seasons().size());
    }
}
