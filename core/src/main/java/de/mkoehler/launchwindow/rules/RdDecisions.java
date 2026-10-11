package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.DecisionEffect;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Rapport;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.SplittableRandom;

/**
 * R&amp;D decisions ({@code ui-spec.md} component 26, the user's idea, 2026-10-10): choices about one
 * component under R&amp;D, put to the player mostly in the lulls between flights, decided whenever the
 * player likes during the turn and worked out at once.
 *
 * <p><b>Offered</b> when a turn resolves ({@link #advanceSeason}, after R&amp;D): near certain in a lull
 * (no mission in flight, and a component with engineers on it more than a season from
 * {@link #READY_RELIABILITY}, or {@link #WAITING_SEASONS} seasons and more since the last flight),
 * rarely otherwise, and growing with the seasons since the last flight.
 * One at a time; none after a lapse until the player acts again. A story arc's next step waits for
 * the next lull. Every draw uses its own salted seed, so the season's other draws are unchanged.</p>
 *
 * <p><b>Decided</b> by {@link #decide}: the option's outcome is drawn from the decision's own seed,
 * fixed when it was offered, so it can't be rerolled and a replay of the journal draws the same.
 * <b>Settled</b> at the next resolution: a decision still open applies its default.</p>
 *
 * <p>Sizes are relative: a reliability effect is seasons of the component's expected R&amp;D gain
 * ({@link ResearchPlanning#expectedSeasonGain}), a cost or funds effect a share of a season's
 * funding.</p>
 *
 * @author Mario Koehler
 */
public final class RdDecisions {

    /** The reliability a component counts as ready at; a lull is waiting for it. */
    public static final double READY_RELIABILITY = 60;

    /** Seasons without a flight, with nothing in flight, that make a lull whatever the hardware. */
    public static final int WAITING_SEASONS = 2;

    /** The chance of a decision in a lull. */
    public static final double LULL_CHANCE = 0.85;

    /** The chance otherwise, at least, and what each season since the last flight adds, and the most. */
    public static final double QUIET_CHANCE_MIN = 0.05;
    /** What each season since the last flight adds to the chance outside a lull. */
    public static final double QUIET_CHANCE_PER_SEASON = 0.02;
    /** The most the chance reaches outside a lull. */
    public static final double QUIET_CHANCE_MAX = 0.35;

    /** The smallest season's gain a reliability effect is sized from, for a component nobody works on. */
    static final double MIN_SEASON_GAIN = 1.0;

    private static final long OFFER_SALT = 0x0DEC_1510L;

    private RdDecisions() {
    }

    /** What deciding did. */
    public enum Outcome {
        /** Decided; the outcome is on the pending decision. */
        DECIDED,
        /** No decision is waiting. */
        NO_DECISION,
        /** It's decided already. */
        ALREADY_DECIDED,
        /** The decision or the option isn't known. */
        UNKNOWN_OPTION,
        /** The component isn't owned and active any more. */
        COMPONENT_GONE,
        /** The person isn't free for this option, or the pair isn't two people who could work on it. */
        PERSON_NOT_FREE,
        /** Nobody works on the component any more, so there's no R&amp;D for the decision to act on. */
        NO_TEAM,
        /** The funds don't cover the option's cost. */
        INSUFFICIENT_FUNDS,
        /** A hands-on decision's puzzle started; it settles when it ends or the turn resolves. */
        STARTED,
        /** No hands-on puzzle is in progress. */
        NO_PUZZLE
    }

    // ---- Who an option can take

    /**
     * Returns the engineers who could oversee: idle and available, best first in the component's
     * skill.
     *
     * @param snapshot  the player
     * @param component the component
     * @return the engineers
     */
    public static List<SetSpecialist> overseers(PlayerSnapshot snapshot, MissionComponent component) {
        return snapshot.getSetSpecialists().stream()
            .filter(engineer -> engineer.getAssignmentState() == AssignmentState.IDLE
                && engineer.getUnavailableSeasons() == 0)
            .sorted(Comparator.comparingDouble((SetSpecialist e) -> e.getSkill(component.associatedSkill())).reversed())
            .toList();
    }

