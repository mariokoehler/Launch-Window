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
 * {@code design.md} &sect;2.2): skill growth (training, R&amp;D, flying a
 * mission), training-state transitions, the hiring pool, and the Expert
 * trait. This pass only ever touches {@link Personnel#getAge()},
 * {@link Personnel#getMorale()}, and roster membership - it never reads or
 * writes a skill map. It also never touches {@link PlayerSnapshot#getSeason()}
 * itself; season advancement is {@code PlayerStateService}'s job
 * ({@code design.md} &sect;3.7), not the rules engine's.</p>
 *
 * <p><b>Order of operations per employee, matching BASPM's own shape</b>
 * ({@code baspm.md} &sect;9.2): morale updates first (idle loses, busy
 * gains), then age, then - only while idle - resignation is checked against
 * the just-updated morale, and only if not resigned, retirement is checked
 * against the just-updated age. A busy employee's morale and age still
 * update every season; only the resignation/retirement *check* is
 * idle-gated, matching BASPM's own "busy employees are protected" property,
 * which {@code design.md} &sect;2.2(c) explicitly decided to keep.</p>
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
     * At or below this morale, an idle employee resigns that same season.
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

    /** Retirement age, in whole years, for the SET roster. */
    public static final int SET_RETIREMENT_AGE = 70;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private PersonnelLifecycle() {
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
            DepartureReason reason = advanceOne(person, retirementAge);
            if (reason != null) {
                events.add(new PersonnelDeparture(group, person.getId(), person.getName(), reason,
                    person.getAge(), person.getMorale()));
                iterator.remove();
            }
        }
    }

    /**
     * Advances one employee's age and morale, and decides whether they
     * depart this season.
     *
     * @param person        the employee to advance
     * @param retirementAge their roster's retirement age, in whole years
     * @return why they depart, or {@code null} if they stay
     */
    private static DepartureReason advanceOne(Personnel person, int retirementAge) {
        boolean idle = person.getAssignmentState() == AssignmentState.IDLE;
        int moraleDelta = idle ? -IDLE_MORALE_LOSS_PER_SEASON : BUSY_MORALE_GAIN_PER_SEASON;
        person.setMorale(clamp(person.getMorale() + moraleDelta, MORALE_MIN, MORALE_MAX));
        advanceAge(person);

        if (!idle) {
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
