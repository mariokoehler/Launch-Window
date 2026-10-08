package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.rules.OfferResult;
import de.mkoehler.launchwindow.rules.OfferSettled;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.Poaching;

import java.util.List;
import java.util.Locale;

/**
 * What the poaching screens say ({@code ui-spec.md} &sect;4.16, Poaching canvas, 2026-10-08): why a
 * file's person can't be approached, the offer's cost and answer, the standing orders, and the
 * turn report's lines. Plain logic, no libGDX; every number comes from {@link Poaching}.
 *
 * @author Mario Koehler
 */
public final class PoachTexts {

    private PoachTexts() {
    }

    /**
     * Returns the turn someone in a file joined their agency, as far as the file tells.
     *
     * @param file the file
     * @return the turn
     */
    public static int joinedTurn(PersonnelFile file) {
        return Math.max(1, file.turn() - file.tenureSeasons());
    }

    /**
     * Returns a file's "With NASA since …" line.
     *
     * @param file   the file
     * @param agency the rival's name
     * @return the line
     */
    public static String since(PersonnelFile file, String agency) {
        return "With " + agency + " since " + Museum.date(joinedTurn(file));
    }

    /**
     * Returns why a file's person can't be approached.
     *
     * @param blocker the reason
     * @param file    the file
     * @param agency  the rival's name
     * @return the sentence
     */
    public static String blocker(Poaching.Blocker blocker, PersonnelFile file, String agency) {
        return switch (blocker) {
            case ASTRONAUT -> "Astronauts can't be approached.";
            case TOO_NEW -> "With the agency under " + Poaching.MIN_TENURE_SEASONS + " seasons: joined " + agency + " in "
                + Museum.date(joinedTurn(file)) + ".";
            case APPROACHED_RECENTLY -> "Approached recently: wait until "
                + Museum.date(Poaching.approachableAgain(file.lastApproachedTurn())) + ".";
        };
    }

    /**
     * Returns a group's roster for the offer's room line: "SET Centre" or "Mission Control".
     *
     * @param group the group, by {@link PersonnelGroup} name
     * @return the building's name
     */
    public static String building(String group) {
        return PersonnelGroup.SET.name().equals(group) ? "SET Centre" : "Mission Control";
    }

    /**
     * Returns an amount as the offer dialog shows it: "45.0".
     *
     * @param amount the amount
     * @return the text
     */
    public static String money(double amount) {
        return String.format(Locale.ROOT, "%.1f", amount);
    }

    /**
     * Returns the queued offer's cost line: "Salary 38 → 57 if accepted, and a signing bonus of 57."
     *
     * @param file         the file, for today's salary
     * @param raisePercent the raise
     * @return the line
     */
    public static String ifAccepted(PersonnelFile file, int raisePercent) {
        long salary = Poaching.offeredSalary(file.salary(), raisePercent);
        return "Salary " + Math.round(file.salary()) + " → " + salary + " if accepted, and a signing bonus of "
            + Poaching.signingBonus(salary) + ".";
    }

    /**
     * Returns the answer's range as text: "19–28 %", or "90 %" when both ends meet.
     *
     * @param range the range
     * @return the text
     */
    public static String range(int[] range) {
        return range[0] == range[1] ? range[0] + " %" : range[0] + "–" + range[1] + " %";
    }

    /**
     * Returns a raise as a label: "+50 %".
     *
     * @param percent the raise
     * @return the label
     */
    public static String raise(int percent) {
        return "+" + percent + " %";
    }

    /**
     * Returns an agency default's segment label: "Never" or "+25 %".
     *
     * @param percent the default
     * @return the label
     */
    public static String matchLabel(int percent) {
        return percent == 0 ? "Never" : raise(percent);
    }

    /**
     * Returns what an agency default does, for the retention row's first segment: "matches up to +25 %".
     *
     * @param percent the default
     * @return the text
     */
    public static String defaultDoes(int percent) {
        return percent == 0 ? "never matches" : "matches up to " + raise(percent);
    }

    /**
     * Returns the retention row's caption for one person.
     *
     * @param retention the standing order
     * @param percent   the agency default for their group
     * @param name      their full name
     * @return the caption
     */
    public static String retentionCaption(Retention retention, int percent, String name) {
        String surname = People.surname(name);
        return switch (retention) {
            case KEEP -> "Every rival offer is matched, whatever it is.";
            case LET_GO -> "No offer is matched. Whether " + surname + " goes is " + surname + "'s to decide.";
            case DEFAULT -> percent == 0 ? "No offer is matched. Whether " + surname + " goes is " + surname
                + "'s to decide." : "Offers up to " + raise(percent) + " are matched. A bigger one is " + surname
                + "'s to decide.";
        };
    }

    /**
     * Returns the department card's "set otherwise" line, or {@code ""} when everyone is on the default.
     *
     * @param people the department
     * @param group  its group
     * @return the line, such as "2 engineers set otherwise: Martirosyan keeps at any price, Saar is let go."
     */
    public static String setOtherwise(List<? extends Personnel> people, PersonnelGroup group) {
        List<? extends Personnel> other = people.stream().filter(person -> person.getRetention() != Retention.DEFAULT)
            .toList();
        if (other.isEmpty()) {
            return "";
        }
        String noun = group == PersonnelGroup.SET ? (other.size() == 1 ? "engineer" : "engineers")
            : (other.size() == 1 ? "flight controller" : "flight controllers");
        List<String> parts = other.stream().map(person -> People.surname(person.getName())
            + (person.getRetention() == Retention.KEEP ? " keeps at any price" : " is let go")).toList();
        return other.size() + " " + noun + " set otherwise: " + String.join(", ", parts) + ".";
    }

