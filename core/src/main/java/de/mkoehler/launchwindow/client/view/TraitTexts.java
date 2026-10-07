package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Trait;

import java.util.Map;

/**
 * The words for traits and hints ({@code design.md} &sect;2.2(d), {@code ui-spec.md} component 20):
 * a trait's chip label, how it reads in a sentence ("turns out to be a workaholic"), and what it
 * does, in plain words. No pronouns: names don't tell how someone wants to be referred to.
 *
 * @author Mario Koehler
 */
public final class TraitTexts {

    private static final Map<String, String> HINTS = Map.ofEntries(
        Map.entry("driven", "driven"), Map.entry("calm", "calm"), Map.entry("sociable", "sociable"),
        Map.entry("exacting", "exacting"), Map.entry("independent", "independent"),
        Map.entry("keen-to-learn", "keen to learn"), Map.entry("patient", "patient"),
        Map.entry("reliable", "reliable"), Map.entry("pleasant", "pleasant to work with"),
        Map.entry("hard-working", "hard-working"));

    private TraitTexts() {
    }

    /**
     * Returns a trait's chip label, such as "Workaholic".
     *
     * @param trait the trait
     * @return the label
     */
    public static String label(Trait trait) {
        return switch (trait) {
            case WORKAHOLIC -> "Workaholic";
            case STEADY -> "Steady";
            case AMBITIOUS -> "Ambitious";
            case GLORY_SEEKER -> "Glory seeker";
            case PERFECTIONIST -> "Perfectionist";
            case COOL_UNDER_PRESSURE -> "Cool under pressure";
            case RESTLESS -> "Restless";
            case QUICK_STUDY -> "Quick study";
            case MENTOR -> "Mentor";
            case LONER -> "Loner";
            case EASY_GOING -> "Easy-going";
            case ABRASIVE -> "Abrasive";
            case MUST_BE_THE_BEST -> "Must be the best";
            case TEAM_PLAYER -> "Team player";
        };
    }

    /**
     * Returns how a trait reads after "turns out to be", such as "a workaholic".
     *
     * @param trait the trait
     * @return the phrase
     */
    public static String inSentence(Trait trait) {
        return switch (trait) {
            case WORKAHOLIC -> "a workaholic";
            case STEADY -> "steady as a rock";
            case AMBITIOUS -> "ambitious";
            case GLORY_SEEKER -> "a glory seeker";
            case PERFECTIONIST -> "a perfectionist";
            case COOL_UNDER_PRESSURE -> "cool under pressure";
            case RESTLESS -> "restless";
            case QUICK_STUDY -> "a quick study";
            case MENTOR -> "a natural mentor";
            case LONER -> "a loner";
            case EASY_GOING -> "easy-going";
            case ABRASIVE -> "abrasive";
            case MUST_BE_THE_BEST -> "someone who must be the best";
            case TEAM_PLAYER -> "a team player";
        };
    }

    /**
     * Returns what a trait does, in plain words, for its tooltip.
     *
     * @param trait the trait
     * @return the text
     */
    public static String effect(Trait trait) {
        return switch (trait) {
            case WORKAHOLIC -> "More R&D progress. Loses morale fast when idle.";
            case STEADY -> "Morale moves only half as much, up or down.";
            case AMBITIOUS -> "Gains more from training. Loses morale after a long time without any.";
            case GLORY_SEEKER -> "Learns more on every flight. Loses morale each time a crewed flight goes up and leaves this astronaut on the ground.";
            case PERFECTIONIST -> "Slower R&D, but raises the component's R&D cap while on its team.";
            case COOL_UNDER_PRESSURE -> "Helps rescue a failing step on every mission this person flies or works a console for.";
            case RESTLESS -> "Quick on a new component, slow after a long stint on the same one.";
            case QUICK_STUDY -> "Gains more from training.";
            case MENTOR -> "Less skilled teammates learn faster.";
            case LONER -> "Works best alone; drags down a team of three or more.";
            case EASY_GOING -> "Gets on with almost anyone, so teams warm up faster.";
            case ABRASIVE -> "Rubs people the wrong way, so teams sour faster.";
            case MUST_BE_THE_BEST -> "Works harder as the team's most skilled, sulks when not.";
            case TEAM_PLAYER -> "Lifts the whole team's chemistry a little.";
        };
    }

    /**
     * Returns a hint's words, such as "keen to learn".
     *
     * @param hint the hint's id
     * @return the words, or {@code null} for none
     */
    public static String hint(String hint) {
        return hint == null ? null : HINTS.getOrDefault(hint, hint);
    }
}
