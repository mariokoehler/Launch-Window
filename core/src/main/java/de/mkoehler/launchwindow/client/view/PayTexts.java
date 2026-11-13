package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.rules.ExpectationRose;
import de.mkoehler.launchwindow.rules.ExpectationsRoseAtReview;
import de.mkoehler.launchwindow.rules.PayExpectations;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Every line pay expectations show ({@code ui-spec.md} &sect;4.23), numbers read from {@link PayExpectations}.
 *
 * @author Mario Koehler
 */
public final class PayTexts {

    private PayTexts() {
    }

    /**
     * Formats money as the built screens do.
     *
     * @param amount the amount
     * @return "$ 42.0"
     */
    public static String money(double amount) {
        return String.format(Locale.ROOT, "$ %,.1f", amount);
    }

    /**
     * Returns how far under someone is, as shown: a whole percentage.
     *
     * @param person the person
     * @return the percentage, rounded
     */
    public static long under(Personnel person) {
        return Math.round(PayExpectations.underpaidPercent(person));
    }

    /**
     * The person page's line under the pay bar.
     *
     * @param person the person
     * @return "Underpaid by 18 %: morale can't rise above 47." or "Paid as expected"
     */
    public static String standing(Personnel person) {
        if (PayExpectations.underpaidPercent(person) <= 0) {
            return "Paid as expected";
        }
        return "Underpaid by " + under(person) + " %: morale can't rise above " + PayExpectations.moraleCeiling(person)
            + ".";
    }

    /**
     * The morale tooltip's sentence on what morale does at work.
     *
     * @return the sentence
     */
    public static String atWork() {
        long percent = Math.round(PayExpectations.MORALE_SKILL * 100);
        return "From " + PayExpectations.HIGH_MORALE + ", skills count " + percent + " % more on missions and in R&D; "
            + "below " + PayExpectations.LOW_MORALE + ", " + percent + " % less.";
    }

    /**
     * The morale tooltip's note when pay holds morale, or {@code null}.
     *
     * @param person the person
     * @return "Pay holds it to 47 or less: underpaid by 18 %."
     */
    public static String holds(Personnel person) {
        int ceiling = PayExpectations.moraleCeiling(person);
        return ceiling >= PersonnelLifecycle.MORALE_MAX ? null
            : "Pay holds it to " + ceiling + " or less: underpaid by " + under(person) + " %.";
    }

    /**
     * The roster chip's tooltip text.
     *
     * @param person the person
     * @return "Paid $ 42.0, expects $ 51.0. Morale can't rise above 47."
     */
    public static String chipText(Personnel person) {
        return "Paid " + money(person.getSalary()) + ", expects " + money(person.expectedSalary()) + ". Morale can't "
            + "rise above " + PayExpectations.moraleCeiling(person) + ".";
    }

    /**
     * A department's place name, as the report links say it.
     *
     * @param group the department
     * @return "SET Centre"
     */
    public static String place(PersonnelGroup group) {
        return switch (group) {
            case SET -> "SET Centre";
            case ASTRONAUT -> "Astronaut Complex";
            case MISSION_CONTROL -> "Mission Control";
        };
    }

    /**
     * The report's line for a review's rise.
     *
     * @param review the event
     * @return "Pay expectations rose 10 % with the funding review: 23 people now underpaid."
     */
    public static String reviewLine(ExpectationsRoseAtReview review) {
        String now = review.underpaid() == 0 ? "nobody is underpaid yet."
            : review.underpaid() == 1 ? "1 person now underpaid." : review.underpaid() + " people now underpaid.";
        return "Pay expectations rose " + review.percent() + " % with the funding review: " + now;
    }

    /**
     * The report's line for one person's rise, after their name.
     *
     * @param rose the event
     * @return "expects 15 % more: a First, the Expert trait."
     */
    public static String riseLine(ExpectationRose rose) {
        List<String> causes = new ArrayList<>();
        for (String reason : rose.reasons()) {
            String[] parts = reason.split(":", 2);
            String detail = parts.length > 1 ? parts[1] : "";
            causes.add(switch (parts[0]) {
                case "FLIGHT" -> switch (detail) {
                    case "CAPCOM" -> "a CAPCOM shift";
                    case "CONTROLLER" -> "a console shift";
                    default -> "a flight";
                };
                case "FIRST" -> "a First";
                case "EXPERT" -> "the Expert trait";
                case "AMBITION" -> "an ambition come true";
                case "RD_STINT" -> "R&D on the " + ContentNames.of(detail);
                default -> parts[0].toLowerCase(Locale.ROOT);
            });
        }
        return "expects " + rose.percent() + " % more" + (causes.isEmpty() ? "." : ": " + String.join(", ", causes)
            + ".");
    }
}
