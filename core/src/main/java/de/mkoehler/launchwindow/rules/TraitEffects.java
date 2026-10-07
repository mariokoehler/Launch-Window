package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TrainingKind;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * What personal traits do to a person's own numbers ({@code design.md} &sect;2.2(d)): R&amp;D,
 * training, flight growth, the season's morale change, and a mission's saving throws. Every caller
 * that previews a number for the player passes {@code knownOnly}, so a hidden trait never shows in
 * a preview. First pass: the morale effects apply to the season's own change only, not to salary
 * changes, press stories or random events.
 *
 * @author Mario Koehler
 */
public final class TraitEffects {

    /** A Workaholic's R&amp;D gain. */
    public static final double WORKAHOLIC_RD = 1.1;

    /** How many times faster a Workaholic loses morale when idle. */
    public static final int WORKAHOLIC_IDLE_MORALE = 3;

    /** An Ambitious person's training gain. */
    public static final double AMBITIOUS_TRAINING = 1.2;

    /** Seasons without training after which an Ambitious person starts losing morale. */
    public static final int AMBITIOUS_PATIENCE_SEASONS = 6;

    /** What an Ambitious person loses each season after that. */
    public static final int AMBITIOUS_MORALE_LOSS = 2;

    /** A Glory seeker's skill growth from flying. */
    public static final double GLORY_SEEKER_FLIGHT = 1.5;

    /** What a Glory seeker loses whenever a crewed mission goes up without them. */
    public static final int GLORY_SEEKER_PASSED_OVER = 4;

    /** A Perfectionist's R&amp;D gain. */
    public static final double PERFECTIONIST_RD = 0.85;

    /** How much higher a component's R&amp;D cap is while a Perfectionist works on it. */
    public static final double PERFECTIONIST_CAP = 3;

    /** What each Cool under pressure member adds to a mission step's saving throw, in points. */
    public static final double COOL_SAVING_THROW = 3;

    /** A Restless engineer's R&amp;D gain in their first seasons on a component. */
    public static final double RESTLESS_FRESH = 1.15;

    /** How many seasons count as fresh. */
    public static final int RESTLESS_FRESH_SEASONS = 2;

    /** A Restless engineer's R&amp;D gain after a long stint on one component. */
    public static final double RESTLESS_STALE = 0.85;

    /** Seasons on one component after which a Restless engineer slows down. */
    public static final int RESTLESS_STALE_AFTER = 8;

    /** A Quick study's training gain. */
    public static final double QUICK_STUDY_TRAINING = 1.25;

    /** How much faster someone learns with a more skilled Mentor on the team. */
    public static final double MENTORED = 1.2;

    private TraitEffects() {
    }

    private static boolean counts(Personnel person, Trait trait, boolean knownOnly) {
        return knownOnly ? Personality.known(person, trait) : Personality.has(person, trait);
    }

    /**
     * Returns what an engineer's traits do to their R&amp;D gain on a component.
     *
     * @param engineer    the engineer
     * @param componentId the component they're working on
     * @param knownOnly   whether to count only revealed traits
     * @return the factor
     */
    public static double rdGain(SetSpecialist engineer, String componentId, boolean knownOnly) {
        double factor = 1;
        if (counts(engineer, Trait.WORKAHOLIC, knownOnly)) {
            factor *= WORKAHOLIC_RD;
        }
        if (counts(engineer, Trait.PERFECTIONIST, knownOnly)) {
            factor *= PERFECTIONIST_RD;
        }
        if (counts(engineer, Trait.RESTLESS, knownOnly)) {
            int done = componentId.equals(engineer.getProjectId()) ? engineer.getProjectSeasons() : 0;
            if (done < RESTLESS_FRESH_SEASONS) {
                factor *= RESTLESS_FRESH;
            } else if (done >= RESTLESS_STALE_AFTER) {
                factor *= RESTLESS_STALE;
            }
        }
        return factor;
    }

    /**
     * Returns how much higher a component's R&amp;D cap is with this team on it.
     *
     * @param team      the engineers on it
     * @param knownOnly whether to count only revealed traits
     * @return the extra reliability points
     */
    public static double capBonus(List<SetSpecialist> team, boolean knownOnly) {
        return team.stream().anyMatch(member -> counts(member, Trait.PERFECTIONIST, knownOnly)) ? PERFECTIONIST_CAP : 0;
    }

