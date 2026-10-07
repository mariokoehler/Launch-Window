package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Mission Control's view logic ({@code ui-spec.md} &sect;4.7): the flight controllers' summary,
 * filters and states (with the mission and console each one holds), and the missions split into
 * scheduled (launched, not flown yet) and active (in flight).
 *
 * @author Mario Koehler
 */
public final class MissionControl {

    private MissionControl() {
    }

    /**
     * The roster filters.
     */
    public enum Filter {
        ALL("All"),
        IDLE("Idle"),
        IN_TRAINING("In training"),
        ON_A_MISSION("On a mission"),
        UNAVAILABLE("Unavailable"),
        /** Astronauts only: on PR duty. */
        ON_PR_TOUR("On PR tour");

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
     * The department summary strip.
     *
     * @param headcount      the controllers
     * @param cap            the building's roster cap
     * @param level          Mission Control's level
     * @param nextCap        the next level's cap, or -1 at the top level
     * @param onMission      controllers on a mission
     * @param idle           idle and available controllers
     * @param inTraining     controllers in training
     * @param experts        controllers holding the Expert trait in any skill
     * @param averageMorale  the average morale, 0 with nobody
     * @param totalSalary    the salaries per season
     */
    public record Summary(int headcount, int cap, int level, int nextCap, int onMission, int idle, int inTraining,
                          int experts, int averageMorale, double totalSalary) {
    }

    /**
     * Where someone serves on a mission.
     *
     * @param mission       the mission
     * @param configuration its configuration
     * @param slotTitle     the seat or console title ("BOOSTER", "COSMONAUT")
     */
    public record Post(InProgressMission mission, MissionConfiguration configuration, String slotTitle) {
    }

    /**
     * Works out the summary strip.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the summary
     */
    public static Summary summary(PlayerSnapshot state, ContentCatalog catalog) {
        List<MissionControlOfficer> roster = state.getMissionControlOfficers();
        int level = state.buildingLevel(Building.MISSION_CONTROL);
        int maxLevel = catalog.buildings().of(Building.MISSION_CONTROL).maxLevel();
        int nextCap = level < maxLevel ? catalog.buildings().capacity(Building.MISSION_CONTROL, level + 1) : -1;
        int onMission = 0;
        int idle = 0;
        int training = 0;
        int experts = 0;
        int morale = 0;
        double salary = 0;
        for (MissionControlOfficer officer : roster) {
            boolean unavailable = officer.getUnavailableSeasons() > 0;
            if (!unavailable && officer.getAssignmentState() == AssignmentState.ASSIGNED) {
                onMission++;
            } else if (!unavailable && officer.getAssignmentState() == AssignmentState.IDLE) {
                idle++;
            } else if (!unavailable && officer.getAssignmentState() == AssignmentState.IN_TRAINING) {
                training++;
            }
            if (!officer.getExpertSkills().isEmpty()) {
                experts++;
            }
            morale += officer.getMorale();
            salary += officer.getSalary();
        }
        return new Summary(roster.size(), catalog.buildings().capacity(Building.MISSION_CONTROL, level), level,
            nextCap, onMission, idle, training, experts, roster.isEmpty() ? 0 : Math.round(morale / (float) roster.size()),
            salary);
    }

    /**
     * Finds where someone serves on a mission: a crew seat or a console still in use.
     *
     * @param personId the person
     * @param state    the player's state
     * @param catalog  the content pack
     * @return the post, or {@code null} if they're on none
     */
    public static Post post(String personId, PlayerSnapshot state, ContentCatalog catalog) {
        for (InProgressMission mission : state.getInProgressMissions().values()) {
            MissionConfiguration configuration = catalog.missionConfiguration(mission.getMissionConfigurationId())
                .orElse(null);
            if (configuration == null) {
                continue;
            }
            int crew = mission.getCrewSlotIds().indexOf(personId);
            if (crew >= 0 && crew < configuration.crewSlots().size()) {
                return new Post(mission, configuration, configuration.crewSlots().get(crew));
            }
            int control = mission.getControlSlotIds().indexOf(personId);
            if (control >= 0 && control < configuration.controlSlots().size() && !released(mission, configuration,
                control)) {
                return new Post(mission, configuration, configuration.controlSlots().get(control).title());
            }
        }
        return null;
    }

