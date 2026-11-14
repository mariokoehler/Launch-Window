package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.InvestigationCauses;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.Incident;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.InvestigationConcluded;
import de.mkoehler.launchwindow.rules.InvestigationLapsed;
import de.mkoehler.launchwindow.rules.InvestigationOffered;
import de.mkoehler.launchwindow.rules.Investigations;
import de.mkoehler.launchwindow.rules.OfficialInvestigationOpened;
import de.mkoehler.launchwindow.rules.OfficialReportIssued;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The words of incident investigations ({@code design.md} &sect;2.14, {@code ui-spec.md} &sect;4.24): the
 * turn report's lines, the choose-a-lead dialog's rows, and a board's front page, built from the rules'
 * events and {@code investigation-causes.json}. No numbers of its own: the odds come from
 * {@link Investigations}.
 *
 * @author Mario Koehler
 */
public final class InvestigationsView {

    private InvestigationsView() {
    }

    /** What a report line is about, which decides its controls. */
    public enum Kind {
        /** An investigation can be started; the controls read today's state. */
        OFFERED,
        /** The agency's investigation found the cause. */
        FOUND,
        /** It didn't. */
        UNDETERMINED,
        /** An offer closed unanswered. */
        LAPSED,
        /** A board investigates a lost crew. */
        BOARD_OPENED,
        /** A board reported. */
        BOARD_REPORTED
    }

    /**
     * One line of the report's Investigations card.
     *
     * @param kind       what it's about
     * @param incidentId the incident
     * @param title      the line's title
     * @param text       its sentences
     * @param value      the right-hand figure ("+5.3"), or {@code ""}
     * @param detail     a muted last line, or {@code ""}
     */
    public record ReportLine(Kind kind, String incidentId, String title, String text, String value, String detail) {
    }

