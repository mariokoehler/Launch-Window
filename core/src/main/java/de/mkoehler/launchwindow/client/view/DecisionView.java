package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Decisions;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.DecisionEffect;
import de.mkoehler.launchwindow.model.PendingDecision;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.DecisionSettled;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Words the R&amp;D decisions ({@code ui-spec.md} component 26) for the R&amp;D card's banner, the turn
 * report and the hub: the type's label, what an option shows in advance, the outcome with names
 * filled in, and the effects as lines. The rules work everything out ({@code RdDecisions}); this only
 * reads and words it.
 *
 * @author Mario Koehler
 */
public final class DecisionView {

    private DecisionView() {
    }

    /**
     * One effect as a line: its words and whether it's good.
     *
     * @param text the words, such as "Reliability +4.2"
     * @param good whether it helps
     */
    public record EffectLine(String text, boolean good) {
    }

    /**
     * Returns the waiting or decided decision about a component, if any.
     *
     * @param state       the player's state
     * @param componentId the component
     * @return the decision
     */
    public static Optional<PendingDecision> about(PlayerSnapshot state, String componentId) {
        PendingDecision pending = state.getPendingDecision();
        return pending != null && pending.componentId().equals(componentId) && active(state, componentId)
            ? Optional.of(pending) : Optional.empty();
    }

    private static boolean active(PlayerSnapshot state, String componentId) {
        var owned = state.getMissionComponents().get(componentId);
        return owned != null && owned.isActive();
    }

    /**
     * Returns whether a decision waits for the player.
     *
     * @param state the player's state
     * @return whether one is open
     */
    public static boolean waiting(PlayerSnapshot state) {
        PendingDecision pending = state.getPendingDecision();
        return pending != null && !pending.decided() && active(state, pending.componentId());
    }

    /**
     * Returns a decision's content.
     *
     * @param pending the decision
     * @return its content, empty if it's gone from the file
     */
    public static Optional<Decisions.Decision> content(PendingDecision pending) {
        return Decisions.standard().byId(pending.decisionId());
    }

    /**
     * Returns a type's label, as the banner's eyebrow shows it.
     *
     * @param type the type
     * @return the label
     */
    public static String typeLabel(Decisions.Type type) {
        return switch (type) {
            case RESOURCE -> "Resource for a chance";
            case KNOWLEDGE -> "Knowledge";
            case AMBIGUOUS -> "Ambiguous";
            case OVERSEE -> "Who oversees it";
            case TRADEOFF -> "Trade-offs over time";
            case DIAGNOSIS -> "Diagnosis";
            case OPPORTUNITY -> "Opportunity with strings";
            case PAIRING -> "Pairing";
            case INTEL -> "Intelligence tip";
            case HANDS_ON -> "Hands-on";
        };
    }

    /**
     * Returns the banner's eyebrow: "DECISION · RESOURCE FOR A CHANCE", or a story arc's
     * "FOLLOW-UP · ATLAS".
     *
     * @param decision    the decision
     * @param componentId the component
     * @return the eyebrow, in capitals
     */
    public static String eyebrow(Decisions.Decision decision, String componentId) {
        String text = decision.step() > 1 ? "Follow-up · " + ContentNames.of(componentId)
            : "Decision · " + typeLabel(decision.type());
        return text.toUpperCase(Locale.ROOT);
    }

    /**
     * Returns how many steps the arc a decision is in can run to, for "Step 2 of up to 3".
     *
     * @param decision the decision
     * @return the steps
     */
    public static int arcLength(Decisions.Decision decision) {
        int steps = decision.step();
        Decisions.Decision next = decision;
        while (next.followUp() != null) {
            next = Decisions.standard().byId(next.followUp()).orElse(null);
            if (next == null) {
                break;
            }
            steps = Math.max(steps, next.step());
        }
        return steps;
    }

    /**
     * Returns what choosing an option costs, in money.
     *
     * @param option the option
     * @param state  the player's state
     * @return the cost, 0 for nothing
     */
    public static long cost(Decisions.Option option, PlayerSnapshot state) {
        return Math.round(option.cost() * state.getFundingRatePerSeason());
    }

    /**
     * Returns what an option shows in advance, its cost first: "$ 900 · 70 % chance of...".
     *
     * @param option the option
     * @param state  the player's state
     * @return the line, or {@code ""}
     */
    public static String shows(Decisions.Option option, PlayerSnapshot state) {
        long cost = cost(option, state);
        String shows = option.shows().endsWith(".") ? option.shows().substring(0, option.shows().length() - 1)
            : option.shows();
        if (cost <= 0) {
            return shows;
        }
        return shows.isEmpty() ? money(cost) : money(cost) + " · " + shows;
    }

    /**
     * Fills an outcome's names: {@code {name}} and {@code {second}} for the people the option took.
     *
     * @param text   the text
     * @param state  the player's state
     * @param person the person, or {@code null}
     * @param second the second of a pair, or {@code null}
     * @return the text
     */
    public static String fill(String text, PlayerSnapshot state, String person, String second) {
        return text.replace("{name}", name(state, person)).replace("{second}", name(state, second));
    }

    private static String name(PlayerSnapshot state, String id) {
        if (id == null) {
            return "the engineer";
        }
        return state.getSetSpecialists().stream().filter(person -> person.getId().equals(id)).findFirst()
            .map(Personnel::getName).orElse("the engineer");
    }