    /**
     * Returns the component's R&amp;D team.
     *
     * @param snapshot    the player
     * @param componentId the component
     * @return the engineers on it
     */
    public static List<SetSpecialist> team(PlayerSnapshot snapshot, String componentId) {
        return snapshot.getSetSpecialists().stream()
            .filter(engineer -> engineer.getAssignmentState() == AssignmentState.ASSIGNED
                && componentId.equals(engineer.getRdTargetComponentId()))
            .toList();
    }

    /**
     * Returns the engineers who could be one of a pair: the team, then the idle ones.
     *
     * @param snapshot  the player
     * @param component the component
     * @return the engineers
     */
    public static List<SetSpecialist> pairable(PlayerSnapshot snapshot, MissionComponent component) {
        List<SetSpecialist> people = new ArrayList<>(team(snapshot, component.id()));
        people.addAll(overseers(snapshot, component));
        return people;
    }

    // ---- Odds

    /**
     * Returns the chance of an option's good outcomes with the people picked, for the odds that
     * depend on them: an overseer's skill, a pair's rapport, the Intelligence Office's level. For
     * fixed odds, the good outcomes' share of the weights.
     *
     * @param snapshot  the player
     * @param catalog   the content
     * @param pending   the decision
     * @param option    the option
     * @param person    the person picked, or {@code null}
     * @param second    the second of a pair, or {@code null}
     * @return the chance, 0 to 1
     */
    public static double goodChance(PlayerSnapshot snapshot, ContentCatalog catalog, PendingDecision pending,
                                    Decisions.Option option, String person, String second) {
        double[] weights = weights(snapshot, catalog, pending, option, person, second);
        double good = 0;
        double total = 0;
        for (int i = 0; i < weights.length; i++) {
            total += weights[i];
            if (option.outcomes().get(i).tone() == Decisions.Tone.GOOD) {
                good += weights[i];
            }
        }
        return total <= 0 ? 0 : good / total;
    }

    /** The outcomes' weights for this option and people, normalised to sum to 1. */
    private static double[] weights(PlayerSnapshot snapshot, ContentCatalog catalog, PendingDecision pending,
                                    Decisions.Option option, String person, String second) {
        List<Decisions.Outcome> outcomes = option.outcomes();
        double[] weights = new double[outcomes.size()];
        List<SetSpecialist> team = new ArrayList<>(team(snapshot, pending.componentId()));
        for (String id : new String[] {person, second}) {
            SetSpecialist picked = engineer(snapshot, id);
            if (picked != null && !team.contains(picked)) {
                team.add(picked);
            }
        }
        for (int i = 0; i < weights.length; i++) {
            Decisions.Outcome outcome = outcomes.get(i);
            double weight = Math.max(0, outcome.weight());
            if (outcome.favouredBy() != null && team.stream().anyMatch(e -> Personality.has(e, outcome.favouredBy()))) {
                weight *= 2;
            }
            weights[i] = weight;
        }
        normalise(weights);
        double good = switch (option.odds()) {
            case FIXED -> Double.NaN;
            case SKILL -> {
                SetSpecialist overseer = engineer(snapshot, person);
                MissionComponent component = catalog.missionComponent(pending.componentId()).orElse(null);
                double skill = overseer == null || component == null ? 0
                    : overseer.getSkill(component.associatedSkill());
                yield clamp(0.25 + 0.007 * skill, 0.3, 0.95);
            }
            case RAPPORT -> {
                Rapport pair = person == null || second == null ? null : Personality.rapport(snapshot, person, second);
                yield clamp(0.5 + (pair == null ? 0 : pair.rapport()) / 200.0, 0.15, 0.9);
            }
            case INTEL -> clamp(0.35 + 0.15 * snapshot.getIntelligence().getLevel(), 0.35, 0.9);
        };
        if (!Double.isNaN(good)) {
            // The good outcomes share the chance by their weights, the rest share what's left.
            double goodWeight = 0;
            for (int i = 0; i < weights.length; i++) {
                if (outcomes.get(i).tone() == Decisions.Tone.GOOD) {
                    goodWeight += weights[i];
                }
            }
            double restWeight = 1 - goodWeight;
            for (int i = 0; i < weights.length; i++) {
                boolean isGood = outcomes.get(i).tone() == Decisions.Tone.GOOD;
                weights[i] = isGood ? (goodWeight > 0 ? weights[i] / goodWeight * good : 0)
                    : (restWeight > 0 ? weights[i] / restWeight * (1 - good) : 0);
            }
        }
        return weights;
    }

