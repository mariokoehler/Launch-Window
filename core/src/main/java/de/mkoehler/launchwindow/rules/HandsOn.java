package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.HandsOnState.Phase;
import de.mkoehler.launchwindow.model.HandsOnState.Roll;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntToDoubleFunction;

/**
 * <p>The hands-on puzzle's rules ({@code ui-spec.md} component 26, type 11; {@code design.md} &sect;5,
 * Backlog, from the user's "Probe Command Minigame" spec): a pure state machine over a
 * {@link HandsOnState}, knowing nothing of agencies. {@code RdDecisions} keeps the state on the open
 * decision and supplies the rolls from its seed.</p>
 *
 * <p><b>Planning:</b> commands are queued into the round's slots; their inputs are paid at once and
 * refunded by undo or reset; their outputs come only at execution, so they can't pay for another
 * command the same round. <b>Execution:</b> the queue resolves in slot order; a rolling command
 * fails (main output one less, never below 0), goes as written, or goes exceptionally well (one
 * more). On a fail the player may pay {@link HandsOnLevel#resist()} of the first resource to make it
 * a normal result; the roll is drawn first, so the answer never changes what the next roll is.
 * <b>The end:</b> the bonus met after a round ends the puzzle at once; otherwise it ends after the
 * last round, a success when the objective is held. Held amounts count, not totals made.</p>
 *
 * <p>Every step returns the new state, or the same one with the refusal; nothing is mutated.</p>
 *
 * @author Mario Koehler
 */
public final class HandsOn {

    /** The fail chance with nobody's skill against it. */
    public static final double BASE_FAIL = 0.35;
    /** The fail chance never goes under this. */
    public static final double MIN_FAIL = 0.10;
    /** What each point of the leader's skill takes off the fail chance. */
    public static final double FAIL_PER_SKILL = 0.0025;
    /** The chance of an exceptional result. */
    public static final double EXCEPTIONAL = 0.15;
    /** The chance of an exceptional result with good chemistry on the team. */
    public static final double EXCEPTIONAL_GOOD_CHEMISTRY = 0.20;

    private HandsOn() {
    }

    /** Why a step was refused, or that it went through. */
    public enum Outcome {
        /** Done. */
        DONE,
        /** The puzzle isn't in the phase for that step. */
        WRONG_PHASE,
        /** The round's slots are full. */
        NO_SLOT,
        /** The command's inputs can't be paid. */
        CANT_PAY,
        /** The level has no such command. */
        UNKNOWN_COMMAND,
        /** Nothing is queued to undo. */
        NOTHING_QUEUED,
        /** Calling in an expert costs more than the player holds. */
        CANT_RESIST
    }

    /**
     * A step's result.
     *
     * @param state   the new state, or the old one when refused
     * @param outcome what happened
     */
    public record Step(HandsOnState state, Outcome outcome) {
    }

    /**
     * Returns the fail chance a leader's skill gives.
     *
     * @param skill the best skill on the team in the component's skill, 0 to 100
     * @return the chance
     */
    public static double failChance(double skill) {
        return Math.max(MIN_FAIL, BASE_FAIL - FAIL_PER_SKILL * skill);
    }

    /**
     * Starts a puzzle.
     *
     * @param level             the level
     * @param failChance        the fail chance, fixed for the puzzle
     * @param exceptionalChance the exceptional chance, fixed for the puzzle
     * @return round 1, planning
     */
    public static HandsOnState start(HandsOnLevel level, double failChance, double exceptionalChance) {
        Map<String, Integer> held = new LinkedHashMap<>();
        for (HandsOnLevel.Resource resource : level.resources()) {
            held.put(resource.id(), level.start().getOrDefault(resource.id(), 0));
        }
        return new HandsOnState(held, 1, List.of(), List.of(), 0, Phase.PLANNING, 0, 0, failChance,
            exceptionalChance, null);
    }

    /**
     * Returns what the player is short of to queue a command now, by resource; empty when it can be
     * paid. A full queue isn't counted here.
     *
     * @param level   the level
     * @param state   the puzzle
     * @param command the command
     * @return the shortfall
     */
    public static Map<String, Integer> shortOf(HandsOnLevel level, HandsOnState state, HandsOnLevel.Command command) {
        Map<String, Integer> shortOf = new LinkedHashMap<>();
        command.input().forEach((resource, amount) -> {
            int missing = amount - state.held(resource);
            if (missing > 0) {
                shortOf.put(resource, missing);
            }
        });
        return shortOf;
    }

