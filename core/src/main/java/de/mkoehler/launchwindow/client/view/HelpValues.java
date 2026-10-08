package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.Chemistry;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.Personality;
import de.mkoehler.launchwindow.rules.Poaching;
import de.mkoehler.launchwindow.rules.PrestigeConversion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The numbers the help's text names ({@link Help}), read from the rules' own constants and the
 * game's budget schedule, so a rule change shows in the help at once. Each token is written
 * {@code {name}} in {@code help.json}; the values are bare numbers or short lists, and the text
 * adds the unit.
 *
 * @author Mario Koehler
 */
public final class HelpValues {

    private static final Pattern TOKEN = Pattern.compile("\\{([a-zA-Z0-9.]+)}");

    private final Map<String, String> values;

    private HelpValues(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    /**
     * Builds the values for a game.
     *
     * @param budget the game's budget schedule
     * @return the values
     */
    public static HelpValues of(BudgetSchedule budget) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("game.startDate", GameDate.ofTurn(1).label());

        v.put("budget.startFunds", whole(budget.initialFunds()));
        v.put("budget.startPrestige", whole(budget.initialPrestige()));
        v.put("budget.startFunding", whole(budget.initialFundingRate()));
        v.put("budget.seasonsPerReview", whole(BudgetProgress.SEASONS_PER_REVIEW));
        v.put("budget.fundsPerPrestige", whole(budget.fundsPerPrestige()));
        v.put("budget.coveredSeasons", whole(PrestigeConversion.COVERED_SEASONS));

        v.put("rnd.slots", whole(MissionComponent.RD_SLOT_COUNT));

        v.put("chemistry.band", whole(Math.round(Chemistry.OUTPUT_BAND * 100)));
        v.put("chemistry.rapportDrift", whole(Math.round(Personality.RAPPORT_DRIFT)));
        v.put("chemistry.rapportShown", whole(Math.round(Personality.RAPPORT_SHOWN_AT)));
        v.put("chemistry.rapportMax", whole(Math.round(Personality.RAPPORT_MAX)));
        v.put("chemistry.revealMin", whole(Personality.REVEAL_MIN_SEASONS));
        v.put("chemistry.revealMax", whole(Personality.REVEAL_MAX_SEASONS));

        v.put("poaching.raises", list(Poaching.RAISE_PERCENTS.stream().map(raise -> "+" + raise).toList()));
        v.put("poaching.matches", list(Poaching.MATCH_PERCENTS.stream()
            .map(match -> match == 0 ? "never" : "+" + match + " %").toList()));
        v.put("poaching.defaultMatch", whole(Poaching.DEFAULT_MATCH_PERCENT));
        v.put("poaching.minTenure", whole(Poaching.MIN_TENURE_SEASONS));
        v.put("poaching.cooldown", whole(Poaching.APPROACH_COOLDOWN_SEASONS));
        v.put("poaching.offersPerSeason", whole(Poaching.OFFERS_PER_SEASON));
        v.put("poaching.acceptBase", list(Poaching.ACCEPT_BASE_PERCENTS.stream().map(HelpValues::whole).toList()));
        v.put("poaching.loyaltyPivot", whole(Poaching.LOYALTY_PIVOT));
        v.put("poaching.disappointed", whole(Poaching.DISAPPOINTED_POINTS));
        v.put("poaching.lowMoraleBelow", whole(Poaching.LOW_MORALE_BELOW));
        v.put("poaching.lowMorale", whole(Poaching.LOW_MORALE_POINTS));
        v.put("poaching.restless", whole(Poaching.RESTLESS_POINTS));
        v.put("poaching.steady", whole(-Poaching.STEADY_POINTS));
        v.put("poaching.acceptMax", whole(Poaching.ACCEPT_MAX_PERCENT));
        v.put("poaching.matchLoyalty", whole(Poaching.MATCH_LOYALTY));
        v.put("poaching.fileLevel", whole(Intelligence.PERSONNEL_FILE_LEVEL));

        v.put("intelligence.maxLevel", whole(Intelligence.MAX_LEVEL));
        v.put("intelligence.costs", list(java.util.Arrays.stream(Intelligence.COST_PER_SEASON)
            .mapToObj(HelpValues::whole).toList()));
        v.put("intelligence.checkLevel", whole(Intelligence.CHECK_LEVEL));
        v.put("intelligence.assessmentLevel", whole(Intelligence.ASSESSMENT_LEVEL));
        v.put("intelligence.fileLevel", whole(Intelligence.PERSONNEL_FILE_LEVEL));
        v.put("intelligence.passiveCandidate", whole(Intelligence.PASSIVE_CANDIDATE_PERCENT));
        v.put("intelligence.tidbit2", whole(Intelligence.TIDBIT_PERCENT[2]));
        v.put("intelligence.tidbit3", whole(Intelligence.TIDBIT_PERCENT[3]));
        v.put("intelligence.successBase", whole(Intelligence.SUCCESS_BASE_PERCENT));
        v.put("intelligence.successPerLevel", whole(Intelligence.SUCCESS_PER_LEVEL));
        v.put("intelligence.successLowest", whole(Intelligence.SUCCESS_BASE_PERCENT
            - Intelligence.SUCCESS_PER_LEVEL * Intelligence.MAX_LEVEL));
        v.put("intelligence.catchBase", whole(Intelligence.CATCH_BASE_PERCENT));
        v.put("intelligence.catchPerLevel", whole(Intelligence.CATCH_PER_LEVEL));
        v.put("intelligence.caughtPrestige", whole(Intelligence.CAUGHT_PRESTIGE));
        return new HelpValues(v);
    }

    /**
     * Fills a text's tokens. An unknown token is left as it is (the help's test catches it).
     *
     * @param text the text
     * @return the text with its numbers
     */
    public String fill(String text) {
        Matcher matcher = TOKEN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            matcher.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Returns a text's tokens that have no value.
     *
     * @param text the text
     * @return the unknown tokens' names
     */
    public Set<String> unknown(String text) {
        Set<String> unknown = new TreeSet<>();
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            if (!values.containsKey(matcher.group(1))) {
                unknown.add(matcher.group(1));
            }
        }
        return unknown;
    }

    private static String whole(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /** "a, b or c". */
    private static String list(List<String> items) {
        if (items.size() < 2) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " or " + items.get(items.size() - 1);
    }
}