    // ---- Deciding

    /**
     * Decides the waiting decision: checks the option and the people it takes, pays its cost, draws
     * its outcome from the decision's seed and applies the outcome at once.
     *
     * @param snapshot  the player; mutated only when it decides
     * @param catalog   the content
     * @param decisions the decisions
     * @param optionId  the option
     * @param person    the person it takes, or {@code null}
     * @param second    the second of a pair, or {@code null}
     * @return what happened
     * @throws NullPointerException if a required argument is {@code null}
     */
    public static Outcome decide(PlayerSnapshot snapshot, ContentCatalog catalog, Decisions decisions,
                                 String optionId, String person, String second) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(decisions, "decisions must not be null");
        PendingDecision pending = snapshot.getPendingDecision();
        if (pending == null) {
            return Outcome.NO_DECISION;
        }
        if (pending.decided()) {
            return Outcome.ALREADY_DECIDED;
        }
        if (pending.handsOn() != null) {
            // Started: the puzzle decides it now.
            return Outcome.ALREADY_DECIDED;
        }
        Decisions.Decision decision = decisions.byId(pending.decisionId()).orElse(null);
        Decisions.Option option = decision == null ? null : decision.option(optionId).orElse(null);
        if (option == null) {
            return Outcome.UNKNOWN_OPTION;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(pending.componentId());
        MissionComponent component = catalog.missionComponent(pending.componentId()).orElse(null);
        if (owned == null || !owned.isActive() || component == null) {
            return Outcome.COMPONENT_GONE;
        }
        // Effects are sized from the team's R&D; with nobody on it there's nothing to size from.
        if (!option.id().equals(decision.defaultOption()) && team(snapshot, component.id()).isEmpty()) {
            return Outcome.NO_TEAM;
        }
        if (!takesFit(snapshot, component, option.takes(), person, second)) {
            return Outcome.PERSON_NOT_FREE;
        }
        long cost = Math.round(option.cost() * snapshot.getFundingRatePerSeason());
        if (cost > 0 && snapshot.getFunds() < cost) {
            return Outcome.INSUFFICIENT_FUNDS;
        }
        if (decision.playOption().filter(play -> play.id().equals(option.id())).isPresent()) {
            double[] odds = handsOnOdds(snapshot, component);
            snapshot.setPendingDecision(pending.withHandsOn(HandsOn.start(decision.handsOn(), odds[0], odds[1])));
            return Outcome.STARTED;
        }
        settle(snapshot, catalog, decision, option, pending, person, second, cost);
        return Outcome.DECIDED;
    }

    // ---- Hands-on

    /**
     * Returns a hands-on puzzle's odds for a component: the fail chance from the team's best skill in
     * the component's skill, the exceptional chance higher with good chemistry on the team (as the
     * player sees it). Fixed when the puzzle starts.
     *
     * @param snapshot  the player
     * @param component the component
     * @return the fail and the exceptional chance
     */
    public static double[] handsOnOdds(PlayerSnapshot snapshot, MissionComponent component) {
        List<SetSpecialist> team = team(snapshot, component.id());
        double skill = team.stream().mapToDouble(e -> e.getSkill(component.associatedSkill())).max().orElse(0);
        boolean good = team.size() >= 2 && Chemistry.research(snapshot, team, component.associatedSkill(), true)
            .value() > GOOD_CHEMISTRY;
        return new double[] {HandsOn.failChance(skill), good ? HandsOn.EXCEPTIONAL_GOOD_CHEMISTRY : HandsOn.EXCEPTIONAL};
    }

    /** Chemistry above this counts as good (the indicator's "Good"). */
    static final double GOOD_CHEMISTRY = 0.15;