    /**
     * Returns a component's R&amp;D cap with the engineers assigned to it now (plus any extra, such
     * as one about to be assigned): the normal cap, raised while a Perfectionist is on the team,
     * never past the component's own maximum. The one place every cap check reads.
     *
     * @param snapshot  the player
     * @param component the component
     * @param extra     engineers to count as on the team too
     * @param knownOnly whether to count only revealed traits (anything the player is told)
     * @return the cap
     */
    public static double effectiveCap(de.mkoehler.launchwindow.model.PlayerSnapshot snapshot,
            de.mkoehler.launchwindow.content.MissionComponent component, List<SetSpecialist> extra, boolean knownOnly) {
        List<SetSpecialist> team = new java.util.ArrayList<>(extra);
        for (SetSpecialist engineer : snapshot.getSetSpecialists()) {
            if (engineer.getAssignmentState() == AssignmentState.ASSIGNED
                && component.id().equals(engineer.getRdTargetComponentId()) && !team.contains(engineer)) {
                team.add(engineer);
            }
        }
        return Math.min(component.maxReliability(), component.maxRdReliability() + capBonus(team, knownOnly));
    }

    /**
     * Returns what someone's traits do to their training gain.
     *
     * @param person the trainee
     * @return the factor
     */
    public static double trainingGain(Personnel person) {
        double factor = 1;
        if (Personality.has(person, Trait.AMBITIOUS)) {
            factor *= AMBITIOUS_TRAINING;
        }
        if (Personality.has(person, Trait.QUICK_STUDY)) {
            factor *= QUICK_STUDY_TRAINING;
        }
        return factor;
    }

    /**
     * Returns how much faster someone learns from working with a team: a Mentor more skilled than
     * them speeds them up.
     *
     * @param person the learner
     * @param team   their team, including them
     * @param skill  the skill compared
     * @return the factor
     */
    public static double mentored(Personnel person, List<? extends Personnel> team, ToDoubleFunction<Personnel> skill) {
        double own = skill.applyAsDouble(person);
        for (Personnel other : team) {
            if (other != person && Personality.has(other, Trait.MENTOR) && skill.applyAsDouble(other) > own) {
                return MENTORED;
            }
        }
        return 1;
    }

    /**
     * Returns what an astronaut's traits do to their skill growth from a flight.
     *
     * @param astronaut the astronaut
     * @return the factor
     */
    public static double flightGrowth(Personnel astronaut) {
        return Personality.has(astronaut, Trait.GLORY_SEEKER) ? GLORY_SEEKER_FLIGHT : 1;
    }

    /**
     * Returns the saving-throw bonus a mission team's Cool under pressure members bring.
     *
     * @param team      the crew and controllers
     * @param knownOnly whether to count only revealed traits
     * @return the bonus, in percentage points
     */
    public static double savingThrowBonus(List<? extends Personnel> team, boolean knownOnly) {
        return COOL_SAVING_THROW * team.stream().filter(member -> counts(member, Trait.COOL_UNDER_PRESSURE, knownOnly))
            .count();
    }

    /**
     * Returns the season's morale change for someone, after their traits: a Workaholic loses it
     * faster when idle, an Ambitious person loses it after a long time without training, and a
     * Steady person's change is halved (toward zero).
     *
     * @param person the employee
     * @param base   the change before traits
     * @return the change
     */
    public static int seasonMorale(Personnel person, int base) {
        int delta = base;
        boolean idle = person.getAssignmentState() == AssignmentState.IDLE;
        if (idle && delta < 0 && Personality.has(person, Trait.WORKAHOLIC)) {
            delta *= WORKAHOLIC_IDLE_MORALE;
        }
        if (Personality.has(person, Trait.AMBITIOUS) && person.getSeasonsSinceTraining() >= AMBITIOUS_PATIENCE_SEASONS) {
            delta -= AMBITIOUS_MORALE_LOSS;
        }
        if (Personality.has(person, Trait.STEADY)) {
            delta /= 2;
        }
        return delta;
    }

    /**
     * Returns whether someone counts as training this season, for an Ambitious person's patience.
     *
     * @param person the employee
     * @return whether they're in training
     */
    public static boolean inTraining(Personnel person) {
        return person.getTrainingKind() != null && person.getTrainingKind() != TrainingKind.NONE;
    }
}
