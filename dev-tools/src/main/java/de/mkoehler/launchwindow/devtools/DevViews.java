package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Read-only views of a player's state for the {@link DevClient}: enough to find the
 * ids the action commands need ({@code design.md} &sect;5, M4 slice 4e).
 *
 * @author Mario Koehler
 */
public final class DevViews {

    /** The view commands, for {@code help}. */
    public static final String HELP =
        "Views: roster | candidates | components | missions | buildings | goals | news";

    private static final List<String> VIEWS =
        List.of("roster", "candidates", "components", "missions", "buildings", "goals", "news");

    private DevViews() {
    }

    /**
     * Returns whether a command line is a view.
     *
     * @param line the command line
     * @return {@code true} for a view command
     */
    public static boolean isView(String line) {
        return VIEWS.contains(line.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * Renders a view of a player's state.
     *
     * @param line     the view command
     * @param snapshot the player's state
     * @return the rendered text
     */
    public static String render(String line, PlayerSnapshot snapshot) {
        StringBuilder out = new StringBuilder();
        switch (line.strip().toLowerCase(Locale.ROOT)) {
            case "roster" -> {
                people(out, "Astronauts", snapshot.getAstronauts(), DevViews::astronautSkills);
                people(out, "SET", snapshot.getSetSpecialists(), DevViews::setSkills);
                people(out, "Mission Control", snapshot.getMissionControlOfficers(), DevViews::controllerSkills);
            }
            case "candidates" -> {
                people(out, "Astronaut candidates", snapshot.getAstronautCandidates(), DevViews::astronautSkills);
                people(out, "SET candidates", snapshot.getSetSpecialistCandidates(), DevViews::setSkills);
                people(out, "Mission Control candidates", snapshot.getMissionControlCandidates(),
                    DevViews::controllerSkills);
            }
            case "components" -> {
                out.append("Open programs: ").append(snapshot.getOpenedProgramIds()).append('\n');
                for (Map.Entry<String, OwnedMissionComponent> owned : snapshot.getMissionComponents().entrySet()) {
                    out.append(String.format("  %-22s reliability %5.1f (floor %4.1f)%s%n", owned.getKey(),
                        owned.getValue().getCurrentReliability(), owned.getValue().getFloorReliability(),
                        owned.getValue().isActive() ? "" : " inactive"));
                }
            }
            case "missions" -> {
                for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
                    out.append(String.format("  %s %s season %d, rockets %s, crew %s, capcom %s, mc %s%n",
                        mission.getId(), mission.getMissionConfigurationId(), mission.getCurrentSeasonIndex() + 1,
                        mission.getRocketIds(), mission.getAssignedAstronautIds(), mission.getCapcomAstronautIds(),
                        mission.getAssignedMissionControlOfficerIds()));
                }
                out.append("Completed flights: ").append(snapshot.getSuccessfulFlightCounts()).append('\n');
            }
            case "buildings" -> {
                for (Building building : Building.values()) {
                    Integer upgrading = snapshot.getBuildingUpgradeSeasonsRemaining().get(building);
                    out.append(String.format("  %-18s level %d%s%n", building, snapshot.buildingLevel(building),
                        upgrading == null ? "" : ", upgrading (" + upgrading + " season(s) left)"));
                }
            }
            case "goals" -> out.append("Achieved (goal=turn): ").append(snapshot.getAchievedGoals())
                .append("\nCancelled: ").append(snapshot.getCancelledGoalIds()).append('\n');
            case "news" -> snapshot.getNewsLog().forEach(entry -> out.append("  ").append(entry).append('\n'));
            default -> out.append("Unknown view.\n");
        }
        return out.toString().stripTrailing();
    }

    private static <P extends Personnel> void people(StringBuilder out, String title, List<P> people,
            Function<P, String> skills) {
        out.append(title).append(" (").append(people.size()).append("):\n");
        for (P person : people) {
            out.append(String.format("  %-6s %-22s age %2d morale %3d salary %3.0f %-11s %s%n", person.getId(),
                person.getName(), person.getAge(), person.getMorale(), person.getSalary(),
                person.getAssignmentState(), skills.apply(person)));
        }
    }

    private static String astronautSkills(Astronaut astronaut) {
        StringBuilder skills = new StringBuilder();
        for (AstronautSkill skill : AstronautSkill.values()) {
            skills.append(String.format("%s %.0f ", skill, astronaut.getSkill(skill)));
        }
        return skills.toString().strip();
    }

    private static String setSkills(SetSpecialist specialist) {
        StringBuilder skills = new StringBuilder();
        for (SetSkill skill : SetSkill.values()) {
            skills.append(String.format("%s %.0f ", skill, specialist.getSkill(skill)));
        }
        String research = specialist.getRdTargetComponentId() == null ? ""
            : " researching " + specialist.getRdTargetComponentId() + " slot " + specialist.getRdSlot();
        return skills.toString().strip() + research;
    }

    private static String controllerSkills(MissionControlOfficer officer) {
        StringBuilder skills = new StringBuilder();
        for (MissionControlSkill skill : MissionControlSkill.values()) {
            skills.append(String.format("%s %.0f ", skill, officer.getSkill(skill)));
        }
        return skills.toString().strip();
    }
}
