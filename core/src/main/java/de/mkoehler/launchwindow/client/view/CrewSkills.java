package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.StaffingSlot;

import java.util.Map;

/**
 * Icons and names for the skills astronauts and flight controllers bring to a mission: the five
 * astronaut skills, the five Mission Control skills, and a Flight Director slot's average of all
 * five. Skills are named as {@link StaffingSlot#skill()} names them. Astronaut EVA shares the SET
 * EVA Suits icon.
 *
 * @author Mario Koehler
 */
public final class CrewSkills {

    private static final Map<String, String[]> SKILLS = Map.ofEntries(
        Map.entry("LEADERSHIP", new String[] {"skill_leadership", "Leadership"}),
        Map.entry("PILOTING", new String[] {"skill_piloting", "Piloting"}),
        Map.entry("EVA", new String[] {"skill_eva_suits", "EVA"}),
        Map.entry("SCIENCE", new String[] {"skill_science", "Science"}),
        Map.entry("FITNESS", new String[] {"skill_fitness", "Fitness"}),
        Map.entry("PROPULSION", new String[] {"skill_propulsion", "Propulsion"}),
        Map.entry("TRAJECTORY_AND_GNC", new String[] {"skill_trajectory_and_guidance", "Trajectory & guidance"}),
        Map.entry("SPACECRAFT_SYSTEMS", new String[] {"skill_spacecraft_systems", "Spacecraft systems"}),
        Map.entry("CREW_SYSTEMS_AND_PAYLOADS", new String[] {"skill_crew_and_payload", "Crew & payload"}),
        Map.entry("MISSION_OPERATIONS", new String[] {"skill_mission_operations", "Mission operations"}),
        Map.entry(StaffingSlot.FLIGHT_DIRECTOR, new String[] {"skill_flight_director", "Flight Director"}));

    private CrewSkills() {
    }

    /**
     * Returns a skill's icon name, for {@code UiKit.icon}.
     *
     * @param skill the skill's name, such as {@code PILOTING}
     * @return the icon name
     */
    public static String icon(String skill) {
        String[] entry = SKILLS.get(skill);
        return entry == null ? "prestige" : entry[0];
    }

    /**
     * Returns a skill's display name.
     *
     * @param skill the skill's name
     * @return the name, such as "Trajectory &amp; guidance"
     */
    public static String label(String skill) {
        String[] entry = SKILLS.get(skill);
        return entry == null ? ContentNames.of(skill.toLowerCase(java.util.Locale.ROOT).replace('_', '-')) : entry[1];
    }
}
