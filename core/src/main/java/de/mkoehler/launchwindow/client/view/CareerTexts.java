package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.rules.AmbitionSettled;
import de.mkoehler.launchwindow.rules.Careers;

import java.util.Locale;

/**
 * The words for people's careers ({@code design.md} &sect;2.2(d), phase 4): ambitions, their outcomes,
 * loyalty, and the career record's lines. No pronouns: names don't say how someone wants to be
 * referred to.
 *
 * @author Mario Koehler
 */
public final class CareerTexts {

    private CareerTexts() {
    }

    /**
     * Returns what an ambition is, as a short phrase starting with a verb.
     *
     * @param kind the ambition
     * @param ref  its component, or {@code ""}
     * @return the phrase, such as "Bring the Mercury capsule to its R&D cap"
     */
    public static String ambition(Ambition.Kind kind, String ref) {
        return switch (kind) {
            case FLY_IN_SPACE -> "Fly in space";
            case FLY_TO_ORBIT -> "Fly to orbit";
            case DAYS_IN_ORBIT -> "Spend days in orbit";
            case BE_FIRST -> "Be part of a first";
            case JOINT_FLIGHT -> "Fly on a joint flight";
            case CONSOLES_ORBITAL -> "Be on the consoles for a crewed orbital flight";
            case CONSOLES_FIRST -> "Be on the consoles for a first";
            case BECOME_EXPERT -> "Become an Expert";
            case COMPONENT_CAP -> "Bring the " + ContentNames.of(ref) + " to its R&D cap";
            case FIRST_HARDWARE -> "Build hardware that flies a first";
            case TOP_ENGINEER -> "Be the agency's best engineer";
        };
    }

    /**
     * Returns what an ambition takes, for the person page.
     *
     * @param kind the ambition
     * @return the sentence
     */
    public static String ambitionHow(Ambition.Kind kind) {
        return switch (kind) {
            case FLY_IN_SPACE -> "Comes true on a crewed flight with this astronaut in the crew.";
            case FLY_TO_ORBIT -> "Comes true on a crewed orbital flight with this astronaut in the crew.";
            case DAYS_IN_ORBIT -> "Comes true in the second season of a crewed orbital flight lasting two or more.";
            case BE_FIRST -> "Comes true aboard, or as CAPCOM, on a mission that reaches a goal first in the game.";
            case JOINT_FLIGHT -> "Comes true in the crew of a flight of two launches or more.";
            case CONSOLES_ORBITAL -> "Comes true on a control slot of a crewed orbital flight.";
            case CONSOLES_FIRST -> "Comes true on a control slot of a mission that reaches a goal first in the game.";
            case BECOME_EXPERT -> "Comes true with the Expert trait in the best skill: Expert training.";
            case COMPONENT_CAP -> "Comes true on the R&D team when the component reaches its cap.";
            case FIRST_HARDWARE -> "Comes true when a component once worked on flies on a mission that reaches a "
                + "goal first in the game.";
            case TOP_ENGINEER -> "Comes true when no engineer in the agency is better in this engineer's best skill.";
        };
    }

    /**
     * Returns how long the person will wait, or what state they're in.
     *
     * @param person the person
     * @return the badge text, or {@code null} when there's nothing to show
     */
    public static String status(Personnel person) {
        if (person.getAmbition() != null) {
            return seasonsLeft(person.getAmbition().seasonsLeft());
        }
        if (person.getDisappointedSeasons() > 0) {
            return "Disappointed · " + seasons(person.getDisappointedSeasons());
        }
        return null;
    }

    /**
     * Returns a seasons-left count.
     *
     * @param seasons the seasons
     * @return the text, such as "5 seasons left"
     */
    public static String seasonsLeft(int seasons) {
        return seasons(seasons) + " left";
    }

    private static String seasons(int seasons) {
        return seasons + (seasons == 1 ? " season" : " seasons");
    }

    /**
     * Returns a loyalty band, for the person page's caption and intelligence files.
     *
     * @param loyalty the loyalty, 0 to 100
     * @return "Low", "Middling" or "High"
     */
    public static String loyaltyBand(int loyalty) {
        return Careers.loyaltyBand(loyalty);
    }

    /**
     * Returns an ambition outcome as a report line's title and text.
     *
     * @param settled the event
     * @return title and text
     */
    public static String[] report(AmbitionSettled settled) {
        String what = ambition(settled.kind(), settled.ref());
        String lower = Character.toLowerCase(what.charAt(0)) + what.substring(1);
        String name = settled.name();
        return switch (settled.outcome()) {
            case SAID -> new String[] {name + " has an ambition: " + lower, seasons(settled.seasonsLeft())
                + " of patience."};
            case FULFILLED -> new String[] {name + "'s ambition came true: " + lower, change(settled)};
            case PASSED_OVER -> new String[] {name + " was passed over: someone else got the chance to " + lower,
                change(settled) + " " + seasonsLeft(settled.seasonsLeft()) + "."};
            case IMPOSSIBLE -> new String[] {name + " can no longer " + lower, change(settled)};
            case RAN_OUT -> new String[] {name + " gave up hoping to " + lower, change(settled) + " Disappointed for "
                + seasons(Careers.DISAPPOINTED_SEASONS) + "."};
        };
    }

