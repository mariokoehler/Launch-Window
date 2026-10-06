package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.PrPress;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * PR duty for astronauts ({@code design.md} &sect;2.2(c), user decisions 2026-10-06): a standing order
 * for an idle astronaut to tour, give interviews and appear in public instead of training. It counts
 * as busy (morale rises as in training), grows no skill, keeps the astronaut off crews and CAPCOM, and
 * earns prestige every season it lasts. Real precedent: the Mercury Seven's {@code Life} magazine deal
 * and Gagarin's world tours.
 *
 * <ul>
 *   <li><b>Yield</b> ({@link #individualYield}): Leadership / 10, plus fame: 3 per completed flight
 *       and 5 per flight that set a first in the game, at most {@link #MAX_YIELD} a season.</li>
 *   <li><b>Diminishing returns</b> ({@link #effectiveYields}): the public's attention is limited, so
 *       the astronauts on duty are ranked by yield and earn 100 %, 60 %, 30 % and then nothing.</li>
 *   <li><b>Rust</b>: from the {@link #RUST_FREE_SEASONS}+1th season in a row on duty, Piloting and
 *       Fitness each lose {@link #RUST_PER_SEASON} a season. The count lives in the season pass, so a
 *       recall and restart on the same day doesn't reset it. An Expert keeps the trait.</li>
 *   <li><b>Press stories</b>: each season on duty has a {@link #PRESS_CHANCE} chance of making the
 *       papers ({@link PrPress}); a good story adds half the yield (or the share it sets), a bad
 *       one costs half of it, and some stories do more ({@link PressEffects}). No story repeats for
 *       a player until every story of its tone has run once.</li>
 * </ul>
 *
 * <p>Every number here is our own and unconfirmed, to be tuned in play.</p>
 *
 * @author Mario Koehler
 */
public final class PrDuty {

    /** The most one astronaut earns a season, before diminishing returns. */
    public static final long MAX_YIELD = 25;

    /** Fame from each completed flight. */
    public static final long PER_FLIGHT = 3;

    /** Fame from each flight that set a first in the game. */
    public static final long PER_FIRST = 5;

    /** What the 1st, 2nd, 3rd, ... astronaut on duty earns of their yield; later ones earn nothing. */
    public static final List<Double> SHARES = List.of(1.0, 0.6, 0.3);

    /** Seasons in a row on duty before rust sets in. */
    public static final int RUST_FREE_SEASONS = 2;

    /** Points of Piloting and of Fitness lost each rusty season. */
    public static final double RUST_PER_SEASON = 1.0;

    /** The chance a season on duty makes the papers. */
    public static final double PRESS_CHANCE = 0.1;

    /** The chance a story that makes the papers is a good one. */
    public static final double GOOD_PRESS_CHANCE = 0.7;

    /** A press story's effect, as a share of the season's yield (added for good, taken for bad). */
    public static final double PRESS_SHARE = 0.5;

    /** The result of starting or recalling. */
    public enum Outcome {
        /** The astronaut went on duty. */
        STARTED,
        /** The astronaut came off duty and is idle again. */
        RECALLED,
        /** No astronaut with that id. */
        UNKNOWN_ASTRONAUT,
        /** The astronaut isn't idle and available, so can't go on duty. */
        NOT_IDLE,
        /** The astronaut isn't on duty, so can't be recalled. */
        NOT_ON_DUTY
    }

    private PrDuty() {
    }

    /**
     * Sends an idle, available astronaut on PR duty, a standing order until recalled.
     *
     * @param snapshot    the player
     * @param astronautId the astronaut
     * @return the outcome; nothing changes unless {@link Outcome#STARTED}
     */
    public static Outcome start(PlayerSnapshot snapshot, String astronautId) {
        Astronaut astronaut = find(snapshot, astronautId);
        if (astronaut == null) {
            return Outcome.UNKNOWN_ASTRONAUT;
        }
        if (astronaut.getAssignmentState() != AssignmentState.IDLE || astronaut.getUnavailableSeasons() > 0) {
            return Outcome.NOT_IDLE;
        }
        astronaut.setAssignmentState(AssignmentState.PR_DUTY);
        return Outcome.STARTED;
    }

    /**
     * Calls an astronaut back from PR duty; they're idle again at once.
     *
     * @param snapshot    the player
     * @param astronautId the astronaut
     * @return the outcome; nothing changes unless {@link Outcome#RECALLED}
     */
    public static Outcome recall(PlayerSnapshot snapshot, String astronautId) {
        Astronaut astronaut = find(snapshot, astronautId);
        if (astronaut == null) {
            return Outcome.UNKNOWN_ASTRONAUT;
        }
        if (astronaut.getAssignmentState() != AssignmentState.PR_DUTY) {
            return Outcome.NOT_ON_DUTY;
        }
        astronaut.setAssignmentState(AssignmentState.IDLE);
        return Outcome.RECALLED;
    }

    /**
     * Returns what one astronaut earns a season on duty, before diminishing returns.
     *
     * @param astronaut the astronaut
     * @return the yield, 0 to {@link #MAX_YIELD}
     */
    public static long individualYield(Astronaut astronaut) {
        long leadership = Math.round(astronaut.getSkill(AstronautSkill.LEADERSHIP) / 10.0);
        long fame = PER_FLIGHT * astronaut.getFlightsFlown() + PER_FIRST * astronaut.getFirstsFlown();
        return Math.min(MAX_YIELD, leadership + fame);
    }

    /**
     * Returns what each astronaut in a group on duty earns, after diminishing returns: ranked by their
     * own yield (roster order breaks ties), the first earns all of it, the next 60 %, then 30 %, then
     * nothing. The same numbers the season pass, the PR dialog and the Public Affairs Office show.
     *
     * @param onDuty the astronauts on duty, in roster order
     * @return each one's id mapped to their earnings, in roster order
     */
    public static Map<String, Long> effectiveYields(List<Astronaut> onDuty) {
        List<Astronaut> ranked = new ArrayList<>(onDuty);
        ranked.sort(Comparator.comparingLong(PrDuty::individualYield).reversed());
        Map<String, Long> shares = new LinkedHashMap<>();
        for (int rank = 0; rank < ranked.size(); rank++) {
            double share = rank < SHARES.size() ? SHARES.get(rank) : 0.0;
            shares.put(ranked.get(rank).getId(), Math.round(individualYield(ranked.get(rank)) * share));
        }
        Map<String, Long> inRosterOrder = new LinkedHashMap<>();
        for (Astronaut astronaut : onDuty) {
            inRosterOrder.put(astronaut.getId(), shares.get(astronaut.getId()));
        }
        return inRosterOrder;
    }

    /**
     * Returns the astronauts on PR duty, in roster order.
     *
     * @param snapshot the player
     * @return the astronauts on duty
     */
    public static List<Astronaut> onDuty(PlayerSnapshot snapshot) {
        return snapshot.getAstronauts().stream()
            .filter(astronaut -> astronaut.getAssignmentState() == AssignmentState.PR_DUTY).toList();
    }

    /**
     * Returns the prestige the astronauts on duty earn per season now, before press stories.
     *
     * @param snapshot the player
     * @return the total
     */
    public static long perSeason(PlayerSnapshot snapshot) {
        return effectiveYields(onDuty(snapshot)).values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * Runs one season of PR duty: every astronaut on duty earns their share, the papers may pick a
     * tour up, rust sets in after {@link #RUST_FREE_SEASONS} seasons in a row, and an astronaut off
     * duty has their count reset. Draws randomness only for astronauts on duty.
     *
     * @param snapshot the player; mutated in place
     * @param press    the press stories
     * @param random   the season's random source
     * @return one event per astronaut on duty, in roster order
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, PrPress press, RandomGenerator random) {
        return advanceSeason(snapshot, press, random, null, 1);
    }

    /**
     * Runs one season of PR duty, with the content and the season count that some press stories'
     * effects need ({@link PressEffects#usable}).
     *
     * @param snapshot       the player; mutated in place
     * @param press          the press stories
     * @param random         the season's random source
     * @param catalog        the content, or {@code null} (stories that set research back can't run)
     * @param seasonsElapsed the player's seasons elapsed counting this one
     * @return one event per astronaut on duty, in roster order
     */
    public static List<ResolutionEvent> advanceSeason(PlayerSnapshot snapshot, PrPress press, RandomGenerator random,
            ContentCatalog catalog, int seasonsElapsed) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(press, "press must not be null");
        Objects.requireNonNull(random, "random must not be null");
        List<Astronaut> onDuty = onDuty(snapshot);
        Map<String, Long> yields = effectiveYields(onDuty);
        List<ResolutionEvent> events = new ArrayList<>();
        long total = 0;
        for (Astronaut astronaut : snapshot.getAstronauts()) {
            if (astronaut.getAssignmentState() != AssignmentState.PR_DUTY) {
                astronaut.setPrDutySeasons(0);
                continue;
            }
            long yield = yields.get(astronaut.getId());
            long earned = yield;
            String story = null;
            List<PrPress.Effect> effects = List.of();
            if (random.nextDouble() < PRESS_CHANCE) {
                boolean good = random.nextDouble() < GOOD_PRESS_CHANCE;
                story = press.pick(good ? PrPress.Tone.GOOD : PrPress.Tone.BAD, random,
                    snapshot.getUsedPressStoryIds(),
                    candidate -> PressEffects.usable(candidate, snapshot, catalog, seasonsElapsed));
                if (story != null) {
                    PrPress.Story picked = press.byId(story).orElseThrow();
                    long effect = Math.round(yield * PressEffects.prestigeShare(picked, PRESS_SHARE));
                    earned = good ? yield + effect : -effect;
                    effects = PressEffects.apply(picked, astronaut, snapshot, catalog, random);
                }
            }
            int seasons = astronaut.getPrDutySeasons() + 1;
            astronaut.setPrDutySeasons(seasons);
            double rust = 0;
            if (seasons > RUST_FREE_SEASONS) {
                rust = RUST_PER_SEASON;
                for (AstronautSkill skill : List.of(AstronautSkill.PILOTING, AstronautSkill.FITNESS)) {
                    astronaut.setSkill(skill, Math.max(0.0, astronaut.getSkill(skill) - rust));
                }
            }
            total += earned;
            events.add(new PrDutyPerformed(astronaut.getId(), astronaut.getName(), yield, earned, story, rust,
                effects));
        }
        snapshot.setPrestige(Math.max(0, snapshot.getPrestige() + total));
        return events;
    }

    private static Astronaut find(PlayerSnapshot snapshot, String astronautId) {
        return snapshot.getAstronauts().stream().filter(astronaut -> astronaut.getId().equals(astronautId))
            .findFirst().orElse(null);
    }
}
