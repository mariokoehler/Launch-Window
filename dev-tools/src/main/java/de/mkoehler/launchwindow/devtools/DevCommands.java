package de.mkoehler.launchwindow.devtools;

import de.mkoehler.launchwindow.actions.RecallFromPrDutyAction;
import de.mkoehler.launchwindow.actions.StartPrDutyAction;
import de.mkoehler.launchwindow.actions.ReassignControlSlotAction;
import de.mkoehler.launchwindow.actions.AssignResearchAction;
import de.mkoehler.launchwindow.actions.CancelMissionAction;
import de.mkoehler.launchwindow.actions.CloseProgramAction;
import de.mkoehler.launchwindow.actions.CloseRocketProgramAction;
import de.mkoehler.launchwindow.actions.ConvertPrestigeAction;
import de.mkoehler.launchwindow.actions.DismissAction;
import de.mkoehler.launchwindow.actions.HireAction;
import de.mkoehler.launchwindow.actions.LaunchMissionAction;
import de.mkoehler.launchwindow.actions.OpenProgramAction;
import de.mkoehler.launchwindow.actions.OpenRocketProgramAction;
import de.mkoehler.launchwindow.actions.PullOutOfTrainingAction;
import de.mkoehler.launchwindow.actions.ReleaseResearchAction;
import de.mkoehler.launchwindow.actions.StartExpertTrainingAction;
import de.mkoehler.launchwindow.actions.StartTrainingAction;
import de.mkoehler.launchwindow.actions.UpgradeBuildingAction;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.persistence.SetFundsAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * <p>Turns a {@link DevClient} command line into the player action it submits - kept
 * apart from the console loop so the parsing is testable. Every M4 action has a
 * command ({@code design.md} &sect;5, M4 slice 4e).</p>
 *
 * @author Mario Koehler
 */
public final class DevCommands {

    /** The action commands, for {@code help}. */
    public static final String HELP = String.join("\n",
        "Actions:",
        "  open <programId> | close <programId>",
        "  rocket open <rocketId> | rocket close <rocketId>",
        "  hire <ASTRONAUT|SET|MISSION_CONTROL> <candidateId> [bonus] | dismiss <group> <employeeId>",
        "  upgrade <ASTRONAUT_CENTER|MISSION_CONTROL|SET_CENTER|HEADQUARTERS|VEHICLE_ASSEMBLY>",
        "  convert <funds>",
        "  research <specialistId> <componentId> | release <specialistId>",
        "  train <group> <employeeId> <BASIC|ADVANCED|PERMANENT> <SKILL> [targetLevel]",
        "  expert <ASTRONAUT|MISSION_CONTROL> <employeeId> <SKILL> | pullout <group> <employeeId>",
        "  launch <configId> rockets=<id,...> [crew=<id|-,...>] [control=<id|-,...>]  (one entry per slot, - = empty)",
        "  reassign <missionId> <controlSlot> <personId>",
        "  pr <astronautId> | recall <astronautId>   (PR duty)",
        "  cancel <missionInstanceId>",
        "  funds <amount>   (dev mode only)");

    private DevCommands() {
    }

    /**
     * Parses a command line into an action.
     *
     * @param line the command line
     * @return the action
     * @throws IllegalArgumentException if the line isn't a valid action command
     */
    public static PlayerAction parse(String line) {
        String[] words = line.strip().split("\\s+");
        String command = words[0].toLowerCase(Locale.ROOT);
        return switch (command) {
            case "open" -> new OpenProgramAction(arg(words, 1));
            case "close" -> new CloseProgramAction(arg(words, 1));
            case "rocket" -> switch (arg(words, 1).toLowerCase(Locale.ROOT)) {
                case "open" -> new OpenRocketProgramAction(arg(words, 2));
                case "close" -> new CloseRocketProgramAction(arg(words, 2));
                default -> throw new IllegalArgumentException("rocket open|close <rocketId>");
            };
            case "hire" -> new HireAction(group(arg(words, 1)), arg(words, 2),
                words.length > 3 && words[3].equalsIgnoreCase("bonus"));
            case "dismiss" -> new DismissAction(group(arg(words, 1)), arg(words, 2));
            case "upgrade" -> new UpgradeBuildingAction(Building.valueOf(arg(words, 1).toUpperCase(Locale.ROOT)));
            case "convert" -> new ConvertPrestigeAction(Long.parseLong(arg(words, 1)));
            case "research" -> new AssignResearchAction(arg(words, 1), arg(words, 2));
            case "release" -> new ReleaseResearchAction(arg(words, 1));
            case "train" -> new StartTrainingAction(group(arg(words, 1)), arg(words, 2),
                TrainingKind.valueOf(arg(words, 3).toUpperCase(Locale.ROOT)), arg(words, 4).toUpperCase(Locale.ROOT),
                words.length > 5 ? Double.parseDouble(words[5]) : 0);
            case "expert" -> new StartExpertTrainingAction(group(arg(words, 1)), arg(words, 2),
                arg(words, 3).toUpperCase(Locale.ROOT));
            case "pullout" -> new PullOutOfTrainingAction(group(arg(words, 1)), arg(words, 2));
            case "launch" -> launch(words);
            case "cancel" -> new CancelMissionAction(arg(words, 1));
            case "pr" -> new StartPrDutyAction(arg(words, 1));
            case "recall" -> new RecallFromPrDutyAction(arg(words, 1));
            case "reassign" -> new ReassignControlSlotAction(arg(words, 1), Integer.parseInt(arg(words, 2)),
                arg(words, 3));
            case "funds" -> new SetFundsAction(Long.parseLong(arg(words, 1)));
            default -> throw new IllegalArgumentException("Unknown command: " + words[0]);
        };
    }

    private static LaunchMissionAction launch(String[] words) {
        List<String> crew = new ArrayList<>();
        List<String> control = new ArrayList<>();
        List<String> rockets = new ArrayList<>();
        for (int i = 2; i < words.length; i++) {
            String[] pair = words[i].split("=", 2);
            if (pair.length != 2) {
                throw new IllegalArgumentException("Expected key=value, got " + words[i]);
            }
            List<String> ids = new ArrayList<>();
            if (!pair[1].isEmpty()) {
                for (String id : pair[1].split(",")) {
                    ids.add(id.equals("-") ? null : id);
                }
            }
            switch (pair[0].toLowerCase(Locale.ROOT)) {
                case "crew" -> crew.addAll(ids);
                case "control" -> control.addAll(ids);
                case "rockets" -> rockets.addAll(ids);
                default -> throw new IllegalArgumentException("Unknown launch option: " + pair[0]);
            }
        }
        return new LaunchMissionAction(arg(words, 1), crew, control, rockets);
    }

    private static PersonnelGroup group(String word) {
        return PersonnelGroup.valueOf(word.toUpperCase(Locale.ROOT));
    }

    private static String arg(String[] words, int index) {
        if (index >= words.length) {
            throw new IllegalArgumentException("Missing argument " + index + " for " + words[0]);
        }
        return words[index];
    }
}
