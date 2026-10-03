package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.FlightTier;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/**
 * <p>The mission season-advance pass: walks one season's step batch for
 * every one of a player's {@link InProgressMission}s - an M2 rules-engine
 * resolver (see {@code design.md} &sect;5 M2, &sect;2.5(f)). Headless, per
 * {@code design.md} &sect;3.10 - no persistence/network/libGDX
 * dependency.</p>
 *
 * <p><b>Crew/controller skill contribution is an average across every
 * assigned person, not a sum</b> (user decision, 2026-10-02, see
 * {@code design.md} &sect;2.5(f)): for a given skill, this resolver takes
 * the mean skill value across every astronaut/Mission Control officer
 * assigned to the mission, then weights that average by the step's own
 * involvement percentage, exactly like {@link MissionStepResolution}
 * expects. A sum would grow with crew size and push the inputs to
 * {@link MissionStepResolution#computeBaseProbability}'s weighted-average
 * blend past the 0-100 scale {@code design.md} &sect;2.5(e)'s own fix
 * depends on.</p>
 *
 * <p><b>On {@code FAILURE}/{@code CATASTROPHIC}, the mission ends
 * immediately</b> - no further steps run that season or any later season,
 * per {@code baspm.md} &sect;8.2's original trace. <b>A {@code CATASTROPHIC}
 * outcome permanently loses the entire assigned crew</b> (user decision,
 * 2026-10-02, see {@code design.md} &sect;2.5(f) for the real-world
 * precedent this is based on), reported as a {@link PersonnelDeparture}
 * per lost astronaut with {@link DepartureReason#LOST_IN_ACCIDENT}. Mission
 * Control officers are never crew and are never at risk. On any mission
 * end - successful completion, {@code FAILURE}, or {@code CATASTROPHIC} -
 * every surviving assigned person is released: back to
 * {@link AssignmentState#IDLE} with {@link Personnel#setAssignedMissionId}
 * cleared, or retired on the spot with a {@link DepartureReason#RETIRED}
 * {@link PersonnelDeparture} if they have already reached their group's
 * retirement age (user decision, 2026-10-02, see {@link #releaseGroup}'s
 * own Javadoc for why this check happens here rather than waiting for
 * {@link PersonnelLifecycle}'s own next pass).</p>
 *
 * <p><b>Composition order for whoever wires this into one season pass
 * (M4):</b> {@link PersonnelLifecycle}, then this class, then
 * {@link TrainingProgress}, then {@link ResearchProgress}, then
 * {@code HiringPool.refreshCandidates} last - running right after
 * {@link PersonnelLifecycle} means a crew lost or released this season is
 * already reflected before the later resolvers run against the same
 * snapshot.</p>
 *
 * <p><b>Post-mission-season reliability change and mission-based
 * skill growth</b> (see {@code design.md} &sect;2.5(g)) both run once
 * per in-progress mission per season, right after that season's step
 * batch resolves, regardless of outcome - re-deriving
 * {@code baspm.md} &sect;10.4/&sect;9.3, aggregated per season rather
 * than per individual step (user decision, 2026-10-02). See
 * {@link #applyPostSeasonReliabilityChange}/{@link #applyPostSeasonSkillGrowth}'s
 * own Javadoc for the full rules.</p>
 *
 * <p><b>Expert rescue</b> (see {@code design.md} &sect;2.5(i)) lets an
 * Expert-flagged Astronaut/Mission Control officer (see &sect;2.2(c))
 * boost a step's saving-throw probability once the step's first roll has
 * already failed - a roster-wide eligibility pool, not limited to the
 * glitching mission's own assigned crew/controllers. See
 * {@link #resolveStep}/{@link #applyExpertRescue}'s own Javadoc for the
 * full mechanic (eligibility, stacking with exhaustion, the cap).</p>
 *
 * <p><b>Deliberately out of scope for this slice</b> (see
 * {@code design.md} &sect;2.5(g)'s own deferred list): prestige/goals,
 * and the {@code baspm.md} &sect;11.3 goal-dependency reliability
 * modifier (always 0 here). The mission's own cost at launch is done -
 * see {@link MissionLaunch}/{@code design.md} &sect;2.5(h).</p>
 *
 * @author Mario Koehler
 */
public final class MissionProgress {

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MissionProgress() {
    }

