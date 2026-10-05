package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TrainingProgress;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A personnel department as the shared screens see it (Hire, the training, pull-out, dismiss and
 * Manage department dialogs): its group, building, skills with their icons and names, roster and
 * candidate pool. The SET Centre's engineers, the Astronaut Complex's astronauts and Mission Control's
 * flight controllers.
 *
 * @author Mario Koehler
 */
public enum Department {
    /** The SET Centre's engineers. */
    SET(PersonnelGroup.SET, Building.SET_CENTER, "SET Centre", "specialists", "set_employees",
        TrainingProgress.SET_BASIC_TRAINING_SEASONS,
        Arrays.stream(SetSkill.values()).map(Enum::name).toList()),
    /** The Astronaut Complex's astronauts. */
    ASTRONAUT(PersonnelGroup.ASTRONAUT, Building.ASTRONAUT_CENTER, "Astronaut Complex", "astronauts", "astronauts",
        TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS,
        Arrays.stream(AstronautSkill.values()).map(Enum::name).toList()),
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
        return switch (this) {
            case SET -> SetSkills.icon(SetSkill.valueOf(skill));
            case ASTRONAUT, MISSION_CONTROL -> CrewSkills.icon(skill);
        };
    }

    /**
     * Returns a skill's name.
     *
     * @param skill the skill's name
     * @return the display name
     */
    public String label(String skill) {
        return switch (this) {
            case SET -> SetSkills.label(SetSkill.valueOf(skill));
            case ASTRONAUT, MISSION_CONTROL -> CrewSkills.label(skill);
        };
    }

    /**
     * Returns a skill's short name, for a tight tile.
     *
     * @param skill the skill's name
     * @return the short name
     */
    public String shortLabel(String skill) {
        return switch (this) {
            case SET -> SetSkills.shortLabel(SetSkill.valueOf(skill));
            case ASTRONAUT -> CrewSkills.label(skill);
            case MISSION_CONTROL -> MC_SHORT.getOrDefault(skill, skill);
        };
    }

    /**
     * Returns a person's value in a skill.
     *
     * @param person the person, of this department
     * @param skill  the skill's name
     * @return the value, 0-100
     */
    public double skill(Personnel person, String skill) {
        return switch (this) {
            case SET -> SetDepartment.skill((SetSpecialist) person, SetSkill.valueOf(skill));
            case ASTRONAUT -> ((Astronaut) person).getSkill(AstronautSkill.valueOf(skill));
            case MISSION_CONTROL -> ((MissionControlOfficer) person).getSkill(MissionControlSkill.valueOf(skill));
        };
    }

    /**
     * Returns whether this department's people can hold the Expert trait (astronauts and
     * controllers; SET never does), so the training dialog offers Expert training.
     *
     * @return whether Experts exist here
     */
    public boolean hasExperts() {
        return this != SET;
    }

    /**
     * Returns whether a person holds the Expert trait in a skill.
     *
     * @param person the person, of this department
     * @param skill  the skill's name
     * @return whether they're an Expert in it
     */
    public boolean isExpert(Personnel person, String skill) {
        return switch (this) {
            case SET -> false;
            case ASTRONAUT -> ((Astronaut) person).getExpertSkills().contains(AstronautSkill.valueOf(skill));
            case MISSION_CONTROL -> ((MissionControlOfficer) person).getExpertSkills()
                .contains(MissionControlSkill.valueOf(skill));
        };
    }

    /**
     * Returns whether a person holds the Expert trait in any skill.
     *
     * @param person the person, of this department
     * @return whether they're an Expert at all
     */
    public boolean isExpert(Personnel person) {
        return skills.stream().anyMatch(skill -> isExpert(person, skill));
    }

    /**
     * Returns the skills a person could start Expert training in: at or above the threshold
     * ({@link SkillGrowth#EXPERT_TRAINING_SKILL_THRESHOLD}) and not Expert yet.
     *
     * @param person the person, of this department
     * @return the skill names, in the enum's order; empty for SET
     */
    public List<String> expertEligible(Personnel person) {
        if (!hasExperts()) {
            return List.of();
        }
        return skills.stream().filter(skill -> skill(person, skill) >= SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD
            && !isExpert(person, skill)).toList();
    }

    /**
     * Returns the age this department's people retire at.
     *
     * @return the age
     */
    public int retirementAge() {
        return PersonnelLifecycle.retirementAgeFor(group);
    }

    /**
     * Returns a candidate's age line with the years left until they retire, such as
     * "AGE 58 · RETIRES IN 7 YRS" (2026-10-05): age is a trade-off between experience and a long
     * career, so the hire screen shows both sides.
     *
     * @param person the candidate
     * @return the caption text
     */
    public String ageLine(Personnel person) {
        int years = retirementAge() - person.getAge();
        String retires = years <= 1 ? "RETIRES THIS YEAR" : "RETIRES IN " + years + " YRS";
        return "AGE " + person.getAge() + " · " + retires;
    }

    /**
     * Returns the roster.
     *
     * @param state the player's state
     * @return the roster
     */
    public List<? extends Personnel> roster(PlayerSnapshot state) {
        return switch (this) {
            case SET -> state.getSetSpecialists();
            case ASTRONAUT -> state.getAstronauts();
            case MISSION_CONTROL -> state.getMissionControlOfficers();
        };
    }

    /**
     * Returns this season's candidate pool.
     *
     * @param state the player's state
     * @return the candidates
     */
    public List<? extends Personnel> candidates(PlayerSnapshot state) {
        return switch (this) {
            case SET -> state.getSetSpecialistCandidates();
            case ASTRONAUT -> state.getAstronautCandidates();
            case MISSION_CONTROL -> state.getMissionControlCandidates();
        };
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
