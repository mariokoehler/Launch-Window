package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests team chemistry ({@code design.md} &sect;2.2(d)): its value for known teams, what it does to
 * R&amp;D, and that the client's known-only preview hides what's hidden.
 *
 * @author Mario Koehler
 */
class ChemistryTest {

    private static final MissionComponent REDSTONE = new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95,
        90, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
    private static final ContentCatalog CATALOG = new ContentCatalog(List.of(), List.of(), List.of(), List.of(REDSTONE));

    private static SetSpecialist engineer(String id, double skill, int slot, TraitState... traits) {
        SetSpecialist engineer = new SetSpecialist(id, "Engineer " + id, 30, 70, 50);
        engineer.setSkill(SetSkill.ROCKETS, skill);
        engineer.setAssignmentState(AssignmentState.ASSIGNED);
        engineer.setRdTargetComponentId("rocket-redstone");
        engineer.setRdSlot(slot);
        engineer.setTraits(List.of(traits));
        return engineer;
    }

    private static PlayerSnapshot agency(SetSpecialist... engineers) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        snapshot.setSetSpecialists(new ArrayList<>(List.of(engineers)));
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(50.0, 3.0)));
        return snapshot;
    }

    private static Chemistry.Result real(PlayerSnapshot snapshot) {
        return Chemistry.research(snapshot, snapshot.getSetSpecialists(), SetSkill.ROCKETS, false);
    }

    @Test
    void aPairsRapportSetsATwoPersonTeamsChemistry() {
        PlayerSnapshot snapshot = agency(engineer("set-1", 50, 0), engineer("set-2", 40, 1));
        snapshot.setRapports(List.of(new Rapport("set-1", "set-2", 40, 0.5, 8, true)));
        Chemistry.Result chemistry = real(snapshot);
        assertEquals(0.4, chemistry.value(), 1e-9);
        assertEquals(1.06, chemistry.multiplier(), 1e-9);
    }

    @Test
    void teamTraitsAddUpAndTheResultIsClamped() {
        // Three: no rapport yet; a Team player (+0.15) and a crowded Loner (-0.3).
        PlayerSnapshot snapshot = agency(engineer("set-1", 50, 0, new TraitState(Trait.TEAM_PLAYER, 2, false)),
            engineer("set-2", 40, 1, new TraitState(Trait.LONER, 2, false)), engineer("set-3", 30, 2));
        assertEquals(0.15 - 0.3, real(snapshot).value(), 1e-9);
        // A Loner alone: +0.6.
        PlayerSnapshot alone = agency(engineer("set-1", 50, 0, new TraitState(Trait.LONER, 2, false)));
        assertEquals(0.6, real(alone).value(), 1e-9);
        // Everything good at once stays at +1.
        PlayerSnapshot friends = agency(engineer("set-1", 50, 0, new TraitState(Trait.TEAM_PLAYER, 2, false)),
            engineer("set-2", 40, 1, new TraitState(Trait.TEAM_PLAYER, 2, false)));
        friends.setRapports(List.of(new Rapport("set-1", "set-2", 100, 1, 30, true)));
        assertEquals(1, real(friends).value(), 1e-9);
    }

    @Test
    void someoneWhoMustBeTheBestMindsWhoIsBetter() {
        PlayerSnapshot onTop = agency(engineer("set-1", 60, 0, new TraitState(Trait.MUST_BE_THE_BEST, 2, false)),
            engineer("set-2", 40, 1));
        assertEquals(0.2, real(onTop).value(), 1e-9);
        PlayerSnapshot outdone = agency(engineer("set-1", 30, 0, new TraitState(Trait.MUST_BE_THE_BEST, 2, false)),
            engineer("set-2", 40, 1));
        assertEquals(-0.3, real(outdone).value(), 1e-9);
    }

    @Test
    void thePreviewCountsOnlyWhatThePlayerKnows() {
        SetSpecialist known = engineer("set-1", 50, 0, new TraitState(Trait.TEAM_PLAYER, 2, true));
        SetSpecialist hidden = engineer("set-2", 40, 1, new TraitState(Trait.LONER, 2, false));
        SetSpecialist third = engineer("set-3", 30, 2);
        PlayerSnapshot snapshot = agency(known, hidden, third);
        snapshot.setRapports(List.of(new Rapport("set-1", "set-3", 60, 1, 12, true),
            new Rapport("set-2", "set-3", -20, -1, 4, false)));

        Chemistry.Result preview = Chemistry.research(snapshot, snapshot.getSetSpecialists(), SetSkill.ROCKETS, true);
        assertEquals(0.6 / 3 + 0.15, preview.value(), 1e-9);
        assertTrue(preview.unknownFactors());
        assertEquals((0.6 - 0.2) / 3 + 0.15 - 0.3, real(snapshot).value(), 1e-9);
        assertFalse(real(snapshot).unknownFactors());
    }

    @Test
    void whenEverythingIsKnownThePreviewIsTheRealChemistry() {
        SetSpecialist a = engineer("set-1", 50, 0, new TraitState(Trait.TEAM_PLAYER, 2, true));
        SetSpecialist b = engineer("set-2", 40, 1, new TraitState(Trait.MUST_BE_THE_BEST, 3, true));
        a.setWorkSeasons(Personality.REVEAL_MAX_SEASONS);
        b.setWorkSeasons(Personality.REVEAL_MAX_SEASONS);
        PlayerSnapshot snapshot = agency(a, b);
        snapshot.setRapports(List.of(new Rapport("set-1", "set-2", -35, -0.4, 9, true)));
        Chemistry.Result preview = Chemistry.research(snapshot, snapshot.getSetSpecialists(), SetSkill.ROCKETS, true);
        assertEquals(real(snapshot).value(), preview.value(), 1e-12);
        assertFalse(preview.unknownFactors());
    }

    @Test
    void chemistryScalesTheTeamsReliabilityGain() {
        PlayerSnapshot neutral = agency(engineer("set-1", 50, 0), engineer("set-2", 40, 1));
        PlayerSnapshot friends = agency(engineer("set-1", 50, 0), engineer("set-2", 40, 1));
        friends.setRapports(List.of(new Rapport("set-1", "set-2", 50, 0.5, 10, true)));
        ResearchProgress.advanceSeason(neutral, CATALOG, new Random(42L));
        ResearchProgress.advanceSeason(friends, CATALOG, new Random(42L));
        double base = neutral.getMissionComponents().get("rocket-redstone").getCurrentReliability() - 50;
        double gained = friends.getMissionComponents().get("rocket-redstone").getCurrentReliability() - 50;
        assertTrue(base > 0);
        assertEquals(base * (1 + 0.15 * 0.5), gained, 1e-9);
    }
}
