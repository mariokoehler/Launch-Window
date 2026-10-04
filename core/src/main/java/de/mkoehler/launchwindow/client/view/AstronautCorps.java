package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The Astronaut Complex's wording ({@code ui-spec.md} &sect;4.9): the department summary, each
 * astronaut's state ("On Vostok · Orbital · COSMONAUT", or a CAPCOM console), and the hint for an
 * idle astronaut who could train toward Expert.
 *
 * @author Mario Koehler
 */
public final class AstronautCorps {

    private AstronautCorps() {
    }

    /**
     * The department strip's numbers.
     *
     * @param headcount      astronauts on the roster
     * @param cap            the roster cap at the building's level
     * @param onMission      astronauts on a mission, crew or CAPCOM
     * @param idle           idle and available
     * @param inTraining     in training
     * @param experts        astronauts holding the Expert trait in any skill
     * @param nearRetirement within {@link SetDepartment#NEAR_RETIREMENT_YEARS} of the retirement age
     * @param retirementAge  the age astronauts retire at
     * @param averageMorale  the roster's average morale, rounded
     * @param totalSalary    the roster's salaries per season
     */
    public record Summary(int headcount, int cap, int onMission, int idle, int inTraining, int experts,
                          int nearRetirement, int retirementAge, int averageMorale, double totalSalary) {
    }

    /**
     * Sums up the roster.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the summary
     */
    public static Summary summary(PlayerSnapshot state, ContentCatalog catalog) {
        List<Astronaut> roster = state.getAstronauts();
        int retirementAge = PersonnelLifecycle.retirementAgeFor(PersonnelGroup.ASTRONAUT);
        int onMission = 0;
        int idle = 0;
        int training = 0;
        int experts = 0;
        int near = 0;
        double morale = 0;
        double salary = 0;
        for (Astronaut astronaut : roster) {
            boolean available = astronaut.getUnavailableSeasons() == 0;
            if (available && astronaut.getAssignmentState() == AssignmentState.ASSIGNED) {
                onMission++;
            } else if (available && astronaut.getAssignmentState() == AssignmentState.IDLE) {
                idle++;
            } else if (available && astronaut.getAssignmentState() == AssignmentState.IN_TRAINING) {
                training++;
            }
            if (!astronaut.getExpertSkills().isEmpty()) {
                experts++;
            }
            if (astronaut.getAge() >= retirementAge - SetDepartment.NEAR_RETIREMENT_YEARS) {
                near++;
            }
            morale += astronaut.getMorale();
            salary += astronaut.getSalary();
        }
        int cap = catalog.buildings().capacity(Building.ASTRONAUT_CENTER, state.buildingLevel(Building.ASTRONAUT_CENTER));
        return new Summary(roster.size(), cap, onMission, idle, training, experts, near, retirementAge,
            roster.isEmpty() ? 0 : Math.round((float) (morale / roster.size())), salary);
    }

    /**
     * Words an astronaut's state, as on the state badge.
     *
     * @param astronaut the astronaut
     * @param state     the player's state
     * @param catalog   the content pack
     * @return the words
     */
    public static String stateText(Astronaut astronaut, PlayerSnapshot state, ContentCatalog catalog) {
        if (astronaut.getUnavailableSeasons() > 0) {
            int seasons = astronaut.getUnavailableSeasons();
            return "Unavailable · " + seasons + (seasons == 1 ? " season" : " seasons");
        }
        return switch (astronaut.getAssignmentState()) {
            case IDLE -> "Idle";
            case ASSIGNED -> {
                MissionControl.Post post = MissionControl.post(astronaut.getId(), state, catalog);
                yield post == null ? "On a mission"
                    : "On " + MissionControl.missionName(post.configuration()) + " · " + post.slotTitle();
            }
            case IN_TRAINING -> trainingText(astronaut);
        };
    }

    private static String trainingText(Astronaut astronaut) {
        TrainingKind kind = astronaut.getTrainingKind() == TrainingKind.NONE ? null : astronaut.getTrainingKind();
        AstronautSkill target = astronaut.getTrainingTargetSkill();
        String skill = target == null ? "" : CrewSkills.label(target.name());
        if (kind == TrainingKind.PERMANENT) {
            return "In training · Permanent · " + (skill.isEmpty() ? "" : skill + " → ")
                + Math.round(astronaut.getTrainingTargetLevel());
        }
        if (kind == TrainingKind.EXPERT) {
            return "In training · Expert" + (skill.isEmpty() ? "" : " · " + skill);
        }
        int left = Math.max(0, astronaut.getTrainingSeasonsTotal() - astronaut.getTrainingSeasonsCompleted());
        String kindLabel = kind == null ? "" : kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT);
        return "In training · " + kindLabel + (skill.isEmpty() ? "" : " · " + skill) + " · " + left + " left";
    }

    /**
     * Returns whether an astronaut passes a filter.
     *
     * @param astronaut the astronaut
     * @param filter    the filter
     * @return whether shown
     */
    public static boolean matches(Astronaut astronaut, MissionControl.Filter filter) {
        boolean unavailable = astronaut.getUnavailableSeasons() > 0;
        return switch (filter) {
            case ALL -> true;
            case UNAVAILABLE -> unavailable;
            case IDLE -> !unavailable && astronaut.getAssignmentState() == AssignmentState.IDLE;
            case IN_TRAINING -> !unavailable && astronaut.getAssignmentState() == AssignmentState.IN_TRAINING;
            case ON_A_MISSION -> !unavailable && astronaut.getAssignmentState() == AssignmentState.ASSIGNED;
        };
    }

    /**
     * Returns the gold hint for an idle astronaut with a skill that can train toward Expert, or
     * {@code null}.
     *
     * @param astronaut the astronaut
     * @return the hint, such as "Can train toward Expert in Fitness"
     */
    public static String expertHint(Astronaut astronaut) {
        if (astronaut.getUnavailableSeasons() > 0 || astronaut.getAssignmentState() != AssignmentState.IDLE) {
            return null;
        }
        List<String> eligible = Department.ASTRONAUT.expertEligible(astronaut);
        return eligible.isEmpty() ? null : "Can train toward Expert in "
            + eligible.stream().map(CrewSkills::label).collect(Collectors.joining(", "));
    }
}
