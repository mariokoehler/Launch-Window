package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import de.mkoehler.launchwindow.model.TrainingKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests what personal traits do ({@code design.md} &sect;2.2(d)).
 *
 * @author Mario Koehler
 */
class TraitEffectsTest {

    private static Astronaut astronaut(Trait... traits) {
        Astronaut astronaut = new Astronaut("astro-1", "A", 30, 70, 50);
        List<TraitState> states = new ArrayList<>();
        for (Trait trait : traits) {
            states.add(new TraitState(trait, 2, false));
        }
        astronaut.setTraits(states);
        return astronaut;
    }

    @Test
    void traitsShapeTheSeasonsMoraleChange() {
        Astronaut idle = astronaut(Trait.WORKAHOLIC);
        assertEquals(-6, TraitEffects.seasonMorale(idle, -2));
        Astronaut steady = astronaut(Trait.STEADY);
        assertEquals(1, TraitEffects.seasonMorale(steady, 3));
        assertEquals(-1, TraitEffects.seasonMorale(steady, -2));
        Astronaut ambitious = astronaut(Trait.AMBITIOUS);
        ambitious.setAssignmentState(AssignmentState.ASSIGNED);
        assertEquals(3, TraitEffects.seasonMorale(ambitious, 3));
        ambitious.setSeasonsSinceTraining(TraitEffects.AMBITIOUS_PATIENCE_SEASONS);
        assertEquals(1, TraitEffects.seasonMorale(ambitious, 3));
    }

    @Test
    void theLifecycleCountsSeasonsWithoutTraining() {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        Astronaut idle = astronaut();
        Astronaut training = new Astronaut("astro-2", "B", 30, 70, 50);
        training.setTrainingKind(TrainingKind.ADVANCED);
        training.setAssignmentState(AssignmentState.IN_TRAINING);
        training.setSeasonsSinceTraining(4);
        snapshot.setAstronauts(new ArrayList<>(List.of(idle, training)));
        PersonnelLifecycle.advanceSeason(snapshot);
        assertEquals(1, idle.getSeasonsSinceTraining());
        assertEquals(0, training.getSeasonsSinceTraining());
    }

    @Test
    void trainingAndRdFactorsFollowTheTraits() {
        assertEquals(1.2 * 1.25, TraitEffects.trainingGain(astronaut(Trait.AMBITIOUS, Trait.QUICK_STUDY)), 1e-9);
        SetSpecialist restless = new SetSpecialist("set-1", "R", 30, 70, 50);
        restless.setTraits(List.of(new TraitState(Trait.RESTLESS, 2, false)));
        assertEquals(1.15, TraitEffects.rdGain(restless, "atlas", false), 1e-9);
        restless.setProjectId("atlas");
        restless.setProjectSeasons(5);
        assertEquals(1.0, TraitEffects.rdGain(restless, "atlas", false), 1e-9);
        restless.setProjectSeasons(8);
        assertEquals(0.85, TraitEffects.rdGain(restless, "atlas", false), 1e-9);
        assertEquals(1.15, TraitEffects.rdGain(restless, "redstone", false), 1e-9);
        // A hidden trait never shows in a preview.
        assertEquals(1.0, TraitEffects.rdGain(restless, "atlas", true), 1e-9);
    }

    @Test
    void coolHeadsAddToTheSavingThrow() {
        Astronaut cool = astronaut(Trait.COOL_UNDER_PRESSURE);
        assertEquals(3, TraitEffects.savingThrowBonus(List.of(cool, astronaut()), false), 1e-9);
        assertEquals(0, TraitEffects.savingThrowBonus(List.of(cool), true), 1e-9);
    }

    @Test
    void aPerfectionistRaisesTheCapAndLeavingReleasesTheOthersOnce() {
        MissionComponent redstone = new MissionComponent("rocket-redstone", SetSkill.ROCKETS, 3, 95, 90,
            List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
        ContentCatalog catalog = new ContentCatalog(List.of(), List.of(), List.of(), List.of(redstone));
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 0, 0, 0, 1);
        SetSpecialist perfectionist = new SetSpecialist("set-1", "P", 30, 70, 50);
        perfectionist.setTraits(List.of(new TraitState(Trait.PERFECTIONIST, 2, false)));
        SetSpecialist other = new SetSpecialist("set-2", "O", 30, 70, 50);
        for (SetSpecialist engineer : List.of(perfectionist, other)) {
            engineer.setSkill(SetSkill.ROCKETS, 90);
            engineer.setAssignmentState(AssignmentState.ASSIGNED);
            engineer.setRdTargetComponentId("rocket-redstone");
        }
        perfectionist.setRdSlot(0);
        other.setRdSlot(1);
        snapshot.setSetSpecialists(new ArrayList<>(List.of(perfectionist, other)));
        snapshot.setMissionComponents(Map.of("rocket-redstone", new OwnedMissionComponent(89.5, 3.0)));

        // With the Perfectionist, R&D goes past the normal cap of 90 (toward 93).
        Random random = new Random(1);
        for (int season = 0; season < 50
            && snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability() <= 90; season++) {
            ResearchProgress.advanceSeason(snapshot, catalog, random);
        }
        double reliability = snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability();
        assertTrue(reliability > 90 && reliability < 93, String.valueOf(reliability));
        assertEquals(AssignmentState.ASSIGNED, other.getAssignmentState());

        // The Perfectionist leaves: the other is released once, and nothing more happens after that.
        snapshot.getSetSpecialists().remove(perfectionist);
        List<ResolutionEvent> first = ResearchProgress.advanceSeason(snapshot, catalog, random);
        assertTrue(first.stream().anyMatch(ResearchComponentMaxed.class::isInstance), first.toString());
        assertEquals(AssignmentState.IDLE, other.getAssignmentState());
        assertTrue(ResearchProgress.advanceSeason(snapshot, catalog, random).isEmpty());
        assertEquals(reliability, snapshot.getMissionComponents().get("rocket-redstone").getCurrentReliability(), 1e-9);
    }
}
