package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.HandsOnLevel;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.HandsOnState;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.rules.HandsOn;
import de.mkoehler.launchwindow.rules.RdDecisions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * What the hands-on decision's banner and screen say ({@code ui-spec.md} component 26, type 11, the
 * canvas {@code Launch Window — Hands-on}). Reads the rules ({@link HandsOn}, {@link RdDecisions});
 * never ports them.
 *
 * @author Mario Koehler
 */
public final class HandsOnView {

    private HandsOnView() {
    }

    /**
     * Returns the hands-on decision waiting or in progress, with its level.
     *
     * @param state the agency
     * @return the decision, or empty when none is
     */
    public static Optional<Decisions.Decision> decision(PlayerSnapshot state) {
        PendingDecision pending = state.getPendingDecision();
        return pending == null ? Optional.empty() : DecisionView.content(pending)
            .filter(decision -> decision.handsOn() != null);
    }

    /**
     * Words an amount of a resource: "2 firings", "1 shift".
     *
     * @param level    the level
     * @param resource the resource
     * @param amount   the amount
     * @return the words
     */
    public static String amount(HandsOnLevel level, String resource, int amount) {
        HandsOnLevel.Resource r = level.resource(resource).orElseThrow();
        return amount + " " + (amount == 1 ? r.one() : r.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Words a target: "3 firings, 4 measurements".
     *
     * @param level  the level
     * @param target the amounts
     * @return the words
     */
    public static String target(HandsOnLevel level, Map<String, Integer> target) {
        List<String> parts = new ArrayList<>();
        target.forEach((resource, amount) -> parts.add(amount(level, resource, amount)));
        return String.join(", ", parts);
    }

    /**
     * The banner's caption: "Up to 4 rounds of 3 commands · objective 3 firings, 4 measurements · bonus
     * 6 and 7".
     *
     * @param level the level
     * @return the words
     */
    public static String caption(HandsOnLevel level) {
        return "Up to " + level.rounds() + " rounds of " + level.slots() + " commands · objective "
            + target(level, level.objective()) + " · bonus " + String.join(" and ",
            level.bonus().values().stream().map(String::valueOf).toList());
    }

    /**
     * The team's lead on a component: its most skilled engineer in the component's skill.
     *
     * @param state     the agency
     * @param component the component
     * @return the engineer, or {@code null} when nobody works on it
     */
    public static SetSpecialist lead(PlayerSnapshot state, MissionComponent component) {
        return RdDecisions.team(state, component.id()).stream()
            .max(Comparator.comparingDouble(e -> e.getSkill(component.associatedSkill()))).orElse(null);
    }

    /**
     * The odds before the puzzle starts: "Arkhipov leads: 17 % of steps fail, 15 % go exceptionally well."
     *
     * @param state     the agency
     * @param catalog   the content pack
     * @param component the component
     * @return the words
     */
    public static String oddsLine(PlayerSnapshot state, ContentCatalog catalog, MissionComponent component) {
        double[] odds = RdDecisions.handsOnOdds(state, component);
        SetSpecialist lead = lead(state, component);
        return (lead == null ? "Nobody leads" : People.surname(lead.getName()) + " leads") + ": "
            + percent(odds[0]) + " % of steps fail, " + percent(odds[1]) + " % go exceptionally well.";
    }

    /**
     * Rounds a chance to whole percent.
     *
     * @param chance the chance
     * @return the percent
     */
    public static long percent(double chance) {
        return Math.round(chance * 100);
    }

    /**
     * Words a command's shortfall: "Short of 1 measurement".
     *
     * @param level   the level
     * @param shortOf the shortfall
     * @return the words, or {@code null} when nothing is short
     */
    public static String shortOf(HandsOnLevel level, Map<String, Integer> shortOf) {
        if (shortOf.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        shortOf.forEach((resource, amount) -> parts.add(amount(level, resource, amount)));
        return "Short of " + String.join(" and ", parts);
    }

    /**
     * Words a result's headline: "Bonus success", "Success", "Not enough".
     *
     * @param result the result
     * @return the words
     */
    public static String result(HandsOnState.Result result) {
        return switch (result) {
            case BONUS -> "Bonus success";
            case SUCCESS -> "Success";
            case FAILURE -> "Not enough";
        };
    }

    /**
     * Words an in-progress puzzle for the banner and the report: "round 2 of 4 · 2 / 3 firings · 3 / 4
     * measurements".
     *
     * @param level the level
     * @param state the puzzle
     * @return the words
     */
    public static String progress(HandsOnLevel level, HandsOnState state) {
        List<String> parts = new ArrayList<>();
        parts.add("round " + state.round() + " of " + level.rounds());
        level.objective().forEach((resource, amount) -> parts.add(state.held(resource) + " / " + amount + " "
            + level.resource(resource).orElseThrow().name().toLowerCase(Locale.ROOT)));
        return String.join(" · ", parts);
    }

    /**
     * Words a slot's badge: "Queued", "Done", "Failed −1", "Exceptional +1", "Waiting", "Done · no roll".
     *
     * @param slot the slot's result, or {@code null} while queued
     * @param waiting whether it waits behind a fail
     * @return the words
     */
    public static String badge(HandsOnState.Slot slot, boolean waiting) {
        if (waiting) {
            return "Waiting";
        }
        if (slot == null) {
            return "Queued";
        }
        if (slot.rolled() == HandsOnState.Roll.FAIL && slot.gained().isEmpty()) {
            return "Failed −1";
        }
        return switch (slot.rolled()) {
            case FAIL -> slot.resisted() ? "Done · expert" : "Failed −1";
            case NORMAL -> "Done";
            case EXCEPTIONAL -> "Exceptional +1";
            case NONE -> "Done · no roll";
        };
    }

    /**
     * The effect a result is worth, from the playing option's outcome: "About 3 seasons of R&D".
     *
     * @param decision the decision
     * @param result   the result
     * @return the words
     */
    public static String worth(Decisions.Decision decision, HandsOnState.Result result) {
        double seasons = decision.outcome(result.name().toLowerCase(Locale.ROOT))
            .flatMap(outcome -> outcome.effects().stream().filter(effect -> effect.kind()
                == de.mkoehler.launchwindow.model.DecisionEffect.Kind.RELIABILITY).findFirst())
            .map(Decisions.Effect::amount).orElse(0.0);
        if (seasons <= 0) {
            return "Nothing";
        }
        return "About " + (seasons == 1.5 ? "1½" : seasons == Math.rint(seasons) ? String.valueOf((int) seasons)
            : String.valueOf(seasons)) + (seasons == 1 ? " season" : " seasons") + " of R&D";
    }
}
