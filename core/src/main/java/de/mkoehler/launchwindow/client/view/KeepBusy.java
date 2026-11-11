package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Personnel;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Keep Busy" button's plan (the user's request, 2026-11-11, {@code ui-spec.md} component 11):
 * every idle person in a department goes into permanent training to {@link #TARGET} in their
 * highest skill still below it; someone at {@link #TARGET} or more in every skill is left alone.
 * Ties go to the skill first in the department's order. Headless and read-only.
 *
 * @author Mario Koehler
 */
public final class KeepBusy {

    /** The permanent training target, the training dialog's default too. */
    public static final int TARGET = 90;

    /**
     * One person and the skill they'd train.
     *
     * @param person the person, idle and available
     * @param skill  the skill's name
     */
    public record Assignment(Personnel person, String skill) {
    }

    private KeepBusy() {
    }

    /**
     * Returns who would go into training, and in what, in roster order.
     *
     * @param department the department
     * @param roster     its roster
     * @return the assignments, empty when nobody is idle or everyone idle is at the target
     */
    public static List<Assignment> plan(Department department, List<? extends Personnel> roster) {
        List<Assignment> plan = new ArrayList<>();
        for (Personnel person : roster) {
            // As the server checks (StartTrainingOutcome.NOT_IDLE): idle and available this season.
            if (person.getAssignmentState() != AssignmentState.IDLE || person.getUnavailableSeasons() > 0) {
                continue;
            }
            String best = null;
            double bestValue = -1;
            for (String skill : department.skills()) {
                double value = department.skill(person, skill);
                if (value < TARGET && value > bestValue) {
                    best = skill;
                    bestValue = value;
                }
            }
            if (best != null) {
                plan.add(new Assignment(person, best));
            }
        }
        return List.copyOf(plan);
    }
}
