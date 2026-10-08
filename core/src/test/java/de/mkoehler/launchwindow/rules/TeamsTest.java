package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests teamwork ({@code design.md} &sect;2.2(d)): work seasons, pairs' rapport, and traits shown.
 *
 * @author Mario Koehler
 */
class TeamsTest {

    private static PlayerSnapshot agency(SetSpecialist... engineers) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Hanseatic Space Agency", Archetype.SOVIET, 25_000, 0, 0, 1);
        snapshot.setSetSpecialists(new ArrayList<>(List.of(engineers)));
        return snapshot;
    }

    private static SetSpecialist engineer(String id, TraitState... traits) {
        SetSpecialist engineer = new SetSpecialist(id, "Engineer " + id, 30, 70, 50);
        engineer.setTraits(List.of(traits));
        return engineer;
    }

    private static Teams.Team rd(String... ids) {
        return new Teams.Team(Teams.RD, "r7-sputnik", List.of(ids));
    }

    @Test
    void everyoneInATeamGetsAWorkSeasonAndEachPairDrifts() {
        SetSpecialist a = engineer("set-1");
        SetSpecialist b = engineer("set-2");
        SetSpecialist alone = engineer("set-3");
        PlayerSnapshot snapshot = agency(a, b, alone);
        double affinity = Personality.affinity(snapshot, a, b);

        Teams.advanceSeason(snapshot, List.of(rd("set-1", "set-2"), new Teams.Team(Teams.RD, "atlas", List.of("set-3"))));

        assertEquals(1, a.getWorkSeasons());
        assertEquals(1, b.getWorkSeasons());
        assertEquals(1, alone.getWorkSeasons());
        assertEquals(1, snapshot.getRapports().size());
        Rapport pair = snapshot.getRapports().get(0);
        assertEquals(Personality.RAPPORT_DRIFT * affinity, pair.rapport(), 1e-9);
        assertEquals(affinity, pair.affinity(), 1e-9);
        assertEquals(1, pair.seasonsTogether());
    }

    @Test
    void aTraitShowsAtItsRevealPointWithAnEvent() {
        SetSpecialist a = engineer("set-1", new TraitState(Trait.WORKAHOLIC, 2, false));
        PlayerSnapshot snapshot = agency(a);
        assertTrue(Teams.advanceSeason(snapshot, List.of(rd("set-1"))).isEmpty());
        List<ResolutionEvent> events = Teams.advanceSeason(snapshot, List.of(rd("set-1")));
        assertEquals(List.of(new TraitRevealed(PersonnelGroup.SET, "set-1", "Engineer set-1", Trait.WORKAHOLIC,
            Teams.RD, "r7-sputnik", 2)), events);
        assertTrue(a.getTraits().get(0).revealed());
        assertTrue(Teams.advanceSeason(snapshot, List.of(rd("set-1"))).isEmpty());
    }

    @Test
    void aClashShowsAndGivesAnAbrasiveMemberAway() {
        // Two Abrasive engineers drift down at least RAPPORT_DRIFT x 0.2 a season and clash within a few dozen seasons.
        SetSpecialist a = engineer("set-1", new TraitState(Trait.ABRASIVE, 99, false));
        SetSpecialist b = engineer("set-2", new TraitState(Trait.ABRASIVE, 99, false));
        PlayerSnapshot snapshot = agency(a, b);
        List<ResolutionEvent> seen = new ArrayList<>();
        for (int season = 0; season < 40 && seen.isEmpty(); season++) {
            seen.addAll(Teams.advanceSeason(snapshot, List.of(rd("set-1", "set-2"))));
        }
        assertTrue(seen.get(0) instanceof RelationshipShown shown && !shown.positive(), seen.toString());
        assertEquals(2, seen.stream().filter(TraitRevealed.class::isInstance).count());
        assertTrue(snapshot.getRapports().get(0).shown());
        assertTrue(snapshot.getRapports().get(0).rapport() <= -Personality.RAPPORT_SHOWN_AT);
    }

    @Test
    void aStrongClashShowsBeforeTheLatestRevealAndGivesTheTraitAwayFirst() {
        // At the strongest affinity a pair shows in three seasons, ahead of a reveal due in four.
        SetSpecialist a = engineer("set-1", new TraitState(Trait.ABRASIVE, Personality.REVEAL_MAX_SEASONS, false));
        SetSpecialist b = engineer("set-2");
        PlayerSnapshot snapshot = agency(a, b);
        snapshot.getRapports().add(new Rapport("set-1", "set-2", 0, -1, 0, false));
        List<ResolutionEvent> events = new ArrayList<>();
        for (int season = 0; season < 3; season++) {
            events = Teams.advanceSeason(snapshot, List.of(rd("set-1", "set-2")));
        }
        assertEquals(3, a.getWorkSeasons());
        assertEquals(2, events.size(), events.toString());
        assertTrue(events.get(0) instanceof RelationshipShown shown && !shown.positive());
        assertEquals(new TraitRevealed(PersonnelGroup.SET, "set-1", "Engineer set-1", Trait.ABRASIVE, Teams.RD,
            "r7-sputnik", 3), events.get(1));
    }

    @Test
    void aPairLeavesWithEitherPerson() {
        SetSpecialist a = engineer("set-1");
        SetSpecialist b = engineer("set-2");
        PlayerSnapshot snapshot = agency(a, b);
        Teams.advanceSeason(snapshot, List.of(rd("set-1", "set-2")));
        snapshot.getSetSpecialists().remove(b);
        Teams.advanceSeason(snapshot, List.of(rd("set-1", "set-2")));
        assertTrue(snapshot.getRapports().isEmpty());
        assertEquals(2, a.getWorkSeasons());
    }

    @Test
    void aMissionIsTwoTeamsItsCrewWithCapcomAndItsControllers() {
        PlayerSnapshot snapshot = agency();
        Astronaut pilot = new Astronaut("astro-1", "Pilot", 30, 70, 50);
        snapshot.setAstronauts(new ArrayList<>(List.of(pilot)));
        de.mkoehler.launchwindow.model.InProgressMission mission = new de.mkoehler.launchwindow.model.InProgressMission(
            "m0", "vostok-orbital", List.of("astro-1"), java.util.Arrays.asList("mco-1", null, "astro-1"));
        snapshot.setInProgressMissions(java.util.Map.of("m0", mission));
        List<Teams.Team> teams = Teams.capture(snapshot, de.mkoehler.launchwindow.content.ContentPackLoader.load(
            java.nio.file.Path.of("..", "assets", "content", "early-era")));
        assertEquals(List.of(new Teams.Team(Teams.MISSION, "vostok-orbital", List.of("astro-1")),
            new Teams.Team(Teams.MISSION, "vostok-orbital", List.of("mco-1"))), teams);
    }
}