    /**
     * Words what a decision did, one line per effect.
     *
     * @param effects the effects
     * @param state   the player's state, for names
     * @return the lines
     */
    public static List<EffectLine> effects(List<DecisionEffect> effects, PlayerSnapshot state) {
        List<EffectLine> lines = new ArrayList<>();
        for (DecisionEffect effect : effects) {
            double amount = effect.amount();
            lines.add(switch (effect.kind()) {
                case RELIABILITY -> new EffectLine(String.format(Locale.ROOT, "Reliability %s%.1f", sign(amount),
                    Math.abs(amount)), amount > 0);
                case FUNDS -> new EffectLine("Funds " + (amount < 0 ? "− " : "+ ") + money(Math.round(Math.abs(
                    amount))), amount > 0);
                case RD_FACTOR -> new EffectLine(String.format(Locale.ROOT, "R&D %.0f %% %s for %s", Math.abs(amount - 1)
                    * 100, amount >= 1 ? "faster" : "slower", Outlook.seasons(effect.seasons())), amount >= 1);
                case MORALE -> new EffectLine(String.format(Locale.ROOT, "%s %s%.0f", effect.subject() == null
                    ? "The team's morale" : name(state, effect.subject()) + "'s morale", sign(amount), Math.abs(amount)),
                    amount > 0);
                case UNAVAILABLE -> new EffectLine(name(state, effect.subject()) + " away for "
                    + Outlook.seasons(effect.seasons()), false);
                case RAPPORT -> {
                    String[] pair = effect.subject() == null ? new String[0] : effect.subject().split("\\+");
                    String who = pair.length == 2 ? People.surname(name(state, pair[0])) + " and "
                        + People.surname(name(state, pair[1])) : "The pair";
                    yield new EffectLine(String.format(Locale.ROOT, "%s: rapport %s%.0f", who, sign(amount),
                        Math.abs(amount)), amount > 0);
                }
                case SKILL -> new EffectLine(String.format(Locale.ROOT, "%s: skill %s%.1f", name(state,
                    effect.subject()), sign(amount), Math.abs(amount)), amount > 0);
            });
        }
        return lines;
    }

    /**
     * Words a settled decision for the turn report's news: what applied, and its effects.
     *
     * @param settled the event
     * @param state   the player's state, for names
     * @return the line's title and text, empty if the decision is gone from the file
     */
    public static Optional<String[]> reportLine(DecisionSettled settled, PlayerSnapshot state) {
        Decisions.Decision decision = Decisions.standard().byId(settled.decisionId()).orElse(null);
        if (decision == null) {
            return Optional.empty();
        }
        String option = decision.option(settled.optionId()).map(Decisions.Option::label).orElse("");
        String outcome = decision.outcome(settled.outcomeId()).map(Decisions.Outcome::headline).orElse("");
        String title = (settled.lapsed() ? "You left the decision on " : "Your decision on ")
            + ContentNames.of(settled.componentId()) + ": " + decision.headline();
        List<String> effects = effects(settled.effects(), state).stream().map(EffectLine::text).toList();
        String text = (settled.lapsed() ? "Nobody decided, so “" + option + "” applied. No new decision "
            + "comes after a lapse until you act again." : "“" + option + "”: " + fill(outcome, state, null,
            null) + ".") + (effects.isEmpty() ? "" : " " + String.join(", ", effects) + ".");
        return Optional.of(new String[] {title, text});
    }

    /**
     * Returns a settled decision's tone for the news: good when it gained, bad when it cost, neutral
     * when nothing changed (a lapse that skipped).
     *
     * @param settled the event
     * @return 1, -1 or 0
     */
    public static int tone(DecisionSettled settled) {
        boolean lost = settled.effects().stream().anyMatch(effect -> effect.kind() == DecisionEffect.Kind.RELIABILITY
            && effect.amount() < 0);
        boolean gained = settled.effects().stream().anyMatch(effect -> effect.kind() == DecisionEffect.Kind.RELIABILITY
            && effect.amount() > 0 || effect.kind() == DecisionEffect.Kind.RD_FACTOR && effect.amount() > 1);
        return lost ? -1 : gained ? 1 : 0;
    }

    /**
     * Returns the countdown words for "Until the turn resolves · 6 h 12 m", from the chips' text.
     *
     * @param nextTurnText the chips' "NEXT TURN IN 6 H 12 M"
     * @return the words
     */
    public static String untilResolved(String nextTurnText) {
        String rest = nextTurnText == null ? "" : nextTurnText.replaceFirst("(?i)^next turn in\\s*", "");
        return rest.isEmpty() || rest.equalsIgnoreCase(nextTurnText) ? "Until the turn resolves"
            : "Until the turn resolves \u00b7 " + rest.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns which R&amp;D board a component is on: the VAB's for a rocket.
     *
     * @param componentId the component
     * @param catalog     the content pack
     * @return whether it's a rocket
     */
    public static boolean rocket(String componentId, ContentCatalog catalog) {
        return catalog.missionComponent(componentId).map(MissionComponent::hasRocketSpec).orElse(false);
    }

    private static String sign(double amount) {
        return amount < 0 ? "−" : "+";
    }

    private static String money(long amount) {
        return String.format(Locale.ROOT, "$ %,.1f", (double) amount);
    }
}