    /**
     * Returns the Investigations card's lines for a report, in the report's order.
     *
     * @param report  the report
     * @param catalog the content pack
     * @param words   the causes' words
     * @return the lines; empty on a turn with none
     */
    public static List<ReportLine> reportLines(TurnReport report, ContentCatalog catalog, InvestigationCauses words) {
        List<ReportLine> lines = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            switch (event) {
                case InvestigationOffered offered -> lines.add(new ReportLine(Kind.OFFERED, offered.incidentId(),
                    "Investigate the failure of " + mission(catalog, offered.missionConfigurationId()) + "?",
                    "It failed at " + TurnReportView.stepInSentence(offered.missionConfigurationId(), offered.stepId())
                        + " and cost " + points(offered.totalLoss()) + " of reliability. An engineer leading an "
                        + "investigation for a season may find the cause and win some of it back.",
                    "", "Can be started until the end of " + GameDate.ofTurn(offered.lastTurn()).label() + "."));
                case InvestigationConcluded concluded when concluded.found() -> {
                    Optional<InvestigationCauses.Cause> cause = words.cause(concluded.causeId());
                    String component = ContentNames.of(concluded.culpritComponentId());
                    lines.add(new ReportLine(Kind.FOUND, concluded.incidentId(),
                        "Cause found: " + cause.map(c -> c.shortly()).orElse("the " + component),
                        cause.map(c -> fill(c.finding(), component) + " " + fill(c.fix(), component)).orElse(""),
                        String.format(Locale.ROOT, "+%.1f", concluded.totalRecovered()),
                        "Led by " + concluded.leadName() + ", on the "
                            + mission(catalog, concluded.missionConfigurationId()) + " failure. Given back: "
                            + recoveries(concluded) + "."));
                }
                case InvestigationConcluded concluded -> lines.add(new ReportLine(Kind.UNDETERMINED,
                    concluded.incidentId(),
                    "No cause found for the failure of " + mission(catalog, concluded.missionConfigurationId()),
                    "The investigation ran its season without an answer it could stand behind, so nothing comes back.",
                    "", "Led by " + concluded.leadName() + "."));
                case InvestigationLapsed lapsed -> lines.add(new ReportLine(Kind.LAPSED, lapsed.incidentId(),
                    "The failure of " + mission(catalog, lapsed.missionConfigurationId()) + " wasn't investigated",
                    "Its offer has closed.", "", ""));
                case OfficialInvestigationOpened opened -> lines.add(new ReportLine(Kind.BOARD_OPENED,
                    opened.incidentId(),
                    "An independent board investigates the loss of " + mission(catalog,
                        opened.missionConfigurationId()),
                    "Its report is due at the end of " + GameDate.ofTurn(opened.dueTurn()).label()
                        + ", when crewed flights may fly again. It costs the agency nothing and gives nothing back.",
                    "", ""));
                case OfficialReportIssued issued -> {
                    Optional<InvestigationCauses.Cause> cause = words.cause(issued.causeId());
                    String component = ContentNames.of(issued.culpritComponentId());
                    String factor = words.factor(issued.factorId()).map(f -> " " + f.text()).orElse("");
                    lines.add(new ReportLine(Kind.BOARD_REPORTED, issued.incidentId(),
                        "The board's report on " + mission(catalog, issued.missionConfigurationId()) + ": "
                            + cause.map(c -> c.shortly()).orElse("the " + component),
                        cause.map(c -> fill(c.finding(), component) + factor + " " + fill(c.fix(), component))
                            .orElse(""),
                        "", "In memory of " + FrontPageView.join(issued.crewNames()) + "."));
                }
                default -> { }
            }
        }
        return lines;
    }

    /**
     * Returns lines for the agency's investigations still open today that a report doesn't already tell: an offer
     * from an earlier turn, or one under way. Shown on the latest report only, so an offer isn't missed on its
     * second turn.
     *
     * @param state   today's state
     * @param told    the report's own lines
     * @param catalog the content pack
     * @return the lines, oldest first
     */
    public static List<ReportLine> stillOpen(PlayerSnapshot state, List<ReportLine> told, ContentCatalog catalog) {
        List<ReportLine> lines = new ArrayList<>();
        for (Incident incident : Investigations.agencyIncidents(state)) {
            if (told.stream().anyMatch(line -> line.incidentId().equals(incident.id()))) {
                continue;
            }
            lines.add(new ReportLine(Kind.OFFERED, incident.id(),
                "Investigate the failure of " + mission(catalog, incident.missionConfigurationId()) + "?",
                lossSummary(incident, catalog) + " Still open from " + GameDate.ofTurn(incident.turn()).label() + ".",
                "", "Can be started until the end of " + GameDate.ofTurn(Investigations.lastTurn(incident)).label()
                    + "."));
        }
        return lines;
    }

    /**
     * Returns the incident a line's controls act on today, if it's still waiting or under way.
     *
     * @param state      today's state
     * @param incidentId the incident
     * @return the incident
     */
    public static Optional<Incident> open(PlayerSnapshot state, String incidentId) {
        return Investigations.agencyIncident(state, incidentId);
    }

    /**
     * Returns the dialog's lead for an incident: its title and its one-line summary.
     *
     * @param incident the incident
     * @param catalog  the content pack
     * @return the summary, such as "Lost at ascent: Atlas −6.2, Mercury capsule −1.4"
     */
    public static String lossSummary(Incident incident, ContentCatalog catalog) {
        List<String> parts = new ArrayList<>();
        for (Incident.Part part : incident.components()) {
            if (part.loss() > 0) {
                parts.add(String.format(Locale.ROOT, "%s −%.1f", ContentNames.of(part.componentId()), part.loss()));
            }
        }
        return "Failed at " + TurnReportView.stepInSentence(incident.missionConfigurationId(), incident.stepId())
            + ". Lost: " + String.join(", ", parts) + ".";
    }

    /**
     * Returns a prospect's words for the dialog's row.
     *
     * @param prospect the prospect
     * @return for example "65 % chance · +4.1 back if found · leaves 0.9 of R&amp;D"
     */
    public static String prospectLine(Investigations.Prospect prospect) {
        String line = String.format(Locale.ROOT, "%d %% chance · %s back if found", Math.round(prospect.chance() * 100),
            String.format(Locale.ROOT, "+%.1f", prospect.recovery()));
        if (prospect.kept() >= 0) {
            return line + String.format(Locale.ROOT, " · leaves training, keeping %d %% of the round",
                Math.round(prospect.kept() * 100));
        }
        return prospect.forgone() > 0.05
            ? line + String.format(Locale.ROOT, " · leaves %.1f of R&D", prospect.forgone()) : line;
    }

    /**
     * Builds a board's report page's words ({@code FrontPage.Kind#REPORT}), shaped as a memorial page's so the
     * paper lays it out the same way.
     *
     * @param page    the printed page
     * @param words   the boards' words
     * @param catalog the content pack, for the mission's name
     * @return the words, or empty if the page carries no report
     */
    public static Optional<MemorialView.Page> reportPage(FrontPage page, InvestigationCauses words,
                                                         ContentCatalog catalog) {
        FrontPage.Report report = page.report();
        if (report == null || report.loss() == null) {
            return Optional.empty();
        }
        FrontPage.Loss loss = report.loss();
        InvestigationCauses.BoardStory story = words.boardStory(page.storyId())
            .orElse(words.boardStories().isEmpty() ? null : words.boardStories().getFirst());
        if (story == null) {
            return Optional.empty();
        }
        String component = ContentNames.of(report.componentId());
        Optional<InvestigationCauses.Cause> cause = words.cause(report.causeId());
        Map<String, String> tokens = new HashMap<>();
        tokens.put("mission", mission(catalog, loss.missionConfigurationId()));
        tokens.put("program", catalog.missionConfiguration(loss.missionConfigurationId())
            .map(c -> ContentNames.of(c.programId())).orElse(ContentNames.of(loss.missionConfigurationId())));
        tokens.put("component", component);
        tokens.put("cause", cause.map(c -> c.shortly()).orElse("a fault"));
        tokens.put("season", GameDate.ofTurn(loss.gameTurn()).label());
        tokens.put("agency", loss.agencyName());
        tokens.put("crew", FrontPageView.join(loss.crew().stream().map(FrontPage.Lost::name).toList()));
        String headline = FrontPageView.fill(story.headline(), tokens).toUpperCase(Locale.ROOT);
        List<String> article = new ArrayList<>();
        article.add(FrontPageView.fill(story.lead(), tokens));
        cause.ifPresent(c -> article.add(fill(c.finding(), component)));
        words.factor(report.factorId()).ifPresent(f -> article.add(f.text()));
        cause.ifPresent(c -> article.add("The changes made since: " + lowerFirst(fill(c.fix(), component))));
        GameDate date = GameDate.ofTurn(page.gameTurn());
        String folio = "VOL. " + FrontPageView.roman(date.year() - GameDate.START_YEAR + 1) + " · No. " + page.gameTurn();
        return Optional.of(new MemorialView.Page(headline, headline, FrontPageView.fill(story.deck(), tokens), folio,
            date.label().toUpperCase(Locale.ROOT), page.weather(), loss.crew(), article, null, List.of(),
            FrontPageView.fill(story.close(), tokens)));
    }

    /**
     * Fills a cause's {@code {component}}.
     *
     * @param text      the words
     * @param component the component's name
     * @return the words filled
     */
    public static String fill(String text, String component) {
        return text.replace("{component}", component);
    }

    private static String lowerFirst(String text) {
        if (text.length() < 2 || Character.isUpperCase(text.charAt(1))) {
            return text;
        }
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String recoveries(InvestigationConcluded concluded) {
        List<String> parts = new ArrayList<>();
        for (InvestigationConcluded.Recovery recovery : concluded.recoveries()) {
            parts.add(String.format(Locale.ROOT, "%s +%.1f", ContentNames.of(recovery.componentId()),
                recovery.points()));
        }
        return parts.isEmpty() ? "nothing, the hardware was already at its best" : String.join(", ", parts);
    }

    private static String points(double points) {
        return String.format(Locale.ROOT, "%.1f %s", points, Math.abs(points - 1.0) < 0.05 ? "point" : "points");
    }

    private static String mission(ContentCatalog catalog, String configurationId) {
        return catalog.missionConfiguration(configurationId).map(MissionControl::missionName)
            .orElse(ContentNames.of(configurationId));
    }
}
