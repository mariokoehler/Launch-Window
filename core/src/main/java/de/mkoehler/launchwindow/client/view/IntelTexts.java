package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.model.Dossier;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.IntelligenceState;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.Intelligence;

import java.util.Locale;

/**
 * The Intelligence Office's words ({@code ui-spec.md} &sect;4.15, Intelligence Office canvas,
 * 2026-10-07): levels, odds, dossier lines, tidbits and findings. Numbers come from
 * {@link Intelligence}'s constants, never typed in here.
 *
 * @author Mario Koehler
 */
public final class IntelTexts {

    private IntelTexts() {
    }

    /**
     * Returns a level's name.
     *
     * @param level the level
     * @return the name, such as "Analysis section"
     */
    public static String levelName(int level) {
        return Intelligence.LEVEL_NAMES.get(Math.clamp(level, 0, Intelligence.MAX_LEVEL));
    }

    /**
     * Returns the footer chip's text: the level, the target when it differs, and the actions used.
     *
     * @param state the player's state
     * @return the text, such as "L2 → 3 · 1 / 2"
     */
    public static String footer(PlayerSnapshot state) {
        IntelligenceState intel = state.getIntelligence();
        String text = "L" + intel.getLevel();
        if (intel.getTargetLevel() != intel.getLevel()) {
            text += " → " + intel.getTargetLevel();
        }
        if (intel.getLevel() > 0) {
            text += " · " + intel.getActionsUsed() + " / " + intel.getLevel();
        }
        return text;
    }

    /**
     * Returns when the target level arrives, or {@code null} when the level is at its target.
     *
     * @param state the player's state
     * @return the badge text, such as "Reaches level 3 in 1 season"
     */
    public static String reaches(PlayerSnapshot state) {
        IntelligenceState intel = state.getIntelligence();
        int steps = Math.abs(intel.getTargetLevel() - intel.getLevel());
        if (steps == 0) {
            return null;
        }
        return "Reaches level " + intel.getTargetLevel() + " in " + steps + (steps == 1 ? " season" : " seasons");
    }

    /**
     * Returns what the next resolutions charge on the way to the target.
     *
     * @param state the player's state
     * @return the line, or {@code null} when the level is at its target
     */
    public static String onTheWay(PlayerSnapshot state) {
        IntelligenceState intel = state.getIntelligence();
        int next = intel.getLevel() + Integer.signum(intel.getTargetLevel() - intel.getLevel());
        if (next == intel.getLevel()) {
            return null;
        }
        return money(Intelligence.cost(next)) + " at the next resolution, when level " + next
            + " arrives. Paid with the fixed costs.";
    }

    /**
     * Returns a cost per season.
     *
     * @param cost the cost
     * @return the text, such as "$ 350 a season", or "free"
     */
    public static String perSeason(long cost) {
        return cost == 0 ? "free" : money(cost) + " a season";
    }

    private static String money(long amount) {
        return String.format(Locale.ROOT, "$ %,d", amount);
    }

    /**
     * Returns what a level unlocks, for the ladder.
     *
     * @param level the level
     * @return the text
     */
    public static String unlocks(int level) {
        return switch (level) {
            case 1 -> "Background check on a candidate";
            case 2 -> "Agency assessment: one area of one rival";
            case 3 -> "Personnel file on one of a rival's people";
            default -> "Nothing";
        };
    }

    /**
     * Returns a level's passive, for the ladder.
     *
     * @param level the level
     * @return the text
     */
    public static String passive(int level) {
        return switch (level) {
            case 1 -> Intelligence.PASSIVE_CANDIDATE_PERCENT + " % per candidate: a trait shows on the hire screen";
            case 2 -> Intelligence.TIDBIT_PERCENT[2] + " % a season: a tidbit about a rival's R&D or plans";
            case 3 -> "Tidbits " + Intelligence.TIDBIT_PERCENT[3] + " % a season, from any area, people included";
            default -> "None";
        };
    }

