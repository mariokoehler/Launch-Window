package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.content.PrPress.Effect;
import de.mkoehler.launchwindow.content.PrPress.EffectKind;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.HiringPoolBonus;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * What a press story does besides its prestige ({@code design.md} &sect;2.2(c), user decision
 * 2026-10-06): a bigger hiring pool, morale, a skill, cash, a higher funding rate, timed R&amp;D or
 * cost multipliers, or a setback to research. The effects are authored in {@code pr-press.json}
 * ({@link PrPress.EffectKind} says what each one's fields mean).
 *
 * <p>The PR phase runs before the budget and before {@link RandomEvents} counts the timed slots
 * down, so a running-cost effect of N seasons covers this season's budget and the next N - 1, and
 * an R&amp;D effect is stored one season longer to cover this season's research and the next
 * N - 1 too. A pool bonus of N covers this season's refresh and the next N - 1.</p>
 *
 * @author Mario Koehler
 */
public final class PressEffects {

    private PressEffects() {
    }

    /**
     * Whether a story can run this season: a research setback needs a component in R&amp;D that is
     * expected to gain something, and a raised funding rate needs at least
     * {@link RandomEvents#MIN_SEASONS_UNTIL_REVIEW} seasons before a review resets it (none this
     * season, since the review runs after the PR phase).
     *
     * @param story          the story
     * @param snapshot       the player
     * @param catalog        the content, or {@code null} when there is none (a research setback can't run)
     * @param seasonsElapsed the player's seasons elapsed counting this one
     * @return whether every effect has something to act on
     */
    public static boolean usable(PrPress.Story story, PlayerSnapshot snapshot, ContentCatalog catalog,
            int seasonsElapsed) {
        Objects.requireNonNull(story, "story must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        if (!story.effects(EffectKind.RESEARCH_SETBACK).isEmpty()
                && researchSetbackCandidates(snapshot, catalog).isEmpty()) {
            return false;
        }
        if (!story.effects(EffectKind.FUNDING_RATE).isEmpty()) {
            int untilReview = RandomEvents.seasonsUntilReview(seasonsElapsed);
            boolean reviewThisSeason = seasonsElapsed % BudgetProgress.SEASONS_PER_REVIEW == 0;
            return !reviewThisSeason && untilReview >= RandomEvents.MIN_SEASONS_UNTIL_REVIEW;
        }
        return true;
    }

    /**
     * Returns the share of the season's yield a story adds or takes.
     *
     * @param story        the story
     * @param defaultShare the share when the story doesn't set one
     * @return the share
     */
    public static double prestigeShare(PrPress.Story story, double defaultShare) {
        return story.effects(EffectKind.PRESTIGE_SHARE).stream().mapToDouble(Effect::amount).findFirst()
            .orElse(defaultShare);
    }

    /**
     * Applies a story's effects, apart from its prestige share. Call only for a story that is
     * {@link #usable}.
     *
     * @param story     the story
     * @param astronaut the astronaut it is about
     * @param snapshot  the player; mutated in place
     * @param catalog   the content, or {@code null}
     * @param random    the random source (only a research setback draws, to pick its component)
     * @return the effects as applied, for the turn report; {@link EffectKind#PRESTIGE_SHARE} is left out
     */
    public static List<Effect> apply(PrPress.Story story, Astronaut astronaut, PlayerSnapshot snapshot,
            ContentCatalog catalog, RandomGenerator random) {
        Objects.requireNonNull(story, "story must not be null");
        Objects.requireNonNull(astronaut, "astronaut must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(random, "random must not be null");
        List<Effect> applied = new ArrayList<>();
        for (Effect effect : story.effects()) {
            switch (effect.kind()) {
                case PRESTIGE_SHARE -> {
                }
                case HIRING_POOL -> {
                    snapshot.getHiringPoolBonuses().add(new HiringPoolBonus(group(effect).name(),
                        (int) effect.amount(), 0, effect.seasons()));
                    applied.add(effect);
                }
                case STANDOUT_CANDIDATE -> {
                    snapshot.getHiringPoolBonuses().add(new HiringPoolBonus(group(effect).name(),
                        (int) effect.amount(), (int) effect.amount(), effect.seasons()));
                    applied.add(effect);
                }
                case GROUP_MORALE -> {
                    for (Personnel person : RandomEvents.groupRoster(snapshot, group(effect))) {
                        shiftMorale(person, effect.amount());
                    }
                    applied.add(effect);
                }
                case ASTRONAUT_MORALE -> {
                    shiftMorale(astronaut, effect.amount());
                    applied.add(effect);
                }
                case ASTRONAUT_SKILL -> {
                    AstronautSkill skill = AstronautSkill.valueOf(effect.target());
                    astronaut.setSkill(skill, Math.max(0.0, astronaut.getSkill(skill) + effect.amount()));
                    applied.add(effect);
                }
                case FUNDS -> {
                    long money = Math.round(snapshot.getFundingRatePerSeason() * effect.amount());
                    snapshot.setFunds(snapshot.getFunds() + money);
                    applied.add(new Effect(EffectKind.FUNDS, null, money, 0));
                }
                case FUNDING_RATE -> {
                    long raise = (long) Math.floor(snapshot.getFundingRatePerSeason() * effect.amount());
                    snapshot.setFundingRatePerSeason(snapshot.getFundingRatePerSeason() + raise);
                    applied.add(new Effect(EffectKind.FUNDING_RATE, null, raise, 0));
                }
                case RD_RESULTS -> {
                    // One longer: RandomEvents counts the slot down before this season's research.
                    TimedEffects.addRdResults(snapshot, effect.amount(), effect.seasons() + 1);
                    applied.add(effect);
                }
                case RUNNING_COSTS -> {
                    TimedEffects.addCostMultiplier(snapshot, effect.amount(), effect.seasons());
                    applied.add(effect);
                }
                case RESEARCH_SETBACK -> {
                    List<String> candidates = researchSetbackCandidates(snapshot, catalog);
                    if (!candidates.isEmpty()) {
                        String componentId = candidates.get(random.nextInt(candidates.size()));
                        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
                        double cut = ResearchPlanning.expectedSeasonGain(snapshot, catalog, componentId)
                            * effect.amount();
                        double before = owned.getCurrentReliability();
                        owned.setCurrentReliability(Math.max(owned.getFloorReliability(), before - cut));
                        applied.add(new Effect(EffectKind.RESEARCH_SETBACK, componentId,
                            before - owned.getCurrentReliability(), 0));
                    }
                }
            }
        }
        return applied;
    }

    /** Components in R&amp;D that are expected to gain something this season, in roster order. */
    private static List<String> researchSetbackCandidates(PlayerSnapshot snapshot, ContentCatalog catalog) {
        if (catalog == null) {
            return List.of();
        }
        return RandomEvents.componentsInResearch(snapshot).stream()
            .filter(id -> snapshot.getMissionComponents().containsKey(id))
            .filter(id -> ResearchPlanning.expectedSeasonGain(snapshot, catalog, id) > 0)
            .toList();
    }

    private static PersonnelGroup group(Effect effect) {
        return PersonnelGroup.valueOf(effect.target());
    }

    private static void shiftMorale(Personnel person, double amount) {
        person.setMorale((int) Math.max(0, Math.min(100, person.getMorale() + Math.round(amount))));
    }
}
