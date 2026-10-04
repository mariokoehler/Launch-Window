package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.SetSkill;

import java.util.Locale;

/**
 * How the five SET skills are named and drawn: their labels (long, and short for the training
 * dialog's skill picker) and their icon asset names under {@code assets/ui/icons/}.
 *
 * @author Mario Koehler
 */
public final class SetSkills {

    /**
     * Not instantiable; this class only holds the lookups.
     */
    private SetSkills() {
    }

    /**
     * Returns a skill's full label.
     *
     * @param skill the skill
     * @return for example "Human-rated rockets"
     */
    public static String label(SetSkill skill) {
        return switch (skill) {
            case ROCKETS -> "Rockets";
            case SPACE_PROBES -> "Space probes";
            case HUMAN_RATED_ROCKETS -> "Human-rated rockets";
            case CREWED_SPACECRAFT -> "Crewed spacecraft";
            case EVA_SUITS -> "EVA suits";
        };
    }

    /**
     * Returns a skill's short label, for tight spaces.
     *
     * @param skill the skill
     * @return for example "HR rockets"
     */
    public static String shortLabel(SetSkill skill) {
        return switch (skill) {
            case ROCKETS -> "Rockets";
            case SPACE_PROBES -> "Probes";
            case HUMAN_RATED_ROCKETS -> "HR rockets";
            case CREWED_SPACECRAFT -> "Crewed SC";
            case EVA_SUITS -> "EVA";
        };
    }

    /**
     * Returns a skill's icon asset name.
     *
     * @param skill the skill
     * @return for example {@code skill_eva_suits}
     */
    public static String icon(SetSkill skill) {
        return "skill_" + skill.name().toLowerCase(Locale.ROOT);
    }
}
