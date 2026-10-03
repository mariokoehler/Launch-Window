package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for BASPM-style staffing slots ({@code baspm.md} &sect;10.11): per-slot
 * reading, Flight Director slots, {@link MissionStaffing#autoAssign}, and the free
 * {@link MissionStaffing#reassign} of a control slot.
 *
 * @author Mario Koehler
 */
class MissionStaffingTest {

    private static final ContentCatalog PACK =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule", SetSkill.CREWED_SPACECRAFT, 3,
        90, 80, List.of(1.0, 2.0, 3.0, 4.0), 0, List.of());
    private static final Program PROGRAM = new Program("program", "early-space", 0, 0, List.of("capsule"),
        List.of());
    /** BOOSTER (Propulsion), FLIGHT (Flight Director), and an unused SYSTEMS slot. */
    private static final MissionConfiguration TWO_SLOTS = new MissionConfiguration("two-slots", "program",
        List.of(new MissionSeason(List.of(new MissionStep("ops",
            List.of(new MissionStep.ComponentInvolvement("capsule", 100)),
            List.of(new MissionStep.ControllerInvolvement(0, 50), new MissionStep.ControllerInvolvement(1, 50)),
            List.of(), false)))),
        List.of(), List.of(StaffingSlot.controller("BOOSTER", MissionControlSkill.PROPULSION),
            StaffingSlot.flightDirector("FLIGHT"),
            StaffingSlot.controller("SYSTEMS", MissionControlSkill.SPACECRAFT_SYSTEMS)),
        0, 0, List.of(), List.of(), List.of(), 0, 0);
    private static final ContentCatalog CATALOG =
        new ContentCatalog(List.of(CATEGORY), List.of(PROGRAM), List.of(TWO_SLOTS), List.of(CAPSULE));

    private static MissionControlOfficer controller(String id, double propulsion, double others) {
        MissionControlOfficer officer = new MissionControlOfficer(id, id, 40, 70, 50);
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            officer.setSkill(skill, skill == MissionControlSkill.PROPULSION ? propulsion : others);
        }
        return officer;
    }

    private static PlayerSnapshot player(MissionControlOfficer... controllers) {
        PlayerSnapshot snapshot = new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
        snapshot.setMissionControlOfficers(new ArrayList<>(List.of(controllers)));
        return snapshot;
    }

    @Test
    void flightDirectorSlotReadsTheAverageOfTheFiveSkills() {
        MissionControlOfficer officer = controller("fd", 90, 40);
        PlayerSnapshot snapshot = player(officer);

        assertEquals(50.0, MissionProgress.slotValue(snapshot, StaffingSlot.flightDirector("FLIGHT"), "fd"));
        assertEquals(90.0, MissionProgress.slotValue(snapshot,
            StaffingSlot.controller("BOOSTER", MissionControlSkill.PROPULSION), "fd"));
        assertEquals(0.0, MissionProgress.slotValue(snapshot, StaffingSlot.flightDirector("FLIGHT"), null));
    }

    @Test
    void autoAssignFillsFlightDirectorSlotsFirst() {
        // "a" is the better Flight Director (average 66) and the better booster (90); filling the
        // FLIGHT slot first gives it "a", so BOOSTER gets "b". Slot order alone would do the opposite.
        PlayerSnapshot snapshot = player(controller("a", 90, 60), controller("b", 80, 50));

        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, TWO_SLOTS);

        assertEquals("b", staffing.controlSlotIds().get(0));
        assertEquals("a", staffing.controlSlotIds().get(1));
        assertNull(staffing.controlSlotIds().get(2), "a slot no step uses stays empty");
    }

    @Test
    void autoAssignSkipsBusyPeopleAndLeavesASlotEmptyWhenNobodyIsLeft() {
        MissionControlOfficer busy = controller("busy", 99, 99);
        busy.setAssignmentState(AssignmentState.IN_TRAINING);
        PlayerSnapshot snapshot = player(busy, controller("only", 50, 50));

        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, TWO_SLOTS);

        assertEquals("only", staffing.controlSlotIds().get(1));
        assertNull(staffing.controlSlotIds().get(0));
    }

    @Test
    void autoAssignStaffsAPackLayoutOneToOne() {
        MissionConfiguration mercury = PACK.missionConfiguration("mercury-orbital").orElseThrow();
        List<MissionControlOfficer> controllers = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            controllers.add(controller("mc" + i, 30 + i, 40));
        }
        PlayerSnapshot snapshot = player(controllers.toArray(MissionControlOfficer[]::new));
        snapshot.setAstronauts(List.of(new Astronaut("a1", "a1", 30, 70, 50), new Astronaut("a2", "a2", 30, 70, 50)));

        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(snapshot, mercury);

        assertEquals(1, staffing.crewSlotIds().size());
        assertEquals(11, staffing.controlSlotIds().size());
        Set<String> people = new HashSet<>();
        for (int slot = 0; slot < mercury.controlSlots().size(); slot++) {
            String holder = staffing.controlSlotIds().get(slot);
            assertEquals(mercury.usedControlSlots().contains(slot), holder != null, "slot " + slot);
            if (holder != null) {
                assertTrue(people.add(holder), holder + " placed twice");
                boolean astronaut = holder.startsWith("a");
                assertEquals(mercury.controlSlots().get(slot).kind() == StaffingSlot.Kind.ASTRONAUT, astronaut);
            }
        }
        assertTrue(people.add(staffing.crewSlotIds().get(0)), "the crew isn't also on the ground");
        assertEquals(9, mercury.requiredMissionControlOfficerCount());
        assertEquals(1, mercury.requiredCapcomCount());
    }

    @Test
    void controllersGrowTheirSlotsSkillAndAFlightDirectorGrowsAllFiveEvenly() {
        MissionControlOfficer booster = controller("booster", 50, 50);
        MissionControlOfficer flight = controller("flight", 50, 50);
        PlayerSnapshot snapshot = player(booster, flight);
        snapshot.setOpenedProgramIds(List.of("program"));
        snapshot.setMissionComponents(new java.util.HashMap<>(Map.of("capsule", new OwnedMissionComponent(80, 80))));
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(snapshot,
            new LaunchRequest("two-slots", List.of(), java.util.Arrays.asList("booster", "flight", null), List.of()),
            CATALOG));

        MissionProgress.advanceSeason(snapshot, CATALOG, new Random(3));

        assertTrue(booster.getSkill(MissionControlSkill.PROPULSION) > 50);
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            if (skill != MissionControlSkill.PROPULSION) {
                assertEquals(50.0, booster.getSkill(skill), skill + " was not the booster's slot");
            }
        }
        double gain = flight.getSkill(MissionControlSkill.PROPULSION) - 50;
        assertTrue(gain > 0);
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            assertEquals(gain, flight.getSkill(skill) - 50, 1e-9, "a Flight Director's gain is spread evenly");
        }
        assertEquals(AssignmentState.IDLE, flight.getAssignmentState(), "released when the mission ends");
    }

    private static PlayerSnapshot launchedTwoSlots() {
        PlayerSnapshot snapshot = player(controller("booster", 50, 50), controller("flight", 50, 50),
            controller("spare", 70, 70));
        snapshot.setAstronauts(new ArrayList<>(List.of(new Astronaut("astro", "astro", 30, 70, 50))));
        snapshot.setOpenedProgramIds(List.of("program"));
        snapshot.setMissionComponents(new java.util.HashMap<>(Map.of("capsule", new OwnedMissionComponent(80, 80))));
        assertEquals(MissionLaunchOutcome.LAUNCHED, MissionLaunch.launch(snapshot,
            new LaunchRequest("two-slots", List.of(), List.of("booster", "flight"), List.of()), CATALOG));
        return snapshot;
    }

    @Test
    void reassignSwapsAControlSlotForFreeAndReleasesThePreviousHolder() {
        PlayerSnapshot snapshot = launchedTwoSlots();
        InProgressMission mission = snapshot.getInProgressMissions().values().iterator().next();
        mission.setCurrentSeasonIndex(0);
        long funds = snapshot.getFunds();

        assertEquals(MissionStaffing.ReassignOutcome.REASSIGNED,
            MissionStaffing.reassign(snapshot, mission.getId(), 1, "spare", CATALOG));

        assertEquals("spare", mission.getControlSlotIds().get(1));
        MissionControlOfficer spare = find(snapshot, "spare");
        MissionControlOfficer flight = find(snapshot, "flight");
        assertEquals(AssignmentState.ASSIGNED, spare.getAssignmentState());
        assertEquals(mission.getId(), spare.getAssignedMissionId());
        assertEquals(AssignmentState.IDLE, flight.getAssignmentState());
        assertNull(flight.getAssignedMissionId());
        assertEquals(funds, snapshot.getFunds(), "re-staffing is free");
    }

    @Test
    void reassignRefusesWithoutChangingAnything() {
        PlayerSnapshot snapshot = launchedTwoSlots();
        String missionId = snapshot.getInProgressMissions().keySet().iterator().next();
        List<String> before = new ArrayList<>(snapshot.getInProgressMissions().get(missionId).getControlSlotIds());

        assertEquals(MissionStaffing.ReassignOutcome.UNKNOWN_MISSION,
            MissionStaffing.reassign(snapshot, "nope", 0, "spare", CATALOG));
        assertEquals(MissionStaffing.ReassignOutcome.UNKNOWN_SLOT,
            MissionStaffing.reassign(snapshot, missionId, 3, "spare", CATALOG));
        assertEquals(MissionStaffing.ReassignOutcome.SLOT_NOT_USED,
            MissionStaffing.reassign(snapshot, missionId, 2, "spare", CATALOG));
        assertEquals(MissionStaffing.ReassignOutcome.WRONG_EMPLOYEE_TYPE,
            MissionStaffing.reassign(snapshot, missionId, 0, "astro", CATALOG));
        assertEquals(MissionStaffing.ReassignOutcome.PERSON_NOT_AVAILABLE,
            MissionStaffing.reassign(snapshot, missionId, 0, "flight", CATALOG), "already in the other slot");
        find(snapshot, "spare").setUnavailableSeasons(1);
        assertEquals(MissionStaffing.ReassignOutcome.PERSON_NOT_AVAILABLE,
            MissionStaffing.reassign(snapshot, missionId, 0, "spare", CATALOG));

        assertEquals(before, snapshot.getInProgressMissions().get(missionId).getControlSlotIds());
        assertEquals(AssignmentState.IDLE, find(snapshot, "spare").getAssignmentState());
    }

    private static MissionControlOfficer find(PlayerSnapshot snapshot, String id) {
        MissionControlOfficer officer = snapshot.getMissionControlOfficers().stream()
            .filter(o -> Objects.equals(o.getId(), id)).findFirst().orElse(null);
        assertNotNull(officer);
        return officer;
    }
}
