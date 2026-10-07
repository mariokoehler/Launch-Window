package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.List;
import java.util.Objects;

/**
 * The department salary adjustment (gap G8, {@code ui-spec.md} component 13): a one-off change to
 * every salary in one personnel group by a whole percentage from -50 to +50, with every morale in
 * the group moving by +p for a raise and by -3&times;p for a cut, clamped to 0-100 (BASPM's own
 * rule, {@code baspm.md} &sect;9.8, adopted as is by user decision 2026-10-03).
 *
 * <p>The Manage department dialog previews the outcome with {@link #projectedSalary} and
 * {@link #projectedAverageMorale}, the same code {@link #adjust} applies, so the preview can never
 * disagree with the result (gap G7). Unlike BASPM, each department may be adjusted only once per
 * season (user decision 2026-10-06): {@link #adjust} records the group on the snapshot, and the
 * season pass clears the record ({@link #startNewSeason}).</p>
 *
 * @author Mario Koehler
 */
public final class DepartmentSalaries {

    /** The largest cut, in percent. */
    public static final int MIN_PERCENT = -50;

    /** The largest raise, in percent. */
    public static final int MAX_PERCENT = 50;

    /** The result of {@link #adjust}. */
    public enum AdjustOutcome {
        /** Every salary and morale in the group changed. */
        ADJUSTED,
        /** The percentage is outside -50 to +50, or zero. */
        INVALID_PERCENT,
        /** The group has nobody in it. */
        EMPTY_DEPARTMENT,
        /** The group's salaries were already adjusted this season. */
        ALREADY_ADJUSTED
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private DepartmentSalaries() {
    }

    /**
     * Returns how much every morale in the group moves for an adjustment.
     *
     * @param percent the salary change, -50 to +50
     * @return +p for a raise, 3&times;p (negative) for a cut
     */
    public static int moraleChange(int percent) {
        return percent >= 0 ? percent : 3 * percent;
    }

    /**
     * Returns one employee's salary after an adjustment.
     *
     * @param salary  the current salary
     * @param percent the salary change
     * @return the new salary
     */
    public static double adjustedSalary(double salary, int percent) {
        return salary * (1.0 + percent / 100.0);
    }

    /**
     * Returns one employee's morale after an adjustment.
     *
     * @param morale  the current morale
     * @param percent the salary change
     * @return the new morale, clamped to 0-100
     */
    public static int adjustedMorale(int morale, int percent) {
        return Math.max(0, Math.min(100, morale + moraleChange(percent)));
    }

    /**
     * Returns the group's total salary per season after an adjustment.
     *
     * @param roster  the group
     * @param percent the salary change
     * @return the total
     */
    public static double projectedSalary(List<? extends Personnel> roster, int percent) {
        return roster.stream().mapToDouble(person -> adjustedSalary(person.getSalary(), percent)).sum();
    }

    /**
     * Returns the group's average morale after an adjustment, each person clamped on their own.
     *
     * @param roster  the group
     * @param percent the salary change
     * @return the average, rounded; 0 for an empty group
     */
    public static int projectedAverageMorale(List<? extends Personnel> roster, int percent) {
        if (roster.isEmpty()) {
            return 0;
        }
        double sum = roster.stream().mapToInt(person -> adjustedMorale(person.getMorale(), percent)).sum();
        return (int) Math.round(sum / roster.size());
    }

    /**
     * Applies an adjustment to a whole group.
     *
     * @param snapshot the player's state
     * @param group    the group
     * @param percent  the salary change, -50 to +50, not zero
     * @return the outcome; nothing changes unless {@link AdjustOutcome#ADJUSTED}
     */
    public static AdjustOutcome adjust(PlayerSnapshot snapshot, PersonnelGroup group, int percent) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(group, "group must not be null");
        if (percent == 0 || percent < MIN_PERCENT || percent > MAX_PERCENT) {
            return AdjustOutcome.INVALID_PERCENT;
        }
        List<? extends Personnel> roster = switch (group) {
            case ASTRONAUT -> snapshot.getAstronauts();
            case SET -> snapshot.getSetSpecialists();
            case MISSION_CONTROL -> snapshot.getMissionControlOfficers();
        };
        if (roster.isEmpty()) {
            return AdjustOutcome.EMPTY_DEPARTMENT;
        }
        if (adjustedThisSeason(snapshot, group)) {
            return AdjustOutcome.ALREADY_ADJUSTED;
        }
        for (Personnel person : roster) {
            person.setSalary(adjustedSalary(person.getSalary(), percent));
            person.setMorale(adjustedMorale(person.getMorale(), percent));
        }
        Careers.salaryChanged(roster, percent);
        snapshot.getSalaryAdjustedGroups().add(group.name());
        return AdjustOutcome.ADJUSTED;
    }

    /**
     * Returns whether a group's salaries were already adjusted this season.
     *
     * @param snapshot the player's state
     * @param group    the group
     * @return {@code true} if another adjustment this season would be refused
     */
    public static boolean adjustedThisSeason(PlayerSnapshot snapshot, PersonnelGroup group) {
        return snapshot.getSalaryAdjustedGroups().contains(group.name());
    }

    /**
     * Clears the record of this season's adjustments, so every department may be adjusted again.
     * Run once per season pass, after the season's actions.
     *
     * @param snapshot the player's state
     */
    public static void startNewSeason(PlayerSnapshot snapshot) {
        snapshot.getSalaryAdjustedGroups().clear();
    }
}
