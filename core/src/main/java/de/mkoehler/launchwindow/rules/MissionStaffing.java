package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>Staffing a mission's slots ({@code baspm.md} &sect;10.11, {@code design.md}
 * &sect;5 M5): the auto-assign proposal the "assemble mission" screen offers, and the
 * free re-staffing of a control slot on a scheduled or flying mission.</p>
 *
 * <p>Headless and side-effect free except for {@link #reassign}, which mutates the
 * snapshot like every other rules method behind a journaled action.</p>
 *
 * @author Mario Koehler
 */
public final class MissionStaffing {

    /** What {@link #reassign} did. */
    public enum ReassignOutcome {
        /** The slot now holds the new person; the previous holder is idle. */
        REASSIGNED,
        /** No in-progress mission has the given id. */
        UNKNOWN_MISSION,
        /** The configuration has no control slot with that index. */
        UNKNOWN_SLOT,
        /** No step of the current or a later season uses the slot, so it stays empty. */
        SLOT_NOT_USED,
        /** The person is unknown, or not of the slot's employee type. */
        WRONG_EMPLOYEE_TYPE,
        /** The person is not idle, or is unavailable this season. */
        PERSON_NOT_AVAILABLE
    }

    /**
     * A proposed or chosen filling of a configuration's slots.
     *
     * @param crewSlotIds    one astronaut per crew slot, {@code null} where unused or unfilled
     * @param controlSlotIds one person per control slot, {@code null} where unused or unfilled
     */
    public record Staffing(List<String> crewSlotIds, List<String> controlSlotIds) {
    }

    private MissionStaffing() {
    }

    /**
     * Proposes a staffing for a configuration from the player's idle, available
     * people. Crew slots are filled first, each with the astronaut scoring best on the
     * skills the steps weight in that slot; then the control slots in BASPM's order -
     * Flight Director slots first, then every other used slot - each with the best
     * available person of the slot's type at the slot's skill
     * ({@code AssignFlightControllersBestFit}). Nobody is proposed twice. A slot stays
     * {@code null} when nobody is left for it; ties go to roster order.
     *
     * @param snapshot      the player
     * @param configuration the configuration to staff
     * @return the proposal; not applied to anything
     */
    public static Staffing autoAssign(PlayerSnapshot snapshot, MissionConfiguration configuration) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(configuration, "configuration must not be null");
        Set<String> taken = new HashSet<>();

        List<String> crew = nulls(configuration.crewSlots().size());
        for (int slot : configuration.usedCrewSlots()) {
            Map<AstronautSkill, Double> weights = crewWeights(configuration, slot);
            Astronaut best = null;
            double bestScore = -1;
            for (Astronaut astronaut : snapshot.getAstronauts()) {
                if (taken.contains(astronaut.getId()) || !MissionLaunch.isAvailable(astronaut)) {
                    continue;
                }
                double score = 0;
                for (Map.Entry<AstronautSkill, Double> weight : weights.entrySet()) {
                    score += weight.getValue() * astronaut.getSkill(weight.getKey());
                }
                if (score > bestScore) {
                    best = astronaut;
                    bestScore = score;
                }
            }
            if (best != null) {
                crew.set(slot, best.getId());
                taken.add(best.getId());
            }
        }

        List<String> control = nulls(configuration.controlSlots().size());
        List<Integer> order = new ArrayList<>();
        for (int slot : configuration.usedControlSlots()) {
            if (configuration.controlSlots().get(slot).flightDirectorSlot()) {
                order.add(slot);
            }
        }
        for (int slot : configuration.usedControlSlots()) {
            if (!configuration.controlSlots().get(slot).flightDirectorSlot()) {
                order.add(slot);
            }
        }
        for (int slot : order) {
            StaffingSlot staffingSlot = configuration.controlSlots().get(slot);
            List<? extends Personnel> roster = staffingSlot.kind() == StaffingSlot.Kind.ASTRONAUT
                ? snapshot.getAstronauts()
                : snapshot.getMissionControlOfficers();
            Personnel best = null;
            double bestValue = -1;
            for (Personnel person : roster) {
                if (taken.contains(person.getId()) || !MissionLaunch.isAvailable(person)) {
                    continue;
                }
                double value = MissionProgress.slotValue(snapshot, staffingSlot, person.getId());
                if (value > bestValue) {
                    best = person;
                    bestValue = value;
                }
            }
            if (best != null) {
                control.set(slot, best.getId());
                taken.add(best.getId());
            }
        }
        return new Staffing(crew, control);
    }

    /**
     * Puts a different person into one control slot of a scheduled or flying mission,
     * at no cost (user decision 2026-10-03; BASPM re-staffs mission control between
     * seasons, {@code baspm.md} &sect;10.11). The crew can't be changed this way: a crew
     * slot isn't a control slot. The new person must be of the slot's type, idle, and
     * available; the previous holder goes back to idle, or retires if already at their
     * group's retirement age, as at a mission's end. Mutates nothing on a refusal.
     *
     * @param snapshot          the player; mutated in place on success
     * @param missionInstanceId the mission
     * @param slot              the control slot index
     * @param personId          the new holder
     * @param catalog           the content catalog
     * @return the outcome
     */
    public static ReassignOutcome reassign(PlayerSnapshot snapshot, String missionInstanceId, int slot,
            String personId, ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(missionInstanceId, "missionInstanceId must not be null");
        Objects.requireNonNull(personId, "personId must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        InProgressMission mission = snapshot.getInProgressMissions().get(missionInstanceId);
        if (mission == null) {
            return ReassignOutcome.UNKNOWN_MISSION;
        }
        MissionConfiguration configuration =
            catalog.missionConfiguration(mission.getMissionConfigurationId()).orElseThrow();
        if (slot < 0 || slot >= configuration.controlSlots().size()) {
            return ReassignOutcome.UNKNOWN_SLOT;
        }
        // A slot whose last season has flown was released, and stays empty.
        if (!configuration.usedControlSlotsFrom(mission.getCurrentSeasonIndex()).contains(slot)) {
            return ReassignOutcome.SLOT_NOT_USED;
        }
        StaffingSlot staffingSlot = configuration.controlSlots().get(slot);
        Personnel person = staffingSlot.kind() == StaffingSlot.Kind.ASTRONAUT
            ? find(snapshot.getAstronauts(), personId)
            : find(snapshot.getMissionControlOfficers(), personId);
        if (person == null) {
            return ReassignOutcome.WRONG_EMPLOYEE_TYPE;
        }
        if (!MissionLaunch.isAvailable(person)) {
            return ReassignOutcome.PERSON_NOT_AVAILABLE;
        }

        while (mission.getControlSlotIds().size() < configuration.controlSlots().size()) {
            mission.getControlSlotIds().add(null);
        }
        String previousId = mission.getControlSlotIds().get(slot);
        mission.getControlSlotIds().set(slot, personId);
        if (previousId != null) {
            // Retires instead of going idle when already over age, as a mission's end does, so a swap can't
            // keep an over-age person assigned and protected from retirement.
            MissionProgress.releaseOrRetire(snapshot, previousId);
        }
        MissionLaunch.assign(person, missionInstanceId);
        return ReassignOutcome.REASSIGNED;
    }

    /**
     * Returns how well an astronaut fits a crew seat: their skills weighted by how much the
     * configuration's steps lean on each skill in that seat, 0-100. {@link #autoAssign} ranks
     * candidates for a seat by the same weighting.
     *
     * @param astronaut     the astronaut
     * @param configuration the configuration
     * @param slot          the crew seat
     * @return the fit, or 0 for a seat no step uses
     */
    public static double seatFit(Astronaut astronaut, MissionConfiguration configuration, int slot) {
        Map<AstronautSkill, Double> weights = crewWeights(configuration, slot);
        double total = 0;
        double score = 0;
        for (Map.Entry<AstronautSkill, Double> weight : weights.entrySet()) {
            total += weight.getValue();
            score += weight.getValue() * astronaut.getSkill(weight.getKey());
        }
        return total == 0 ? 0 : score / total;
    }

    private static Map<AstronautSkill, Double> crewWeights(MissionConfiguration configuration, int slot) {
        Map<AstronautSkill, Double> weights = new EnumMap<>(AstronautSkill.class);
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
                    if (involvement.slot() == slot) {
                        weights.merge(involvement.skill(), involvement.percent(), Double::sum);
                    }
                }
            }
        }
        return weights;
    }

    private static List<String> nulls(int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(null);
        }
        return list;
    }

    private static <P extends Personnel> P find(List<P> roster, String id) {
        for (P person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }
}