    /**
     * Returns whether a command can be queued now.
     *
     * @param level   the level
     * @param state   the puzzle
     * @param command the command
     * @return whether
     */
    public static boolean canQueue(HandsOnLevel level, HandsOnState state, HandsOnLevel.Command command) {
        return state.phase() == Phase.PLANNING && state.queue().size() < level.slots()
            && shortOf(level, state, command).isEmpty();
    }

    /**
     * Returns what the queued commands will make, as written, by resource.
     *
     * @param level the level
     * @param state the puzzle
     * @return the amounts
     */
    public static Map<String, Integer> queuedOutput(HandsOnLevel level, HandsOnState state) {
        Map<String, Integer> made = new LinkedHashMap<>();
        for (String id : state.queue()) {
            level.command(id).orElseThrow().output().forEach((resource, amount) -> made.merge(resource, amount,
                Integer::sum));
        }
        return made;
    }

    /**
     * Returns whether amounts meet a target.
     *
     * @param held   the amounts held
     * @param target the target
     * @return whether every target amount is held
     */
    public static boolean meets(Map<String, Integer> held, Map<String, Integer> target) {
        return target.entrySet().stream().allMatch(entry -> held.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

    /**
     * Queues a command, paying its inputs.
     *
     * @param level     the level
     * @param state     the puzzle
     * @param commandId the command
     * @return the step
     */
    public static Step queue(HandsOnLevel level, HandsOnState state, String commandId) {
        if (state.phase() != Phase.PLANNING) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        HandsOnLevel.Command command = level.command(commandId).orElse(null);
        if (command == null) {
            return new Step(state, Outcome.UNKNOWN_COMMAND);
        }
        if (state.queue().size() >= level.slots()) {
            return new Step(state, Outcome.NO_SLOT);
        }
        if (!shortOf(level, state, command).isEmpty()) {
            return new Step(state, Outcome.CANT_PAY);
        }
        Map<String, Integer> held = new LinkedHashMap<>(state.held());
        command.input().forEach((resource, amount) -> held.merge(resource, -amount, Integer::sum));
        List<String> queue = new ArrayList<>(state.queue());
        queue.add(commandId);
        return new Step(with(state, held, state.round(), queue, state.results(), state.resultsRound(), Phase.PLANNING,
            0, state.rolls(), null), Outcome.DONE);
    }

    /**
     * Takes the last queued command off, refunding its inputs.
     *
     * @param level the level
     * @param state the puzzle
     * @return the step
     */
    public static Step undo(HandsOnLevel level, HandsOnState state) {
        if (state.phase() != Phase.PLANNING) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        if (state.queue().isEmpty()) {
            return new Step(state, Outcome.NOTHING_QUEUED);
        }
        List<String> queue = new ArrayList<>(state.queue());
        String last = queue.removeLast();
        Map<String, Integer> held = new LinkedHashMap<>(state.held());
        level.command(last).orElseThrow().input().forEach((resource, amount) -> held.merge(resource, amount,
            Integer::sum));
        return new Step(with(state, held, state.round(), queue, state.results(), state.resultsRound(), Phase.PLANNING,
            0, state.rolls(), null), Outcome.DONE);
    }

    /**
     * Clears the queue, refunding everything.
     *
     * @param level the level
     * @param state the puzzle
     * @return the step
     */
    public static Step reset(HandsOnLevel level, HandsOnState state) {
        if (state.phase() != Phase.PLANNING) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        HandsOnState reset = state;
        while (!reset.queue().isEmpty()) {
            reset = undo(level, reset).state();
        }
        return new Step(reset, Outcome.DONE);
    }

    /**
     * Confirms the round's queue (empty is allowed) and executes it up to the end of the round or the
     * first fail that waits for the player's answer.
     *
     * @param level the level
     * @param state the puzzle
     * @param roll  the rolls, by index, each in [0, 1)
     * @return the step
     */
    public static Step confirm(HandsOnLevel level, HandsOnState state, IntToDoubleFunction roll) {
        if (state.phase() != Phase.PLANNING) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        return new Step(execute(level, state, List.of(), 0, state.held(), state.rolls(), roll), Outcome.DONE);
    }

    /**
     * Calls in an expert on the failed slot: pays for it and makes it a normal result, then goes on.
     *
     * @param level the level
     * @param state the puzzle
     * @param roll  the rolls
     * @return the step
     */
    public static Step resist(HandsOnLevel level, HandsOnState state, IntToDoubleFunction roll) {
        if (state.phase() != Phase.AWAITING_RESIST) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        String payer = level.payer().id();
        if (state.held(payer) < level.resist()) {
            return new Step(state, Outcome.CANT_RESIST);
        }
        Map<String, Integer> held = new LinkedHashMap<>(state.held());
        held.merge(payer, -level.resist(), Integer::sum);
        return new Step(answer(level, state, held, true, roll), Outcome.DONE);
    }

    /**
     * Accepts the failed slot as it is, then goes on.
     *
     * @param level the level
     * @param state the puzzle
     * @param roll  the rolls
     * @return the step
     */
    public static Step accept(HandsOnLevel level, HandsOnState state, IntToDoubleFunction roll) {
        if (state.phase() != Phase.AWAITING_RESIST) {
            return new Step(state, Outcome.WRONG_PHASE);
        }
        return new Step(answer(level, state, state.held(), false, roll), Outcome.DONE);
    }

    /**
     * Ends the puzzle as it stands, when the turn resolves before the player finished it: a waiting
     * fail is accepted, an unconfirmed queue refunded, and the rounds left run empty.
     *
     * @param level the level
     * @param state the puzzle
     * @param roll  the rolls
     * @return the finished puzzle
     */
    public static HandsOnState finishAsItStands(HandsOnLevel level, HandsOnState state, IntToDoubleFunction roll) {
        HandsOnState current = state;
        if (current.phase() == Phase.AWAITING_RESIST) {
            current = accept(level, current, roll).state();
        }
        if (current.phase() == Phase.PLANNING) {
            current = reset(level, current).state();
            HandsOnState.Result result = meets(current.held(), level.bonus()) ? HandsOnState.Result.BONUS
                : meets(current.held(), level.objective()) ? HandsOnState.Result.SUCCESS
                : HandsOnState.Result.FAILURE;
            current = with(current, current.held(), current.round(), List.of(), current.results(),
                current.resultsRound(), Phase.FINISHED, 0, current.rolls(), result);
        }
        return current;
    }

    /** Applies the answer to the waiting fail and executes the rest of the round. */
    private static HandsOnState answer(HandsOnLevel level, HandsOnState state, Map<String, Integer> held,
            boolean resisted, IntToDoubleFunction roll) {
        List<HandsOnState.Slot> done = new ArrayList<>(state.results().subList(0, state.slot()));
        HandsOnLevel.Command command = level.command(state.queue().get(state.slot())).orElseThrow();
        Map<String, Integer> afterSlot = new LinkedHashMap<>(held);
        Map<String, Integer> gained = gained(command, resisted ? Roll.NORMAL : Roll.FAIL);
        gained.forEach((resource, amount) -> afterSlot.merge(resource, amount, Integer::sum));
        done.add(new HandsOnState.Slot(command.id(), Roll.FAIL, resisted, gained));
        return execute(level, state, done, state.slot() + 1, afterSlot, state.rolls(), roll);
    }

    /** Executes the queue from a slot until a fail waits for an answer or the round ends. */
    private static HandsOnState execute(HandsOnLevel level, HandsOnState state, List<HandsOnState.Slot> done,
            int from, Map<String, Integer> start, int rolls, IntToDoubleFunction roll) {
        Map<String, Integer> held = new LinkedHashMap<>(start);
        List<HandsOnState.Slot> results = new ArrayList<>(done);
        String payer = level.payer().id();
        int drawn = rolls;
        for (int i = from; i < state.queue().size(); i++) {
            HandsOnLevel.Command command = level.command(state.queue().get(i)).orElseThrow();
            Roll rolled = Roll.NONE;
            if (command.rolls()) {
                double draw = roll.applyAsDouble(drawn++);
                rolled = draw < state.failChance() ? Roll.FAIL
                    : draw < state.failChance() + state.exceptionalChance() ? Roll.EXCEPTIONAL : Roll.NORMAL;
            }
            if (rolled == Roll.FAIL && held.getOrDefault(payer, 0) >= level.resist()) {
                results.add(new HandsOnState.Slot(command.id(), Roll.FAIL, false, Map.of()));
                return with(state, held, state.round(), state.queue(), results, state.round(), Phase.AWAITING_RESIST,
                    i, drawn, null);
            }
            Map<String, Integer> gained = gained(command, rolled);
            gained.forEach((resource, amount) -> held.merge(resource, amount, Integer::sum));
            results.add(new HandsOnState.Slot(command.id(), rolled, false, gained));
        }
        // The round is over.
        int next = state.round() + 1;
        HandsOnState.Result result = meets(held, level.bonus()) ? HandsOnState.Result.BONUS
            : next > level.rounds() ? (meets(held, level.objective()) ? HandsOnState.Result.SUCCESS
            : HandsOnState.Result.FAILURE) : null;
        return with(state, held, result == null ? next : state.round(), List.of(), results, state.round(),
            result == null ? Phase.PLANNING : Phase.FINISHED, 0, drawn, result);
    }

    /** What a command makes with a roll: the main output moved by one, never below 0. */
    private static Map<String, Integer> gained(HandsOnLevel.Command command, Roll rolled) {
        Map<String, Integer> gained = new LinkedHashMap<>(command.output());
        int shift = rolled == Roll.FAIL ? -1 : rolled == Roll.EXCEPTIONAL ? 1 : 0;
        gained.computeIfPresent(command.primary(), (resource, amount) -> Math.max(0, amount + shift));
        return gained;
    }

    private static HandsOnState with(HandsOnState state, Map<String, Integer> held, int round, List<String> queue,
            List<HandsOnState.Slot> results, int resultsRound, Phase phase, int slot, int rolls,
            HandsOnState.Result result) {
        return new HandsOnState(held, round, queue, results, resultsRound, phase, slot, rolls, state.failChance(),
            state.exceptionalChance(), result);
    }

    // ---- The solver

    /**
     * Finds a plan that meets the bonus with every roll going as written, or failing that the
     * objective: the commands per round. The order inside a round doesn't matter, since outputs come
     * only at execution. Used to check a level can be won and by the bots.
     *
     * @param level the level
     * @param from  the puzzle to plan from, in planning with an empty queue
     * @return the rounds' commands, or {@code null} when not even the objective can be met
     */
    public static List<List<String>> solve(HandsOnLevel level, HandsOnState from) {
        Objects.requireNonNull(level, "level must not be null");
        List<List<String>> bonus = search(level, from.held(), from.round(), level.bonus(), new HashMap<>());
        return bonus != null ? bonus : search(level, from.held(), from.round(), level.objective(), new HashMap<>());
    }

    private static List<List<String>> search(HandsOnLevel level, Map<String, Integer> held, int round,
            Map<String, Integer> target, Map<String, Boolean> dead) {
        if (meets(held, target)) {
            return new ArrayList<>();
        }
        if (round > level.rounds()) {
            return null;
        }
        String key = round + "|" + held;
        if (dead.containsKey(key)) {
            return null;
        }
        for (List<String> pick : picks(level, held)) {
            Map<String, Integer> after = new LinkedHashMap<>(held);
            for (String id : pick) {
                HandsOnLevel.Command command = level.command(id).orElseThrow();
                command.input().forEach((resource, amount) -> after.merge(resource, -amount, Integer::sum));
                command.output().forEach((resource, amount) -> after.merge(resource, amount, Integer::sum));
            }
            List<List<String>> rest = search(level, after, round + 1, target, dead);
            if (rest != null) {
                rest.addFirst(pick);
                return rest;
            }
        }
        dead.put(key, true);
        return null;
    }

    /** Every multiset of up to the round's slots of commands whose inputs together can be paid. */
    private static List<List<String>> picks(HandsOnLevel level, Map<String, Integer> held) {
        List<List<String>> picks = new ArrayList<>();
        extend(level, held, 0, new ArrayList<>(), picks);
        // Fullest rounds first: the search finds a plan sooner.
        picks.sort((a, b) -> b.size() - a.size());
        return picks;
    }

    private static void extend(HandsOnLevel level, Map<String, Integer> left, int fromCommand, List<String> chosen,
            List<List<String>> picks) {
        picks.add(List.copyOf(chosen));
        if (chosen.size() >= level.slots()) {
            return;
        }
        for (int i = fromCommand; i < level.commands().size(); i++) {
            HandsOnLevel.Command command = level.commands().get(i);
            boolean affordable = command.input().entrySet().stream()
                .allMatch(entry -> left.getOrDefault(entry.getKey(), 0) >= entry.getValue());
            if (!affordable) {
                continue;
            }
            Map<String, Integer> after = new LinkedHashMap<>(left);
            command.input().forEach((resource, amount) -> after.merge(resource, -amount, Integer::sum));
            chosen.add(command.id());
            extend(level, after, i, chosen, picks);
            chosen.removeLast();
        }
    }
}
