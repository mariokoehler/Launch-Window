package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Finding;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.FindingLearned;
import de.mkoehler.launchwindow.rules.FindingPublished;
import de.mkoehler.launchwindow.rules.Findings;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the screens say about findings and publishing them ({@code design.md} &sect;2.13): a finding's
 * standing for the player, what it makes safer, and the turn report's lines.
 *
 * @author Mario Koehler
 */
public final class FindingsView {

    /** A finding's standing for the player. */
    public enum Status {
        /** Not known: nobody published it, and the agency doesn't hold the goal. */
        UNKNOWN,
        /** Known and classified: the agency holds it and may publish it. */
        CLASSIFIED,
        /** The agency asked to publish it at the next resolution. */
        QUEUED,
        /** The agency published it. */
        PUBLISHED_BY_YOU,
        /** Another agency published it. */
        PUBLIC
    }

    /**
     * A line of the turn report's Findings card.
     *
     * @param goalId the finding
     * @param title  the lead
     * @param text   more
     * @param tone   positive for good news, 0 for neutral
     * @param earned   whether the agency earned the finding this turn, so the card offers to publish it
     * @param prestige what the agency's publication paid, or 0
     */
    public record ReportLine(String goalId, String title, String text, int tone, boolean earned, long prestige) {
    }

    /** Names for step kinds, folding the pack's near-twins into one ("lunar-landing" is a landing). */
    private static final Map<String, String> KIND_NAMES = Map.ofEntries(
        Map.entry("duration*", "long stays in orbit"),
        Map.entry("eva*", "spacewalks"),
        Map.entry("earth-orbit", "Earth orbit"),
        Map.entry("orbital-flight", "Earth orbit"),
        Map.entry("earth-orbital", "Earth orbit"),
        Map.entry("high-orbit-injection", "high orbit"),
        Map.entry("injection-to-elliptical-meo", "high orbit"),
        Map.entry("meo", "high orbit"),
        Map.entry("tli", "trans-lunar injection"),
        Map.entry("loi", "lunar orbit insertion"),
        Map.entry("insertion", "orbit insertion"),
        Map.entry("earth-approach", "Earth approach"),
        Map.entry("earth-approach-and-reentry", "reentry"),
        Map.entry("lunar-landing", "landing"),
        Map.entry("lk-descent", "powered descent"),
        Map.entry("landing-and-deployment", "landing"),
        Map.entry("lander-entry", "entry"),
        Map.entry("big-probe-approach-and-entry", "entry"),
        Map.entry("small-probes-approach-and-entry", "entry"),
        Map.entry("lander-descent-and-landing", "lander descent"),
        Map.entry("big-probe-descent-and-landing", "probe descent"),
        Map.entry("small-probes-landing", "probe descent"),
        Map.entry("lander-deployment-and-de-orbit", "de-orbit"),
        Map.entry("saturn-approach", "approach"),
        Map.entry("saturn-flyby", "flyby"));

    private FindingsView() {
    }

    /**
     * A finding's standing for the player.
     *
     * @param state  the player
     * @param goalId the finding
     * @return its standing
     */
    public static Status status(PlayerSnapshot state, String goalId) {
        if (state.getPublishedFindings().contains(goalId)) {
            return Status.PUBLISHED_BY_YOU;
        }
        if (state.getPublicFindings().containsKey(goalId)) {
            return Status.PUBLIC;
        }
        if (state.getPendingPublications().contains(goalId)) {
            return Status.QUEUED;
        }
        return state.hasAchievedOrCancelledGoal(goalId) ? Status.CLASSIFIED : Status.UNKNOWN;
    }

    /**
     * The findings the player knows, in the pack's order.
     *
     * @param state   the player
     * @param catalog the game's content
     * @return their goals
     */
    public static List<String> known(PlayerSnapshot state, ContentCatalog catalog) {
        return catalog.findings().stream().map(Finding::goalId).filter(goal -> Findings.knows(state, goal)).toList();
    }