    /**
     * Returns a counter-intelligence level as a numeral.
     *
     * @param level the level
     * @return "Baseline", "I", "II" or "III"
     */
    public static String counter(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "Baseline";
        };
    }

    /**
     * Returns what the agency's counter-intelligence does to a rival's operation against it.
     *
     * @param level the agency's level
     * @return the sentence
     */
    public static String counterEffect(int level) {
        return "A rival's operation against you succeeds " + Intelligence.successPercent(level)
            + " % of the time; a failed one is caught " + Intelligence.catchPercent(level) + " % of the time.";
    }

    /**
     * Returns an area's name.
     *
     * @param area the area
     * @return the name, such as "programs and plans"
     */
    public static String area(IntelArea area) {
        return switch (area) {
            case RND -> "R&D";
            case PLANS -> "programs and plans";
            case PERSONNEL -> "personnel";
        };
    }

    /**
     * Returns an area's heading, capitalised.
     *
     * @param area the area
     * @return the heading
     */
    public static String areaTitle(IntelArea area) {
        String name = area(area);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Returns what an area of an assessment covers.
     *
     * @param area the area
     * @return the sentence
     */
    public static String areaCovers(IntelArea area) {
        return switch (area) {
            case RND -> "Reliability of every component they own, as ranges.";
            case PLANS -> "Open programs, missions in flight, what their engineers work on, hardware not yet flown, "
                + "and their Intelligence level.";
            case PERSONNEL -> "Head counts and average skill per group.";
        };
    }

    /**
     * Returns a dossier's stamp.
     *
     * @param turn the game turn it dates from
     * @return the stamp, such as "As of Spring 1962"
     */
    public static String asOf(int turn) {
        return "As of " + GameDate.ofTurn(turn).label();
    }

    /**
     * Returns a dossier's stamp: its date, and for a follow-up which look in a row it was, since
     * each narrows the ranges (2026-10-27).
     *
     * @param dossier the dossier
     * @return the stamp's text
     */
    public static String stamp(Dossier dossier) {
        return dossier.looks() <= 1 ? asOf(dossier.turn())
            : asOf(dossier.turn()) + " \u00b7 " + ordinal(dossier.looks()) + " look";
    }

    private static String ordinal(int n) {
        String suffix = n % 100 >= 11 && n % 100 <= 13 ? "th"
            : switch (n % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        return n + suffix;
    }

    /**
     * Returns a rival's known level, with where it came from, for a folder's header.
     *
     * @param folder the folder, or {@code null}
     * @return the text, such as "Intelligence level 1 as of Spring 1962"
     */
    public static String knownLevel(RivalFolder folder) {
        if (folder == null || folder.getKnownLevel() < 0) {
            return "Intelligence level unknown";
        }
        return "Intelligence level " + folder.getKnownLevel() + " as of " + GameDate.ofTurn(folder.getKnownLevelTurn())
            .label();
    }

    /**
     * Returns where a rival's known level came from.
     *
     * @param folder the folder
     * @return the phrase, such as "from your plans dossier"
     */
    public static String levelSource(RivalFolder folder) {
        if (folder == null || folder.getKnownLevelSource() == null) {
            return "";
        }
        return switch (folder.getKnownLevelSource()) {
            case RivalFolder.FROM_DOSSIER -> "from your plans dossier";
            case RivalFolder.FROM_BEING_CAUGHT -> "learned when their counter-intelligence caught your agent";
            case RivalFolder.FROM_A_CATCH -> "learned when you caught their agent";
            default -> "";
        };
    }

    /**
     * Returns a targeted operation's chance of success against a rival, exact when their level is
     * known, else the full range.
     *
     * @param folder the rival's folder, or {@code null}
     * @return the text, such as "70 %" or "40–85 %"
     */
    public static String odds(RivalFolder folder) {
        if (folder == null || folder.getKnownLevel() < 0) {
            return Intelligence.successPercent(Intelligence.MAX_LEVEL) + "–" + Intelligence.successPercent(0) + " %";
        }
        return Intelligence.successPercent(folder.getKnownLevel()) + " %";
    }

    /**
     * Returns the line under the odds: where the level comes from, or why there's a range.
     *
     * @param folder the rival's folder, or {@code null}
     * @return the line
     */
    public static String oddsSource(RivalFolder folder) {
        if (folder == null || folder.getKnownLevel() < 0) {
            return "Intelligence level unknown: " + odds(folder) + ", depending on their counter-intelligence";
        }
        return "Their counter-intelligence was level " + folder.getKnownLevel() + " as of "
            + GameDate.ofTurn(folder.getKnownLevelTurn()).label() + ", " + levelSource(folder);
    }

    /**
     * Returns the chance of being caught against a rival, or {@code null} when their level is unknown.
     *
     * @param folder the rival's folder, or {@code null}
     * @return the text, such as "6 %"
     */
    public static String caughtOdds(RivalFolder folder) {
        if (folder == null || folder.getKnownLevel() < 0) {
            return Intelligence.caughtOverallPercent(0) + "–" + Intelligence.caughtOverallPercent(Intelligence.MAX_LEVEL)
                + " %";
        }
        return Intelligence.caughtOverallPercent(folder.getKnownLevel()) + " %";
    }

    /**
     * Returns a component, program or configuration's name.
     */
    private static String name(String id) {
        return ContentNames.of(id);
    }

    /**
     * Returns a fact as a dossier row's label and value.
     *
     * @param fact the fact
     * @return label and value
     */
    public static String[] row(IntelFact fact) {
        return switch (fact.kind()) {
            case COMPONENT_RELIABILITY -> new String[] {name(fact.ref()), range(fact) + " %"};
            case OPEN_PROGRAM -> new String[] {"Open programs", name(fact.ref())};
            case IN_FLIGHT -> new String[] {"In flight", name(fact.ref())};
            case RD_FOCUS -> new String[] {"R&D on", name(fact.ref()) + " · " + engineers(fact.count())};
            case SHELF -> new String[] {"Not flown yet", name(fact.ref())};
            case INTEL_LEVEL -> new String[] {"Intelligence", "Level " + fact.count() + " · "
                + levelName(fact.count())};
            case HEADCOUNT -> new String[] {groupName(fact.ref()), String.valueOf(fact.count())};
            case AVERAGE_SKILL -> new String[] {groupName(fact.ref()) + ", average skill", range(fact)};
            case SKILL -> new String[] {TurnReportView.skillLabel(fact.ref()), range(fact)};
            case CONTRACT -> new String[] {"Contract", name(fact.ref()) + " · tier " + fact.count()};
            case CONTRACT_RECORD -> new String[] {"Contract record, " + name(fact.ref()),
                fact.low() + " flown · " + fact.high() + " failed · " + signed(fact.count())};
        };
    }

    /**
     * Returns a fact as a tidbit's sentence.
     *
     * @param fact the fact
     * @return the sentence
     */
    public static String tidbit(IntelFact fact) {
        return switch (fact.kind()) {
            case COMPONENT_RELIABILITY -> "Their " + name(fact.ref()) + " is somewhere around " + range(fact) + " %.";
            case OPEN_PROGRAM -> "They have " + name(fact.ref()) + " open.";
            case IN_FLIGHT -> "They have " + name(fact.ref()) + " in flight.";
            case RD_FOCUS -> engineersCap(fact.count()) + " work on their " + name(fact.ref()) + ".";
            case SHELF -> "Their " + name(fact.ref()) + " hasn't flown yet.";
            case INTEL_LEVEL -> "Their Intelligence Office is at level " + fact.count() + ".";
            case HEADCOUNT -> "They have " + fact.count() + " " + groupName(fact.ref()).toLowerCase(Locale.ROOT) + ".";
            case AVERAGE_SKILL -> "Their " + groupName(fact.ref()).toLowerCase(Locale.ROOT) + " average "
                + range(fact) + " in skill.";
            case SKILL -> "One of their people rates " + range(fact) + " in "
                + TurnReportView.skillLabel(fact.ref()) + ".";
            case CONTRACT -> "They hold a tier " + fact.count() + " contract for " + name(fact.ref()) + ".";
            case CONTRACT_RECORD -> "They have flown " + fact.low() + " contracts for " + name(fact.ref())
                + " and failed " + fact.high() + ".";
        };
    }

    private static String signed(int points) {
        return (points > 0 ? "+" : points < 0 ? "−" : "") + Math.abs(points);
    }

    private static String range(IntelFact fact) {
        return fact.low() + "–" + fact.high();
    }

    private static String engineers(int count) {
        return count + (count == 1 ? " engineer" : " engineers");
    }

    private static String engineersCap(int count) {
        return count == 1 ? "One engineer" : count + " engineers";
    }

    /**
     * Returns a group's plural name.
     *
     * @param group the group, by {@code PersonnelGroup} name
     * @return the name, such as "Astronauts"
     */
    public static String groupName(String group) {
        return switch (group) {
            case "ASTRONAUT" -> "Astronauts";
            case "SET" -> "Engineers";
            case "MISSION_CONTROL" -> "Flight controllers";
            default -> group;
        };
    }

    /**
     * Returns a group's singular name, for a personnel file.
     *
     * @param group the group, by {@code PersonnelGroup} name
     * @return the name, such as "Engineer"
     */
    public static String groupOne(String group) {
        return switch (group) {
            case "ASTRONAUT" -> "Astronaut";
            case "SET" -> "Engineer";
            case "MISSION_CONTROL" -> "Flight controller";
            default -> group;
        };
    }

    /**
     * Returns an operation's description.
     *
     * @param kind  the operation
     * @param area  its area, for an assessment
     * @param group its group, for a personnel file
     * @return the text, such as "Agency assessment · R&D"
     */
    public static String operation(IntelOperation.Kind kind, IntelArea area, String group) {
        if (kind == IntelOperation.Kind.OFFER) {
            return "Offer";
        }
        if (kind == IntelOperation.Kind.PERSONNEL_FILE) {
            return "Personnel file" + (group == null ? "" : " · " + groupName(group).toLowerCase(Locale.ROOT));
        }
        return "Agency assessment" + (area == null ? "" : " · " + area(area));
    }

    /**
     * Returns a finding as a headline and a detail line, for the turn report and last season's results.
     *
     * @param finding the finding
     * @return headline and detail
     */
    public static String[] finding(IntelFinding finding) {
        String rival = finding.rivalAgency() == null ? "Unknown agency" : finding.rivalAgency();
        String what = finding.operationKind() == null ? "" : " · " + (finding.operationKind()
            == IntelOperation.Kind.ASSESSMENT && finding.area() != null ? area(finding.area())
            : finding.operationKind() == IntelOperation.Kind.OFFER ? "offer" : "personnel file");
        return switch (finding.kind()) {
            case DOSSIER -> new String[] {rival + what, "A new dossier, as of "
                + GameDate.ofTurn(finding.dossier().turn()).label() + "."
                + (finding.level() >= 0 ? " Their Intelligence level: " + finding.level() + "." : "")};
            case PERSONNEL_FILE -> new String[] {rival + what, "A file on " + finding.file().name() + ", "
                + groupOne(finding.file().group()).toLowerCase(Locale.ROOT) + "."};
            case TIDBIT -> new String[] {"Tidbit · " + rival, tidbit(finding.tidbit().fact())};
            case FAILED -> new String[] {rival + what, "Failed and learned nothing. Not caught, so their level stays "
                + "unknown."};
            case CAUGHT -> new String[] {rival + what, "Blown: their counter-intelligence caught your agent. −"
                + Intelligence.CAUGHT_PRESTIGE + " prestige. Their counter-intelligence was level " + finding.level()
                + "."};
            case CAUGHT_THEIRS -> new String[] {"Caught · " + rival, "Your counter-intelligence caught their agent"
                + (finding.operationKind() == null ? "" : " on " + (finding.operationKind()
                == IntelOperation.Kind.ASSESSMENT ? "an assessment" : "a personnel file"))
                + ". Their Intelligence level: " + finding.level() + "."};
            case NO_TARGET -> new String[] {rival + what, "Nothing to look at: the target wasn't there."};
            case NOT_SETTLED -> new String[] {"Operation" + what, "Queued too late for this turn; nothing happened."};
            // Offers report through their own line (OfferSettled), never as a finding.
            case OFFER_MADE, OFFER_RECEIVED -> new String[] {rival + what, ""};
        };
    }

    /**
     * Returns whether a finding is good news, bad news or neither: 1, -1 or 0.
     *
     * @param finding the finding
     * @return the tone
     */
    public static int tone(IntelFinding finding) {
        return switch (finding.kind()) {
            case DOSSIER, PERSONNEL_FILE, TIDBIT, CAUGHT_THEIRS -> 1;
            case CAUGHT -> -1;
            default -> 0;
        };
    }
}
