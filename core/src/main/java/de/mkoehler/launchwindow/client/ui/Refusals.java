package de.mkoehler.launchwindow.client.ui;

import java.util.Map;

/**
 * Words the server's refusal codes for the action result toast ({@code ui-spec.md} &sect;5, which
 * has the suggested wording). A code without wording here is shown as it came, so a new refusal is
 * never silently swallowed.
 *
 * @author Mario Koehler
 */
public final class Refusals {

    private static final Map<String, String> WORDING = Map.ofEntries(
        Map.entry("INSUFFICIENT_FUNDS", "Not enough funds."),
        Map.entry("ROSTER_FULL", "No room. Upgrade the building."),
        Map.entry("UNKNOWN_CANDIDATE", "That candidate has left the pool."),
        Map.entry("ASSIGNED_TO_MISSION", "Can't dismiss someone on a mission."),
        Map.entry("NOT_IDLE", "They're busy."),
        Map.entry("NOT_ON_DUTY", "They aren't on a PR tour."),
        Map.entry("WRONG_STATE", "They're busy."),
        Map.entry("AT_RD_CEILING", "R&D can't improve this further."),
        Map.entry("NO_FREE_SLOT", "All four R&D slots are taken."),
        Map.entry("COMPONENT_NOT_AVAILABLE", "That component isn't owned and active."),
        Map.entry("TARGET_NOT_ABOVE_SKILL", "Pick a target above the current skill."),
        Map.entry("SKILL_BELOW_THRESHOLD", "Expert training needs a skill of 80."),
        Map.entry("ALREADY_EXPERT_IN_SKILL", "Already an Expert."),
        Map.entry("NOT_IN_TRAINING", "They're not in training any more."),
        Map.entry("BASIC_TRAINING", "Basic training can't be cut short."),
        Map.entry("UNKNOWN_EMPLOYEE", "That person isn't on the roster any more."),
        Map.entry("UNKNOWN_PERSONNEL", "That person isn't on the roster any more."),
        Map.entry("UNKNOWN_SPECIALIST", "That person isn't on the roster any more."),
        Map.entry("UNKNOWN_SKILL", "That isn't one of their skills."),
        Map.entry("UNSUPPORTED_KIND", "That kind of training isn't available here."),
        Map.entry("INVALID_PERCENT", "Pick a change between −50 % and +50 %."),
        Map.entry("EMPTY_DEPARTMENT", "There's nobody in the department yet."),
        Map.entry("ALREADY_ADJUSTED", "This department's salaries were already changed this season. Try again "
            + "after the next turn."),
        Map.entry("HEADQUARTERS_FULL", "Headquarters can't run more programs. Upgrade it or close one."),
        Map.entry("WRONG_AGENCY", "That belongs to another agency."),
        Map.entry("GOALS_NOT_MET", "Achieve the required goals first."),
        Map.entry("ALREADY_OPEN", "Already open."),
        Map.entry("NOT_OPEN", "Not open."),
        Map.entry("UNKNOWN_PROGRAM", "That program doesn't exist in this game."),
        Map.entry("MISSIONS_IN_FLIGHT", "Can't close while a mission uses it."),
        Map.entry("UNKNOWN_MISSION_CONFIGURATION", "That mission doesn't exist in this game."),
        Map.entry("PROGRAM_NOT_OPEN", "Open the program first."),
        Map.entry("UNKNOWN_ASTRONAUT", "Someone in a slot isn't on the roster any more."),
        Map.entry("UNKNOWN_MISSION_CONTROL_OFFICER", "Someone in a slot isn't on the roster any more."),
        Map.entry("ASTRONAUT_NOT_IDLE", "Someone in a slot isn't free any more."),
        Map.entry("MISSION_CONTROL_OFFICER_NOT_IDLE", "Someone in a slot isn't free any more."),
        Map.entry("DUPLICATE_ASTRONAUT", "Someone is in two slots."),
        Map.entry("DUPLICATE_MISSION_CONTROL_OFFICER", "Someone is in two slots."),
        Map.entry("WRONG_ASTRONAUT_COUNT", "Fill every slot the mission uses."),
        Map.entry("WRONG_MISSION_CONTROL_OFFICER_COUNT", "Fill every slot the mission uses."),
        Map.entry("WRONG_CAPCOM_COUNT", "Fill every slot the mission uses."),
        Map.entry("CAPCOM_NOT_AVAILABLE", "CAPCOM needs a free astronaut who isn't in the crew."),
        Map.entry("LAUNCH_CAP_REACHED", "No more launches this turn: the launch window is full."),
        Map.entry("MISSION_COMPONENT_UNAVAILABLE", "A component is grounded this turn."),
        Map.entry("WRONG_ROCKET_COUNT", "Pick a rocket for every launch."),
        Map.entry("ROCKET_NOT_AVAILABLE", "Pick a rocket for every launch."),
        Map.entry("ROCKET_CANNOT_LIFT_PAYLOAD", "That rocket can't fly this launch."),
        Map.entry("UNKNOWN_MISSION", "That mission isn't scheduled or in flight any more."),
        Map.entry("CREWED_MISSION_IN_FLIGHT", "A crewed mission in flight can't be abandoned."),
        Map.entry("UNKNOWN_SLOT", "That console doesn't exist on this mission."),
        Map.entry("SLOT_NOT_USED", "That console isn't needed any more."),
        Map.entry("WRONG_EMPLOYEE_TYPE", "That console takes someone of another type."),
        Map.entry("PERSON_NOT_AVAILABLE", "They're not free any more."),
        Map.entry("VAB_FULL", "The VAB can't hold more rocket programs. Upgrade it or close one."),
        Map.entry("ROCKET_CLASS_TOO_LARGE", "Upgrade the VAB to build this rocket class."),
        Map.entry("UNKNOWN_ROCKET", "That rocket doesn't exist in this game."),
        Map.entry("MAX_LEVEL", "Already at the top level."),
        Map.entry("ALREADY_UPGRADING", "Already upgrading."));

    /**
     * Not instantiable; this class only holds the wording.
     */
    private Refusals() {
    }

    /**
     * Words a refusal code.
     *
     * @param code the server's code, such as {@code ROSTER_FULL}
     * @return the wording, or the code itself if it has none
     */
    public static String text(String code) {
        if (code == null || code.isEmpty()) {
            return "The server refused that.";
        }
        return WORDING.getOrDefault(code, code);
    }
}