    /**
     * Advances every one of the given snapshot's in-progress missions by
     * one season: resolves the current season's step batch for each,
     * ending (and removing) any mission that fails, loses its crew, or
     * completes its final season.
     *
     * @param snapshot the player snapshot to advance; mutated in place
     * @param catalog  the content catalog mission configurations and
     *                 mission components are looked up in
     * @param random   the random source {@link MissionStepResolution}
     *                 draws its rolls from
     * @return this pass's {@link MissionStepResolved}, {@link PersonnelDeparture},
     *     {@link MissionReliabilityChanged}, {@link MissionSkillGrowthApplied},
     *     {@link ResearchComponentMaxed}, and {@link ExpertRescueApplied}
     *     events, in no particular cross-mission order; empty if there
     *     were no in-progress missions
     * @throws NullPointerException if any argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random) {
        return advanceSeason(snapshot, catalog, random, new GoalTally(), 0);
    }

    /**
     * Advances every in-progress mission by one season, with goals settling
     * against a game-wide {@link GoalTally}. The tally is read here and only
     * changed by the caller's {@link GoalTally#commit} after every player has
     * resolved, so same-turn achievers all price first-time (design.md &sect;2.6(d)).
     *
     * @param snapshot   the player snapshot to advance; mutated in place
     * @param catalog    the content catalog every mission was launched from
     * @param random     the random source for step rolls and post-flight draws
     * @param goalTally  the game-wide goal counts, read but not changed by this call
     * @param gameTurn   the game turn this resolution settles, stamped on achieved goals
     * @return the events produced, in resolution order
     * @throws NullPointerException if any argument is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random, GoalTally goalTally, int gameTurn) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(random, "random must not be null");
        Objects.requireNonNull(goalTally, "goalTally must not be null");

        List<ResolutionEvent> events = new ArrayList<>();
        // Missions this pass ends are removed after the loop below, never during it - mutating an
        // InProgressMission's own fields in place is safe mid-iteration, but removing a map entry
        // while iterating its values() is not.
        List<String> endedMissionIds = new ArrayList<>();
        // Expert-rescue exhaustion (design.md §2.5(i)) - scoped to this one advanceSeason call
        // ("the same turn evaluation"), never persisted on the snapshot; shared across every
        // mission/step this call processes, since the Expert pool is roster-wide, not mission-scoped.
        Map<String, Integer> expertUseCounts = new LinkedHashMap<>();

        for (InProgressMission mission : snapshot.getInProgressMissions().values()) {
            advanceOne(snapshot, catalog, random, mission, events, endedMissionIds, expertUseCounts, goalTally,
                gameTurn);
        }

        for (String missionId : endedMissionIds) {
            snapshot.getInProgressMissions().remove(missionId);
        }
        return events;
    }

    /**
     * Advances one in-progress mission by one season.
     *
     * @param snapshot        the player snapshot this mission belongs to
     * @param catalog         the content catalog
     * @param random          the random source
     * @param mission         the mission to advance; mutated in place
     * @param events          the list to append this mission's events to
     * @param endedMissionIds the list to append this mission's id to if it ends
     * @param expertUseCounts this {@code advanceSeason} call's shared
     *                        Expert-rescue exhaustion map; mutated in place
     */
    private static void advanceOne(PlayerSnapshot snapshot, ContentCatalog catalog, RandomGenerator random,
            InProgressMission mission, List<ResolutionEvent> events, List<String> endedMissionIds,
            Map<String, Integer> expertUseCounts, GoalTally goalTally, int gameTurn) {
        MissionConfiguration configuration = catalog.missionConfiguration(mission.getMissionConfigurationId())
            .orElse(null);
        if (configuration == null) {
            // Defensive skip, not expected against a validated catalog MissionLaunch already
            // checked the configuration existed in - mirrors ResearchProgress's own
            // skip-rather-than-throw posture for an inconsistency that should never actually happen.
            return;
        }

        MissionSeason season = configuration.seasons().get(mission.getCurrentSeasonIndex());
        List<MissionStep> executedSteps = new ArrayList<>();
        List<MissionStepOutcome> executedOutcomes = new ArrayList<>();
        boolean missionEndedThisSeason = false;
        // Read live, at this mission's own resolution, not stamped at launch: BASPM's own step handlers call
        // GetMissionRunCount during resolution (baspm.md §11.7), so a copy of this configuration that
        // completed earlier in the same pass already counts. Missions run in launch order.
        FlightTier tier = MissionPrestige.tierFor(snapshot.getSuccessfulFlightCounts().getOrDefault(configuration.id(), 0));
        long tierTotal = MissionPrestige.tierTotal(configuration, tier);
        long seasonPrestige = 0;
        for (MissionStep step : season.steps()) {
            MissionStepOutcome outcome = resolveStep(snapshot, catalog, random, mission, step, expertUseCounts,
                events);
            events.add(new MissionStepResolved(mission.getId(), configuration.id(), mission.getCurrentSeasonIndex(),
                step.id(), outcome));
            executedSteps.add(step);
            executedOutcomes.add(outcome);
            // A step that ends or fails the mission awards no goals - BASPM's ProcessStepFailed has none
            // (baspm.md §11.7). The other outcomes award theirs right after the step itself.
            long goalPrestige = awardsGoals(outcome)
                ? settleStepGoals(snapshot, catalog, mission, step, goalTally, gameTurn, events)
                : 0;
            seasonPrestige = stepPrestige(seasonPrestige, outcome, step, tierTotal, goalPrestige);

            if (outcome == MissionStepOutcome.FAILURE || outcome == MissionStepOutcome.CATASTROPHIC) {
                missionEndedThisSeason = true;
                if (outcome == MissionStepOutcome.CATASTROPHIC) {
                    loseCrew(snapshot, mission, events);
                }
                break;
            }
        }
        applySeasonPrestige(snapshot, mission, configuration, seasonPrestige, events);

        // Both apply once per season a mission actually flies, regardless of outcome - see this
        // class's own Javadoc - and must run before releaseSurvivors below, so a crew member's
        // final season (completion, or a retirement triggered by this same release) still counts;
        // a CATASTROPHIC season's lost crew already can't grow, since loseCrew above already
        // removed them from the roster.
        applyPostSeasonReliabilityChange(snapshot, catalog, mission, executedSteps, executedOutcomes, random,
            events);
        applyPostSeasonSkillGrowth(snapshot, mission, executedSteps, random, events);

        // Recent-history turns feed the random-event gates (design.md §2.7). A failure is FAILURE or
        // CATASTROPHIC; a season that didn't end in one counts as a failure-free flight season.
        if (missionEndedThisSeason) {
            snapshot.setLastFailureTurn(gameTurn);
        } else {
            snapshot.setLastFlightSeasonTurn(gameTurn);
        }

        if (missionEndedThisSeason) {
            releaseSurvivors(snapshot, mission, events);
            endedMissionIds.add(mission.getId());
            return;
        }

        int nextSeasonIndex = mission.getCurrentSeasonIndex() + 1;
        if (nextSeasonIndex >= configuration.durationInSeasons()) {
            releaseSurvivors(snapshot, mission, events);
            // Only a completed, non-failed flight counts toward the repeat tier - design.md §2.6(d).
            // A failing season returned above, so reaching this branch means no step failed.
            snapshot.getSuccessfulFlightCounts().merge(configuration.id(), 1, Integer::sum);
            endedMissionIds.add(mission.getId());
        } else {
            mission.setCurrentSeasonIndex(nextSeasonIndex);
        }
    }

