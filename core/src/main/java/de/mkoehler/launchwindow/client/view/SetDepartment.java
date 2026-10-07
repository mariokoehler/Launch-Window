package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What the SET Centre's personnel overview shows about its people ({@code ui-spec.md} component 11):
 * each row's state wording, the filters, the sort, and the department summary strip. Plain logic,
 * no libGDX, so it's tested directly.
 *
 * @author Mario Koehler
 */
public final class SetDepartment {

    /**
     * How close to the retirement age counts as "near retirement" in the summary strip, in years.
     * <b>Unconfirmed default</b> (2026-10-04): the mockup used a fixed age of 60, but SET retire at
     * {@link PersonnelLifecycle#SET_RETIREMENT_AGE}.
     */
    public static final int NEAR_RETIREMENT_YEARS = 5;

    /**
     * The overview's filter chips.
     */
    public enum Filter {
        /** Everyone. */
        ALL("All"),
        /** Idle and available. */
        IDLE("Idle"),
        /** In training of any kind. */
        IN_TRAINING("In training"),
        /** On payload or rocket R&amp;D. */
        RESEARCHING("Researching"),
        /** Out for a number of seasons (a random event). */
        UNAVAILABLE("Unavailable");

        private final String label;

        Filter(String label) {
            this.label = label;
        }

        /**
         * Returns the chip's label.
         *
         * @return the label
         */
        public String label() {
            return label;
        }
    }

    /**
     * The department summary strip's numbers.
     *
     * @param headcount      people on the roster
     * @param cap            the SET Centre's roster cap at its current level
     * @param inTraining     people in training
     * @param researching    people on R&amp;D
     * @param nearRetirement people within {@link #NEAR_RETIREMENT_YEARS} of the retirement age
     * @param averageMorale  the roster's average morale, rounded; 0 for an empty roster
     * @param totalSalary    the roster's salaries per season, together
     * @param busy           people not idle and available (the footer's "busy")
     */
    public record Summary(int headcount, int cap, int inTraining, int researching, int nearRetirement,
                          int averageMorale, double totalSalary, int busy) {
    }

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private SetDepartment() {
    }

    /**
     * Returns a specialist's skill value, 0 if somehow missing.
     *
     * @param specialist the specialist
     * @param skill      the skill
     * @return the value, 0 to 100
     */
    public static double skill(SetSpecialist specialist, SetSkill skill) {
        Double value = specialist.getSkills().get(skill);
        return value == null ? 0.0 : value;
    }

    /**
     * Returns the row's state wording, as on the state badge.
     *
     * @param specialist the specialist
     * @return for example "Idle", "In training · Advanced · Rockets · 2 left" or "Researching · Vostok Capsule ·
     *     slot 1"
     */
    public static String stateText(SetSpecialist specialist) {
        if (specialist.getUnavailableSeasons() > 0) {
            int seasons = specialist.getUnavailableSeasons();
            return "Unavailable · " + seasons + (seasons == 1 ? " season" : " seasons");
        }
        return switch (specialist.getAssignmentState()) {
            case IDLE -> "Idle";
            case ASSIGNED -> "Researching · " + ContentNames.of(specialist.getRdTargetComponentId())
                + " · slot " + (specialist.getRdSlot() == null ? "?" : specialist.getRdSlot() + 1);
            case IN_TRAINING -> trainingText(specialist);
            case PR_DUTY -> "On PR tour";
        };
    }

    private static String trainingText(SetSpecialist specialist) {
        TrainingKind kind = specialist.getTrainingKind();
        if (kind == TrainingKind.PERMANENT) {
            SetSkill target = specialist.getTrainingTargetSkill();
            return "In training · Permanent · " + (target == null ? "" : SetSkills.label(target) + " → ")
                + Math.round(specialist.getTrainingTargetLevel());
        }
        int left = Math.max(0, specialist.getTrainingSeasonsTotal() - specialist.getTrainingSeasonsCompleted());
        String kindLabel = kind == null ? "" : kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT);
        // The skill being trained, as the astronauts' rows show it.
        SetSkill target = specialist.getTrainingTargetSkill();
        String skill = target == null ? "" : " · " + SetSkills.label(target);
        return "In training · " + kindLabel + skill + " · " + left + " left";
    }

    /**
     * Returns whether the row's state badge is the gold "something is happening" one, rather than
     * a plain pill (idle or unavailable).
     *
     * @param specialist the specialist
     * @return whether in training or researching
     */
    public static boolean isActive(SetSpecialist specialist) {
        return specialist.getUnavailableSeasons() == 0 && specialist.getAssignmentState() != AssignmentState.IDLE;
    }

    /**
     * Returns whether someone counts as busy: anyone not idle and available.
     *
     * @param person the employee
     * @return whether busy
     */
    public static boolean isBusy(Personnel person) {
        return person.getUnavailableSeasons() > 0 || person.getAssignmentState() != AssignmentState.IDLE;
    }

    /**
     * Returns whether a specialist passes a filter.
     *
     * @param specialist the specialist
     * @param filter     the filter
     * @return whether shown
     */
    public static boolean matches(SetSpecialist specialist, Filter filter) {
        boolean unavailable = specialist.getUnavailableSeasons() > 0;
        return switch (filter) {
            case ALL -> true;
            case UNAVAILABLE -> unavailable;
            case IDLE -> !unavailable && specialist.getAssignmentState() == AssignmentState.IDLE;
            case IN_TRAINING -> !unavailable && specialist.getAssignmentState() == AssignmentState.IN_TRAINING;
            case RESEARCHING -> !unavailable && specialist.getAssignmentState() == AssignmentState.ASSIGNED;
        };
    }

    /**
     * Filters and sorts a roster for display.
     *
     * @param roster     the roster
     * @param filter     the filter
     * @param sortBy     the skill to sort by
     * @param descending whether highest first
     * @return the rows, in display order
     */
    public static List<SetSpecialist> rows(List<SetSpecialist> roster, Filter filter, SetSkill sortBy,
                                           boolean descending) {
        Comparator<SetSpecialist> bySkill = Comparator.comparingDouble(specialist -> skill(specialist, sortBy));
        return roster.stream()
            .filter(specialist -> matches(specialist, filter))
            .sorted(descending ? bySkill.reversed() : bySkill)
            .toList();
    }

    /**
     * Returns the SET Centre's roster cap at its current level.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the cap
     */
    public static int cap(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.buildings().capacity(Building.SET_CENTER, state.buildingLevel(Building.SET_CENTER));
    }

    /**
     * Works out the department summary strip.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the summary
     */
    public static Summary summary(PlayerSnapshot state, ContentCatalog catalog) {
        List<SetSpecialist> roster = state.getSetSpecialists();
        int nearAge = PersonnelLifecycle.retirementAgeFor(PersonnelGroup.SET) - NEAR_RETIREMENT_YEARS;
        int morale = 0;
        double salary = 0;
        int training = 0;
        int researching = 0;
        int near = 0;
        int busy = 0;
        for (SetSpecialist specialist : roster) {
            morale += specialist.getMorale();
            salary += specialist.getSalary();
            training += specialist.getAssignmentState() == AssignmentState.IN_TRAINING ? 1 : 0;
            researching += specialist.getAssignmentState() == AssignmentState.ASSIGNED ? 1 : 0;
            near += specialist.getAge() >= nearAge ? 1 : 0;
            busy += isBusy(specialist) ? 1 : 0;
        }
        int average = roster.isEmpty() ? 0 : Math.round((float) morale / roster.size());
        return new Summary(roster.size(), cap(state, catalog), training, researching, near, average, salary, busy);
    }
}
