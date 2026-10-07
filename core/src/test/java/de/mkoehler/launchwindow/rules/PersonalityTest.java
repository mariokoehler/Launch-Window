package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests employees' personalities ({@code design.md} &sect;2.2(d)): trait draws and hints, pair
 * affinity, and the client's stripped copy of the state.
 *
 * @author Mario Koehler
 */
class PersonalityTest {

    private static PlayerSnapshot agency() {
        return new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 25_000, 0, 0, 1);
    }

    @Test
    void everyHintFitsSeveralTraitsAndEveryTraitHasAHint() {
        Set<Trait> covered = EnumSet.noneOf(Trait.class);
        Personality.HINTS.forEach((hint, traits) -> {
            assertTrue(traits.size() >= 2, hint + " would give its trait away");
            covered.addAll(traits);
        });
        assertEquals(EnumSet.allOf(Trait.class), covered);
        Personality.BLAND_HINTS.forEach(hint -> assertFalse(Personality.HINTS.containsKey(hint), hint));
    }

    @Test
    void candidatesGetTraitsTheirGroupCanHaveAndAFittingHint() {
        PlayerSnapshot snapshot = agency();
        int[] counts = new int[3];
        int total = 3_000;
        for (int i = 0; i < total; i++) {
            SetSpecialist candidate = new SetSpecialist("set-" + i, "Test", 30, 70, 50);
            Personality.assign(snapshot, candidate, PersonnelGroup.SET);
            List<TraitState> traits = candidate.getTraits();
            counts[traits.size()]++;
            assertEquals(traits.size(), new HashSet<>(traits.stream().map(TraitState::trait).toList()).size());
            for (TraitState state : traits) {
                assertTrue(state.trait().allowedFor("SET"), state.trait().name());
                assertFalse(state.revealed());
                assertTrue(state.revealAt() >= Personality.REVEAL_MIN_SEASONS
                    && state.revealAt() <= Personality.REVEAL_MAX_SEASONS);
            }
            String hint = candidate.getHint();
            if (traits.isEmpty()) {
                assertTrue(Personality.BLAND_HINTS.contains(hint), hint);
            } else if (!Personality.BLAND_HINTS.contains(hint)) {
                assertTrue(traits.stream().anyMatch(state -> Personality.HINTS.get(hint).contains(state.trait())),
                    hint + " fits none of " + traits);
            }
        }
        // 20 / 50 / 30 %, within a few points.
        assertEquals(20, 100.0 * counts[0] / total, 3);
        assertEquals(50, 100.0 * counts[1] / total, 3);
        assertEquals(30, 100.0 * counts[2] / total, 3);
    }

    @Test
    void theSameCandidateAlwaysGetsTheSamePersonality() {
        Astronaut a = new Astronaut("astro-9", "Test", 30, 70, 50);
        Astronaut b = new Astronaut("astro-9", "Test", 30, 70, 50);
        Personality.assign(agency(), a, PersonnelGroup.ASTRONAUT);
        Personality.assign(agency(), b, PersonnelGroup.ASTRONAUT);
        assertEquals(a.getTraits(), b.getTraits());
        assertEquals(a.getHint(), b.getHint());
    }

    @Test
    void affinityIsSymmetricBoundedAndPushedByTraits() {
        PlayerSnapshot snapshot = agency();
        Astronaut a = new Astronaut("astro-1", "A", 30, 70, 50);
        Astronaut b = new Astronaut("astro-2", "B", 30, 70, 50);
        double plain = Personality.affinity(snapshot, a, b);
        assertEquals(plain, Personality.affinity(snapshot, b, a));
        assertTrue(plain >= -1 && plain <= 1);
        a.setTraits(List.of(new TraitState(Trait.ABRASIVE, 2, false)));
        b.setTraits(List.of(new TraitState(Trait.ABRASIVE, 2, false)));
        double abrasive = Personality.affinity(snapshot, a, b);
        assertEquals(Math.max(-1, plain + 2 * Personality.AFFINITY_ABRASIVE), abrasive, 1e-9);
    }

    @Test
    void theClientsCopyHoldsNothingHidden() {
        PlayerSnapshot snapshot = agency();
        Astronaut known = new Astronaut("astro-1", "A", 30, 70, 50);
        known.setTraits(List.of(new TraitState(Trait.WORKAHOLIC, 3, true), new TraitState(Trait.LONER, 4, false)));
        known.setHint("driven");
        Astronaut other = new Astronaut("astro-2", "B", 30, 70, 50);
        snapshot.setAstronauts(List.of(known, other));
        Astronaut candidate = new Astronaut("astro-3", "C", 30, 70, 50);
        candidate.setTraits(List.of(new TraitState(Trait.ABRASIVE, 2, false)));
        candidate.setHint("independent");
        snapshot.setAstronautCandidates(List.of(candidate));
        snapshot.setRapports(List.of(new Rapport("astro-1", "astro-2", 40, 0.7, 8, true),
            new Rapport("astro-2", "astro-3", 10, -0.5, 2, false)));

        PlayerSnapshot copy = Personality.forClient(snapshot, new ObjectMapper());

        Personnel copied = copy.getAstronauts().get(0);
        assertEquals(List.of(new TraitState(Trait.WORKAHOLIC, 0, true)), copied.getTraits());
        assertEquals("driven", copied.getHint());
        assertTrue(copy.getAstronautCandidates().get(0).getTraits().isEmpty());
        assertEquals("independent", copy.getAstronautCandidates().get(0).getHint());
        assertEquals(List.of(new Rapport("astro-1", "astro-2", 40, 0, 8, true)), copy.getRapports());
        // The real state is untouched.
        assertEquals(2, snapshot.getAstronauts().get(0).getTraits().size());
        assertEquals(2, snapshot.getRapports().size());
        assertNull(Personality.rapport(copy, "astro-2", "astro-3"));
    }
}
