package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.EventStories;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.rules.PrDutyPerformed;
import de.mkoehler.launchwindow.content.SpaceRaceNews;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RandomEventKind;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TrainingKind;
import de.mkoehler.launchwindow.rules.BudgetReviewed;
import de.mkoehler.launchwindow.rules.BuildingUpgraded;
import de.mkoehler.launchwindow.rules.ExpertTrainingCompleted;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.MissionPrestigeChanged;
import de.mkoehler.launchwindow.rules.MissionReliabilityChanged;
import de.mkoehler.launchwindow.rules.MissionSkillGrowthApplied;
import de.mkoehler.launchwindow.rules.MissionStepOutcome;
import de.mkoehler.launchwindow.rules.MissionStepResolved;
import de.mkoehler.launchwindow.rules.PassengerLost;
import de.mkoehler.launchwindow.rules.PersonnelDeparture;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.RandomEventOccurred;
import de.mkoehler.launchwindow.rules.ResearchCompleted;
import de.mkoehler.launchwindow.rules.ResearchComponentMaxed;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.SeasonFundsSettled;
import de.mkoehler.launchwindow.rules.RelationshipShown;
import de.mkoehler.launchwindow.rules.Teams;
import de.mkoehler.launchwindow.rules.TrainingCompleted;
import de.mkoehler.launchwindow.rules.TraitRevealed;
import de.mkoehler.launchwindow.rules.TurnReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The turn report's view logic ({@code ui-spec.md} &sect;4.4): turns a {@link TurnReport}'s facts into
 * the screen's lines. Wording only; every number comes from the report.
 *
 * @author Mario Koehler
 */
public final class TurnReportView {

    /** Idle people at or below this morale get a warning line (unconfirmed, from the mockup). */
    public static final int MORALE_WARNING = PersonnelLifecycle.RESIGNATION_MORALE_FLOOR + 15;

    private TurnReportView() {
    }

    /**
     * One at-a-glance stat.
     *
     * @param label the label
     * @param value the value
     * @param note  a one-line note, or {@code ""}
     * @param tone  1 for good news, -1 for bad, 0 for neutral
     */
    public record Glance(String label, String value, String note, int tone) {
    }

    /**
     * A mission that flew.
     *
     * @param flown    the report's facts about it
     * @param name     its name ("Vostok · Orbital")
     * @param result   what came of it, in a sentence
     * @param outcomes each step's outcome, in order
     * @param prestige the prestige it earned or lost this season
     */
    public record MissionLine(TurnReport.MissionFlown flown, String name, String result,
                              List<MissionStepOutcome> outcomes, long prestige) {
    }

    /**
     * A goal settled.
     *
     * @param goalId   the goal
     * @param first    whether it was first in the game
     * @param how      how it came ("reached on the landing step", "credited through the cascade")
     * @param prestige the prestige it paid
     */
    public record GoalLine(String goalId, boolean first, String how, long prestige) {
    }

    /**
     * A component's reliability across the turn.
     *
     * @param componentId the component
     * @param before      before the turn
     * @param after       after it
     * @param who         who earned it, and what flights added
     */
    public record RndLine(String componentId, double before, double after, String who) {
    }

    /**
     * A line of news, or of people.
     *
     * @param title the lead, or the line itself
     * @param text  more, or {@code ""}
     * @param tag   a short tag ("Retired"), or {@code ""}
     * @param tone     1 for good news, -1 for bad, 0 for neutral
     * @param personId whose page the line opens, or {@code null} for none
     */
    public record Line(String title, String text, String tag, int tone, String personId) {

        /**
         * A line that opens no page.
         *
         * @param title the lead, or the line itself
         * @param text  more, or {@code ""}
         * @param tag   a short tag, or {@code ""}
         * @param tone  1 for good news, -1 for bad, 0 for neutral
         */
        public Line(String title, String text, String tag, int tone) {
            this(title, text, tag, tone, null);
        }
    }

    /**
     * A line of people news ({@code ui-spec.md} &sect;4.4, People canvas, 2026-10-07): a trait that
     * came out, or a relationship that showed.
     *
     * @param text     the line, such as "After 4 seasons of teamwork, Grace Okada turns out to be a team
     *                 player."
     * @param trait    the trait revealed, or {@code null} for a relationship
     * @param positive for a relationship, whether they work well together (else they clash)
     * @param personId whose page the line opens: the person, or the first of the two
     */
    public record PeopleNews(String text, Trait trait, boolean positive, String personId) {
    }

    /**
     * A budget line.
     *
     * @param label  the label
     * @param amount the amount, positive for income
     */
    public record BudgetLine(String label, long amount) {
    }

    private static <T> List<T> events(TurnReport report, Class<T> type) {
        return report.events().stream().filter(type::isInstance).map(type::cast).toList();
    }

