package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>Tests for the goal gate on {@link ProgramOpening#open}: a program is
 * blocked until every goal it requires is achieved or cancelled - an AND gate,
 * per {@code baspm.md} &sect;11.3 (corrected 2026-10-03).</p>
 *
 * @author Mario Koehler
 */
class ProgramOpeningGoalGateTest {

    private static final MissionComponent CAPSULE = new MissionComponent("capsule-mercury", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 0, List.of());
    private static final ProgramCategory CATEGORY = new ProgramCategory("early-space", "Early Space");
    private static final List<Goal> GOALS = List.of(new Goal("orbit", 1, 1, List.of()),
        new Goal("crewed", 1, 1, List.of()));

    private static ContentCatalog catalogWith(List<String> requiredGoalIds) {
        Program program = new Program("mercury-program", "early-space", 100, 0, List.of("capsule-mercury"),
            List.of(), requiredGoalIds);
        return new ContentCatalog(List.of(CATEGORY), List.of(program), List.of(), List.of(CAPSULE), GOALS);
    }

    private static PlayerSnapshot richPlayer() {
        return new PlayerSnapshot("Agency", Archetype.NASA, 10_000, 0, 0, 1);
    }

    /**
     * A program requiring two goals stays closed until both are achieved or cancelled.
     */
    @Test
    void programStaysClosedUntilEveryRequiredGoalIsFulfilled() {
        ContentCatalog catalog = catalogWith(List.of("orbit", "crewed"));
        PlayerSnapshot player = richPlayer();

        assertEquals(OpenProgramOutcome.GOALS_NOT_MET, ProgramOpening.open(player, "mercury-program", catalog));

        player.getAchievedGoals().put("orbit", 1);
        assertEquals(OpenProgramOutcome.GOALS_NOT_MET, ProgramOpening.open(player, "mercury-program", catalog));

        player.getCancelledGoalIds().add("crewed");
        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(player, "mercury-program", catalog));
    }

    /**
     * A program with no goal requirement is never blocked by the gate.
     */
    @Test
    void programWithNoGoalRequirementIsNotGated() {
        ContentCatalog catalog = catalogWith(List.of());
        assertEquals(OpenProgramOutcome.OPENED, ProgramOpening.open(richPlayer(), "mercury-program", catalog));
    }
}
