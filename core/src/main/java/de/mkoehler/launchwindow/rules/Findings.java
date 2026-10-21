package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentStrings;
import de.mkoehler.launchwindow.content.Finding;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * <p>Findings and publishing them ({@code design.md} &sect;2.13, the user's idea, 2026-10-19): the first
 * cooperation. A finding is a science goal the pack lists in {@code findings.json}, with the steps its
 * result makes safer. An agency knows a finding when it holds the goal (achieved or credited through a
 * cascade) or when any agency has published it; each known finding that matches a step adds
 * {@link #BONUS_POINTS} to the step's base chance, and bonuses from different findings stack. A finding
 * nobody knows never hurts: BASPM's odds stay the baseline.</p>
 *
 * <p>Publishing is something a player does, not a policy: an agency that holds a finding may ask to
 * publish it in any turn until anyone has, and the asks are settled once a turn for every agency
 * ({@link FindingPublications}). A publication pays the publisher {@link #PRESTIGE_SHARE} of the goal's
 * first-time prestige, scaled by the share of its rivals it helped (those that didn't know it), split
 * between agencies that published the same finding at the same resolution.</p>
 *
 * @author Mario Koehler
 */
public final class Findings {

    /** The base-chance points a known finding adds to each step it matches (user, 2026-10-21). */
    public static final double BONUS_POINTS = 4.0;

    /** The most a publication pays, as a share of the goal's first-time prestige (user, 2026-10-21). */
    public static final double PRESTIGE_SHARE = 0.33;

    private static double bonusPoints = BONUS_POINTS;

    /** What asking to publish, or withdrawing the ask, came to. */
    public enum PublishOutcome {
        /** Asked: it's published at the next resolution. */
        QUEUED,
        /** The ask is withdrawn. */
        WITHDRAWN,
        /** The goal isn't a finding. */
        NOT_A_FINDING,
        /** The agency doesn't hold the goal. */
        NOT_KNOWN,
        /** Someone published it already. */
        ALREADY_PUBLIC,
        /** It's asked for already. */
        ALREADY_QUEUED,
        /** There's no ask to withdraw. */
        NOT_QUEUED
    }

    private Findings() {
    }

    /**
     * Returns the bonus in force, {@link #BONUS_POINTS} unless a playout changed it.
     *
     * @return the points per known finding
     */
    public static double bonusPoints() {
        return bonusPoints;
    }

    /**
     * Sets the bonus, for playouts that measure the rule (0 turns it off); put {@link #BONUS_POINTS} back after.
     *
     * @param value the points, from 0
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public static void setBonusPoints(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("the bonus must not be negative: " + value);
        }
        bonusPoints = value;
    }

    /**
     * A step's kind, as findings name it: its id without the pack's numbering and without the
     * a/b of a two-craft flight ({@code 05-gemini-a-reentry} and {@code 07-reentry-b} are both
     * {@code reentry}).
     *
     * @param stepId the step's id
     * @return its kind
     */
    public static String kind(String stepId) {
        String kind = ContentStrings.stepType(stepId).replaceFirst("^(gemini|spc)-[ab]-", "");
        return kind.replaceFirst("-[ab]$", "");
    }

    /**
     * The body a configuration's flights are browsed under: its program's category's.
     *
     * @param catalog       the content pack
     * @param configuration the configuration
     * @return the body, Earth when the pack names none
     */
    public static CelestialBody body(ContentCatalog catalog, MissionConfiguration configuration) {
        return catalog.program(configuration.programId())
            .flatMap(program -> catalog.category(program.categoryId()))
            .map(ProgramCategory::body)
            .orElse(CelestialBody.EARTH);
    }

    /**
     * The findings that match a step, known or not.
     *
     * @param catalog       the content pack
     * @param configuration the step's configuration
     * @param step          the step
     * @return the findings' goals, in the pack's order
     */
    public static List<String> matching(ContentCatalog catalog, MissionConfiguration configuration, MissionStep step) {
        Objects.requireNonNull(catalog, "catalog must not be null");
        if (catalog.findings().isEmpty()) {
            return List.of();
        }
        String kind = kind(step.id());
        CelestialBody body = body(catalog, configuration);
        boolean crewed = !step.crewInvolvements().isEmpty();
        List<String> found = new ArrayList<>();
        for (Finding finding : catalog.findings()) {
            if (finding.matches().stream().anyMatch(match -> match.matches(kind, body, crewed))) {
                found.add(finding.goalId());
            }
        }
        return found;
    }

    /**
     * The known findings that make a step safer.
     *
     * @param catalog       the content pack
     * @param configuration the step's configuration
     * @param step          the step
     * @param known         whether the agency knows a finding, by goal ({@link #knowing})
     * @return the findings' goals, in the pack's order
     */
    public static List<String> applying(ContentCatalog catalog, MissionConfiguration configuration, MissionStep step,
            Predicate<String> known) {
        return matching(catalog, configuration, step).stream().filter(known).toList();
    }

    /**
     * The base-chance points known findings add to a step: {@link #bonusPoints()} for each.
     *
     * @param catalog       the content pack
     * @param configuration the step's configuration
     * @param step          the step
     * @param known         whether the agency knows a finding, by goal ({@link #knowing})
     * @return the points, from 0
     */
    public static double stepBonus(ContentCatalog catalog, MissionConfiguration configuration, MissionStep step,
            Predicate<String> known) {
        return applying(catalog, configuration, step, known).size() * bonusPoints;
    }

    /**
     * Whether an agency knows a finding as things stand: the goals it holds, and every published one.
     *
     * @param snapshot the agency
     * @param held     the goals it holds, as the caller counts them (a flight in progress holds the
     *                 goals its earlier steps earned)
     * @return the test, by goal
     */
    public static Predicate<String> knowing(PlayerSnapshot snapshot, Predicate<String> held) {
        return goalId -> held.test(goalId) || snapshot.getPublicFindings().containsKey(goalId);
    }

    /**
     * Whether an agency knows a finding.
     *
     * @param snapshot the agency
     * @param goalId   the finding
     * @return whether it holds the goal or the finding is public
     */
    public static boolean knows(PlayerSnapshot snapshot, String goalId) {
        return knowing(snapshot, snapshot::hasAchievedOrCancelledGoal).test(goalId);
    }

    /**
     * Whether an agency may ask to publish a finding now: it holds the goal, nobody has published it,
     * and it hasn't asked already.
     *
     * @param snapshot the agency
     * @param goalId   the goal
     * @param catalog  the content pack
     * @return whether a Publish button belongs on it
     */
    public static boolean publishable(PlayerSnapshot snapshot, String goalId, ContentCatalog catalog) {
        return catalog.finding(goalId).isPresent() && snapshot.hasAchievedOrCancelledGoal(goalId)
            && !snapshot.getPublicFindings().containsKey(goalId) && !snapshot.getPendingPublications().contains(goalId);
    }

    /**
     * Asks to publish a finding at the next resolution, or withdraws the ask ({@code PublishFindingAction}).
     *
     * @param snapshot the agency; changed only when the outcome is {@code QUEUED} or {@code WITHDRAWN}
     * @param goalId   the finding
     * @param publish  {@code true} to ask, {@code false} to withdraw
     * @param catalog  the content pack
     * @return the outcome
     */
    public static PublishOutcome publish(PlayerSnapshot snapshot, String goalId, boolean publish,
            ContentCatalog catalog) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        if (goalId == null || catalog.finding(goalId).isEmpty()) {
            return PublishOutcome.NOT_A_FINDING;
        }
        if (!publish) {
            return snapshot.getPendingPublications().remove(goalId) ? PublishOutcome.WITHDRAWN
                : PublishOutcome.NOT_QUEUED;
        }
        if (snapshot.getPublicFindings().containsKey(goalId)) {
            return PublishOutcome.ALREADY_PUBLIC;
        }
        if (!snapshot.hasAchievedOrCancelledGoal(goalId)) {
            return PublishOutcome.NOT_KNOWN;
        }
        if (snapshot.getPendingPublications().contains(goalId)) {
            return PublishOutcome.ALREADY_QUEUED;
        }
        snapshot.getPendingPublications().add(goalId);
        return PublishOutcome.QUEUED;
    }

    /**
     * What a publication pays in all: {@link #PRESTIGE_SHARE} of the goal's first-time prestige, times
     * the share of rivals it helped. Rounded; nothing in a game without rivals.
     *
     * @param goal   the finding's goal
     * @param helped the rivals that didn't know it
     * @param rivals every rival
     * @return the prestige, before it's split between publishers
     */
    public static long payment(Goal goal, int helped, int rivals) {
        if (rivals <= 0 || helped <= 0) {
            return 0;
        }
        return Math.round(goal.firstTimePrestige() * PRESTIGE_SHARE * Math.min(helped, rivals) / rivals);
    }

    /**
     * One agency's part of the turn's publications: makes them public, pays its own, and clears its asks.
     * Runs before the budget, so a review this season counts the prestige.
     *
     * @param snapshot     the agency
     * @param publications its part of the turn's publications ({@link FindingPublications.Settlement#turnFor})
     * @param gameTurn     the turn
     * @return a {@link FindingPublished} for each of its own, a {@link FindingLearned} for each of the others'
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot,
            List<FindingPublications.News> publications, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        for (FindingPublications.News news : publications == null ? List.<FindingPublications.News>of()
                : publications) {
            boolean knewIt = knows(snapshot, news.goalId());
            snapshot.getPublicFindings().putIfAbsent(news.goalId(), gameTurn);
            if (news.byYou()) {
                snapshot.setPrestige(snapshot.getPrestige() + news.prestige());
                if (!snapshot.getPublishedFindings().contains(news.goalId())) {
                    snapshot.getPublishedFindings().add(news.goalId());
                }
                events.add(new FindingPublished(news.goalId(), news.prestige(), news.helped(), news.rivals(),
                    news.publishers()));
            } else {
                events.add(new FindingLearned(news.goalId(), news.publishers(), knewIt));
            }
        }
        // Every ask was settled this turn: a published one above, one that came too late to nothing.
        snapshot.getPendingPublications().clear();
        return events;
    }
}