    /**
     * Returns the at-a-glance strip.
     *
     * @param report  the report
     * @param catalog the content pack
     * @return the six stats
     */
    public static List<Glance> glance(TurnReport report, ContentCatalog catalog) {
        List<Glance> glance = new ArrayList<>();
        long prestige = report.prestigeAfter() - report.prestigeBefore();
        glance.add(new Glance("Prestige", signed(prestige), String.format(Locale.ROOT, "now %,d", report.prestigeAfter()),
            Long.signum(prestige)));
        long funds = report.fundsAfter() - report.fundsBefore();
        glance.add(new Glance("Funds", signedMoney(funds), "now " + money(report.fundsAfter()), Long.signum(funds)));
        List<MissionLine> missions = missions(report, catalog);
        long failed = missions.stream().filter(line -> ended(line.outcomes())).count();
        long completed = missions.stream().filter(line -> line.flown().ended() && !ended(line.outcomes())).count();
        String missionNote = missions.isEmpty() ? "" : failed > 0 ? failed + " failed"
            : completed == missions.size() ? (missions.size() == 1 ? "completed" : "all completed")
            : completed > 0 ? completed + " completed, " + (missions.size() - completed) + " still flying"
            : missions.size() == 1 ? "still on track" : "all still on track";
        glance.add(new Glance("Missions", missions.isEmpty() ? "None flown" : missions.size() + " flown", missionNote,
            failed > 0 ? -1 : 0));
        List<GoalLine> goals = goals(report);
        long firsts = goals.stream().filter(GoalLine::first).count();
        glance.add(new Glance("Goals", goals.isEmpty() ? "None new" : goals.size() + " new",
            firsts == 0 ? "" : firsts == goals.size() && firsts == 1 ? "first in the game"
                : firsts + " of them first in the game", goals.isEmpty() ? 0 : 1));
        List<PersonnelDeparture> departures = events(report, PersonnelDeparture.class);
        // Poaching moves people both ways (ui-spec.md 4.16): who came over, and who was hired away.
        List<de.mkoehler.launchwindow.rules.OfferSettled> offers = events(report,
            de.mkoehler.launchwindow.rules.OfferSettled.class);
        long arrived = offers.stream().filter(offer -> offer.made() && PoachTexts.moved(offer.outcome())).count();
        long poached = offers.stream().filter(offer -> !offer.made() && PoachTexts.moved(offer.outcome())).count();
        long out = departures.size() + poached;
        String value = arrived > 0 ? arrived + " in · " + out + " out" : out == 0 ? "No one left" : out + " left";
        List<String> notes = new ArrayList<>();
        if (!departures.isEmpty()) {
            notes.add(departureNote(departures));
        }
        if (poached > 0) {
            notes.add(poached + " hired away");
        }
        if (arrived > 0) {
            notes.add(arrived + " from a rival");
        }
        glance.add(new Glance("People", value, String.join(", ", notes), out > 0 ? -1 : arrived > 0 ? 1 : 0));
        Optional<BudgetReviewed> review = events(report, BudgetReviewed.class).stream().findFirst();
        if (review.isPresent()) {
            glance.add(new Glance("Funding review", money(review.get().fundingRate()) + " / season",
                "review #" + review.get().reviewNumber(), 0));
        } else {
            List<RandomEventOccurred> random = events(report, RandomEventOccurred.class);
            long good = random.stream().filter(event -> event.kind().isPositive()).count();
            glance.add(new Glance("Events", random.isEmpty() ? "None" : String.valueOf(random.size()),
                random.isEmpty() ? "a quiet season" : good + " good, " + (random.size() - good) + " bad", 0));
        }
        return glance;
    }

