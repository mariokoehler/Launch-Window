package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link PayExpectations} ({@code design.md} &sect;2.2(c), pay expectations).
 *
 * @author Mario Koehler
 */
class PayExpectationsTest {

    @AfterEach
    void allOff() {
        PayExpectations.setParts(false, false, false, false);
    }

    private static SetSpecialist engineer(String id, int morale, double salary) {
        SetSpecialist engineer = new SetSpecialist(id, "E " + id, 40, morale, 50);
        engineer.setSalary(salary);
        return engineer;
    }

    private static PlayerSnapshot agency(SetSpecialist... people) {
        PlayerSnapshot state = new PlayerSnapshot("Orion", Archetype.NASA, 10_000, 0, 0, 1);
        state.getSetSpecialists().addAll(List.of(people));
        return state;
    }

    /** Off, as the game runs until the screens are built: no ceiling, no effect on skill, no rises. */
    @Test
    void everyPartIsOffByDefault() {
        SetSpecialist underpaid = engineer("a", 95, 30);
        underpaid.setExpectedSalary(60.0);
        assertFalse(PayExpectations.anyOn());
        assertEquals(100, PayExpectations.moraleCeiling(underpaid));
        assertEquals(1.0, PayExpectations.skillFactor(underpaid));
        PlayerSnapshot state = agency(underpaid);
        assertTrue(PayExpectations.advanceSeason(state, 16, 16).isEmpty());
        assertEquals(60.0, underpaid.expectedSalary());
        assertEquals(null, PayExpectations.nextBotStep(state, 0));
    }

    /** The ceiling falls 3 points per percent underpaid; work fills only up to it, and above it morale drifts. */
    @Test
    void theCeilingCapsWorkAndMoraleAboveItDrifts() {
        PayExpectations.setParts(false, false, true, false);
        SetSpecialist person = engineer("a", 90, 45);
        person.setExpectedSalary(50.0);
        assertEquals(10.0, PayExpectations.underpaidPercent(person), 1e-9);
        assertEquals(70, PayExpectations.moraleCeiling(person));
        assertEquals(88, PayExpectations.withCeiling(person, 3), "above the ceiling: down by the drift");
        assertEquals(86, PayExpectations.withCeiling(person, -4), "a bigger idle loss still applies");
        person.setMorale(70);
        assertEquals(70, PayExpectations.withCeiling(person, 3), "at the ceiling work adds nothing");
        person.setMorale(60);
        assertEquals(63, PayExpectations.withCeiling(person, 3), "below it, as before");
        person.setMorale(71);
        assertEquals(70, PayExpectations.withCeiling(person, 3), "the drift stops at the ceiling");
        person.setSalary(50);
        assertEquals(100, PayExpectations.moraleCeiling(person), "paid as expected: no ceiling");
    }

    /** From 80 people work 5 % better, below 50 5 % worse. */
    @Test
    void moraleChangesHowWellPeopleWork() {
        PayExpectations.setParts(false, false, false, true);
        assertEquals(1.05, PayExpectations.skillFactor(engineer("a", 80, 40)), 1e-9);
        assertEquals(1.0, PayExpectations.skillFactor(engineer("b", 79, 40)), 1e-9);
        assertEquals(0.95, PayExpectations.skillFactor(engineer("c", 49, 40)), 1e-9);
    }

    /** A review raises everyone 10 %; performance adds per career line of the turn, at most 15 % a turn. */
    @Test
    void expectationsRiseAtAReviewAndWithPerformance() {
        PayExpectations.setParts(true, true, false, false);
        SetSpecialist steady = engineer("a", 80, 40);
        SetSpecialist star = engineer("b", 80, 40);
        star.getCareer().add(new CareerEntry(16, CareerEntry.Kind.FIRST, "g", "c", "", 0, 0));
        star.getCareer().add(new CareerEntry(16, CareerEntry.Kind.EXPERT, "s", "", "", 0, 0));
        star.getCareer().add(new CareerEntry(15, CareerEntry.Kind.FIRST, "old", "c", "", 0, 0));
        PlayerSnapshot state = agency(steady, star);

        List<ResolutionEvent> events = PayExpectations.advanceSeason(state, 16, 16);

        assertEquals(44.0, steady.expectedSalary(), 1e-9, "the review alone");
        assertEquals(40 * 1.15 * 1.10, star.expectedSalary(), 1e-9, "First and Expert, capped at 15 %, then the review");
        assertEquals(2, events.size());
        ExpectationRose rose = (ExpectationRose) events.get(0);
        assertEquals(List.of("FIRST", "EXPERT"), rose.reasons());
        assertTrue(events.get(1) instanceof ExpectationsRoseAtReview);
        PayExpectations.advanceSeason(state, 17, 17);
        assertEquals(44.0, steady.expectedSalary(), 1e-9, "no review, no rise");
    }