    /** The steps of a hands-on puzzle. */
    public enum HandsOnStep {
        /** Queue a command. */
        QUEUE,
        /** Take the last command off. */
        UNDO,
        /** Clear the queue. */
        RESET,
        /** Run the round. */
        CONFIRM,
        /** Call in an expert on a fail. */
        RESIST,
        /** Accept a fail. */
        ACCEPT
    }

    /**
     * Takes one step of the hands-on puzzle in progress; when it ends the puzzle, the decision settles
     * at once with the playing option's outcome for the result.
     *
     * @param snapshot  the player; mutated only when the step goes through
     * @param catalog   the content
     * @param decisions the decisions
     * @param step      the step
     * @param commandId the command, for {@link HandsOnStep#QUEUE}
     * @return the puzzle's outcome name, or this class's {@link Outcome} name when there's no puzzle
     */
    public static String handsOn(PlayerSnapshot snapshot, ContentCatalog catalog, Decisions decisions,
                                 HandsOnStep step, String commandId) {
        Objects.requireNonNull(step, "step must not be null");
        PendingDecision pending = snapshot.getPendingDecision();
        Decisions.Decision decision = pending == null ? null : decisions.byId(pending.decisionId()).orElse(null);
        if (pending == null || !pending.handsOnInProgress() || decision == null || decision.handsOn() == null) {
            return Outcome.NO_PUZZLE.name();
        }
        HandsOnLevel level = decision.handsOn();
        HandsOnState state = pending.handsOn();
        java.util.function.IntToDoubleFunction roll = rolls(pending);
        HandsOn.Step done = switch (step) {
            case QUEUE -> HandsOn.queue(level, state, commandId);
            case UNDO -> HandsOn.undo(level, state);
            case RESET -> HandsOn.reset(level, state);
            case CONFIRM -> HandsOn.confirm(level, state, roll);
            case RESIST -> HandsOn.resist(level, state, roll);
            case ACCEPT -> HandsOn.accept(level, state, roll);
        };
        if (done.outcome() != HandsOn.Outcome.DONE) {
            return done.outcome().name();
        }
        snapshot.setPendingDecision(pending.withHandsOn(done.state()));
        if (done.state().phase() == HandsOnState.Phase.FINISHED) {
            settleHandsOn(snapshot, catalog, decision);
        }
        return HandsOn.Outcome.DONE.name();
    }

    /** A puzzle's rolls: the decision's seed and the roll's index, so a replay draws the same. */
    private static java.util.function.IntToDoubleFunction rolls(PendingDecision pending) {
        return index -> new SplittableRandom(pending.seed() + (index + 1) * 0x9E3779B97F4A7C15L).nextDouble();
    }

    /** Settles a finished puzzle with its playing option's outcome for the result. */
    private static void settleHandsOn(PlayerSnapshot snapshot, ContentCatalog catalog, Decisions.Decision decision) {
        PendingDecision pending = snapshot.getPendingDecision();
        Decisions.Option play = decision.playOption().orElseThrow();
        String id = pending.handsOn().result().name().toLowerCase(java.util.Locale.ROOT);
        Decisions.Outcome outcome = play.outcomes().stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
        settle(snapshot, catalog, decision, play, pending, null, null, 0, outcome);
    }

    private static boolean takesFit(PlayerSnapshot snapshot, MissionComponent component, Decisions.Takes takes,
                                    String person, String second) {
        return switch (takes) {
            case NOBODY -> true;
            case OVERSEER -> overseers(snapshot, component).stream().anyMatch(e -> e.getId().equals(person));
            case SECONDED -> team(snapshot, component.id()).stream().anyMatch(e -> e.getId().equals(person));
            case PAIR -> person != null && second != null && !person.equals(second)
                && pairable(snapshot, component).stream().anyMatch(e -> e.getId().equals(person))
                && pairable(snapshot, component).stream().anyMatch(e -> e.getId().equals(second));
        };
    }

    /** Pays, draws, applies and records the outcome on the pending decision. */
    private static Decisions.Outcome settle(PlayerSnapshot snapshot, ContentCatalog catalog,
                                            Decisions.Decision decision, Decisions.Option option,
                                            PendingDecision pending, String person, String second, long cost) {
        return settle(snapshot, catalog, decision, option, pending, person, second, cost,
            draw(snapshot, catalog, pending, option, person, second));
    }

