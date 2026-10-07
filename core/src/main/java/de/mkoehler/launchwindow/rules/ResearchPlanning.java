package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Planning help for R&amp;D, for the client's R&amp;D boards: the reliability a component can expect
 * to gain next season from the engineers on it, and the best team the player could put on it now.
 * Headless and read-only; it follows {@link ResearchProgress}'s season pass step for step, with
 * each slot's random draw replaced by the middle of its band, so a board's projection is the
 * rules' own average and never a client re-derivation.
 *
 * @author Mario Koehler
 */
public final class ResearchPlanning {

    private ResearchPlanning() {
    }

    /**
     * Returns the reliability a component can expect to gain next season from the engineers on it
     * now: the average of {@link ResearchProgress}'s season pass for this team.
     *
     * @param snapshot    the player
     * @param catalog     the content
     * @param componentId the component
     * @return the expected gain in reliability points, 0 if nobody researches it or it's at its ceiling
     * @throws NullPointerException if an argument is {@code null}
     */
    public static double expectedSeasonGain(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(componentId, "componentId must not be null");
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        if (owned == null || component == null) {
            return 0;
        }
        SetSpecialist[] team = new SetSpecialist[MissionComponent.RD_SLOT_COUNT];
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            Integer slot = specialist.getRdSlot();
            if (specialist.getAssignmentState() == AssignmentState.ASSIGNED
                && componentId.equals(specialist.getRdTargetComponentId()) && slot != null && slot >= 0
                && slot < team.length && team[slot] == null) {
                team[slot] = specialist;
            }
        }
        // The chemistry the player can see: revealed traits and shown relationships only.
        List<SetSpecialist> members = new java.util.ArrayList<>();
        for (SetSpecialist member : team) {
            if (member != null) {
                members.add(member);
            }
        }
        double chemistry = Chemistry.research(snapshot, members, component.associatedSkill(), true).multiplier();
        return expectedSeasonGain(component, owned.getCurrentReliability(), team,
            snapshot.getRdResultsMultiplier() * chemistry);
    }

    /**
     * Returns the expected gain for a given team, slot by slot.
     *
     * @param component  the component
     * @param current    its current reliability
     * @param team       who sits in each of the four slots, {@code null} for an empty one
     * @param multiplier the player's current R&amp;D results multiplier (random events)
     * @return the expected gain in reliability points
     */
    static double expectedSeasonGain(MissionComponent component, double current, SetSpecialist[] team,
                                     double multiplier) {
        // Traits as the player knows them: a revealed Perfectionist's higher cap, revealed R&D traits.
        List<SetSpecialist> members = new java.util.ArrayList<>();
        for (SetSpecialist member : team) {
            if (member != null) {
                members.add(member);
            }
        }
        double cap = Math.min(component.maxReliability(),
            component.maxRdReliability() + TraitEffects.capBonus(members, true));
        double nominal = 0;
        for (int slot = 0; slot < team.length; slot++) {
            if (team[slot] == null) {
                continue;
            }
            List<Double> bands = component.rdSlotBandMaxAmounts();
            double raw = (SkillGrowth.rdSlotBandLower(bands, slot) + SkillGrowth.rdSlotBandUpper(bands, slot)) / 2.0;
            nominal += SkillGrowth.rdReliabilityGain(raw, current, cap,
                team[slot].getSkill(component.associatedSkill())) * multiplier
                * TraitEffects.rdGain(team[slot], component.id(), true);
        }
        if (nominal <= 0) {
            return 0;
        }
        double headroom = Math.max(0.0, cap - current);
        double applied = headroom < ResearchProgress.HEADROOM_STAGNATION_THRESHOLD ? headroom : nominal;
        return Math.min(cap, current + applied) - current;
    }

    /**
     * Returns the best R&amp;D team the player could put on a component now, best first: the
     * engineers already on it plus every idle, available one, by their skill in the component's
     * skill, at most four. Seated in this order, the best engineer takes the richest slot, which
     * gives the largest expected gain. Engineers busy elsewhere (training, another component) are
     * left alone.
     *
     * @param snapshot  the player
     * @param component the component
     * @return the engineers, best first
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<SetSpecialist> bestTeam(PlayerSnapshot snapshot, MissionComponent component) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(component, "component must not be null");
        List<SetSpecialist> pool = new ArrayList<>();
        for (SetSpecialist specialist : snapshot.getSetSpecialists()) {
            boolean onIt = specialist.getAssignmentState() == AssignmentState.ASSIGNED
                && component.id().equals(specialist.getRdTargetComponentId());
            boolean free = specialist.getAssignmentState() == AssignmentState.IDLE;
            // Only those who could be seated again after a release: assignResearch needs them available.
            if ((onIt || free) && specialist.getUnavailableSeasons() == 0) {
                pool.add(specialist);
            }
        }
        pool.sort(Comparator.comparingDouble((SetSpecialist s) -> s.getSkill(component.associatedSkill())).reversed());
        return List.copyOf(pool.subList(0, Math.min(MissionComponent.RD_SLOT_COUNT, pool.size())));
    }
}
