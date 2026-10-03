package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.actions.ReassignControlSlotAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DevCommands}.
 *
 * @author Mario Koehler
 */
class DevCommandsTest {

    @Test
    void parsesTheLaunchCommandWithItsLists() {
        LaunchMissionAction launch = (LaunchMissionAction) DevCommands.parse(
            "launch vostok-orbital rockets=r7-vostok crew=p1 control=p3,-,p2");

        assertEquals("vostok-orbital", launch.getMissionConfigurationId());
        assertEquals(List.of("r7-vostok"), launch.getRocketIds());
        assertEquals(List.of("p1"), launch.getCrewSlotIds());
        assertEquals(Arrays.asList("p3", null, "p2"), launch.getControlSlotIds());
    }

    @Test
    void parsesTheReassignCommand() {
        ReassignControlSlotAction reassign = (ReassignControlSlotAction) DevCommands.parse("reassign m0 4 p9");

        assertEquals("m0", reassign.getMissionInstanceId());
        assertEquals(4, reassign.getSlot());
        assertEquals("p9", reassign.getPersonId());
    }

    @Test
    void parsesGroupsBuildingsAndTrainingCaseInsensitively() {
        HireAction hire = (HireAction) DevCommands.parse("hire set p7");
        assertEquals(PersonnelGroup.SET, hire.getGroup());
        assertEquals("p7", hire.getCandidateId());
        assertEquals(Building.MISSION_CONTROL,
            ((UpgradeBuildingAction) DevCommands.parse("upgrade mission_control")).getBuilding());
        StartTrainingAction train = (StartTrainingAction) DevCommands.parse("train set p7 permanent rockets 60");
        assertEquals(TrainingKind.PERMANENT, train.getKind());
        assertEquals("ROCKETS", train.getSkill());
        assertEquals(60, train.getTargetLevel());
        assertEquals("r7-sputnik", ((OpenRocketProgramAction) DevCommands.parse("rocket open r7-sputnik"))
            .getRocketId());
    }

    @Test
    void rejectsUnknownCommandsAndMissingArguments() {
        assertThrows(IllegalArgumentException.class, () -> DevCommands.parse("fly to the moon"));
        assertThrows(IllegalArgumentException.class, () -> DevCommands.parse("hire set"));
        assertThrows(IllegalArgumentException.class, () -> DevCommands.parse("launch x rockets"));
    }
}