    /** Pays, applies and records a given outcome on the pending decision. */
    private static Decisions.Outcome settle(PlayerSnapshot snapshot, ContentCatalog catalog,
                                            Decisions.Decision decision, Decisions.Option option,
                                            PendingDecision pending, String person, String second, long cost,
                                            Decisions.Outcome outcome) {
        List<DecisionEffect> applied = new ArrayList<>();
        if (cost > 0) {
            snapshot.setFunds(snapshot.getFunds() - cost);
            applied.add(new DecisionEffect(DecisionEffect.Kind.FUNDS, -cost, 0, null));
        }
        if (outcome != null) {
            for (Decisions.Effect effect : outcome.effects()) {
                DecisionEffect done = apply(snapshot, catalog, pending.componentId(), effect, person, second);
                if (done != null) {
                    applied.add(done);
                }
            }
            if (outcome.continues() && decision.followUp() != null) {
                snapshot.setFollowUpDecisionId(decision.followUp());
                snapshot.setFollowUpComponentId(pending.componentId());
            }
        }
        snapshot.setPendingDecision(pending.decide(option.id(), outcome == null ? null : outcome.id(), person, second,
            applied));
        return outcome;
    }

    private static Decisions.Outcome draw(PlayerSnapshot snapshot, ContentCatalog catalog, PendingDecision pending,
                                          Decisions.Option option, String person, String second) {
        if (option.outcomes().isEmpty()) {
            return null;
        }
        double[] weights = weights(snapshot, catalog, pending, option, person, second);
        double roll = new SplittableRandom(pending.seed()).nextDouble();
        double sum = 0;
        for (int i = 0; i < weights.length; i++) {
            sum += weights[i];
            if (roll < sum) {
                return option.outcomes().get(i);
            }
        }
        return option.outcomes().get(option.outcomes().size() - 1);
    }

