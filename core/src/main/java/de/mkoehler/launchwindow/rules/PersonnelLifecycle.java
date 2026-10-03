package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSpecialist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * <p>The personnel season-advance pass: age, morale, and automatic
 * resignation/retirement for every employee across all three rosters of a
 * {@link PlayerSnapshot} - the first M2 rules-engine slice
 * (see {@code design.md} &sect;5 M2, and &sect;2.2(a)/(c) for the ported
 * BASPM mechanic this re-derives). Headless: no persistence, network, or
 * libGDX dependency, per {@code design.md} &sect;3.10's "headless rules
 * first" principle - {@link #advanceSeason} takes and mutates a plain
 * {@link PlayerSnapshot}, nothing else.</p>
 *
 * <p><b>Deliberately out of scope for this slice</b> (later M2 slices, see
 * {@code design.md} &sect;2.2): skill growth (R&amp;D, flying a mission -
 * training itself is {@link TrainingProgress}, a separate resolver), the
 * hiring pool, and the Expert trait. This pass only ever touches
 * {@link Personnel#getAge()}, {@link Personnel#getMorale()}, and roster
 * membership - it never reads or writes a skill map. It also never touches
 * {@link PlayerSnapshot#getSeason()} itself; season advancement is
 * {@code PlayerStateService}'s job ({@code design.md} &sect;3.7), not the
 * rules engine's.</p>
 *
 * <p><b>Order of operations per employee:</b> morale updates first (idle
 * loses, busy gains), then age, then resignation is checked against the
 * just-updated morale (only if {@link #isProtectedFromDeparture} says no),
 * and only if not resigned, retirement is checked against the just-updated
 * age (same protection check). A protected employee's morale and age still
 * update every season; only the resignation/retirement *check* is skipped
 * for them.</p>
 *
 * <p><b>The departure-protection rule itself - reversed from BASPM's own
 * shape, not just re-derived numbers (user decision, 2026-09-30, see
 * {@code design.md} &sect;2.2(c)):</b> BASPM protects any busy (non-idle)
 * employee uniformly across all three groups ({@code baspm.md} &sect;9.2).
 * Launch Window instead protects <b>per group</b>, via
 * {@link #isProtectedFromDeparture}: SET is <b>never</b> protected,
 * regardless of {@link AssignmentState} - SET only ever works on mission
 * components, so there's no in-fiction reason to block them from leaving
 * mid-assignment the way pulling a crew off a launching rocket would be a
 * problem. Astronaut and Mission Control are protected <b>only</b> while
 * {@link AssignmentState#ASSIGNED} - critically, <b>not</b> while
 * {@link AssignmentState#IN_TRAINING}, which used to protect uniformly
 * alongside {@code ASSIGNED} before this reversal. This is a deliberate,
 * user-directed departure from the "keep BASPM's busy-protects-everyone
 * shape" decision recorded earlier the same session in {@code design.md}
 * &sect;2.2(c) - that earlier entry is superseded for the *shape* of the
 * rule, not overwritten, so the history stays legible.</p>
 *
 * <p><b>Protecting someone while genuinely on a mission is correct, but
 * naively releasing and immediately relaunching them every season would
 * reopen the same "never actually unprotected long enough to depart" gap
 * in a different shape</b> - {@link MissionProgress}'s own retire-on-
 * release check closes this (see its own Javadoc and {@code design.md}
 * &sect;2.5(f)), rather than this method's protection rule needing to
 * change again.</p>
 *
 * <p><b>Two interpretations of the user's original instruction - both
 * confirmed by the user after review (2026-09-30), not left as unilateral
 * judgment calls:</b> (1) the user's wording named retirement specifically
 * ("no reason not to retire them"); confirmed the rule extends to
 * resignation too, not just retirement. (2) {@link AssignmentState#ASSIGNED}
 * is used here as the proxy for "on an active mission" for Astronaut/
 * Mission Control - the user accepted this proxy, and beyond just accepting
 * it, gave a reason it likely isn't even a real gap: a mission is only ever
 * {@code SCHEDULED} until the *next* turn evaluation, at which point it
 * becomes {@code ACTIVE} - and that same turn evaluation is also the first
 * (and only per-turn) opportunity for a resignation/retirement check to
 * run at all. So there is no real multi-turn window where a crew sits
 * merely {@code ASSIGNED}-but-not-yet-flying while a departure check could
 * fire on them - by the time any check runs, a scheduled mission has
 * already become active. <b>Confirmed mechanically, not just argued,
 * once mission simulation actually shipped (2026-10-02, {@code design.md}
 * &sect;2.5(f)):</b> {@code MissionLaunch}/{@code MissionProgress} launch
 * a mission and resolve its first season's step batch in the same pass,
 * so this reasoning about the intended scheduling shape turned out to
 * match the real implementation.</p>
 *
 * <p><b>All the numeric constants below are Launch Window's own values, not
 * BASPM's</b> - {@code design.md} &sect;1.2 forbids reusing BASPM's own
 * tuning constants verbatim, so every threshold here was re-derived to a
 * different concrete number while keeping the same mechanic shape.
 * <b>(Unconfirmed default, {@code design.md} &sect;2.2(c), 2026-09-30):</b>
 * these are placeholder-but-concrete starting values, expected to be tuned
 * later via the bot-driven playout harness ({@code design.md} &sect;3.10),
 * not final game balance.</p>
 *
 * @author Mario Koehler
 */
public final class PersonnelLifecycle {

    /**
     * How many seasons make up one whole year of age - the same "one turn
     * is one BASPM-style season, a quarter year" fact already established
     * by {@code design.md} &sect;1/&sect;3.6's turn cadence, not a
     * re-derived balance number.
     */
    public static final int SEASONS_PER_YEAR = 4;

    /** Morale lost per season by an idle employee. */
    public static final int IDLE_MORALE_LOSS_PER_SEASON = 2;

    /** Morale gained per season by a busy (assigned or in-training) employee. */
    public static final int BUSY_MORALE_GAIN_PER_SEASON = 3;

    /** The lowest morale can fall to. */
    public static final int MORALE_MIN = 0;

    /** The highest morale can rise to. */
    public static final int MORALE_MAX = 100;

    /**
     * At or below this morale, an employee not protected from departure
     * (see {@link #isProtectedFromDeparture}) resigns that same season.
     */
    public static final int RESIGNATION_MORALE_FLOOR = 35;

    /**
     * The lower bound of the "disgruntled" reporting band (inclusive) -
     * worth a warning in a future season-news report, but not itself a
     * lifecycle-state change. Not yet consumed anywhere in code; no news/
     * report system exists yet to read it.
     */
    public static final int DISGRUNTLED_MORALE_BAND_MIN = 36;

    /**
     * The upper bound of the "disgruntled" reporting band (inclusive). See
     * {@link #DISGRUNTLED_MORALE_BAND_MIN}.
     */
    public static final int DISGRUNTLED_MORALE_BAND_MAX = 44;

    /** Retirement age, in whole years, for the astronaut roster. */
    public static final int ASTRONAUT_RETIREMENT_AGE = 50;

    /** Retirement age, in whole years, for the Mission Control roster. */
    public static final int MISSION_CONTROL_RETIREMENT_AGE = 60;

    /**
     * Retirement age, in whole years, for the SET roster. Lowered from an
     * initial 70 to 65 the same session (user decision, 2026-09-30) - 70
     * "seemed a bit extreme."
     */
    public static final int SET_RETIREMENT_AGE = 65;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelLifecycle() {
    }

    /**
     * Returns the given group's retirement age, in whole years.
     * Package-private: a shared lookup for any resolver in this package
     * that needs to check age against retirement (e.g. {@link MissionProgress}'s
     * retire-on-release check), not a public part of this class's own API.
     *
     * @param group the personnel group
     * @return that group's retirement age
     */
    static int retirementAgeFor(PersonnelGroup group) {
        return switch (group) {
            case ASTRONAUT -> ASTRONAUT_RETIREMENT_AGE;
            case SET -> SET_RETIREMENT_AGE;
            case MISSION_CONTROL -> MISSION_CONTROL_RETIREMENT_AGE;
        };
    }

    /**
     * Runs one season-advance pass over every employee in all three of the
     * given snapshot's rosters, mutating it in place: updates age and
     * morale, and removes anyone who automatically resigned or retired this
     * season.
     *
     * @param snapshot the player snapshot to advance; mutated in place
     * @return the departures this pass caused, in no particular cross-roster
     *     order; empty if nobody left
     * @throws NullPointerException if {@code snapshot} is {@code null}
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        List<ResolutionEvent> events = new ArrayList<>();
        advanceGroup(snapshot.getAstronauts(), PersonnelGroup.ASTRONAUT, ASTRONAUT_RETIREMENT_AGE, events);
        advanceGroup(snapshot.getSetSpecialists(), PersonnelGroup.SET, SET_RETIREMENT_AGE, events);
        advanceGroup(snapshot.getMissionControlOfficers(), PersonnelGroup.MISSION_CONTROL,
            MISSION_CONTROL_RETIREMENT_AGE, events);
        return events;
    }

    /**
     * Whether an employee is protected from the resignation/retirement
     * check this season - the per-group rule this class's own Javadoc
     * explains in full. SET is never protected; Astronaut and Mission
     * Control are protected only while {@link AssignmentState#ASSIGNED}.
     *
     * @param group the employee's personnel group
     * @param state the employee's current assignment state
     * @return {@code true} if the resignation/retirement check must be
     *     skipped for them this season
     */
    static boolean isProtectedFromDeparture(PersonnelGroup group, AssignmentState state) {
        if (group == PersonnelGroup.SET) {
            return false;
        }
        return state == AssignmentState.ASSIGNED;
    }

    /**
     * Advances one roster, removing anyone who departs.
     *
     * @param roster        the roster to advance and prune, of any {@link Personnel}
     *                      subtype ({@link de.mkoehler.launchwindow.model.Astronaut},
     *                      {@link SetSpecialist}, {@link MissionControlOfficer})
     * @param group         the roster's own {@link PersonnelGroup} label, for reporting
     * @param retirementAge the roster's retirement age, in whole years
     * @param events        the list to append any {@link PersonnelDeparture} to
     */
    private static <T extends Personnel> void advanceGroup(List<T> roster, PersonnelGroup group,
            int retirementAge, List<ResolutionEvent> events) {
        Iterator<T> iterator = roster.iterator();
        while (iterator.hasNext()) {
            T person = iterator.next();
            // advanceOne never itself mutates assignmentState, so reading it here (after
            // morale/age have already advanced) still reflects what this employee was doing
            // this season.
            DepartureReason reason = advanceOne(person, group, retirementAge);
            if (reason != null) {
                events.add(new PersonnelDeparture(group, person.getId(), person.getName(), reason,
                    person.getAge(), person.getMorale(), person.getAssignmentState(),
                    rdTargetComponentIdAtExit(person, group)));
                iterator.remove();
            }
        }
    }

    /**
     * Returns the departing employee's R&amp;D target component id, for
     * {@link PersonnelDeparture#rdTargetComponentIdAtExit()} - only
     * {@code SET} employees do R&amp;D, so every other group's departure
     * reports {@code null} unconditionally rather than inspecting the
     * employee at all.
     *
     * @param person the departing employee
     * @param group  their personnel group
     * @return their R&amp;D target component id, or {@code null} if they
     *     aren't {@code SET} or have no target set
     */
    static String rdTargetComponentIdAtExit(Personnel person, PersonnelGroup group) {
        if (group != PersonnelGroup.SET) {
            return null;
        }
        return ((SetSpecialist) person).getRdTargetComponentId();
    }

    /**
     * Advances one employee's age and morale, and decides whether they
     * depart this season.
     *
     * @param person        the employee to advance
     * @param group         their personnel group
     * @param retirementAge their roster's retirement age, in whole years
     * @return why they depart, or {@code null} if they stay
     */
    private static DepartureReason advanceOne(Personnel person, PersonnelGroup group, int retirementAge) {
        boolean idle = person.getAssignmentState() == AssignmentState.IDLE;
        int moraleDelta = idle ? -IDLE_MORALE_LOSS_PER_SEASON : BUSY_MORALE_GAIN_PER_SEASON;
        person.setMorale(clamp(person.getMorale() + moraleDelta, MORALE_MIN, MORALE_MAX));
        advanceAge(person);

        if (isProtectedFromDeparture(group, person.getAssignmentState())) {
            return null;
        }
        if (person.getMorale() <= RESIGNATION_MORALE_FLOOR) {
            return DepartureReason.RESIGNED;
        }
        if (person.getAge() >= retirementAge) {
            return DepartureReason.RETIRED;
        }
        return null;
    }

    /**
     * Advances one employee's age by one season, rolling the fractional
     * {@link Personnel#getAgingProgressSeasons()} counter over into a whole
     * extra year of {@link Personnel#getAge()} every {@link #SEASONS_PER_YEAR}
     * seasons.
     *
     * @param person the employee to age by one season
     */
    private static void advanceAge(Personnel person) {
        int progress = person.getAgingProgressSeasons() + 1;
        if (progress >= SEASONS_PER_YEAR) {
            person.setAge(person.getAge() + 1);
            progress = 0;
        }
        person.setAgingProgressSeasons(progress);
    }

    /**
     * Clamps a value to a closed range.
     *
     * @param value the value to clamp
     * @param min   the lowest allowed value
     * @param max   the highest allowed value
     * @return {@code value}, clamped to {@code [min, max]}
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
