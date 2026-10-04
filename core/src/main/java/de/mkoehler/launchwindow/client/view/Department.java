package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A personnel department as the shared screens see it (Hire, the training, pull-out, dismiss and
 * Manage department dialogs): its group, building, skills with their icons and names, roster and
 * candidate pool. The SET Centre's engineers and Mission Control's flight controllers.
 *
 * @author Mario Koehler
 */
public enum Department {
    /** The SET Centre's engineers. */
    SET(PersonnelGroup.SET, Building.SET_CENTER, "SET Centre", "specialists", "set_employees",
        TrainingProgress.SET_BASIC_TRAINING_SEASONS,
        Arrays.stream(SetSkill.values()).map(Enum::name).toList()),
    /** Mission Control's flight controllers. */
    MISSION_CONTROL(PersonnelGroup.MISSION_CONTROL, Building.MISSION_CONTROL, "Mission Control", "flight controllers",
        "flight_controllers", TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS,
        Arrays.stream(MissionControlSkill.values()).map(Enum::name).toList());

    private static final Map<String, String> MC_SHORT = Map.of(
        "PROPULSION", "Propulsion",
        "TRAJECTORY_AND_GNC", "Trajectory",
        "SPACECRAFT_SYSTEMS", "Systems",
        "CREW_SYSTEMS_AND_PAYLOADS", "Crew",
        "MISSION_OPERATIONS", "Operations");

    private final PersonnelGroup group;
    private final Building building;
    private final String place;
    private final String people;
    private final String icon;
    private final int basicSeasons;
    private final List<String> skills;

    Department(PersonnelGroup group, Building building, String place, String people, String icon, int basicSeasons,
               List<String> skills) {
        this.group = group;
        this.building = building;
        this.place = place;
        this.people = people;
        this.icon = icon;
        this.basicSeasons = basicSeasons;
        this.skills = skills;
    }

    /**
     * Returns the personnel group the actions name.
     *
     * @return the group
     */
    public PersonnelGroup group() {
        return group;
    }

    /**
     * Returns the building that caps the roster.
     *
     * @return the building
     */
    public Building building() {
        return building;
    }

    /**
     * Returns the department's place name ("SET Centre").
     *
     * @return the name
     */
    public String place() {
        return place;
    }

    /**
     * Returns what its people are called, in the plural ("flight controllers").
     *
     * @return the words
     */
    public String people() {
        return people;
    }

    /**
     * Returns the department's icon, as on the footer chip.
     *
     * @return the icon name
     */
    public String icon() {
        return icon;
    }

    /**
     * Returns how long basic training takes.
     *
     * @return the seasons
     */
    public int basicSeasons() {
        return basicSeasons;
    }

    /**
     * Returns the skills, by name, in the enum's order.
     *
     * @return the skill names
     */
    public List<String> skills() {
        return skills;
    }

    /**
     * Returns a skill's icon.
     *
     * @param skill the skill's name
     * @return the icon name
     */
    public String icon(String skill) {
        return this == SET ? SetSkills.icon(SetSkill.valueOf(skill)) : CrewSkills.icon(skill);
    }

    /**
     * Returns a skill's name.
     *
     * @param skill the skill's name
     * @return the display name
     */
    public String label(String skill) {
        return this == SET ? SetSkills.label(SetSkill.valueOf(skill)) : CrewSkills.label(skill);
    }

    /**
     * Returns a skill's short name, for a tight tile.
     *
     * @param skill the skill's name
     * @return the short name
     */
    public String shortLabel(String skill) {
        return this == SET ? SetSkills.shortLabel(SetSkill.valueOf(skill)) : MC_SHORT.getOrDefault(skill, skill);
    }

    /**
     * Returns a person's value in a skill.
     *
     * @param person the person, of this department
     * @param skill  the skill's name
     * @return the value, 0-100
     */
    public double skill(Personnel person, String skill) {
        if (this == SET) {
            return SetDepartment.skill((SetSpecialist) person, SetSkill.valueOf(skill));
        }
        return ((MissionControlOfficer) person).getSkill(MissionControlSkill.valueOf(skill));
    }

    /**
     * Returns the roster.
     *
     * @param state the player's state
     * @return the roster
     */
    public List<? extends Personnel> roster(PlayerSnapshot state) {
        return this == SET ? state.getSetSpecialists() : state.getMissionControlOfficers();
    }

    /**
     * Returns this season's candidate pool.
     *
     * @param state the player's state
     * @return the candidates
     */
    public List<? extends Personnel> candidates(PlayerSnapshot state) {
        return this == SET ? state.getSetSpecialistCandidates() : state.getMissionControlCandidates();
    }

    /**
     * Returns the roster cap at the building's level.
     *
     * @param state   the player's state
     * @param catalog the content pack
     * @return the cap
     */
    public int cap(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.buildings().capacity(building, state.buildingLevel(building));
    }
}