    /**
     * A short line on a finding's standing.
     *
     * @param status the standing
     * @return the words
     */
    public static String statusText(Status status) {
        return switch (status) {
            case UNKNOWN -> "Not known";
            case CLASSIFIED -> "Classified";
            case QUEUED -> "Published at the next resolution";
            case PUBLISHED_BY_YOU -> "Published by you";
            case PUBLIC -> "Published";
        };
    }

    /**
     * What a finding makes safer, as a sentence's object: "crewed reentry and orbit insertion around
     * Earth".
     *
     * @param catalog the game's content
     * @param goalId  the finding
     * @return the words, or {@code ""} when the goal isn't a finding
     */
    public static String safer(ContentCatalog catalog, String goalId) {
        Finding finding = catalog.finding(goalId).orElse(null);
        if (finding == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Finding.StepMatch match : finding.matches()) {
            List<String> kinds = match.kinds().stream().map(FindingsView::kindName).distinct().toList();
            String words = (Boolean.TRUE.equals(match.crewed()) ? "crewed " : Boolean.FALSE.equals(match.crewed())
                ? "uncrewed " : "") + joined(kinds);
            parts.add(match.body() == null ? words : words + " " + bodyWords(match.body()));
        }
        return joined(parts);
    }

    /**
     * The turn report's Findings lines: findings earned this turn, the agency's publications, and
     * those of others.
     *
     * @param report  the report
     * @param catalog the game's content
     * @return the lines, in the report's order
     */
    public static List<ReportLine> reportLines(TurnReport report, ContentCatalog catalog) {
        List<ReportLine> lines = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof GoalSettled goal && catalog.finding(goal.goalId()).isPresent()) {
                lines.add(new ReportLine(goal.goalId(), "New finding: " + ContentNames.of(goal.goalId()),
                    "Your flights know it now. Safer: " + safer(catalog, goal.goalId()) + ".", 1, true, 0));
            } else if (event instanceof FindingPublished published) {
                String with = published.coPublishers().isEmpty() ? ""
                    : " With " + joined(published.coPublishers()) + ", who published it too.";
                lines.add(new ReportLine(published.goalId(), "You published " + ContentNames.of(published.goalId()),
                    (published.helped() == 0 ? "Every rival knew it already." : "It helped " + published.helped()
                        + " of your " + published.rivals() + (published.rivals() == 1 ? " rival." : " rivals."))
                        + with, published.prestige() > 0 ? 1 : 0, false, published.prestige()));
            } else if (event instanceof FindingLearned learned) {
                lines.add(new ReportLine(learned.goalId(), joined(learned.publishers()) + " published "
                    + ContentNames.of(learned.goalId()), learned.knewIt() ? "You knew it already."
                    : "Safer for you from now on: " + safer(catalog, learned.goalId()) + ".", learned.knewIt() ? 0 : 1,
                    false, 0));
            }
        }
        return lines;
    }

    /**
     * The points a known finding adds to a step's base chance, signed: "+4".
     *
     * @return the words
     */
    public static String bonusText() {
        return pointsText(1);
    }

    /**
     * The points some known findings add to a step's base chance, signed: "+8" for two.
     *
     * @param findings how many findings
     * @return the words
     */
    public static String pointsText(int findings) {
        double points = Findings.bonusPoints() * findings;
        return "+" + (points == Math.rint(points) ? String.valueOf((long) points)
            : String.format(Locale.ROOT, "%.1f", points));
    }

    static String kindName(String kind) {
        String name = KIND_NAMES.get(kind);
        if (name != null) {
            return name;
        }
        if (kind.endsWith("-transit")) {
            return "interplanetary cruise";
        }
        return kind.replace("*", "").replace('-', ' ').strip();
    }

    private static String bodyWords(CelestialBody body) {
        return switch (body) {
            case EARTH -> "around Earth";
            case MOON -> "on flights to the Moon";
            default -> "on flights to " + body.name().charAt(0) + body.name().substring(1).toLowerCase(Locale.ROOT);
        };
    }

    private static String joined(List<String> words) {
        if (words.size() <= 1) {
            return words.isEmpty() ? "" : words.getFirst();
        }
        return String.join(", ", words.subList(0, words.size() - 1)) + " and " + words.getLast();
    }
}
