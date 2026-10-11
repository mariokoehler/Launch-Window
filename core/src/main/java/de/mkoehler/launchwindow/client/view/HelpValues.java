package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.BuildingSchedule;
import de.mkoehler.launchwindow.content.RandomEventRoster;
import de.mkoehler.launchwindow.model.Ambition;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.Careers;
import de.mkoehler.launchwindow.rules.DepartmentSalaries;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.MissionPrestige;
import de.mkoehler.launchwindow.rules.MissionStepResolution;
import de.mkoehler.launchwindow.rules.PersonnelLifecycle;
import de.mkoehler.launchwindow.rules.PrDuty;
import de.mkoehler.launchwindow.rules.SkillGrowth;
import de.mkoehler.launchwindow.rules.TraitEffects;
import de.mkoehler.launchwindow.rules.TrainingProgress;
import de.mkoehler.launchwindow.rules.Chemistry;
import de.mkoehler.launchwindow.rules.Contracts;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.Personality;
import de.mkoehler.launchwindow.rules.Poaching;
import de.mkoehler.launchwindow.rules.PrestigeConversion;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
     * @param budget    the game's budget schedule
     * @param buildings the content pack's buildings
     * @return the values
     */
    public static HelpValues of(BudgetSchedule budget, BuildingSchedule buildings) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("game.startDate", GameDate.ofTurn(1).label());

        v.put("budget.startFunds", whole(budget.initialFunds()));
        v.put("budget.startPrestige", whole(budget.initialPrestige()));
        v.put("budget.startFunding", whole(budget.initialFundingRate()));
        v.put("budget.seasonsPerReview", whole(BudgetProgress.SEASONS_PER_REVIEW));
        v.put("budget.fundsPerPrestige", whole(budget.fundsPerPrestige()));
        v.put("budget.coveredSeasons", whole(PrestigeConversion.COVERED_SEASONS));

        v.put("rnd.slots", whole(MissionComponent.RD_SLOT_COUNT));
        v.put("outlook.ready", whole(Math.round(Advisor.READY_RELIABILITY)));
        List<String> milestones = new java.util.ArrayList<>();
        for (double milestone : Outlook.MILESTONES) {
            milestones.add(whole(Math.round(milestone)));
        }
        v.put("outlook.milestones", milestones.size() == 1 ? milestones.get(0)
            : String.join(", ", milestones.subList(0, milestones.size() - 1)) + " and "
                + milestones.get(milestones.size() - 1));

        v.put("buildings.astronauts", caps(buildings, Building.ASTRONAUT_CENTER));
        v.put("buildings.controllers", caps(buildings, Building.MISSION_CONTROL));
        v.put("buildings.engineers", caps(buildings, Building.SET_CENTER));
        v.put("buildings.programs", caps(buildings, Building.HEADQUARTERS));
        v.put("buildings.rockets", caps(buildings, Building.VEHICLE_ASSEMBLY));
        v.put("buildings.upkeepStart", whole(buildings.levels().keySet().stream()
            .mapToLong(building -> buildings.maintenance(building, 1)).sum()));

        v.put("people.idleMorale", whole(PersonnelLifecycle.IDLE_MORALE_LOSS_PER_SEASON));
        v.put("people.busyMorale", whole(PersonnelLifecycle.BUSY_MORALE_GAIN_PER_SEASON));
        v.put("people.resignAt", whole(PersonnelLifecycle.RESIGNATION_MORALE_FLOOR));
        v.put("people.retireAstronaut", whole(PersonnelLifecycle.ASTRONAUT_RETIREMENT_AGE));
        v.put("people.retireController", whole(PersonnelLifecycle.MISSION_CONTROL_RETIREMENT_AGE));
        v.put("people.retireEngineer", whole(PersonnelLifecycle.SET_RETIREMENT_AGE));
        v.put("people.salaryMin", whole(HiringPool.SALARY_MIN));
        v.put("people.salaryMax", whole(HiringPool.SALARY_MAX));
        v.put("people.salaryAdjust", whole(DepartmentSalaries.MAX_PERCENT));
        v.put("hiring.batch", whole(HiringPool.HIRING_BATCH_SIZE));
        v.put("hiring.signingMorale", whole(Math.round((HiringPool.SIGNING_BONUS_MORALE_FACTOR - 1) * 100)));
        v.put("hiring.experience", whole(Math.round(HiringPool.EXPERIENCE_SKILL_RANGE)));
        v.put("hiring.rookieLearning", HiringPool.ROOKIE_MIN_LEARNING_POTENTIAL + " to "
            + HiringPool.ROOKIE_MAX_LEARNING_POTENTIAL);
        v.put("hiring.veteranLearning", HiringPool.VETERAN_MIN_LEARNING_POTENTIAL + " to "
            + HiringPool.VETERAN_MAX_LEARNING_POTENTIAL);
        v.put("training.basicAstronaut", whole(TrainingProgress.ASTRONAUT_BASIC_TRAINING_SEASONS));
        v.put("training.basicEngineer", whole(TrainingProgress.SET_BASIC_TRAINING_SEASONS));
        v.put("training.basicController", whole(TrainingProgress.MISSION_CONTROL_BASIC_TRAINING_SEASONS));
        v.put("training.advanced", whole(TrainingProgress.ADVANCED_TRAINING_SEASONS));
        v.put("training.expertSkill", whole(Math.round(SkillGrowth.EXPERT_TRAINING_SKILL_THRESHOLD)));
        v.put("training.expertBase", whole(Math.round(SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE)));
        v.put("training.expertBest", whole(Math.round(SkillGrowth.EXPERT_TRAINING_BASE_SUCCESS_CHANCE
            + SkillGrowth.EXPERT_TRAINING_SKILL_BONUS_MAX)));

        v.put("pr.maxYield", whole(PrDuty.MAX_YIELD));
        v.put("pr.perFlight", whole(PrDuty.PER_FLIGHT));
        v.put("pr.perFirst", whole(PrDuty.PER_FIRST));
        v.put("pr.shares", and(PrDuty.SHARES.stream().map(share -> whole(Math.round(share * 100))).toList()));
        v.put("pr.rustFree", whole(PrDuty.RUST_FREE_SEASONS));
        v.put("pr.rust", whole(Math.round(PrDuty.RUST_PER_SEASON)));
        v.put("pr.pressChance", whole(Math.round(PrDuty.PRESS_CHANCE * 100)));
        v.put("pr.goodPress", whole(Math.round(PrDuty.GOOD_PRESS_CHANCE * 100)));
        v.put("pr.pressShare", whole(Math.round(PrDuty.PRESS_SHARE * 100)));
        v.put("pr.expertShare", whole(Math.round(MissionStepResolution.EXPERT_RESCUE_ON_PR_TOUR_EFFICIENCY * 100)));

        v.put("ambition.chanceAmbitious", whole(Careers.AMBITION_PERCENT_AMBITIOUS));
        v.put("ambition.chanceGlory", whole(Careers.AMBITION_PERCENT_GLORY_SEEKER));
        v.put("ambition.chanceOthers", whole(Careers.AMBITION_PERCENT_OTHERS));
        v.put("ambition.nextAmbitious", whole(Careers.NEXT_PERCENT_AMBITIOUS));
        v.put("ambition.nextOthers", whole(Careers.NEXT_PERCENT_OTHERS));
        v.put("ambition.sayAfter", whole(Careers.SAY_AFTER_SEASONS));
        v.put("ambition.windowMin", whole(Arrays.stream(Ambition.Kind.values()).mapToInt(Ambition.Kind::window)
            .min().orElse(0)));
        v.put("ambition.windowMax", whole(Arrays.stream(Ambition.Kind.values()).mapToInt(Ambition.Kind::window)
            .max().orElse(0)));
        v.put("ambition.ambitiousWindow", whole(-Careers.WINDOW_AMBITIOUS));
        v.put("ambition.steadyWindow", whole(Careers.WINDOW_STEADY));
        v.put("ambition.fulfilledMorale", whole(Careers.FULFILLED_MORALE));
        v.put("ambition.fulfilledLoyalty", whole(Careers.FULFILLED_LOYALTY));
        v.put("ambition.fulfilledLoyaltyAmbitious", whole(Careers.FULFILLED_LOYALTY_AMBITIOUS));
        v.put("ambition.passedMorale", whole(-Careers.PASSED_OVER_MORALE));
        v.put("ambition.passedLoyalty", whole(-Careers.PASSED_OVER_LOYALTY));
        v.put("ambition.impossibleMorale", whole(-Careers.IMPOSSIBLE_MORALE));
        v.put("ambition.impossibleLoyalty", whole(-Careers.IMPOSSIBLE_LOYALTY));
        v.put("ambition.ranOutMorale", whole(-Careers.RAN_OUT_MORALE));
        v.put("ambition.ranOutLoyalty", whole(-Careers.RAN_OUT_LOYALTY));
        v.put("ambition.disappointed", whole(Careers.DISAPPOINTED_SEASONS));
        v.put("loyalty.start", whole(Careers.LOYALTY_START));
        v.put("loyalty.perSeason", whole(Careers.LOYALTY_PER_SEASON));
        v.put("careers.kept", whole(Careers.CAREER_KEPT));

        v.put("trait.workaholicRd", percentMore(TraitEffects.WORKAHOLIC_RD));
        v.put("trait.workaholicIdle", whole(TraitEffects.WORKAHOLIC_IDLE_MORALE));
        v.put("trait.ambitiousTraining", percentMore(TraitEffects.AMBITIOUS_TRAINING));
        v.put("trait.ambitiousPatience", whole(TraitEffects.AMBITIOUS_PATIENCE_SEASONS));
        v.put("trait.ambitiousLoss", whole(TraitEffects.AMBITIOUS_MORALE_LOSS));
        v.put("trait.gloryFlight", percentMore(TraitEffects.GLORY_SEEKER_FLIGHT));
        v.put("trait.gloryLoss", whole(TraitEffects.GLORY_SEEKER_PASSED_OVER));
        v.put("trait.perfectionistRd", percentLess(TraitEffects.PERFECTIONIST_RD));
        v.put("trait.perfectionistCap", whole(Math.round(TraitEffects.PERFECTIONIST_CAP)));
        v.put("trait.cool", whole(Math.round(TraitEffects.COOL_SAVING_THROW)));
        v.put("trait.restlessFresh", percentMore(TraitEffects.RESTLESS_FRESH));
        v.put("trait.restlessFreshSeasons", whole(TraitEffects.RESTLESS_FRESH_SEASONS));
        v.put("trait.restlessStale", percentLess(TraitEffects.RESTLESS_STALE));
        v.put("trait.restlessStaleAfter", whole(TraitEffects.RESTLESS_STALE_AFTER));
        v.put("trait.quickStudy", percentMore(TraitEffects.QUICK_STUDY_TRAINING));
        v.put("trait.mentored", percentMore(TraitEffects.MENTORED));

        v.put("missions.blendAbove", whole(Math.round(MissionStepResolution.CREW_BLEND_HARDWARE_THRESHOLD)));
        v.put("missions.crewWeight", whole(Math.round(MissionStepResolution.CREW_BLEND_WEIGHT * 100)));
        v.put("missions.catastrophic", whole(Math.round(MissionStepResolution.CATASTROPHIC_BAND_FRACTION * 100)));
        v.put("missions.failure", whole(Math.round(MissionStepResolution.FAILURE_BAND_FRACTION * 100)));
        v.put("missions.minor", whole(Math.round((1 - MissionStepResolution.CATASTROPHIC_BAND_FRACTION
            - MissionStepResolution.FAILURE_BAND_FRACTION) * 100)));
        v.put("missions.expertBoosts", and(Stream.of(MissionStepResolution.EXPERT_RESCUE_FIRST_USE_BOOST_PERCENT,
            MissionStepResolution.EXPERT_RESCUE_SECOND_USE_BOOST_PERCENT,
            MissionStepResolution.EXPERT_RESCUE_THIRD_USE_BOOST_PERCENT).map(boost -> whole(Math.round(boost)))
            .toList()));
        v.put("missions.expertCap", whole(Math.round(MissionStepResolution.EXPERT_RESCUE_PROBABILITY_CAP)));
        v.put("missions.expertAway", whole(Math.round(
            MissionStepResolution.EXPERT_RESCUE_OFF_MISSION_CONTROLLER_EFFICIENCY * 100)));
        v.put("prestige.second", whole(MissionPrestige.SECOND_TIME_PERCENT));
        v.put("prestige.later", whole(MissionPrestige.LATER_TIME_PERCENT));
        v.put("events.bad", whole(RandomEventRoster.baspmClose().badChancePercent()));
        v.put("events.good", whole(RandomEventRoster.baspmClose().goodChancePercent()));

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
        v.put("poaching.matchCeiling", whole(Poaching.MATCH_CEILING_FACTOR));
        v.put("poaching.fileLevel", whole(Intelligence.PERSONNEL_FILE_LEVEL));

        v.put("intelligence.maxLevel", whole(Intelligence.MAX_LEVEL));
        v.put("intelligence.costs", list(Arrays.stream(Intelligence.COST_PER_SEASON)
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

        v.put("contracts.listingTurns", whole(Contracts.LISTING_TURNS));
        v.put("contracts.flownPoints", "+" + whole(Contracts.RECORD_FLOWN_POINTS));
        v.put("contracts.failedPoints", "\u2212" + whole(-Contracts.RECORD_FAILED_POINTS));
        v.put("contracts.recordCap", whole(Contracts.RECORD_CAP));
        v.put("contracts.tiers", whole(Contracts.TIERS));
        v.put("contracts.margins", and(Contracts.MARGIN_PERCENTS.stream().map(HelpValues::whole).toList()));
        v.put("contracts.window", whole(Contracts.LAUNCH_WINDOW_TURNS));
        v.put("contracts.lapsePrestige", whole(Contracts.LAPSE_PRESTIGE));
        v.put("contracts.crewLossPrestige", whole(Contracts.CREW_LOSS_PRESTIGE));
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

    /** A factor as the percent it adds: 1.2 is "20". */
    private static String percentMore(double factor) {
        return whole(Math.round((factor - 1) * 100));
    }

    /** A factor as the percent it takes off: 0.85 is "15". */
    private static String percentLess(double factor) {
        return whole(Math.round((1 - factor) * 100));
    }

    private static String caps(BuildingSchedule buildings, Building building) {
        return and(buildings.of(building).caps().stream().map(cap -> whole(cap)).toList());
    }

    /** "a, b and c". */
    private static String and(List<String> items) {
        if (items.size() < 2) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    /** "a, b or c". */
    private static String list(List<String> items) {
        if (items.size() < 2) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " or " + items.get(items.size() - 1);
    }
}