    /**
     * Folds one step's outcome into the season's running prestige total.
     * Successful steps add their full payout, a {@code MINOR_GLITCH} adds
     * half of its payout (rounded down), and a failing step <b>replaces</b>
     * the running total with its own negative deduction rather than adding
     * to it - so a failed season forfeits whatever its earlier steps earned
     * (see {@code design.md} &sect;2.6(d) and {@code baspm.md} &sect;11.7,
     * which trace the same replacement in BASPM's own season-end code).
     *
     * @param seasonSoFar the season's running total before this step
     * @param outcome     this step's resolved outcome
     * @param step        the step, for its share and failure deduction
     * @param tierTotal   the configuration's total at the flight's tier
     * @param goalPrestige the prestige the goals this step settled paid; folded
     *                    into the same total as the step's own share, so a
     *                    {@code MINOR_GLITCH} halves it too (baspm.md §11.7)
     * @return the running total after this step
     */
    private static long stepPrestige(long seasonSoFar, MissionStepOutcome outcome, MissionStep step, long tierTotal,
            long goalPrestige) {
        long payout = MissionPrestige.stepPayout(tierTotal, step.prestigeSharePercent()) + goalPrestige;
        return switch (outcome) {
            case SUCCESSFUL, MINOR_GLITCH_SOLVED -> seasonSoFar + payout;
            case MINOR_GLITCH -> seasonSoFar + payout / 2;
            case FAILURE, CATASTROPHIC -> -step.failurePrestigeDeduction();
        };
    }

    /**
     * Whether a step with this outcome awards its goals. Only the outcomes
     * that don't end the mission do - a failing step awards none.
     *
     * @param outcome the step's outcome
     * @return {@code true} unless the outcome is {@code FAILURE} or {@code CATASTROPHIC}
     */
    private static boolean awardsGoals(MissionStepOutcome outcome) {
        return outcome != MissionStepOutcome.FAILURE && outcome != MissionStepOutcome.CATASTROPHIC;
    }

    /**
     * Settles every goal a step achieves that this player hasn't already
     * fulfilled, returning the prestige they paid in total, including every
     * cancelled goal's own payout. Each settlement is reported as a
     * {@link GoalSettled} event; the game-wide tally is not changed here
     * (see {@link GoalTally#commit}).
     *
     * @param snapshot   the player whose goals settle
     * @param catalog    the content catalog, for each goal's definition
     * @param mission    the mission whose step this is
     * @param step       the step whose goals settle
     * @param goalTally  the game-wide counts as of the start of this resolution
     * @param gameTurn   the turn stamped on every goal this player achieves
     * @param events     the list to append each {@link GoalSettled} event to
     * @return the total prestige those settlements paid
     */
    private static long settleStepGoals(PlayerSnapshot snapshot, ContentCatalog catalog, InProgressMission mission,
            MissionStep step, GoalTally goalTally, int gameTurn, List<ResolutionEvent> events) {
        long total = 0;
        for (String goalId : step.goalIdsAchieved()) {
            if (!snapshot.hasAchievedOrCancelledGoal(goalId)) {
                total += settleGoal(snapshot, catalog, goalId, false, mission, goalTally, gameTurn, events);
            }
        }
        return total;
    }

    /**
     * Settles one goal, then recursively cancels every goal it cancels that
     * this player hasn't already fulfilled. A cancelled goal pays its own
     * first-time or second-time prestige and counts toward the tally, exactly
     * like an achieved one (baspm.md §11.7).
     *
     * @param snapshot  the player whose goal settles
     * @param catalog   the content catalog
     * @param goalId    the goal to settle
     * @param cancelled whether this settlement is a cancellation rather than an achievement
     * @param mission   the mission whose step caused this settlement
     * @param goalTally the game-wide counts as of the start of this resolution
     * @param gameTurn  the turn stamped on an achieved goal
     * @param events    the list to append the {@link GoalSettled} events to
     * @return the prestige this settlement and its whole cancellation cascade paid
     */
    private static long settleGoal(PlayerSnapshot snapshot, ContentCatalog catalog, String goalId, boolean cancelled,
            InProgressMission mission, GoalTally goalTally, int gameTurn, List<ResolutionEvent> events) {
        Goal goal = catalog.goal(goalId).orElseThrow(
            () -> new IllegalStateException("MissionStep references unknown Goal " + goalId));
        // Same-turn rule: the count read here is the committed count, never one this pass has advanced.
        long prestige = goalTally.countOf(goalId) == 0 ? goal.firstTimePrestige() : goal.secondTimePrestige();
        if (cancelled) {
            snapshot.getCancelledGoalIds().add(goalId);
        } else {
            snapshot.getAchievedGoals().put(goalId, gameTurn);
        }
        events.add(new GoalSettled(mission.getId(), goalId, cancelled, prestige));

        long total = prestige;
        for (String cancelledId : goal.cancelsGoalIds()) {
            if (!snapshot.hasAchievedOrCancelledGoal(cancelledId)) {
                total += settleGoal(snapshot, catalog, cancelledId, true, mission, goalTally, gameTurn, events);
            }
        }
        return total;
    }

