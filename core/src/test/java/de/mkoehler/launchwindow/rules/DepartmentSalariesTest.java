package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link DepartmentSalaries} (gap G8, BASPM's one-off adjustment).
 *
 * @author Mario Koehler
 */
class DepartmentSalariesTest {

    private static SetSpecialist engineer(String id, int morale, double salary) {
        SetSpecialist engineer = new SetSpecialist(id, "E " + id, 40, morale, 50);
        engineer.setSalary(salary);
        return engineer;
    }

    /**
     * A raise adds its percentage to morale; a cut costs three times as much; both clamp to 0-100.
     */
    @Test
    void moraleMovesWithTheSalary() {
        assertEquals(10, DepartmentSalaries.moraleChange(10));
        assertEquals(-30, DepartmentSalaries.moraleChange(-10));
        assertEquals(100, DepartmentSalaries.adjustedMorale(95, 20));
        assertEquals(0, DepartmentSalaries.adjustedMorale(20, -10));
    }

    /**
     * Applying it changes every salary and morale in the group, and the preview predicted exactly that.
     */
    @Test
    void theAdjustmentMatchesItsPreview() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        state.getSetSpecialists().addAll(List.of(engineer("a", 60, 50.0), engineer("b", 95, 40.0)));
        double projectedSalary = DepartmentSalaries.projectedSalary(state.getSetSpecialists(), 10);
        int projectedMorale = DepartmentSalaries.projectedAverageMorale(state.getSetSpecialists(), 10);

        assertEquals(DepartmentSalaries.AdjustOutcome.ADJUSTED,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, 10));

        assertEquals(55.0, state.getSetSpecialists().get(0).getSalary(), 1e-9);
        assertEquals(44.0, state.getSetSpecialists().get(1).getSalary(), 1e-9);
        assertEquals(70, state.getSetSpecialists().get(0).getMorale());
        assertEquals(100, state.getSetSpecialists().get(1).getMorale(), "clamped at 100");
        assertEquals(99.0, projectedSalary, 1e-9);
        assertEquals(85, projectedMorale);
    }

    /**
     * Out-of-range or zero percentages and an empty department change nothing.
     */
    @Test
    void badRequestsChangeNothing() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        assertEquals(DepartmentSalaries.AdjustOutcome.EMPTY_DEPARTMENT,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, 10));
        state.getSetSpecialists().add(engineer("a", 60, 50.0));
        assertEquals(DepartmentSalaries.AdjustOutcome.INVALID_PERCENT,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, 51));
        assertEquals(DepartmentSalaries.AdjustOutcome.INVALID_PERCENT,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, 0));
        assertEquals(50.0, state.getSetSpecialists().get(0).getSalary(), 1e-9);
        assertEquals(60, state.getSetSpecialists().get(0).getMorale());
    }

    /**
     * A department may be adjusted once per season: a second adjustment is refused and changes
     * nothing, another department is unaffected, and the season pass's reset allows it again.
     */
    @Test
    void eachDepartmentOncePerSeason() {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 0, 0, 0, 1);
        state.getSetSpecialists().add(engineer("a", 60, 50.0));
        assertEquals(DepartmentSalaries.AdjustOutcome.ADJUSTED, DepartmentSalaries.adjust(state, PersonnelGroup.SET, 10));
        assertTrue(DepartmentSalaries.adjustedThisSeason(state, PersonnelGroup.SET));
        assertFalse(DepartmentSalaries.adjustedThisSeason(state, PersonnelGroup.ASTRONAUT));

        assertEquals(DepartmentSalaries.AdjustOutcome.ALREADY_ADJUSTED,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, -10));
        assertEquals(55.0, state.getSetSpecialists().get(0).getSalary(), 1e-9, "the refused cut changed nothing");
        assertEquals(70, state.getSetSpecialists().get(0).getMorale());

        DepartmentSalaries.startNewSeason(state);
        assertEquals(DepartmentSalaries.AdjustOutcome.ADJUSTED,
            DepartmentSalaries.adjust(state, PersonnelGroup.SET, -10));
    }
}