    /** Applies one written effect and returns what it really did, or {@code null} when nothing. */
    private static DecisionEffect apply(PlayerSnapshot snapshot, ContentCatalog catalog, String componentId,
                                        Decisions.Effect effect, String person, String second) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        switch (effect.kind()) {
            case RELIABILITY -> {
                if (owned == null || component == null) {
                    return null;
                }
                double gain = Math.max(MIN_SEASON_GAIN, ResearchPlanning.expectedSeasonGain(snapshot, catalog,
                    componentId));
                double cap = TraitEffects.effectiveCap(snapshot, component, List.of(), false);
                double before = owned.getCurrentReliability();
                double after = effect.amount() >= 0 ? Math.max(before, Math.min(cap, before + effect.amount() * gain))
                    : Math.max(owned.getFloorReliability(), before + effect.amount() * gain);
                owned.setCurrentReliability(after);
                return after == before ? null : new DecisionEffect(DecisionEffect.Kind.RELIABILITY, after - before, 0,
                    null);
            }
            case FUNDS -> {
                long amount = Math.round(effect.amount() * snapshot.getFundingRatePerSeason());
                snapshot.setFunds(snapshot.getFunds() + amount);
                return amount == 0 ? null : new DecisionEffect(DecisionEffect.Kind.FUNDS, amount, 0, null);
            }
            case RD_FACTOR -> {
                if (owned == null) {
                    return null;
                }
                owned.setRdFactor(effect.amount());
                owned.setRdFactorSeasons(effect.seasons());
                return new DecisionEffect(DecisionEffect.Kind.RD_FACTOR, effect.amount(), effect.seasons(), null);
            }
            case MORALE -> {
                List<SetSpecialist> people = person != null ? picked(snapshot, person, second)
                    : team(snapshot, componentId);
                for (SetSpecialist engineer : people) {
                    engineer.setMorale((int) clamp(engineer.getMorale() + effect.amount(), 0, 100));
                }
                return people.isEmpty() ? null : new DecisionEffect(DecisionEffect.Kind.MORALE, effect.amount(), 0,
                    person);
            }
            case UNAVAILABLE -> {
                SetSpecialist engineer = engineer(snapshot, person);
                if (engineer == null) {
                    return null;
                }
                // Seconded away: off the component's team for the seasons they're gone.
                if (engineer.getAssignmentState() == AssignmentState.ASSIGNED) {
                    engineer.setAssignmentState(AssignmentState.IDLE);
                    engineer.setRdTargetComponentId(null);
                    engineer.setRdSlot(null);
                }
                engineer.setUnavailableSeasons(Math.max(engineer.getUnavailableSeasons(), effect.seasons()));
                return new DecisionEffect(DecisionEffect.Kind.UNAVAILABLE, effect.seasons(), effect.seasons(), person);
            }
            case RAPPORT -> {
                SetSpecialist a = engineer(snapshot, person);
                SetSpecialist b = engineer(snapshot, second);
                if (a == null || b == null) {
                    return null;
                }
                shiftRapport(snapshot, a, b, effect.amount());
                return new DecisionEffect(DecisionEffect.Kind.RAPPORT, effect.amount(), 0, person + "+" + second);
            }
            case SKILL -> {
                SetSpecialist engineer = engineer(snapshot, person);
                if (engineer == null || component == null) {
                    return null;
                }
                double before = engineer.getSkill(component.associatedSkill());
                engineer.setSkill(component.associatedSkill(),
                    clamp(before + effect.amount(), 0, SkillGrowth.SKILL_CAP));
                return new DecisionEffect(DecisionEffect.Kind.SKILL, engineer.getSkill(component.associatedSkill())
                    - before, 0, person);
            }
            default -> {
                return null;
            }
        }
    }

    private static void shiftRapport(PlayerSnapshot snapshot, SetSpecialist a, SetSpecialist b, double amount) {
        List<Rapport> rapports = new ArrayList<>(snapshot.getRapports());
        for (int i = 0; i < rapports.size(); i++) {
            Rapport pair = rapports.get(i);
            if (pair.isPair(a.getId(), b.getId())) {
                rapports.set(i, new Rapport(pair.first(), pair.second(), clamp(pair.rapport() + amount, -100, 100),
                    pair.affinity(), pair.seasonsTogether(), true));
                snapshot.setRapports(rapports);
                return;
            }
        }
        String first = a.getId().compareTo(b.getId()) <= 0 ? a.getId() : b.getId();
        String other = first.equals(a.getId()) ? b.getId() : a.getId();
        rapports.add(new Rapport(first, other, clamp(amount, -100, 100), Personality.affinity(snapshot, a, b), 0,
            true));
        snapshot.setRapports(rapports);
    }

    private static List<SetSpecialist> picked(PlayerSnapshot snapshot, String person, String second) {
        List<SetSpecialist> people = new ArrayList<>();
        for (String id : new String[] {person, second}) {
            SetSpecialist engineer = engineer(snapshot, id);
            if (engineer != null) {
                people.add(engineer);
            }
        }
        return people;
    }

    private static SetSpecialist engineer(PlayerSnapshot snapshot, String id) {
        if (id == null) {
            return null;
        }
        return snapshot.getSetSpecialists().stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    // ---- The season pass

    /**
     * Runs the decisions' part of a season pass, after R&amp;D: ages the components' R&amp;D factors,
     * settles last turn's decision (its default applied if still open), then may offer a new one.
     *
     * @param snapshot  the player; mutated in place
     * @param catalog   the content
     * @param decisions the decisions
     * @param gameTurn  the turn resolving
     * @return the events
     * @throws NullPointerException if an argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
                                                      Decisions decisions, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(decisions, "decisions must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        for (OwnedMissionComponent owned : snapshot.getMissionComponents().values()) {
            if (owned.getRdFactorSeasons() > 0) {
                owned.setRdFactorSeasons(owned.getRdFactorSeasons() - 1);
            }
        }
        settleLast(snapshot, catalog, decisions, events);
        offer(snapshot, catalog, decisions, gameTurn, events);
        return events;
    }

    private static void settleLast(PlayerSnapshot snapshot, ContentCatalog catalog, Decisions decisions,
                                   List<ResolutionEvent> events) {
        PendingDecision pending = snapshot.getPendingDecision();
        if (pending == null) {
            return;
        }
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(pending.componentId());
        if (!pending.decided() && (owned == null || !owned.isActive())) {
            // Its program closed during the turn: the decision goes with it, nothing applies.
            snapshot.setPendingDecision(null);
            return;
        }
        Decisions.Decision decision = decisions.byId(pending.decisionId()).orElse(null);
        if (pending.handsOnInProgress() && decision != null && decision.handsOn() != null) {
            // Started, so it counts: it settles as it stands, and the player did act.
            snapshot.setPendingDecision(pending.withHandsOn(HandsOn.finishAsItStands(decision.handsOn(),
                pending.handsOn(), rolls(pending))));
            settleHandsOn(snapshot, catalog, decision);
            pending = snapshot.getPendingDecision();
        }
        boolean lapsed = !pending.decided();
        if (lapsed && decision != null) {
            Decisions.Option fallback = decision.defaultChoice();
            settle(snapshot, catalog, decision, fallback, pending, null, null, 0);
            snapshot.setDecisionPaused(true);
            pending = snapshot.getPendingDecision();
        }
        events.add(new DecisionSettled(pending.decisionId(), pending.componentId(), pending.optionId(),
            pending.outcomeId(), lapsed, pending.effects()));
        snapshot.setPendingDecision(null);
    }

    private static void offer(PlayerSnapshot snapshot, ContentCatalog catalog, Decisions decisions, int gameTurn,
                              List<ResolutionEvent> events) {
        if (snapshot.isDecisionPaused() || decisions.decisions().isEmpty()) {
            return;
        }
        List<String> candidates = new ArrayList<>();
        List<String> lulled = new ArrayList<>();
        boolean flying = !snapshot.getInProgressMissions().isEmpty();
        // Waiting is a lull too once nothing has flown for a while: for money, training, a rocket.
        int lastFlight = snapshot.getLastFlightSeasonTurn();
        boolean waiting = !flying && (lastFlight < 0 ? gameTurn : gameTurn - lastFlight) >= WAITING_SEASONS;
        for (var entry : snapshot.getMissionComponents().entrySet()) {
            MissionComponent component = catalog.missionComponent(entry.getKey()).orElse(null);
            OwnedMissionComponent owned = entry.getValue();
            if (component == null || !owned.isActive() || team(snapshot, component.id()).isEmpty()
                    || owned.getCurrentReliability()
                        >= TraitEffects.effectiveCap(snapshot, component, List.of(), false)) {
                continue;
            }
            candidates.add(component.id());
            if (waiting) {
                lulled.add(component.id());
            } else if (!flying && owned.getCurrentReliability() < READY_RELIABILITY) {
                OptionalInt seasons = ResearchPlanning.project(snapshot, catalog, component.id())
                    .seasonsTo(READY_RELIABILITY);
                if (seasons.isEmpty() || seasons.getAsInt() > 1) {
                    lulled.add(component.id());
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        boolean lull = !lulled.isEmpty();
        SplittableRandom random = new SplittableRandom(Intelligence.seed(snapshot.getAgencyName(),
            "decision/" + gameTurn) ^ OFFER_SALT);
        if (random.nextDouble() >= chance(snapshot, gameTurn, lull, flying)) {
            return;
        }
        List<String> pool = lull ? lulled : candidates;
        // The component furthest from ready: the one the agency waits on longest.
        String componentId = pool.stream().min(Comparator.comparingDouble(id ->
            snapshot.getMissionComponents().get(id).getCurrentReliability())).orElseThrow();
        Decisions.Decision decision = followUp(snapshot, decisions, lull, candidates);
        if (decision != null) {
            componentId = snapshot.getFollowUpComponentId();
            snapshot.setFollowUpDecisionId(null);
            snapshot.setFollowUpComponentId(null);
        } else {
            MissionComponent component = catalog.missionComponent(componentId).orElseThrow();
            decision = pick(snapshot, decisions, component, random);
        }
        if (decision == null) {
            return;
        }
        long seed = Intelligence.seed(snapshot.getAgencyName(), "decision-outcome/" + gameTurn + "/" + decision.id());
        snapshot.setPendingDecision(PendingDecision.offered(decision.id(), componentId, gameTurn, seed));
        snapshot.setLastDecisionTurn(gameTurn);
        events.add(new DecisionOffered(decision.id(), componentId, gameTurn));
    }

    /**
     * Returns the chance of a decision this season: {@link #LULL_CHANCE} in a lull; otherwise it grows
     * with the seasons since the last flight, halved while a mission flies or right after a decision.
     *
     * @param snapshot the player
     * @param turn     the turn
     * @param lull     whether it's a lull
     * @param flying   whether a mission is in flight
     * @return the chance
     */
    static double chance(PlayerSnapshot snapshot, int turn, boolean lull, boolean flying) {
        if (lull) {
            return LULL_CHANCE;
        }
        int last = snapshot.getLastFlightSeasonTurn();
        int quiet = last < 0 ? turn : turn - last;
        double chance = clamp(QUIET_CHANCE_MIN + QUIET_CHANCE_PER_SEASON * quiet, QUIET_CHANCE_MIN, QUIET_CHANCE_MAX);
        if (flying) {
            chance /= 2;
        }
        if (snapshot.getLastDecisionTurn() == turn - 1) {
            chance /= 2;
        }
        return chance;
    }

    /** The story arc's next step, when one waits, it's a lull, and its component can still take it. */
    private static Decisions.Decision followUp(PlayerSnapshot snapshot, Decisions decisions, boolean lull,
                                               List<String> candidates) {
        String id = snapshot.getFollowUpDecisionId();
        if (id == null) {
            return null;
        }
        if (!candidates.contains(snapshot.getFollowUpComponentId())) {
            // The component is done or gone: the arc ends.
            snapshot.setFollowUpDecisionId(null);
            snapshot.setFollowUpComponentId(null);
            return null;
        }
        return lull ? decisions.byId(id).orElse(null) : null;
    }

    /**
     * Picks a decision that fits the component and the agency, one the player hasn't had since they
     * last had all of them.
     */
    private static Decisions.Decision pick(PlayerSnapshot snapshot, Decisions decisions, MissionComponent component,
                                           SplittableRandom random) {
        List<Decisions.Decision> usable = decisions.decisions().stream()
            .filter(decision -> decision.step() == 1)
            .filter(decision -> fits(decision, component))
            .filter(decision -> usable(snapshot, decision, component))
            .toList();
        if (usable.isEmpty()) {
            return null;
        }
        List<String> used = snapshot.getUsedDecisionIds();
        used.removeIf(id -> decisions.byId(id).isEmpty());
        List<Decisions.Decision> fresh = usable.stream().filter(decision -> !used.contains(decision.id())).toList();
        if (fresh.isEmpty()) {
            usable.forEach(decision -> used.remove(decision.id()));
            fresh = usable;
        }
        Decisions.Decision decision = fresh.get(random.nextInt(fresh.size()));
        used.add(decision.id());
        return decision;
    }

    private static boolean fits(Decisions.Decision decision, MissionComponent component) {
        return switch (decision.fits()) {
            case ANY -> true;
            case ROCKET -> component.hasRocketSpec();
            case PAYLOAD -> !component.hasRocketSpec();
        };
    }

    /** Whether the agency can take the decision now: someone to oversee, to second, to pair, an office. */
    private static boolean usable(PlayerSnapshot snapshot, Decisions.Decision decision, MissionComponent component) {
        if (decision.type() == Decisions.Type.INTEL && snapshot.getIntelligence().getLevel() <= 0) {
            return false;
        }
        for (Decisions.Option option : decision.options()) {
            boolean ok = switch (option.takes()) {
                case NOBODY -> true;
                case OVERSEER -> !overseers(snapshot, component).isEmpty();
                case SECONDED -> !team(snapshot, component.id()).isEmpty();
                case PAIR -> pairable(snapshot, component).size() >= 2;
            };
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void normalise(double[] weights) {
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        if (total <= 0) {
            java.util.Arrays.fill(weights, weights.length == 0 ? 0 : 1.0 / weights.length);
            return;
        }
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= total;
        }
    }
}
