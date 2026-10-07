package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.MissionLaunch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What a mission configuration needs in people, against what the agency has (play-test request,
 * 2026-10-07): so a player can plan hiring and training seasons ahead, not find out at assembly.
 * "Free" is exactly what assembly counts ({@link MissionLaunch#isAvailable}).
 *
 * @author Mario Koehler
 */
public final class Staffing {

    private Staffing() {
    }

    /**
     * One group's need and supply.
     *
     * @param group        the group's plural name ("astronauts", "flight controllers")
     * @param needed       how many the mission needs (astronauts: crew and CAPCOM together)
     * @param free         how many are free now
     * @param inTraining   how many are in training that can be cut short (not basic)
     * @param inBasic      how many are in basic training
     * @param basicSoonest the seasons until the first of them finishes basic training, or 0 for none
     * @param busy         how many are on missions or R&amp;D, or unavailable
     */
    public record Group(String group, int needed, int free, int inTraining, int inBasic, int basicSoonest, int busy) {

        /**
         * Returns whether the agency is short of free people for this mission.
         *
         * @return whether short
         */
        public boolean isShort() {
            return free < needed;
        }
    }

    /**
     * Returns the astronauts' and flight controllers' need and supply; a group the mission doesn't
     * need is left out.
     *
     * @param configuration the configuration
     * @param state         the agency
     * @return the groups
     */
    public static List<Group> of(MissionConfiguration configuration, PlayerSnapshot state) {
        List<Group> groups = new ArrayList<>();
        int astronauts = Missions.crewCount(configuration) + Missions.capcomCount(configuration);
        if (astronauts > 0) {
            groups.add(group("astronauts", astronauts, state.getAstronauts()));
        }
        int controllers = Missions.controllerCount(configuration);
        if (controllers > 0) {
            groups.add(group("flight controllers", controllers, state.getMissionControlOfficers()));
        }
        return groups;
    }

    private static Group group(String name, int needed, List<? extends Personnel> roster) {
        int free = 0;
        int training = 0;
        int basic = 0;
        int soonest = 0;
        for (Personnel person : roster) {
            if (MissionLaunch.isAvailable(person)) {
                free++;
            } else if (person.getAssignmentState() == AssignmentState.IN_TRAINING
                    && person.getTrainingKind() == TrainingKind.BASIC) {
                basic++;
                int left = Math.max(1, person.getTrainingSeasonsTotal() - person.getTrainingSeasonsCompleted());
                soonest = soonest == 0 ? left : Math.min(soonest, left);
            } else if (person.getAssignmentState() == AssignmentState.IN_TRAINING) {
                training++;
            }
        }
        return new Group(name, needed, free, training, basic, soonest, roster.size() - free - training - basic);
    }

    /**
     * Returns the short line a configuration card shows: free people against the need.
     *
     * @param configuration the configuration
     * @param state         the agency
     * @return the line, such as "Free now: 3 of 2 astronauts · 4 of 6 controllers", or {@code null}
     */
    public static String cardLine(MissionConfiguration configuration, PlayerSnapshot state) {
        List<String> parts = new ArrayList<>();
        for (Group group : of(configuration, state)) {
            String noun = group.group().equals("flight controllers") ? "controllers" : group.group();
            parts.add(group.free() + " of " + group.needed() + " " + noun);
        }
        return parts.isEmpty() ? null : "Free now: " + String.join(" · ", parts);
    }

    /**
     * Returns whether the agency is short for this configuration in any group.
     *
     * @param configuration the configuration
     * @param state         the agency
     * @return whether short
     */
    public static boolean isShort(MissionConfiguration configuration, PlayerSnapshot state) {
        return of(configuration, state).stream().anyMatch(Group::isShort);
    }

    /**
     * Returns what could close a group's gap, as a sentence, or {@code null} when it isn't short.
     *
     * @param group the group
     * @return the sentence
     */
    public static String advice(Group group) {
        if (!group.isShort()) {
            return null;
        }
        int gap = group.needed() - group.free();
        List<String> parts = new ArrayList<>();
        if (group.inBasic() > 0) {
            parts.add(group.inBasic() + " in basic training (the first done in " + group.basicSoonest()
                + (group.basicSoonest() == 1 ? " season)" : " seasons)"));
        }
        if (group.inTraining() > 0) {
            parts.add(group.inTraining() + " in training that can be cut short");
        }
        String coming = parts.isEmpty() ? "" : " Coming: " + String.join(", ", parts) + ".";
        return gap + " short of free " + group.group() + "." + coming
            + (group.inBasic() + group.inTraining() < gap ? " Hiring takes basic training first, so plan ahead." : "");
    }

    /**
     * Returns each seat the mission uses and the skill it reads most: the crew's seats from how the
     * steps weigh them, each control slot from its own skill.
     *
     * @param configuration the configuration
     * @return title and skill, crew first
     */
    public static List<String[]> seats(MissionConfiguration configuration) {
        List<String[]> seats = new ArrayList<>();
        Map<Integer, Map<String, Double>> crew = new LinkedHashMap<>();
        for (MissionSeason season : configuration.seasons()) {
            for (MissionStep step : season.steps()) {
                for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
                    crew.computeIfAbsent(involvement.slot(), slot -> new LinkedHashMap<>())
                        .merge(involvement.skill().name(), involvement.percent(), Double::sum);
                }
            }
        }
        for (int slot : configuration.usedCrewSlots()) {
            String skill = crew.getOrDefault(slot, Map.of()).entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
            seats.add(new String[] {title(configuration.crewSlots().get(slot)),
                skill == null ? "" : CrewSkills.label(skill)});
        }
        for (int slot : configuration.usedControlSlots()) {
            StaffingSlot seat = configuration.controlSlots().get(slot);
            String skill = StaffingSlot.FLIGHT_DIRECTOR.equals(seat.skill()) ? "all five skills, averaged"
                : CrewSkills.label(seat.skill());
            seats.add(new String[] {title(seat.title()) + (seat.kind() == StaffingSlot.Kind.ASTRONAUT
                ? " (an astronaut)" : ""), skill});
        }
        return seats;
    }

    /** Console call signs, written in capitals as they always were. */
    private static final java.util.Set<String> CALL_SIGNS = java.util.Set.of("CAPCOM", "FIDO", "GUIDO", "RETRO");

    /**
     * Returns a seat title in normal case ("COSMONAUT A" reads "Cosmonaut A", "FLIGHT DIRECTOR"
     * reads "Flight director"); call signs and single letters stay in capitals.
     *
     * @param title the pack's title
     * @return the title
     */
    static String title(String title) {
        String[] words = title.trim().split("\s+");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            String shown = CALL_SIGNS.contains(word) || word.length() == 1 ? word
                : i == 0 ? word.charAt(0) + word.substring(1).toLowerCase(java.util.Locale.ROOT)
                : word.toLowerCase(java.util.Locale.ROOT);
            out.append(i == 0 ? "" : " ").append(shown);
        }
        return out.toString();
    }
}