    /**
     * Applies one season's net prestige change to the player, clamped so
     * cumulative prestige never goes below zero, and reports it. The
     * clamp is the only place a season's requested change can be cut short.
     *
     * @param snapshot      the player snapshot whose prestige changes
     * @param mission       the mission this season belongs to
     * @param configuration the configuration being flown
     * @param requested     the season's net before the zero floor
     * @param events        the list to append the {@link MissionPrestigeChanged} event to
     */
    private static void applySeasonPrestige(PlayerSnapshot snapshot, InProgressMission mission,
            MissionConfiguration configuration, long requested, List<ResolutionEvent> events) {
        // A season that pays nothing is a no-op and reports nothing, so prestige-free configurations
        // don't add noise to the event stream.
        if (requested == 0) {
            return;
        }
        long before = snapshot.getPrestige();
        long after = Math.max(0, before + requested);
        snapshot.setPrestige(after);
        events.add(new MissionPrestigeChanged(mission.getId(), configuration.id(), mission.getCurrentSeasonIndex(),
            requested, after - before));
    }

    /**
     * Resolves one step's outcome against the mission's assigned personnel
     * and the player's owned mission components. <b>Draws its own rolls
     * explicitly, rather than delegating to {@link MissionStepResolution}'s
     * own auto-drawing overload</b> (design.md &sect;2.5(i)): an Expert's
     * rescue may only boost the saving-throw probability, and may only be
     * "used" (consulted and exhausted), once the first roll has actually
     * failed and a saving throw is genuinely needed - never merely for
     * being eligible. The saving-throw probability itself is therefore
     * only computed (and Expert-boosted) after that first roll is known to
     * have failed, immediately before the saving roll is drawn.
     *
     * @param snapshot        the player snapshot owning the components/personnel
     * @param catalog         the content catalog
     * @param random          the random source
     * @param mission         the mission this step belongs to
     * @param step            the step to resolve
     * @param expertUseCounts this {@code advanceSeason} call's shared
     *                        Expert-rescue exhaustion map; mutated in place
     * @param events          the list to append any {@link ExpertRescueApplied}
     *                        events to
     * @return the resolved outcome
     */
    private static MissionStepOutcome resolveStep(PlayerSnapshot snapshot, ContentCatalog catalog,
            RandomGenerator random, InProgressMission mission, MissionStep step, Map<String, Integer> expertUseCounts,
            List<ResolutionEvent> events) {
        double componentSum = componentContributionSum(snapshot, catalog, mission, step);
        boolean crewInvolved = !step.crewInvolvements().isEmpty();
        double crewSum = crewInvolved
            ? averageContributionSum(step.crewInvolvements(), mission.getAssignedAstronautIds(),
                snapshot.getAstronauts(), MissionStep.CrewInvolvement::skill, MissionStep.CrewInvolvement::percent,
                Astronaut::getSkill)
            : 0;
        double baseProbability = MissionStepResolution.computeBaseProbability(componentSum, crewInvolved, crewSum);

        double firstRoll = MissionStepResolution.drawRoll(random);
        if (MissionStepResolution.firstRollSucceeded(baseProbability, firstRoll)) {
            return MissionStepOutcome.SUCCESSFUL;
        }

        double controllerSum = averageContributionSum(step.controllerInvolvements(),
            mission.getAssignedMissionControlOfficerIds(), snapshot.getMissionControlOfficers(),
            MissionStep.ControllerInvolvement::skill, MissionStep.ControllerInvolvement::percent,
            MissionControlOfficer::getSkill);
        double savingThrowProbability = MissionStepResolution.computeSavingThrowProbability(baseProbability,
            controllerSum);
        savingThrowProbability = applyExpertRescue(snapshot, mission, step, savingThrowProbability, expertUseCounts,
            events);

        double savingRoll = MissionStepResolution.drawRoll(random);
        return MissionStepResolution.resolve(baseProbability, savingThrowProbability, step.crewRisk(), firstRoll,
            savingRoll);
    }

    /**
     * Lets every roster-wide eligible Expert (see {@code design.md}
     * &sect;2.5(i) for the full rule) boost {@code savingThrowProbability}
     * in sequence, each against the previous Expert's own result, then
     * applies {@link MissionStepResolution#EXPERT_RESCUE_PROBABILITY_CAP}
     * once at the end.
     *
     * <p>Eligibility: any Astronaut/Mission Control officer whose own
     * Expert-skill set shares at least one skill with {@code step}'s
     * {@code crewInvolvements}/{@code controllerInvolvements} - idle,
     * training, or assigned to <em>this</em> mission, all at full
     * strength. An Astronaut assigned to a <em>different</em> active
     * mission is excluded entirely (physically elsewhere); a Mission
     * Control officer assigned to a different active mission is never
     * excluded, but scales to 75% efficiency instead (still on Earth,
     * just not fully dedicated). SET is never eligible (no Expert trait
     * possible).</p>
     *
     * @param snapshot             the player snapshot the roster is read from
     * @param mission              the mission whose step is being rescued
     * @param step                 the step needing a saving throw
     * @param savingThrowProbability the saving-throw probability before
     *                             any Expert's own contribution
     * @param expertUseCounts      this {@code advanceSeason} call's shared
     *                             exhaustion map; mutated in place
     * @param events               the list to append any
     *                             {@link ExpertRescueApplied} events to
     * @return the final saving-throw probability, capped
     */
    private static double applyExpertRescue(PlayerSnapshot snapshot, InProgressMission mission, MissionStep step,
            double savingThrowProbability, Map<String, Integer> expertUseCounts, List<ResolutionEvent> events) {
        double unboosted = savingThrowProbability;
        double boosted = unboosted;

        Set<AstronautSkill> stepCrewSkills = new LinkedHashSet<>();
        for (MissionStep.CrewInvolvement involvement : step.crewInvolvements()) {
            stepCrewSkills.add(involvement.skill());
        }
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            AstronautSkill matchedSkill = firstOverlap(astronaut.getExpertSkills(), stepCrewSkills);
            if (matchedSkill == null) {
                continue;
            }
            if (astronaut.getAssignmentState() == AssignmentState.ASSIGNED
                    && !mission.getId().equals(astronaut.getAssignedMissionId())) {
                // Physically aboard a different spacecraft - not available at all, user decision
                // 2026-10-02 - never consulted, never exhausted.
                continue;
            }
            boosted = applyOneExpert(mission, step, PersonnelGroup.ASTRONAUT, astronaut.getId(), astronaut.getName(),
                matchedSkill.name(), 1.0, boosted, expertUseCounts, events);
        }