    private static String departureNote(List<PersonnelDeparture> departures) {
        Map<String, Integer> reasons = new LinkedHashMap<>();
        for (PersonnelDeparture departure : departures) {
            reasons.merge(departure.reason().name().toLowerCase(Locale.ROOT).replace('_', ' '), 1, Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        reasons.forEach((reason, count) -> parts.add(count + " " + reason));
        return String.join(", ", parts);
    }

    private static boolean ended(List<MissionStepOutcome> outcomes) {
        if (outcomes.isEmpty()) {
            return false;
        }
        MissionStepOutcome last = outcomes.get(outcomes.size() - 1);
        return last == MissionStepOutcome.FAILURE || last == MissionStepOutcome.CATASTROPHIC;
    }

    /**
     * Returns the missions that flew, with their step outcomes, result and prestige.
     *
     * @param report  the report
     * @param catalog the content pack
     * @return the lines, in launch order
     */
    public static List<MissionLine> missions(TurnReport report, ContentCatalog catalog) {
        List<MissionLine> lines = new ArrayList<>();
        for (TurnReport.MissionFlown flown : report.missions()) {
            List<MissionStepResolved> steps = steps(report, flown.missionInstanceId());
            List<MissionStepOutcome> outcomes = steps.stream().map(MissionStepResolved::outcome).toList();
            long prestige = events(report, MissionPrestigeChanged.class).stream()
                .filter(event -> event.missionInstanceId().equals(flown.missionInstanceId()))
                .mapToLong(MissionPrestigeChanged::applied).sum();
            Optional<MissionConfiguration> configuration = catalog.missionConfiguration(flown.missionConfigurationId());
            String name = configuration.map(MissionControl::missionName).orElse(ContentNames.of(
                flown.missionConfigurationId()));
            if (flown.seasonsTotal() > 1) {
                name += ", season " + (flown.seasonIndex() + 1) + " of " + flown.seasonsTotal();
            }
            lines.add(new MissionLine(flown, name, result(flown, steps), outcomes, prestige));
        }
        return lines;
    }

    /**
     * Returns a mission's step events from the report, in order.
     *
     * @param report            the report
     * @param missionInstanceId the mission
     * @return the steps
     */
    public static List<MissionStepResolved> steps(TurnReport report, String missionInstanceId) {
        return events(report, MissionStepResolved.class).stream()
            .filter(step -> step.missionInstanceId().equals(missionInstanceId)).toList();
    }

    private static String result(TurnReport.MissionFlown flown, List<MissionStepResolved> steps) {
        if (steps.isEmpty()) {
            return "";
        }
        MissionStepResolved last = steps.get(steps.size() - 1);
        String stepName = stepName(last.missionConfigurationId(), last.stepId());
        if (last.outcome() == MissionStepOutcome.CATASTROPHIC) {
            return "Lost on step " + steps.size() + ", " + stepName + ". The crew didn't survive.";
        }
        if (last.outcome() == MissionStepOutcome.FAILURE) {
            return "Failed on step " + steps.size() + ", " + stepName + ".";
        }
        long saved = steps.stream().filter(step -> step.outcome() == MissionStepOutcome.MINOR_GLITCH_SOLVED).count();
        long glitches = steps.stream().filter(step -> step.outcome() == MissionStepOutcome.MINOR_GLITCH).count();
        String head;
        if (flown.ended()) {
            head = flown.crewNames().isEmpty() ? "Completed." : "Completed. " + String.join(" and ", flown.crewNames())
                + (flown.crewNames().size() == 1 ? " is" : " are") + " back safely.";
        } else {
            head = "Season " + (flown.seasonIndex() + 1) + " done. Season " + (flown.seasonIndex() + 2)
                + " flies at the next turn.";
        }
        List<String> details = new ArrayList<>();
        if (saved > 0) {
            details.add(saved + (saved == 1 ? " glitch" : " glitches") + " saved by the controllers");
        }
        if (glitches > 0) {
            details.add(glitches + (glitches == 1 ? " minor glitch" : " minor glitches"));
        }
        return details.isEmpty() ? head : head + " " + capitalised(String.join(", ", details)) + ".";
    }

    /**
     * Names a step from the pack's text ("Launch preparations" for {@code 01-launch-preparations}).
     *
     * @param configurationId the configuration flown, for steps it names on its own
     * @param stepId          the step's id
     * @return the name
     */
    public static String stepName(String configurationId, String stepId) {
        return ContentNames.step(configurationId, stepId);
    }

    /**
     * Names a step for use inside a sentence ("on the retrofire step"): lowercased, except a joint
     * flight's craft prefix ("on the Craft B: retrofire step").
     *
     * @param configurationId the configuration flown
     * @param stepId          the step's id
     * @return the name
     */
    public static String stepInSentence(String configurationId, String stepId) {
        String name = stepName(configurationId, stepId);
        if (name.startsWith("Craft ") && name.indexOf(": ") > 0) {
            int colon = name.indexOf(": ") + 2;
            return name.substring(0, colon) + name.substring(colon).toLowerCase(Locale.ROOT);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the goals settled, with how each came.
     *
     * @param report the report
     * @return the lines, in the order they settled
     */
    public static List<GoalLine> goals(TurnReport report) {
        List<GoalLine> lines = new ArrayList<>();
        MissionStepResolved lastStep = null;
        for (ResolutionEvent event : report.events()) {
            if (event instanceof MissionStepResolved step) {
                lastStep = step;
            } else if (event instanceof GoalSettled goal) {
                String how = goal.cancelled() ? "credited through the cascade"
                    : lastStep == null ? "reached" : "reached on the "
                        + stepInSentence(lastStep.missionConfigurationId(), lastStep.stepId()) + " step";
                lines.add(new GoalLine(goal.goalId(), goal.firstInGame(), how, goal.prestige()));
            }
        }
        return lines;
    }

    /**
     * Returns the buildings that finished an upgrade.
     *
     * @param report the report
     * @return the lines
     */
    public static List<Line> buildings(TurnReport report) {
        return events(report, BuildingUpgraded.class).stream()
            .map(event -> new Line(Buildings.name(event.building()) + " upgraded to Level " + event.newLevel(),
                "now holds " + event.newCap(), "", 1))
            .toList();
    }

    /**
     * Returns each component whose reliability moved, with who earned it.
     *
     * @param report the report
     * @return the lines
     */
    public static List<RndLine> rnd(TurnReport report) {
        List<RndLine> lines = new ArrayList<>();
        for (TurnReport.ComponentChange change : report.components()) {
            List<ResearchCompleted> research = events(report, ResearchCompleted.class).stream()
                .filter(event -> event.componentId().equals(change.componentId())).toList();
            double flights = events(report, MissionReliabilityChanged.class).stream()
                .filter(event -> event.componentId().equals(change.componentId()))
                .mapToDouble(MissionReliabilityChanged::delta).sum();
            List<String> parts = new ArrayList<>();
            if (!research.isEmpty()) {
                List<String> names = research.stream()
                    .map(event -> event.name() + " (slot " + (event.slot() + 1) + ")").toList();
                parts.add(join(names) + ".");
            }
            if (Math.abs(flights) > 1e-9) {
                parts.add(String.format(Locale.ROOT, "Flying %s %.1f.", flights > 0 ? "added" : "cost", Math.abs(flights)));
            }
            lines.add(new RndLine(change.componentId(), change.before(), change.after(), String.join(" ", parts)));
        }
        return lines;
    }

    /**
     * Returns the components that reached their R&amp;D cap, with who went idle.
     *
     * @param report the report
     * @return the lines
     */
    public static List<Line> capped(TurnReport report) {
        return events(report, ResearchComponentMaxed.class).stream()
            .map(event -> new Line(ContentNames.of(event.componentId()) + " reached its R&D cap.",
                join(event.releasedNames()) + (event.releasedNames().size() == 1 ? " is" : " are")
                    + " idle again. Only flying takes it higher now.", "", 1))
            .toList();
    }

    /**
     * Returns the random events, worded.
     *
     * @param report  the report
     * @param catalog the content pack
     * @return the lines
     */
    /**
     * Returns the report's real space-race story, told for the player's archetype: the headline as the
     * title, the story as the text, and that side's view as the tag, empty if the story has none.
     *
     * @param report    the report
     * @param news      the stories
     * @param archetype the player's archetype
     * @return the story's line, or empty if the report has none (or the story is gone)
     */
    public static Optional<Line> realNews(TurnReport report, SpaceRaceNews news, Archetype archetype) {
        return news.byId(report.newsStoryId()).map(story -> new Line(story.headline(), story.text(),
            story.viewFor(archetype).orElse(""), 0));
    }

    public static List<Line> news(TurnReport report, ContentCatalog catalog) {
        return news(report, catalog, Archetype.NASA);
    }

    /**
     * Returns the season's random events as news, each with its story (2026-10-10).
     *
     * @param report    the report
     * @param catalog   the content pack
     * @param archetype the player's agency, for how a department is named
     * @return the lines, in the order they applied
     */
    public static List<Line> news(TurnReport report, ContentCatalog catalog, Archetype archetype) {
        return events(report, RandomEventOccurred.class).stream()
            .map(event -> newsLine(event.kind(), event.subjectId(), event.magnitude(), event.storyId(),
                event.subjectName(), catalog, archetype, true))
            .toList();
    }

    /**
     * Words one random event with its story (the user's request, 2026-10-10): the story's headline
     * as the title, its sentences as the text, and what the event did as the tag. Without a story
     * (one recorded before stories existed, or gone from the file) the event's own name and effect,
     * as before.
     *
     * @param kind        the event
     * @param subjectId   what it struck, or {@code null}
     * @param magnitude   its size
     * @param storyId     its story, or {@code null}
     * @param subjectName the person it struck, as named then, or {@code null}
     * @param catalog     the content pack
     * @param archetype   the player's agency
     * @param inReport    whether it's on the turn report
     * @return the line
     */
    public static Line newsLine(RandomEventKind kind, String subjectId, double magnitude, String storyId,
                                String subjectName, ContentCatalog catalog, Archetype archetype, boolean inReport) {
        Line plain = newsLine(kind, subjectId, magnitude, catalog, inReport);
        if (subjectName != null) {
            // The effect line names whom it struck: "Someone resigned" reads "Anna Kral resigned".
            String named = plain.text().replaceFirst("^(Someone|A trainee|An astronaut)", subjectName);
            plain = new Line(plain.title(), named, plain.tag(), plain.tone());
        }
        Optional<EventStories.Story> story = EventStories.standard().byId(storyId);
        if (story.isEmpty()) {
            return plain;
        }
        String component = catalog.missionComponent(Optional.ofNullable(subjectId).orElse(""))
            .map(found -> ContentNames.of(found.id())).orElse(null);
        String group = groupNoun(subjectId, archetype);
        return new Line(story.get().headlineFor(subjectName, component, group),
            story.get().textFor(subjectName, component, group), plain.text(), plain.tone());
    }

    /** A department as a story names it, from a morale event's subject. */
    private static String groupNoun(String subjectId, Archetype archetype) {
        if (subjectId == null) {
            return null;
        }
        return switch (subjectId) {
            case "ASTRONAUT" -> archetype == Archetype.SOVIET ? "cosmonauts" : "astronauts";
            case "SET" -> "engineers";
            case "MISSION_CONTROL" -> "flight controllers";
            default -> null;
        };
    }

    /**
     * Words one random event, for the turn report's news and the Public Affairs Office's news log.
     *
     * @param kind      the event
     * @param subjectId what it struck (a component id), or {@code null}
     * @param magnitude its size (points, funds or reliability)
     * @param catalog   the content pack, for component names
     * @param inReport  whether it's on the turn report, which can point to its People column
     * @return the title, text and tone
     */
    public static Line newsLine(RandomEventKind kind, String subjectId, double magnitude, ContentCatalog catalog,
                                boolean inReport) {
        int tone = kind.isPositive() ? 1 : -1;
        String subject = catalog.missionComponent(Optional.ofNullable(subjectId).orElse(""))
            .map(component -> ContentNames.of(component.id())).orElse("");
        String people = inReport ? "; see People." : ".";
        String text = switch (kind) {
            case BASIC_TRAINING_EXTENSION -> "A trainee's basic training takes a season longer.";
            case EMPLOYEE_DISMISSED -> "Someone was let go" + people;
            case EMPLOYEE_FAILED_BASIC_TRAINING -> "A trainee failed basic training and left.";
            case EMPLOYEE_RESIGNING -> "Someone resigned" + people;
            case EMPLOYEE_UNAVAILABLE -> String.format(Locale.ROOT, "Someone is unavailable for %.0f %s.", magnitude,
                magnitude == 1 ? "season" : "seasons");
            case MAX_LAUNCHES_ALLOWED -> "Only one launch is allowed this turn.";
            case NO_LAUNCHES_ALLOWED -> "No launches are allowed this turn.";
            case MISSION_COMPONENTS_UNAVAILABLE -> (subject.isEmpty() ? "A component" : subject)
                + " is grounded for a while.";
            case MORALE_DROP -> String.format(Locale.ROOT, "Morale fell by %.0f.", magnitude);
            case MORALE_BOOST -> String.format(Locale.ROOT, "Morale rose by %.0f.", magnitude);
            case PERISHED_ASTRONAUT -> "An astronaut died in an accident" + people;
            case PRESTIGE_DROP -> String.format(Locale.ROOT, "Prestige fell by %.0f %%.", magnitude);
            case PRESTIGE_POINTS_BOOST -> String.format(Locale.ROOT, "Prestige rose by %.0f %%.", magnitude);
            case RD_RESULTS_SETBACK -> String.format(Locale.ROOT, "R&D results are %.0f %% lower for a while.",
                (1 - magnitude) * 100);
            case RD_RESULTS_BOOST -> String.format(Locale.ROOT, "R&D results are %.0f %% higher for a while.",
                (magnitude - 1) * 100);
            case RELIABILITY_SETBACK -> (subject.isEmpty() ? "A component" : subject) + String.format(Locale.ROOT,
                " lost %.1f reliability.", magnitude);
            case UNEXPECTED_COST -> String.format(Locale.ROOT, "An unexpected cost took %.0f %% of the funds.",
                magnitude);
            case UNEXPECTED_COST_AND_NO_LAUNCHES -> String.format(Locale.ROOT, "An unexpected cost took %.0f %% of "
                + "the funds, and no launches this turn.", magnitude);
            case EXTRA_FUNDING -> String.format(Locale.ROOT, "Funding rose by %.0f %% a season.", magnitude);
            case REDUCED_EXPENDITURES -> String.format(Locale.ROOT, "Running costs are %.0f %% lower for a while.",
                (1 - magnitude) * 100);
        };
        return new Line(title(kind), text, "", tone);
    }

    private static String title(RandomEventKind kind) {
        String words = kind.name().toLowerCase(Locale.ROOT).replace('_', ' ').replace("rd ", "R&D ");
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** Where something came out, for a people line: " Noticed on the Atlas team." */
    private static String noticedOn(String teamKind, String subjectId, ContentCatalog catalog) {
        if (subjectId == null) {
            return "";
        }
        if (Teams.MISSION.equals(teamKind)) {
            return catalog.missionConfiguration(subjectId).map(configuration -> " Noticed on "
                + MissionControl.missionName(configuration) + ".").orElse("");
        }
        return " Noticed on the " + ContentNames.of(subjectId) + " team.";
    }

    /**
     * Returns the Intelligence section ({@code design.md} &sect;2.9): the service's level changes,
     * then every finding, in the order they were applied.
     *
     * @param report the report
     * @return the lines; empty when the service did nothing this season
     */
    public static List<Line> intelligence(TurnReport report) {
        List<Line> lines = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof de.mkoehler.launchwindow.rules.IntelligenceLevelChanged changed) {
                String title = "The Intelligence Office " + (changed.to() > changed.from() ? "grew" : "shrank")
                    + " to level " + changed.to() + ", " + IntelTexts.levelName(changed.to());
                String text = changed.debt() ? "The funds didn't cover it, so the service lost a level."
                    : changed.to() == changed.target() ? "It's at its target now."
                    : "Target: level " + changed.target() + ".";
                lines.add(new Line(title, text, changed.debt() ? "Debt" : "Level", changed.debt() ? -1 : 0));
            }
        }
        for (ResolutionEvent event : report.events()) {
            if (event instanceof de.mkoehler.launchwindow.rules.IntelligenceFound found) {
                String[] text = IntelTexts.finding(found.finding());
                lines.add(new Line(text[0], text[1], found.finding().kind().name(),
                    IntelTexts.tone(found.finding())));
            }
        }
        return lines;
    }

    /**
     * Returns the people news: traits that came out and relationships that showed, in the order
     * they happened.
     *
     * @param report  the report
     * @param catalog the content pack
     * @return the lines
     */
    public static List<PeopleNews> peopleNews(TurnReport report, ContentCatalog catalog) {
        List<PeopleNews> lines = new ArrayList<>();
        for (ResolutionEvent event : report.events()) {
            if (event instanceof TraitRevealed revealed) {
                lines.add(new PeopleNews("After " + seasonCount(revealed.workSeasons()) + " of teamwork, " + revealed.name()
                    + " turns out to be " + TraitTexts.inSentence(revealed.trait()) + "."
                    + noticedOn(revealed.teamKind(), revealed.subjectId(), catalog), revealed.trait(), false,
                    revealed.personnelId()));
            } else if (event instanceof RelationshipShown shown) {
                String after = shown.seasonsTogether() > 0
                    ? ", after " + seasonCount(shown.seasonsTogether()) + " side by side" : "";
                lines.add(new PeopleNews(shown.firstName() + " and " + shown.secondName()
                    + (shown.positive() ? " work well together" : " clash") + after + "."
                    + noticedOn(shown.teamKind(), shown.subjectId(), catalog), null, shown.positive(),
                    shown.firstId()));
            }
        }
        return lines;
    }

    private static String seasonCount(int count) {
        return count + (count == 1 ? " season" : " seasons");
    }

    /**
     * Returns the people lines: Experts earned, training finished, departures, flight growth, and
     * morale warnings for idle people now (derived here, unconfirmed). Traits and relationships are
     * {@link #peopleNews}.
     *
     * @param report  the report
     * @param state   the player's state now, or {@code null} for no morale warnings (an older report)
     * @param catalog the content pack
     * @return the lines
     */
    public static List<Line> people(TurnReport report, PlayerSnapshot state, ContentCatalog catalog) {
        List<Line> lines = new ArrayList<>();
        // Poaching leads the section: offers to our people, then ours (ui-spec.md 4.16).
        events(report, de.mkoehler.launchwindow.rules.OfferSettled.class).stream()
            .sorted(java.util.Comparator.comparing(de.mkoehler.launchwindow.rules.OfferSettled::made))
            .forEach(offer -> lines.add(PoachTexts.reportLine(offer)));
        for (ResolutionEvent event : report.events()) {
            if (event instanceof ExpertTrainingCompleted expert) {
                lines.add(new Line(expert.name() + " earned the Expert trait in " + skillLabel(expert.skillName()), "",
                    "Expert", 1));
            } else if (event instanceof TrainingCompleted training) {
                lines.add(new Line(String.format(Locale.ROOT, "%s finished %s training: %s + %.1f", training.name(),
                    kindLabel(training.trainingKind()), skillLabel(training.skillName()), training.skillGain()), "",
                    "Training", 0));
            } else if (event instanceof PersonnelDeparture departure) {
                lines.add(departure(departure));
            } else if (event instanceof PrDutyPerformed pr) {
                lines.add(prLine(pr));
            } else if (event instanceof de.mkoehler.launchwindow.rules.AmbitionSettled ambition) {
                String[] text = CareerTexts.report(ambition);
                lines.add(new Line(text[0], text[1], "Ambition", CareerTexts.tone(ambition)));
            } else if (event instanceof de.mkoehler.launchwindow.rules.CrewedFlightsGrounded grounded) {
                lines.add(new Line("Crewed flights grounded", "After the loss of the crew of "
                    + ContentNames.of(grounded.missionConfigurationId()) + ", no crewed flight may launch for the next "
                    + grounded.seasons() + " turns while the accident is investigated. Uncrewed flights go on.",
                    "Grounded", -1));
            } else if (event instanceof de.mkoehler.launchwindow.rules.GoalsWithheld withheld) {
                // A goal counts only if the crew comes home (design.md 2.6).
                lines.add(new Line("No goals for " + ContentNames.of(withheld.missionConfigurationId()),
                    "The crew did not come home, so the flight earns none of the goals it reached: "
                        + String.join(", ", withheld.goalIds().stream().map(ContentNames::of).toList()) + ".",
                    "Not credited", -1));
            } else if (event instanceof PassengerLost lost) {
                lines.add(new Line(lost.name() + " did not come home", lost.failedStepId() == null
                    ? "The flight had no way home."
                    : "Lost at " + stepInSentence(lost.missionConfigurationId(), lost.failedStepId()) + ".",
                    "Lost", -1));
            }
        }
        Map<String, List<MissionSkillGrowthApplied>> growth = new LinkedHashMap<>();
        for (MissionSkillGrowthApplied applied : events(report, MissionSkillGrowthApplied.class)) {
            growth.computeIfAbsent(applied.missionInstanceId(), key -> new ArrayList<>()).add(applied);
        }
        for (Map.Entry<String, List<MissionSkillGrowthApplied>> mission : growth.entrySet()) {
            String name = report.missions().stream()
                .filter(flown -> flown.missionInstanceId().equals(mission.getKey())).findFirst()
                .flatMap(flown -> catalog.missionConfiguration(flown.missionConfigurationId()))
                .map(MissionControl::missionName).orElse("the mission");
            List<MissionSkillGrowthApplied> crew = mission.getValue().stream()
                .filter(applied -> applied.group() == PersonnelGroup.ASTRONAUT).toList();
            long controllers = mission.getValue().size() - crew.size();
            for (MissionSkillGrowthApplied astronaut : crew) {
                lines.add(new Line(String.format(Locale.ROOT, "%s grew %.1f in skill on %s", astronaut.name(),
                    astronaut.totalSkillGain(), name), "", "Flight", 1));
            }
            if (controllers > 0) {
                lines.add(new Line(controllers + (controllers == 1 ? " controller" : " controllers")
                    + " grew in skill on " + name, "", "Flight", 1));
            }
        }
        if (state == null) {
            return lines;
        }
        Stream.of(state.getAstronauts(), state.getSetSpecialists(), state.getMissionControlOfficers())
            .flatMap(List::stream)
            .filter(person -> person.getAssignmentState() == AssignmentState.IDLE && person.getMorale() <= MORALE_WARNING)
            .sorted(java.util.Comparator.comparingInt(Personnel::getMorale))
            .limit(3)
            .forEach(person -> lines.add(new Line(person.getName() + "'s morale is down to " + person.getMorale()
                + ": may resign if left idle", "", "Warning", -1)));
        return lines;
    }

    /**
     * Words a season of PR duty: the press story where the papers picked the tour up, what else it
     * did, the prestige, and any rust.
     */
    private static Line prLine(PrDutyPerformed pr) {
        String rust = pr.rust() > 0 ? String.format(Locale.ROOT, " Out of practice: Piloting and Fitness -%.0f.",
            pr.rust()) : "";
        String earned = (pr.prestige() < 0 ? "−" : "+") + Math.abs(pr.prestige()) + " prestige";
        StringBuilder effects = new StringBuilder();
        for (PrPress.Effect effect : pr.pressEffects()) {
            String words = pressEffect(effect, pr.name());
            if (!words.isEmpty()) {
                effects.append(' ').append(words);
            }
        }
        return PrPress.standard().byId(pr.pressStoryId())
            .map(story -> new Line(story.headlineFor(pr.name()) + ": " + earned,
                story.textFor(pr.name()) + effects + rust, "PR", story.tone() == PrPress.Tone.GOOD ? 1 : -1))
            .orElseGet(() -> new Line(pr.name() + "'s PR tour: " + earned, rust.strip(), "PR",
                pr.prestige() > 0 ? 1 : 0));
    }

    /**
     * Words one applied press-story effect as a sentence ("3 more SET candidates in the pool for two
     * seasons."), from the values the season pass stored.
     *
     * @param effect the effect as applied
     * @param name   the astronaut in the story
     * @return the sentence, or "" for an effect the prestige already shows
     */
    public static String pressEffect(PrPress.Effect effect, String name) {
        long amount = Math.round(effect.amount());
        return switch (effect.kind()) {
            case PRESTIGE_SHARE -> "";
            case HIRING_POOL -> amount + " more " + candidates(effect.target(), amount) + " in the pool "
                + seasons(effect.seasons()) + ".";
            case STANDOUT_CANDIDATE -> (amount == 1 ? "An outstanding " + candidates(effect.target(), 1) + " is"
                : amount + " outstanding " + candidates(effect.target(), amount) + " are") + " in the pool "
                + seasons(effect.seasons()) + ".";
            case GROUP_MORALE -> groupLabel(effect.target()) + " morale " + tightSigned(amount) + ".";
            case ASTRONAUT_MORALE -> name + "'s morale " + tightSigned(amount) + ".";
            case ASTRONAUT_SKILL -> name + "'s " + skillLabel(effect.target()) + " " + tightSigned(amount) + ".";
            case FUNDS -> "Funds " + signedMoney(amount) + ".";
            case FUNDING_RATE -> "Funding " + signedMoney(amount) + " a season until the next review.";
            case RD_RESULTS -> "R&D results " + tightSigned(Math.round((effect.amount() - 1) * 100)) + "% "
                + seasons(effect.seasons()) + ".";
            case RUNNING_COSTS -> "Running costs " + tightSigned(Math.round((effect.amount() - 1) * 100)) + "% "
                + seasons(effect.seasons()) + ".";
            case RESEARCH_SETBACK -> String.format(Locale.ROOT, "%s lost %.1f reliability.",
                effect.target() == null ? "A component" : ContentNames.of(effect.target()), effect.amount());
        };
    }

    private static String candidates(String group, long count) {
        String noun = switch (group == null ? "" : group) {
            case "ASTRONAUT" -> "astronaut candidate";
            case "MISSION_CONTROL" -> "controller candidate";
            default -> "SET candidate";
        };
        return count == 1 ? noun : noun + "s";
    }

    private static String groupLabel(String group) {
        return switch (group == null ? "" : group) {
            case "ASTRONAUT" -> "Astronaut";
            case "MISSION_CONTROL" -> "Mission Control";
            default -> "SET";
        };
    }

    private static String seasons(int seasons) {
        return switch (seasons) {
            case 0, 1 -> "this season";
            case 2 -> "for two seasons";
            case 3 -> "for three seasons";
            default -> "for " + seasons + " seasons";
        };
    }

    private static String tightSigned(long value) {
        return (value < 0 ? "−" : "+") + Math.abs(value);
    }

    private static Line departure(PersonnelDeparture departure) {
        String group = switch (departure.group()) {
            case ASTRONAUT -> "astronaut";
            case SET -> "SET";
            case MISSION_CONTROL -> "controller";
        };
        String who = departure.name() + " (" + group + ")";
        return switch (departure.reason()) {
            case RETIRED -> new Line(who + " retired at " + departure.ageAtExit(), "", "Retired", 0);
            case RESIGNED -> new Line(who + " resigned, with morale at " + departure.moraleAtExit(), "", "Resigned", -1);
            case DISMISSED -> new Line(who + " was let go", "", "Left", -1);
            case FAILED_BASIC_TRAINING -> new Line(who + " failed basic training and left", "", "Left", -1);
            default -> new Line(who + " was lost", "", "Lost", -1);
        };
    }

    static String skillLabel(String skill) {
        String crew = CrewSkills.label(skill);
        try {
            return SetSkills.label(de.mkoehler.launchwindow.model.SetSkill.valueOf(skill));
        } catch (IllegalArgumentException notSet) {
            return crew;
        }
    }

    private static String kindLabel(TrainingKind kind) {
        String name = kind.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Returns the budget lines: funding, then the running costs by kind.
     *
     * @param report the report
     * @return the lines, empty if the season settled no funds
     */
    public static List<BudgetLine> budget(TurnReport report) {
        Optional<SeasonFundsSettled> settled = events(report, SeasonFundsSettled.class).stream().findFirst();
        if (settled.isEmpty()) {
            return List.of();
        }
        boolean reviewed = !events(report, BudgetReviewed.class).isEmpty()
            || !events(report, de.mkoehler.launchwindow.rules.FundingRaised.class).isEmpty();
        List<BudgetLine> lines = new ArrayList<>();
        lines.add(new BudgetLine(reviewed ? "Funding (new rate)" : "Funding", settled.get().income()));
        if (settled.get().costs() != null) {
            var costs = settled.get().costs();
            lines.add(new BudgetLine("Salaries (" + costs.employees() + (costs.employees() == 1 ? " person)"
                : " people)"), -costs.salaries()));
            lines.add(new BudgetLine("Component maintenance (" + costs.componentCount() + ")", -costs.components()));
            lines.add(new BudgetLine("Program upkeep (" + costs.programCount() + ")", -costs.programs()));
            lines.add(new BudgetLine("Building upkeep", -costs.buildings()));
            if (costs.intelligence() != 0) {
                lines.add(new BudgetLine("Intelligence Office", -costs.intelligence()));
            }
        } else {
            lines.add(new BudgetLine("Running costs", -settled.get().expenses()));
        }
        return lines;
    }

    /**
     * Returns the season's net: funding less running costs.
     *
     * @param report the report
     * @return the net
     */
    public static long net(TurnReport report) {
        return events(report, SeasonFundsSettled.class).stream().findFirst()
            .map(settled -> settled.income() - settled.expenses()).orElse(0L);
    }

    /**
     * Words a funding review, if one happened.
     *
     * @param report the report
     * @return the title and text, or empty
     */
    public static Optional<Line> review(TurnReport report) {
        return events(report, BudgetReviewed.class).stream().findFirst()
            .map(review -> new Line("Funding review #" + review.reviewNumber() + ".", String.format(Locale.ROOT,
                "At %,d prestige%s, funding is now %s a season until the next review, %d seasons from now.",
                review.prestige(), review.rivalCredit() > 0 ? String.format(Locale.ROOT,
                    " and %,d more for rivals' Firsts", review.rivalCredit()) : "", money(review.fundingRate()),
                de.mkoehler.launchwindow.rules.BudgetProgress.SEASONS_PER_REVIEW), "", 0));
    }

    /**
     * Words a raise of the funding between reviews ({@code design.md} &sect;2.1(f)), if one happened.
     *
     * @param report the report
     * @return the title and text, or empty
     */
    public static Optional<Line> raise(TurnReport report) {
        return events(report, de.mkoehler.launchwindow.rules.FundingRaised.class).stream().findFirst()
            .map(raised -> new Line("Funding raised: " + signedMoney(raised.after() - raised.before()) + " a season.",
                String.format(Locale.ROOT, "Your prestige grew to %,d, and review #%d's bracket pays for it now: "
                    + "funding rises from %s to %s a season. It won't fall before the next review.",
                    raised.prestige(), raised.reviewNumber(), money(raised.before()), money(raised.after())), "", 1));
    }

    /**
     * Words the government's answer to rivals' Firsts last season ({@code design.md} &sect;2.1(e)), if
     * any counted.
     *
     * @param report the report
     * @return the title and text, or empty
     */
    public static Optional<Line> rivalFirsts(TurnReport report) {
        return events(report, de.mkoehler.launchwindow.rules.RivalFirstsAnswered.class).stream().findFirst()
            .map(answered -> {
                boolean reviewed = events(report, BudgetReviewed.class).stream()
                    .anyMatch(review -> review.rivalCredit() > 0);
                String firsts = join(answered.goalIds().stream().map(ContentNames::of).toList());
                String counted = reviewed
                    ? String.format(Locale.ROOT, "This season's review counted %,d more prestige for rivals' Firsts.",
                        answered.creditTotal())
                    : answered.credit() == answered.creditTotal()
                    ? String.format(Locale.ROOT, "Your next funding review counts %,d more prestige.",
                        answered.credit())
                    : String.format(Locale.ROOT, "Your next funding review counts %,d more prestige, %,d in all.",
                        answered.credit(), answered.creditTotal());
                return new Line("Beaten to it: the government answers.", "Another agency was first: "
                    + firsts + ". " + counted, "", 1);
            });
    }

    private static String join(List<String> names) {
        if (names.size() <= 1) {
            return String.join("", names);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    private static String capitalised(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Words a signed whole number ("+ 200", "− 75").
     *
     * @param amount the amount
     * @return the words
     */
    public static String signed(long amount) {
        return (amount < 0 ? "− " : "+ ") + String.format(Locale.ROOT, "%,d", Math.abs(amount));
    }

    /**
     * Words a signed amount of money ("+ $ 2,410.0").
     *
     * @param amount the amount
     * @return the words
     */
    public static String signedMoney(long amount) {
        return (amount < 0 ? "− " : "+ ") + money(Math.abs(amount));
    }

    private static String money(long amount) {
        return String.format(Locale.ROOT, "$ %,.1f", (double) amount);
    }
}