    private static String change(AmbitionSettled settled) {
        return String.format(Locale.ROOT, "Morale %+d, loyalty %+d.", settled.moraleChange(), settled.loyaltyChange());
    }

    /**
     * Returns a report line's tone for an ambition outcome.
     *
     * @param settled the event
     * @return 1, 0 or -1
     */
    public static int tone(AmbitionSettled settled) {
        return switch (settled.outcome()) {
            case FULFILLED -> 1;
            case SAID -> 0;
            default -> -1;
        };
    }

    /**
     * Returns a career line's date.
     *
     * @param entry the line
     * @return the date, such as "Spring 1961"
     */
    public static String date(CareerEntry entry) {
        return entry.turn() >= 1 ? GameDate.ofTurn(entry.turn()).label() : "";
    }

    /**
     * Returns a career line's text.
     *
     * @param entry   the line
     * @param catalog the content pack, for mission names
     * @return the text
     */
    public static String line(CareerEntry entry, ContentCatalog catalog) {
        return switch (entry.kind()) {
            case HIRED -> {
                String hint = People.hintQuote(entry.note());
                yield hint == null ? "Hired." : "Hired. A former colleague: " + hint + ".";
            }
            case TRAINING -> String.format(Locale.ROOT, "%s training done: %s +%.1f",
                capitalise(entry.note().toLowerCase(Locale.ROOT)), TurnReportView.skillLabel(entry.ref()), entry.value());
            case RD_STINT -> String.format(Locale.ROOT, "%s R&D, %s: reliability +%.1f", ContentNames.of(entry.ref()),
                seasons(entry.count()), entry.value());
            case FLIGHT -> mission(entry.ref(), catalog) + ", " + role(entry.note()) + ": "
                + (entry.outcome().isEmpty() ? "success" : "failed at " + TurnReportView.stepInSentence(entry.ref(),
                    entry.outcome()));
            case FIRST -> "Part of a first in the game: " + ContentNames.of(entry.ref()) + ", on "
                + mission(entry.note(), catalog);
            case TRAIT -> "Turned out to be " + traitPhrase(entry.ref());
            case RELATIONSHIP -> (entry.value() > 0 ? "Works well with " : "Clashes with ") + entry.ref() + ", after "
                + seasons(entry.count()) + " together";
            case EXPERT -> "Expert trait earned: " + TurnReportView.skillLabel(entry.ref());
            case AMBITION -> ambitionLine(entry);
        };
    }

    private static String ambitionLine(CareerEntry entry) {
        Ambition.Kind kind = Ambition.Kind.valueOf(entry.ref());
        String what = ambition(kind, entry.note());
        String lower = Character.toLowerCase(what.charAt(0)) + what.substring(1);
        return switch (AmbitionSettled.Outcome.valueOf(entry.outcome())) {
            case SAID -> "Said an ambition: " + lower;
            case FULFILLED -> "Ambition came true: " + lower;
            case PASSED_OVER -> "Passed over: " + lower;
            case IMPOSSIBLE -> "Ambition no longer possible: " + lower;
            case RAN_OUT -> "Gave up an ambition: " + lower;
        };
    }

    /**
     * Returns a career line's dot colour hint: whether it's a highlight (traits, relationships,
     * flights, firsts, ambitions met).
     *
     * @param entry the line
     * @return whether to mark it gold
     */
    public static boolean highlight(CareerEntry entry) {
        return switch (entry.kind()) {
            case TRAIT, RELATIONSHIP, FLIGHT, FIRST, EXPERT -> true;
            case AMBITION -> AmbitionSettled.Outcome.FULFILLED.name().equals(entry.outcome());
            default -> false;
        };
    }

    private static String mission(String configurationId, ContentCatalog catalog) {
        return catalog.missionConfiguration(configurationId).map(MissionControl::missionName)
            .orElse(ContentNames.of(configurationId));
    }

    private static String role(String role) {
        return switch (role) {
            case "CREW" -> "crew";
            case "CAPCOM" -> "CAPCOM";
            default -> "flight controller";
        };
    }

    private static String traitPhrase(String trait) {
        try {
            return TraitTexts.inSentence(Trait.valueOf(trait));
        } catch (IllegalArgumentException unknown) {
            return trait.toLowerCase(Locale.ROOT);
        }
    }

    private static String capitalise(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