        Set<MissionControlSkill> stepControllerSkills = new LinkedHashSet<>();
        for (MissionStep.ControllerInvolvement involvement : step.controllerInvolvements()) {
            stepControllerSkills.add(involvement.skill());
        }
        for (MissionControlOfficer officer : snapshot.getMissionControlOfficers()) {
            MissionControlSkill matchedSkill = firstOverlap(officer.getExpertSkills(), stepControllerSkills);
            if (matchedSkill == null) {
                continue;
            }
            boolean onAnotherMission = officer.getAssignmentState() == AssignmentState.ASSIGNED
                && !mission.getId().equals(officer.getAssignedMissionId());
            double efficiency =
                onAnotherMission ? MissionStepResolution.EXPERT_RESCUE_OFF_MISSION_CONTROLLER_EFFICIENCY : 1.0;
            boosted = applyOneExpert(mission, step, PersonnelGroup.MISSION_CONTROL, officer.getId(),
                officer.getName(), matchedSkill.name(), efficiency, boosted, expertUseCounts, events);
        }

        return MissionStepResolution.capExpertRescue(boosted, unboosted);
    }

    /**
     * Applies one Expert's own rescue attempt: looks up and increments
     * their exhaustion count for this turn, looks up the resulting boost
     * percent (scaled by {@code efficiency}), applies it via
     * {@link MissionStepResolution#applyExpertBoost}, and reports an
     * {@link ExpertRescueApplied} event.
     *
     * @param mission         the mission whose step is being rescued
     * @param step            the step needing a saving throw
     * @param group           which roster this Expert belongs to
     * @param personnelId     this Expert's stable id
     * @param name            this Expert's display name
     * @param skillName       the matched Expert skill's own {@code name()}
     * @param efficiency      {@code 1.0} at full strength, {@code 0.75}
     *                        for an off-mission Mission Control officer
     * @param currentProbability the saving-throw probability before this
     *                        Expert's own contribution
     * @param expertUseCounts this {@code advanceSeason} call's shared
     *                        exhaustion map; mutated in place
     * @param events          the list to append the new event to
     * @return the saving-throw probability after this Expert's own
     *     contribution
     */
    private static double applyOneExpert(InProgressMission mission, MissionStep step, PersonnelGroup group,
            String personnelId, String name, String skillName, double efficiency, double currentProbability,
            Map<String, Integer> expertUseCounts, List<ResolutionEvent> events) {
        int useNumber = expertUseCounts.merge(personnelId, 1, Integer::sum);
        double boostPercent = MissionStepResolution.expertRescueBoostPercentForUse(useNumber) * efficiency;
        double newProbability = MissionStepResolution.applyExpertBoost(currentProbability, boostPercent);
        events.add(new ExpertRescueApplied(mission.getId(), step.id(), group, personnelId, name, skillName,
            boostPercent, currentProbability, newProbability));
        return newProbability;
    }

    /**
     * Returns the first element {@code a} and {@code b} have in common, or
     * {@code null} if they share none.
     *
     * @param a   the first set
     * @param b   the second set
     * @param <S> the shared element type
     * @return the first shared element, or {@code null}
     */
    private static <S> S firstOverlap(Set<S> a, Set<S> b) {
        for (S item : a) {
            if (b.contains(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Computes a step's component contribution sum: each involved component's
     * {@link MissionStepResolution#effectiveReliability} - its current
     * reliability plus its live goal modifier - weighted by its involvement
     * percentage.
     *
     * @param snapshot the player snapshot the components are owned in
     * @param catalog  the content catalog, for the mission's configuration
     * @param mission  the mission the step belongs to
     * @param step     the step to compute the sum for
     * @return the component contribution sum
     */
    private static double componentContributionSum(PlayerSnapshot snapshot, ContentCatalog catalog,
            InProgressMission mission, MissionStep step) {
        MissionConfiguration configuration = catalog.missionConfiguration(mission.getMissionConfigurationId())
            .orElseThrow();
        double sum = 0;
        for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
            // A rocket slot reads the reliability of the rocket the player chose for that launch.
            String componentId = MissionLaunch.resolveComponentId(involvement.componentId(), mission.getRocketIds());
            OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
            double current = owned != null ? owned.getCurrentReliability() : 0;
            double modifier = goalModifierFor(snapshot, configuration, componentId);
            double effective = MissionStepResolution.effectiveReliability(current, modifier);
            sum += involvement.percent() / 100.0 * effective;
        }
        return sum;
    }

    /**
     * Computes one component's goal-linked reliability modifier for a
     * configuration, live against this player's current achieved-or-cancelled
     * goals: each dependency whose goal is still unfulfilled subtracts its
     * amount, and each boost whose goal is fulfilled adds its amount. Read on
     * every step, so a goal achieved earlier in the same flight changes the
     * next step's odds immediately (baspm.md §11.7, MissionSimBaseScreen.cs:918).
     *
     * @param snapshot    the player whose goal state is read
     * @param configuration the configuration the flight belongs to
     * @param componentId the mission component the modifier applies to
     * @return the summed modifier, negative for unmet dependencies
     */
    static double goalModifierFor(PlayerSnapshot snapshot, MissionConfiguration configuration, String componentId) {
        double modifier = 0;
        for (MissionConfiguration.GoalModifier dependency : configuration.goalDependencies()) {
            if (dependency.componentId().equals(componentId)
                    && !snapshot.hasAchievedOrCancelledGoal(dependency.goalId())) {
                modifier -= dependency.amount();
            }
        }
        for (MissionConfiguration.GoalModifier boost : configuration.goalBoosts()) {
            if (boost.componentId().equals(componentId) && snapshot.hasAchievedOrCancelledGoal(boost.goalId())) {
                modifier += boost.amount();
            }
        }
        return modifier;
    }

    /**
     * Computes a contribution sum across a list of skill involvements: for
     * each one, the mean skill value across every assigned person (see
     * {@link #averageSkill}), weighted by that involvement's percentage.
     *
     * @param involvements the skill involvements to sum over
     * @param assignedIds  the ids assigned to this mission in {@code roster}'s
     *                     own group
     * @param roster       the full roster {@code assignedIds} are looked up
     *                     against
     * @param skillOf      reads one involvement's skill
     * @param percentOf    reads one involvement's percentage
     * @param skillValueOf reads one person's value for a given skill
     * @param <I>          the involvement record type
     * @param <S>          the skill enum type
     * @param <P>          the roster's own {@link Personnel} subtype
     * @return the contribution sum
     */
    private static <I, S, P extends Personnel> double averageContributionSum(List<I> involvements,
            List<String> assignedIds, List<P> roster, Function<I, S> skillOf, ToIntFunction<I> percentOf,
            BiFunction<P, S, Double> skillValueOf) {
        double sum = 0;
        for (I involvement : involvements) {
            double average = averageSkill(assignedIds, roster, skillOf.apply(involvement), skillValueOf);
            sum += percentOf.applyAsInt(involvement) / 100.0 * average;
        }
        return sum;
    }

    /**
     * Computes the mean value of one skill across every assigned person
     * actually found on the roster (an assigned id not found on the roster
     * - never expected in practice - contributes nothing and is not
     * counted toward the average).
     *
     * @param assignedIds  the ids to average over
     * @param roster       the roster the ids are looked up against
     * @param skill        the skill to read
     * @param skillValueOf reads one person's value for a given skill
     * @param <S>          the skill enum type
     * @param <P>          the roster's own {@link Personnel} subtype
     * @return the mean skill value, or 0 if nobody is assigned
     */
    private static <S, P extends Personnel> double averageSkill(List<String> assignedIds, List<P> roster, S skill,
            BiFunction<P, S, Double> skillValueOf) {
        double total = 0;
        int count = 0;
        for (String id : assignedIds) {
            P person = findById(roster, id);
            if (person != null) {
                total += skillValueOf.apply(person, skill);
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    /**
     * The least severe of the three season-aggregated reliability-change
     * categories - every involved step this season resolved
     * {@code SUCCESSFUL}/{@code MINOR_GLITCH_SOLVED}. See
     * {@link #categoryFor} and {@code design.md} &sect;2.5(g).
     */
    private static final int CATEGORY_SUCCESS = 0;

    /**
     * The middle reliability-change category - no involved step this
     * season resolved worse than an unsolved {@code MINOR_GLITCH}.
     */
    private static final int CATEGORY_MINOR_GLITCH = 1;

    /**
     * The most severe reliability-change category - an involved step this
     * season resolved {@code FAILURE}/{@code CATASTROPHIC} (necessarily
     * the season's last executed step, since either ends the mission).
     */
    private static final int CATEGORY_FAILURE = 2;

    /**
     * Applies this season's post-flight reliability change to every
     * component involved in any step the season actually executed: for
     * each one, the worst {@link MissionStepOutcome} category it was
     * involved in this season decides a single gain or loss, applied once
     * - see this class's own Javadoc and {@code design.md} &sect;2.5(g)
     * for the full aggregation rule and formula.
     *
     * @param snapshot       the player snapshot the components are owned in
     * @param catalog        the content catalog components are looked up in
     * @param mission        the mission this season belongs to
     * @param executedSteps   the steps this season actually executed, in order
     * @param executedOutcomes each executed step's own resolved outcome, same order
     * @param random          the random source this season's factors are drawn from
     * @param events          the list to append any
     *                        {@link MissionReliabilityChanged}/{@link ResearchComponentMaxed}
     *                        event to
     */
    private static void applyPostSeasonReliabilityChange(PlayerSnapshot snapshot, ContentCatalog catalog,
            InProgressMission mission, List<MissionStep> executedSteps, List<MissionStepOutcome> executedOutcomes,
            RandomGenerator random, List<ResolutionEvent> events) {
        Map<String, Integer> categoryByComponentId = new LinkedHashMap<>();
        Map<String, Integer> failureInvolvementPercentByComponentId = new LinkedHashMap<>();

        for (int i = 0; i < executedSteps.size(); i++) {
            int category = categoryFor(executedOutcomes.get(i));
            for (MissionStep.ComponentInvolvement involvement : executedSteps.get(i).componentInvolvements()) {
                // Aggregated per resolved component, so a rocket slot changes the chosen rocket's reliability.
                String componentId = MissionLaunch.resolveComponentId(involvement.componentId(),
                    mission.getRocketIds());
                Integer existing = categoryByComponentId.get(componentId);
                if (existing == null || category > existing) {
                    categoryByComponentId.put(componentId, category);
                    if (category == CATEGORY_FAILURE) {
                        failureInvolvementPercentByComponentId.put(componentId, involvement.percent());
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : categoryByComponentId.entrySet()) {
            applyComponentReliabilityChange(snapshot, catalog, mission.getId(), entry.getKey(), entry.getValue(),
                failureInvolvementPercentByComponentId.get(entry.getKey()), random, events);
        }
    }

    /**
     * Maps a resolved step outcome onto the reliability-change category it
     * contributes - see this class's own Javadoc for the two unconfirmed-
     * default mappings ({@code MINOR_GLITCH_SOLVED} to {@code SUCCESS},
     * {@code CATASTROPHIC} to {@code FAILURE}).
     *
     * @param outcome the resolved step outcome
     * @return {@link #CATEGORY_SUCCESS}, {@link #CATEGORY_MINOR_GLITCH}, or
     *     {@link #CATEGORY_FAILURE}
     */
    private static int categoryFor(MissionStepOutcome outcome) {
        return switch (outcome) {
            case SUCCESSFUL, MINOR_GLITCH_SOLVED -> CATEGORY_SUCCESS;
            case MINOR_GLITCH -> CATEGORY_MINOR_GLITCH;
            case FAILURE, CATASTROPHIC -> CATEGORY_FAILURE;
        };
    }

    /**
     * Applies one component's own reliability change for this season,
     * reports it, and releases any SET researchers still assigned to it if
     * the change reached its R&amp;D ceiling.
     *
     * @param snapshot                   the player snapshot the component
     *                                    is owned in
     * @param catalog                    the content catalog the component's
     *                                    own definition is looked up in
     * @param missionInstanceId          the mission instance this season
     *                                    belongs to, for the reported event
     * @param componentId                the component to change
     * @param category                   this component's worst category
     *                                    this season
     * @param failureInvolvementPercent  the component's involvement
     *                                    percentage in the step that
     *                                    failed; only meaningful (and only
     *                                    non-{@code null}) when
     *                                    {@code category} is
     *                                    {@link #CATEGORY_FAILURE}
     * @param random                     the random source this season's
     *                                    factor is drawn from
     * @param events                     the list to append this
     *                                    component's events to
     */
    private static void applyComponentReliabilityChange(PlayerSnapshot snapshot, ContentCatalog catalog,
            String missionInstanceId, String componentId, int category, Integer failureInvolvementPercent,
            RandomGenerator random, List<ResolutionEvent> events) {
        OwnedMissionComponent owned = snapshot.getMissionComponents().get(componentId);
        MissionComponent component = catalog.missionComponent(componentId).orElse(null);
        if (owned == null || component == null) {
            // Defensive skip, not expected: MissionLaunch only ever launches a configuration whose
            // every step component is validated (ContentCatalog) to be owned once its program is
            // open.
            return;
        }

        double before = owned.getCurrentReliability();
        double after;
        if (category == CATEGORY_FAILURE) {
            double factor = SkillGrowth.drawMissionReliabilityFailureFactor(random);
            double loss = SkillGrowth.missionReliabilityLoss(before, owned.getFloorReliability(), factor,
                failureInvolvementPercent);
            after = Math.max(owned.getFloorReliability(), before - loss);
        } else {
            double factor = category == CATEGORY_MINOR_GLITCH
                ? SkillGrowth.drawMissionReliabilityMinorGlitchFactor(random)
                : SkillGrowth.drawMissionReliabilitySuccessFactor(random);
            double gain = SkillGrowth.missionReliabilityGain(before, component.maxReliability(), factor);
            after = Math.min(component.maxReliability(), before + gain);
        }
        owned.setCurrentReliability(after);
        events.add(new MissionReliabilityChanged(missionInstanceId, componentId, after - before));

        ResearchComponentMaxed maxedEvent =
            ResearchProgress.releaseResearchersIfMaxed(snapshot, componentId, after, component.maxRdReliability());
        if (maxedEvent != null) {
            events.add(maxedEvent);
        }
    }

    /**
     * Applies this season's mission-based skill growth to every astronaut/
     * Mission Control officer still assigned to {@code mission} - see this
     * class's own Javadoc and {@code design.md} &sect;2.5(g) for the full
     * rule (astronauts grow all five skills; controllers grow every skill
     * exercised by the season's own executed steps).
     *
     * @param snapshot       the player snapshot the assigned personnel belong to
     * @param mission        the mission this season belongs to
     * @param executedSteps  the steps this season actually executed
     * @param random         the random source this season's factors are drawn from
     * @param events         the list to append each
     *                       {@link MissionSkillGrowthApplied} event to
     */
    private static void applyPostSeasonSkillGrowth(PlayerSnapshot snapshot, InProgressMission mission,
            List<MissionStep> executedSteps, RandomGenerator random, List<ResolutionEvent> events) {
        for (String astronautId : mission.getAssignedAstronautIds()) {
            Astronaut astronaut = findById(snapshot.getAstronauts(), astronautId);
            if (astronaut == null) {
                // Lost this season's CATASTROPHIC step; loseCrew already removed them.
                continue;
            }
            double totalGain = 0;
            for (AstronautSkill skill : AstronautSkill.values()) {
                double factor = SkillGrowth.drawMissionSkillGrowthFactor(random);
                double gain = SkillGrowth.diminishingReturnsGain(astronaut.getSkill(skill),
                    astronaut.getLearningPotential(), factor);
                astronaut.setSkill(skill, astronaut.getSkill(skill) + gain);
                totalGain += gain;
            }
            events.add(new MissionSkillGrowthApplied(mission.getId(), astronaut.getId(), astronaut.getName(),
                PersonnelGroup.ASTRONAUT, totalGain));
        }

        Set<MissionControlSkill> skillsExercisedThisSeason = new LinkedHashSet<>();
        for (MissionStep step : executedSteps) {
            for (MissionStep.ControllerInvolvement involvement : step.controllerInvolvements()) {
                skillsExercisedThisSeason.add(involvement.skill());
            }
        }
        for (String controllerId : mission.getAssignedMissionControlOfficerIds()) {
            MissionControlOfficer officer = findById(snapshot.getMissionControlOfficers(), controllerId);
            if (officer == null) {
                continue;
            }
            double totalGain = 0;
            for (MissionControlSkill skill : skillsExercisedThisSeason) {
                double factor = SkillGrowth.drawMissionSkillGrowthFactor(random);
                double gain = SkillGrowth.diminishingReturnsGain(officer.getSkill(skill),
                    officer.getLearningPotential(), factor);
                officer.setSkill(skill, officer.getSkill(skill) + gain);
                totalGain += gain;
            }
            events.add(new MissionSkillGrowthApplied(mission.getId(), officer.getId(), officer.getName(),
                PersonnelGroup.MISSION_CONTROL, totalGain));
        }
    }

    /**
     * Removes every astronaut assigned to {@code mission} from the
     * roster, each reported as a {@link PersonnelDeparture} with
     * {@link DepartureReason#LOST_IN_ACCIDENT}.
     *
     * @param snapshot the player snapshot to remove them from
     * @param mission  the mission whose entire assigned crew is lost
     * @param events   the list to append each departure to
     */
    private static void loseCrew(PlayerSnapshot snapshot, InProgressMission mission, List<ResolutionEvent> events) {
        for (String astronautId : mission.getAssignedAstronautIds()) {
            Iterator<Astronaut> iterator = snapshot.getAstronauts().iterator();
            while (iterator.hasNext()) {
                Astronaut astronaut = iterator.next();
                if (astronaut.getId().equals(astronautId)) {
                    events.add(new PersonnelDeparture(PersonnelGroup.ASTRONAUT, astronaut.getId(),
                        astronaut.getName(), DepartureReason.LOST_IN_ACCIDENT, astronaut.getAge(),
                        astronaut.getMorale(), astronaut.getAssignmentState(), null));
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Releases every one of {@code mission}'s assigned personnel still on
     * their roster: back to {@link AssignmentState#IDLE} with
     * {@link Personnel#setAssignedMissionId} cleared, or straight into
     * retirement (see {@link #releaseGroup}) if they have already reached
     * their group's retirement age. An astronaut {@link #loseCrew} already
     * removed is silently skipped, not an error.
     *
     * @param snapshot the player snapshot the personnel belong to
     * @param mission  the mission that just ended
     * @param events   the list to append any retirement departure to
     */
    private static void releaseSurvivors(PlayerSnapshot snapshot, InProgressMission mission,
            List<ResolutionEvent> events) {
        releaseGroup(snapshot.getAstronauts(), mission.getAssignedAstronautIds(), PersonnelGroup.ASTRONAUT, events);
        releaseGroup(snapshot.getMissionControlOfficers(), mission.getAssignedMissionControlOfficerIds(),
            PersonnelGroup.MISSION_CONTROL, events);
    }

    /**
     * Releases every given id still present on {@code roster}: back to
     * {@link AssignmentState#IDLE} with {@link Personnel#setAssignedMissionId}
     * cleared, unless they have already reached {@code group}'s own
     * retirement age ({@link PersonnelLifecycle#retirementAgeFor}), in
     * which case they retire on the spot instead - reported as a
     * {@link PersonnelDeparture} with {@link DepartureReason#RETIRED}.
     * Without this check, a released over-age person could be relaunched
     * into another mission before a normal {@link PersonnelLifecycle}
     * pass ever gets a chance to retire them, since they would be
     * {@code IDLE} only for the instant between release and the next
     * {@link MissionLaunch#launch} call.
     *
     * @param roster the roster to release from
     * @param ids    the ids to release
     * @param group  {@code roster}'s own {@link PersonnelGroup}, for the
     *               retirement-age lookup and any reported departure
     * @param events the list to append any retirement departure to
     * @param <P>    the roster's own {@link Personnel} subtype
     */
    private static <P extends Personnel> void releaseGroup(List<P> roster, List<String> ids, PersonnelGroup group,
            List<ResolutionEvent> events) {
        int retirementAge = PersonnelLifecycle.retirementAgeFor(group);
        for (String id : ids) {
            Iterator<P> iterator = roster.iterator();
            while (iterator.hasNext()) {
                P person = iterator.next();
                if (!person.getId().equals(id)) {
                    continue;
                }
                if (person.getAge() >= retirementAge) {
                    events.add(new PersonnelDeparture(group, person.getId(), person.getName(),
                        DepartureReason.RETIRED, person.getAge(), person.getMorale(), person.getAssignmentState(),
                        null));
                    iterator.remove();
                } else {
                    person.setAssignmentState(AssignmentState.IDLE);
                    person.setAssignedMissionId(null);
                }
                break;
            }
        }
    }

    /**
     * Finds a {@link Personnel} record by id within a roster.
     *
     * @param roster the roster to search
     * @param id     the id to find
     * @param <P>    the roster's own {@link Personnel} subtype
     * @return the matching record, or {@code null} if none matches
     */
    private static <P extends Personnel> P findById(List<P> roster, String id) {
        for (P person : roster) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        return null;
    }
}
