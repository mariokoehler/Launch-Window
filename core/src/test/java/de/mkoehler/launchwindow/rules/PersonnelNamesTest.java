package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link PersonnelNames} (gap G5).
 *
 * @author Mario Koehler
 */
class PersonnelNamesTest {

    /**
     * A candidate's name is stable for the same id and agency, and varies across candidates.
     */
    @Test
    void namesAreStableAndVaried() {
        assertEquals(PersonnelNames.forCandidate("p12", Archetype.SOVIET, "OKB-1"),
            PersonnelNames.forCandidate("p12", Archetype.SOVIET, "OKB-1"));
        Set<String> names = new HashSet<>();
        for (int i = 1; i <= 40; i++) {
            names.add(PersonnelNames.forCandidate("p" + i, Archetype.NASA, "Orion"));
        }
        assertTrue(names.size() > 30, "40 candidates get mostly different names: " + names.size());
    }

    /**
     * Russian surnames take their feminine form.
     */
    @Test
    void feminineSurnames() {
        assertEquals("Orlova", PersonnelNames.feminine("Orlov"));
        assertEquals("Kalinina", PersonnelNames.feminine("Kalinin"));
        assertEquals("Polyanskaya", PersonnelNames.feminine("Polyansky"));
        assertEquals("Rudenko", PersonnelNames.feminine("Rudenko"));
    }

    /**
     * The hiring pool names its candidates. Two agencies with the same pool seed get the same skills
     * but different names: a name depends on the agency, the skills only on the pool's stream. (That
     * names draw nothing from the pool's stream is shown by the playout harness's recorded numbers
     * staying unchanged, not by this test.)
     */
    @Test
    void thePoolNamesCandidatesWithoutTouchingItsStream() {
        PlayerSnapshot first = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        PlayerSnapshot second = new PlayerSnapshot("Vega", Archetype.SOVIET, 0, 0, 0, 1);
        HiringPool.refreshCandidates(first, new SplittableRandom(7));
        HiringPool.refreshCandidates(second, new SplittableRandom(7));
        assertFalse(first.getSetSpecialistCandidates().get(0).getName().equals("Candidate"));
        assertEquals(first.getSetSpecialistCandidates().get(0).getSkills(),
            second.getSetSpecialistCandidates().get(0).getSkills());
        assertNotEquals(first.getSetSpecialistCandidates().get(0).getName(),
            second.getSetSpecialistCandidates().get(0).getName());
    }
}
