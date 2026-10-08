package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.rules.Chemistry;
import de.mkoehler.launchwindow.rules.Personality;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what the people screens show ({@code ui-spec.md} components 20 to 22): levels, wording,
 * relationships, and that every preview counts only what the player knows.
 *
 * @author Mario Koehler
 */
class PeopleTest {

    private static final ContentCatalog PACK = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final MissionComponent REDSTONE = new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95,
        90, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());

    private static SetSpecialist engineer(String id, String name, TraitState... traits) {
        SetSpecialist engineer = new SetSpecialist(id, name, 30, 70, 50);
        engineer.setSkill(SetSkill.ROCKETS, 50);
        engineer.setTraits(List.of(traits));
        engineer.setWorkSeasons(Personality.REVEAL_MAX_SEASONS);
        return engineer;
    }

    private static PlayerSnapshot agency() {
        return new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
    }

    @Test
    void levelsAndPercentagesFollowTheSpec() {
        assertEquals(People.Level.POOR, People.Level.of(-0.51));
        assertEquals(People.Level.STRAINED, People.Level.of(-0.5));
        assertEquals(People.Level.STRAINED, People.Level.of(-0.16));
        assertEquals(People.Level.NEUTRAL, People.Level.of(-0.15));
        assertEquals(People.Level.NEUTRAL, People.Level.of(0.15));
        assertEquals(People.Level.GOOD, People.Level.of(0.16));
        assertEquals(People.Level.GOOD, People.Level.of(0.5));
        assertEquals(People.Level.EXCELLENT, People.Level.of(0.51));
        // The output band, not a hard-coded 15.
        assertEquals(Math.round(Chemistry.OUTPUT_BAND * 100), People.percent(1));
        assertEquals("+4 %", People.percentText(0.27));
        assertEquals("−11 %", People.percentText(-0.73));
        assertEquals("±0 %", People.percentText(0.01));
    }

    @Test
    void onlyRevealedTraitsAndShownPairsReachTheScreen() {
        SetSpecialist known = engineer("set-1", "Wanda Kowalczyk", new TraitState(Trait.TEAM_PLAYER, 2, true),
            new TraitState(Trait.LONER, 3, false));
        SetSpecialist other = engineer("set-2", "Gunnar Lindqvist");
        SetSpecialist third = engineer("set-3", "Grace Okada");
        PlayerSnapshot state = agency();
        state.setSetSpecialists(new ArrayList<>(List.of(known, other, third)));
        state.setRapports(List.of(new Rapport("set-1", "set-2", 45, 1, 9, true),
            new Rapport("set-1", "set-3", -25, -1, 5, false)));

        assertEquals(List.of(Trait.TEAM_PLAYER), People.traits(known));
        List<People.Relationship> relationships = People.relationships(state, known);
        assertEquals(1, relationships.size());
        assertEquals("set-2", relationships.get(0).partner().getId());
        assertTrue(relationships.get(0).positive());
        assertEquals(9, relationships.get(0).seasonsTogether());
        assertEquals(1, People.marks(state, known, List.of("set-2", "set-3")).size());
        assertTrue(People.marks(state, known, List.of("set-3")).isEmpty());

        Chemistry.Result chemistry = People.research(state, REDSTONE, List.of(known, other), third);
        List<People.ReasonLine> reasons = People.reasons(state, chemistry);
        assertEquals(List.of(new People.ReasonLine("rel_good", "Kowalczyk and Lindqvist work well together", 1),
            new People.ReasonLine("trait_team", "Team player: Kowalczyk", 1)), reasons);
        // The hidden Loner and the unshown clash don't count: the shown pair's share plus the Team player.
        assertEquals(0.45 / 3 + Chemistry.TEAM_PLAYER, chemistry.value(), 1e-9);
    }

    @Test
    void someoneNewMakesThePreviewUncertain() {
        SetSpecialist a = engineer("set-1", "A B");
        SetSpecialist b = engineer("set-2", "C D");
        SetSpecialist fresh = engineer("set-3", "E F");
        fresh.setWorkSeasons(1);
        PlayerSnapshot state = agency();
        state.setSetSpecialists(new ArrayList<>(List.of(a, b, fresh)));
        assertFalse(People.research(state, REDSTONE, List.of(a, b), null).unknownFactors());
        assertTrue(People.research(state, REDSTONE, List.of(a, b), fresh).unknownFactors());
        assertTrue(People.gettingToKnow(fresh));
        assertFalse(People.gettingToKnow(a));
    }

    @Test
    void thePickerPreviewSwapsTheSlotsHolder() {
        MissionConfiguration configuration = PACK.missionConfigurations().iterator().next();
        Astronaut pilot = new Astronaut("astro-1", "Yelena Ivanova", 30, 70, 50);
        Astronaut capcom = new Astronaut("astro-2", "Mikhail Petrov", 30, 70, 50);
        Astronaut friend = new Astronaut("astro-3", "Serik Akhmetov", 30, 70, 50);
        MissionControlOfficer booster = new MissionControlOfficer("mco-1", "Levan Beridze", 30, 70, 50);
        for (var person : List.of(pilot, capcom, friend, booster)) {
            person.setWorkSeasons(Personality.REVEAL_MAX_SEASONS);
        }
        PlayerSnapshot state = agency();
        state.setAstronauts(new ArrayList<>(List.of(pilot, capcom, friend)));
        state.setMissionControlOfficers(new ArrayList<>(List.of(booster)));
        state.setRapports(List.of(new Rapport("astro-1", "astro-2", -60, -1, 12, true),
            new Rapport("astro-1", "astro-3", 60, 1, 12, true)));
        List<String> crew = Arrays.asList("astro-1");
        List<String> control = new ArrayList<>(Collections.nCopies(2, null));
        control.set(0, "astro-2");
        control.set(1, "mco-1");

        Chemistry.MissionChemistry now = People.mission(state, configuration, crew, control);
        Chemistry.MissionChemistry swapped = People.missionWith(state, configuration, crew, control, false, 0,
            "astro-3");
        // The crew with CAPCOM is a team of two, one pair: the clash goes, the friendship comes.
        assertEquals(-0.6, now.crew().value(), 1e-9);
        assertEquals(0.6, swapped.crew().value(), 1e-9);
        // The controller is a team of their own, untouched by either.
        assertEquals(0, now.consoles().value(), 1e-9);
        assertEquals(0, swapped.consoles().value(), 1e-9);
        // The draft itself is untouched.
        assertEquals("astro-2", control.get(0));
        assertEquals(List.of("astro-1", "astro-2", "mco-1"), People.teamIds(crew, control));
        assertEquals(List.of("astro-1", "astro-2"), People.teamIds(state, crew, control, true));
        assertEquals(List.of("mco-1"), People.teamIds(state, crew, control, false));
    }

    @Test
    void wordsHaveNoPronouns() {
        for (Trait trait : Trait.values()) {
            String effect = " " + TraitTexts.effect(trait).toLowerCase(java.util.Locale.ROOT) + " ";
            for (String pronoun : List.of(" they ", " they're ", " them ", " their ", " he ", " she ", " his ",
                " her ", " him ")) {
                assertFalse(effect.replaceAll("[.,]", " ").contains(pronoun), trait + ": " + effect);
            }
        }
        assertEquals("Kowalczyk", People.surname("Wanda Kowalczyk"));
        assertEquals("“keen to learn”", People.hintQuote("keen-to-learn"));
        assertEquals("12 seasons together", People.seasonsTogether(12));
    }
}