    /**
     * Returns whether a console's last season has flown: its holder went idle then, and it stays
     * empty for the rest of the mission.
     *
     * @param mission       the mission
     * @param configuration its configuration
     * @param slot          the console
     * @return whether released
     */
    public static boolean released(InProgressMission mission, MissionConfiguration configuration, int slot) {
        return configuration.usedControlSlots().contains(slot)
            && !configuration.usedControlSlotsFrom(mission.getCurrentSeasonIndex()).contains(slot);
    }

    /**
     * Names a mission by program and configuration ("Vostok · Orbital").
     *
     * @param configuration the configuration
     * @return the name
     */
    public static String missionName(MissionConfiguration configuration) {
        return ContentNames.of(configuration.programId()) + " · " + Missions.name(configuration);
    }

    /**
     * Words a controller's state, as on the state badge ("On Vostok · Orbital · BOOSTER").
     *
     * @param officer the controller
     * @param state   the player's state
     * @param catalog the content pack
     * @return the words
     */
    public static String stateText(MissionControlOfficer officer, PlayerSnapshot state, ContentCatalog catalog) {
        if (officer.getUnavailableSeasons() > 0) {
            int seasons = officer.getUnavailableSeasons();
            return "Unavailable · " + seasons + (seasons == 1 ? " season" : " seasons");
        }
        return switch (officer.getAssignmentState()) {
            case IDLE -> "Idle";
            case ASSIGNED -> {
                Post post = post(officer.getId(), state, catalog);
                yield post == null ? "On a mission" : "On " + missionName(post.configuration()) + " · " + post.slotTitle();
            }
            case IN_TRAINING -> trainingText(officer);
            case PR_DUTY -> "On PR tour";
        };
    }

    private static String trainingText(MissionControlOfficer officer) {
        TrainingKind kind = officer.getTrainingKind();
        if (kind == TrainingKind.PERMANENT) {
            MissionControlSkill target = officer.getTrainingTargetSkill();
            return "In training · Permanent · " + (target == null ? "" : CrewSkills.label(target.name()) + " → ")
                + Math.round(officer.getTrainingTargetLevel());
        }
        if (kind == TrainingKind.EXPERT) {
            MissionControlSkill target = officer.getTrainingTargetSkill();
            return "In training · Expert" + (target == null ? "" : " · " + CrewSkills.label(target.name()));
        }
        int left = Math.max(0, officer.getTrainingSeasonsTotal() - officer.getTrainingSeasonsCompleted());
        String kindLabel = kind == null ? "" : kind.name().charAt(0) + kind.name().substring(1).toLowerCase(Locale.ROOT);
        // The skill being trained, as the astronauts' rows show it.
        MissionControlSkill target = officer.getTrainingTargetSkill();
        String skill = target == null ? "" : " · " + CrewSkills.label(target.name());
        return "In training · " + kindLabel + skill + " · " + left + " left";
    }

    /**
     * Returns whether a controller passes a filter.
     *
     * @param officer the controller
     * @param filter  the filter
     * @return whether shown
     */
    public static boolean matches(MissionControlOfficer officer, Filter filter) {
        boolean unavailable = officer.getUnavailableSeasons() > 0;
        return switch (filter) {
            case ALL -> true;
            case UNAVAILABLE -> unavailable;
            case IDLE -> !unavailable && officer.getAssignmentState() == AssignmentState.IDLE;
            case IN_TRAINING -> !unavailable && officer.getAssignmentState() == AssignmentState.IN_TRAINING;
            case ON_A_MISSION -> !unavailable && officer.getAssignmentState() == AssignmentState.ASSIGNED;
            case ON_PR_TOUR -> false;
        };
    }

    /**
     * Returns the missions launched but not flown yet: they fly at the next turn and can still be
     * scrubbed for a full refund.
     *
     * @param state the player's state
     * @return the missions, in launch order
     */
    public static List<InProgressMission> scheduled(PlayerSnapshot state) {
        return state.getInProgressMissions().values().stream()
            .filter(mission -> mission.getCurrentSeasonIndex() == 0)
            .sorted(Comparator.comparing(InProgressMission::getId))
            .toList();
    }

    /**
     * Returns the missions in flight: at least one season has flown.
     *
     * @param state the player's state
     * @return the missions, in launch order
     */
    public static List<InProgressMission> active(PlayerSnapshot state) {
        return state.getInProgressMissions().values().stream()
            .filter(mission -> mission.getCurrentSeasonIndex() > 0)
            .sorted(Comparator.comparing(InProgressMission::getId))
            .toList();
    }
}
