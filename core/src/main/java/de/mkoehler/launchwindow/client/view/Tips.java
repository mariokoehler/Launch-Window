package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.StaffingSlot;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;

import java.util.Map;

/**
 * The hover tooltips' text ({@code ui-spec.md} component 17): what an icon, value or chip means and
 * what it does in the rules. Kept in one place so the same thing is always explained the same way,
 * and so the numbers come from the rules' own constants.
 *
 * @author Mario Koehler
 */
public final class Tips {

    /** One tooltip: a short title and an explanation. */
    public record Tip(String title, String text) {
    }

    private static final Map<String, String> SKILL_USES = Map.ofEntries(
        // SET: the components each skill researches.
        Map.entry("ROCKETS", "Researches uncrewed rockets, such as the R-7 for Sputnik and Jupiter-C."),
        Map.entry("SPACE_PROBES", "Researches satellites and probes."),
        Map.entry("HUMAN_RATED_ROCKETS", "Researches rockets safe enough to carry a crew, such as Redstone, Atlas "
            + "and the Vostok R-7."),
        Map.entry("CREWED_SPACECRAFT", "Researches capsules and other crewed spacecraft."),
        Map.entry("EVA_SUITS", "Researches spacesuits for work outside the spacecraft."),
        // Astronauts: read by the crew's share of a step; CAPCOM reads Leadership from the ground.
        Map.entry("LEADERSHIP", "Counts in most crewed steps, and is what a CAPCOM brings from the ground."),
        Map.entry("PILOTING", "Counts in most crewed steps, from countdown to landing."),
        Map.entry("FITNESS", "Counts in most crewed steps: launch loads, long flights and the ride home."),
        Map.entry("SCIENCE", "Counts in steps with experiments and observations in orbit."),
        Map.entry("EVA", "Counts in steps outside the spacecraft. No early mission has one yet."),
        // Flight controllers: the console seats that read each skill.
        Map.entry("PROPULSION", "Read at the BOOSTER console, which watches the rocket."),
        Map.entry("TRAJECTORY_AND_GNC", "Read at the trajectory consoles: FIDO, GUIDO, RETRO and navigation, and at "
            + "the first satellite flights' FLIGHT DIRECTOR seat."),
        Map.entry("SPACECRAFT_SYSTEMS", "Read at the spacecraft consoles: systems, environmental and network."),
        Map.entry("CREW_SYSTEMS_AND_PAYLOADS", "Read at the SURGEON and EXPERIMENTS consoles."),
        Map.entry("MISSION_OPERATIONS", "Read at the PROCEDURES console."),
        Map.entry(StaffingSlot.FLIGHT_DIRECTOR, "The average of a controller's five skills: what the FLIGHT "
            + "seat reads on crewed and later flights. The best all-rounders make the best Flight Directors. (The "
            + "first satellite flights' FLIGHT DIRECTOR seat reads Trajectory & guidance instead.)"));

    private Tips() {
    }

    /**
     * Returns a skill's tooltip.
     *
     * @param label the skill's display name
     * @param skill the skill's name, such as {@code PILOTING}
     * @return the tip
     */
    public static Tip skill(String label, String skill) {
        return new Tip(label, SKILL_USES.get(skill));
    }

    /**
     * Returns the tooltip for a stat chip's icon: salary, morale or learning potential.
     *
     * @param icon the chip's icon name
     * @return the tip, or {@code null} for another icon
     */
    public static Tip stat(String icon) {
        return switch (icon) {
            case "funds" -> salary();
            case "morale" -> morale();
            case "learning_potential" -> learningPotential();
            default -> null;
        };
    }

    /**
     * Returns the salary chip's tooltip.
     *
     * @return the tip
     */
    public static Tip salary() {
        return new Tip("Salary", "What they are paid each season. Manage department changes a whole "
            + "department's salaries at once.");
    }

    /**
     * Returns the morale chip's tooltip.
     *
     * @return the tip
     */
    public static Tip morale() {
        return new Tip("Morale", String.format(java.util.Locale.ROOT, "0 to 100. It rises %d a season while they "
            + "are busy (training, R&D or a mission) and falls %d while idle. At %d or less they quit, unless "
            + "they're on a mission. A department's average morale also decides how good its next candidates are.",
            PersonnelLifecycle.BUSY_MORALE_GAIN_PER_SEASON, PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON,
            PersonnelLifecycle.RESIGNATION_MORALE_FLOOR));
    }

    /**
     * Returns the learning potential chip's tooltip.
     *
     * @return the tip
     */
    public static Tip learningPotential() {
        return new Tip("Learning potential", "How much they gain from training and missions, 0 to 100. Young "
            + "candidates usually learn fast; experienced ones start better but improve slowly.");
    }

    /**
     * Returns a footer staff chip's tooltip.
     *
     * @param people   the group, plural, such as "Flight controllers"
     * @param building where a click goes
     * @return the tip
     */
    public static Tip staffCounts(String people, String building) {
        return new Tip(people + ": on staff / busy / places", "Busy means in training, on a mission, on R&D or "
            + "unavailable. Places is what the building holds at its level. Click to open " + building + ".");
    }

    /**
     * Returns the hire screen's signing-bonus tooltip.
     *
     * @return the tip
     */
    public static Tip signingBonus() {
        return new Tip("Signing bonus", String.format(java.util.Locale.ROOT, "A one-off payment of one season's "
            + "salary when hiring. The new recruit's morale rises to %d %% of what it was (at most 100), so they "
            + "start keener and stay longer.", Math.round(HiringPool.SIGNING_BONUS_MORALE_FACTOR * 100)));
    }

    /**
     * Returns the footer funds chip's tooltip.
     *
     * @return the tip
     */
    public static Tip funds() {
        return new Tip("Funds", "Money in the bank. Click to open the Public Affairs Office for the budget, the "
            + "next review and prestige conversion.");
    }

    /**
     * Returns the footer programs chip's tooltip.
     *
     * @return the tip
     */
    public static Tip programs() {
        return new Tip("Programs: open / places", "How many programs are open, and how many Headquarters can "
            + "run at its level. Click to open Headquarters.");
    }

    /**
     * Returns the header prestige chip's tooltip.
     *
     * @return the tip
     */
    public static Tip prestige() {
        return new Tip("Prestige", "Earned by successful missions and firsts, lost by failures. Each budget "
            + "review turns it into the next years' funding.");
    }

    /**
     * Returns the header date chip's tooltip.
     *
     * @return the tip
     */
    public static Tip gameDate() {
        return new Tip("Game date", "The same for every agency in the game. Each turn is one season.");
    }

    /**
     * Returns the header countdown chip's tooltip.
     *
     * @return the tip
     */
    public static Tip countdown() {
        return new Tip("Next turn", "When the server resolves the turn for every agency: missions fly, training "
            + "and R&D move on, and a new season begins.");
    }
}