    /**
     * Returns whether a career line comes from an earlier agency: anything older than the latest move.
     *
     * @param career the career, oldest first
     * @param index  the line's index
     * @return the agency it was with, or {@code null} for this agency
     */
    public static String earlierAgency(List<CareerEntry> career, int index) {
        // The first move after this line names the agency it happened at.
        for (int i = index + 1; i < career.size(); i++) {
            if (career.get(i).kind() == CareerEntry.Kind.JOINED) {
                return career.get(i).ref();
            }
        }
        return null;
    }

    /**
     * Returns the latest move between agencies, or {@code null} if they never moved.
     *
     * @param person the person
     * @return the JOINED line, or {@code null}
     */
    public static CareerEntry joined(Personnel person) {
        List<CareerEntry> career = person.getCareer();
        for (int i = career.size() - 1; i >= 0; i--) {
            if (career.get(i).kind() == CareerEntry.Kind.JOINED) {
                return career.get(i);
            }
        }
        return null;
    }

    /**
     * Words an offer settled for the turn report: the line and its tag.
     *
     * @param offer the offer
     * @return the text, the tag ("Matched", "Left", "Joined", "Declined", "Lapsed"), and the tone
     */
    public static TurnReportView.Line reportLine(OfferSettled offer) {
        TurnReportView.Line line = words(offer);
        return new TurnReportView.Line(line.title(), line.text(), line.tag(), line.tone(), pageOf(offer));
    }

    /**
     * Returns whose page an offer's line opens: one of ours who stayed, or someone who came over.
     * Never the rival's person (their id means nothing in this agency) or someone who left.
     *
     * @param offer the offer
     * @return the person's id here, or {@code null}
     */
    public static String pageOf(OfferSettled offer) {
        boolean ours = offer.made() ? offer.outcome() == OfferResult.Outcome.JOINED
            : offer.outcome() == OfferResult.Outcome.MATCHED || offer.outcome() == OfferResult.Outcome.TURNED_DOWN;
        return ours ? offer.personId() : null;
    }

    private static TurnReportView.Line words(OfferSettled offer) {
        String name = offer.personName() == null || offer.personName().isBlank() ? "someone" : offer.personName();
        String raise = raise(offer.raisePercent());
        String agency = offer.rivalAgency() == null || offer.rivalAgency().isBlank() ? "a rival agency"
            : offer.rivalAgency();
        if (!offer.made()) {
            String opener = Character.toUpperCase(agency.charAt(0)) + agency.substring(1) + " offered " + name + " "
                + raise + ": ";
            String surname = People.surname(name);
            return switch (offer.outcome()) {
                case MATCHED -> new TurnReportView.Line(opener + "matched. Salary now " + Math.round(offer.newSalary())
                    + " a season.", "", "Matched", 1);
                case JOINED -> new TurnReportView.Line(opener + surname + " left for " + agency + ". "
                    + noMatch(offer), "", "Left", -1);
                default -> new TurnReportView.Line(opener + surname + " turned it down. " + noMatch(offer), "", "Declined",
                    1);
            };
        }
        String opener = "Your offer to " + name + " of " + agency + " (" + raise + "): ";
        return switch (offer.outcome()) {
            case JOINED -> new TurnReportView.Line(opener + "joined you. Salary " + Math.round(offer.newSalary())
                + " a season; signing bonus of " + offer.signingBonus() + " paid.", "", "Joined", 1);
            case MATCHED -> new TurnReportView.Line(opener + agency + " matched it.", "", "Matched", 0);
            case TURNED_DOWN -> new TurnReportView.Line(opener + "turned it down.", "", "Declined", 0);
            case LAPSED_NO_ROOM -> new TurnReportView.Line(opener + "lapsed, no room in " + building(offer.group())
                + ".", "", "Lapsed", -1);
            case LAPSED_GONE -> new TurnReportView.Line(opener + "lapsed, no longer there.", "", "Lapsed", 0);
            case LAPSED_NOT_ELIGIBLE -> new TurnReportView.Line(opener + "lapsed: not someone who could be approached this "
                + "season (too new, approached recently, or busy).", "", "Lapsed", 0);
        };
    }

    private static String noMatch(OfferSettled offer) {
        if (Retention.LET_GO.name().equals(offer.retention())) {
            return "Set to “Let go”, so no match was made.";
        }
        if (offer.matchPercent() == 0) {
            return "The agency never matches.";
        }
        return offer.matchPercent() > 0 ? "The agency matches up to " + raise(offer.matchPercent()) + "." : "";
    }

    /**
     * Returns whether an outcome means the person came over or left.
     *
     * @param outcome the outcome
     * @return whether they moved
     */
    public static boolean moved(OfferResult.Outcome outcome) {
        return outcome == OfferResult.Outcome.JOINED;
    }
}