    /** One person, once a season: to their expectation however far, or a capped percentage; morale and loyalty. */
    @Test
    void raisingOnePerson() {
        PayExpectations.setParts(true, true, true, true);
        SetSpecialist person = engineer("a", 50, 40);
        person.setExpectedSalary(100.0);
        PlayerSnapshot state = agency(person);
        int loyalty = person.getLoyalty();

        assertEquals(PayExpectations.RaiseOutcome.INVALID_RAISE, PayExpectations.raise(state, "a", 60));
        assertEquals(PayExpectations.RaiseOutcome.RAISED, PayExpectations.raise(state, "a", 0));
        assertEquals(100.0, person.getSalary(), 1e-9, "meeting it has no cap");
        assertEquals(100, person.getMorale(), "+150, clamped");
        assertEquals(Math.min(100, loyalty + 75), person.getLoyalty());
        assertEquals(PayExpectations.RaiseOutcome.ALREADY_RAISED, PayExpectations.raise(state, "a", 5));
        DepartmentSalaries.startNewSeason(state);
        assertEquals(PayExpectations.RaiseOutcome.RAISED, PayExpectations.raise(state, "a", 5));
        assertEquals(105.0, person.getSalary(), 1e-9);
        assertEquals(100.0, person.expectedSalary(), 1e-9, "a raise leaves the expectation where it was");
        assertEquals(PayExpectations.RaiseOutcome.UNKNOWN_PERSON, PayExpectations.raise(state, "nobody", 5));
    }

    /** The department: each underpaid person to their own expectation, once a season, and refused when none is. */
    @Test
    void bringingADepartmentUpToExpectations() {
        PayExpectations.setParts(true, true, true, true);
        SetSpecialist under = engineer("a", 60, 40);
        under.setExpectedSalary(50.0);
        SetSpecialist paid = engineer("b", 60, 50);
        PlayerSnapshot state = agency(under, paid);
        assertEquals(10.0, PayExpectations.costToMeet(state, PersonnelGroup.SET), 1e-9);

        assertEquals(DepartmentSalaries.AdjustOutcome.ADJUSTED,
            PayExpectations.meetExpectations(state, PersonnelGroup.SET));

        assertEquals(50.0, under.getSalary(), 1e-9);
        assertEquals(85, under.getMorale(), "+25 for a 25 % raise");
        assertEquals(50.0, paid.getSalary(), 1e-9);
        assertEquals(60, paid.getMorale(), "untouched");
        assertEquals(DepartmentSalaries.AdjustOutcome.ALREADY_ADJUSTED,
            PayExpectations.meetExpectations(state, PersonnelGroup.SET));
        DepartmentSalaries.startNewSeason(state);
        assertEquals(DepartmentSalaries.AdjustOutcome.NOBODY_UNDERPAID,
            PayExpectations.meetExpectations(state, PersonnelGroup.SET));
    }

    /** A cut leaves the expectation where it was, so it opens a gap; an unset expectation is pinned first. */
    @Test
    void aCutOpensAGap() {
        PayExpectations.setParts(false, false, true, false);
        SetSpecialist person = engineer("a", 90, 40);
        PlayerSnapshot state = agency(person);
        DepartmentSalaries.adjust(state, PersonnelGroup.SET, -10);
        assertEquals(40.0, person.expectedSalary(), 1e-9);
        assertEquals(36.0, person.getSalary(), 1e-9);
        assertEquals(70, PayExpectations.moraleCeiling(person));
    }

    /** The bots bring a department up when they can spare it, and the season pass's ceiling then lifts. */
    @Test
    void theBotsPay() {
        PayExpectations.setParts(true, true, true, true);
        SetSpecialist person = engineer("a", 70, 40);
        person.setExpectedSalary(44.0);
        person.setAssignmentState(AssignmentState.IDLE);
        PlayerSnapshot state = agency(person);
        PayExpectations.BotStep step = PayExpectations.nextBotStep(state, 0);
        assertEquals(new PayExpectations.BotStep(PersonnelGroup.SET, null), step);
        PayExpectations.botPolicy(state, 0);
        assertEquals(44.0, person.getSalary(), 1e-9);
        assertEquals(null, PayExpectations.nextBotStep(state, 0));
    }
}
