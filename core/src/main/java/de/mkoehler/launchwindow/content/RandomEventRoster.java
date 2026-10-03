package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.RandomEventKind;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <p>The random-event content: which events exist, their magnitude and duration
 * ranges, and the two per-season odds. The resolver's effects and gates are code;
 * these numbers are data, so a content pack can retune them without touching
 * Java ({@code design.md} &sect;2.7(c)).</p>
 *
 * <p>Ranges follow BASPM's own convention, {@code baspm.md} &sect;12.1: an
 * integer draw is {@code [min, max)} (upper-exclusive), a fractional draw is
 * {@code [min, max)} continuous. {@link #baspmClose()} holds BASPM's own magnitudes,
 * per the user's 2026-10-03 instruction to stay close to BASPM's defaults.</p>
 *
 * <p>A content-definition type: shared, read-only game data, never per-player
 * state, and never sent over the network.</p>
 *
 * @param badChancePercent  the chance, in percent, that a season rolls a bad event
 * @param goodChancePercent the chance, in percent, that a season rolls a good event
 * @param events            one spec per {@link RandomEventKind}, each kind exactly once
 * @author Mario Koehler
 */
public record RandomEventRoster(int badChancePercent, int goodChancePercent, List<EventSpec> events) {

    /**
     * One event's ranges.
     *
     * @param kind        the event
     * @param min         the lowest magnitude drawn (percent, points, multiplier, or count)
     * @param max         the highest magnitude drawn; the draw excludes it for integer kinds
     * @param durationMin the fewest seasons the effect lasts; zero for one-off effects
     * @param durationMax the most seasons the effect lasts, exclusive for integer draws; zero for one-off effects
     */
    public record EventSpec(RandomEventKind kind, double min, double max, int durationMin, int durationMax) {

        /**
         * Validates the ranges.
         *
         * @throws NullPointerException     if {@code kind} is {@code null}
         * @throws IllegalArgumentException if a range is inverted or a duration is negative
         */
        public EventSpec {
            Objects.requireNonNull(kind, "kind must not be null");
            if (min > max) {
                throw new IllegalArgumentException(kind + " magnitude range is inverted");
            }
            if (durationMin < 0 || durationMax < durationMin) {
                throw new IllegalArgumentException(kind + " duration range is invalid");
            }
        }
    }

    /**
     * Validates the odds and that every event kind has exactly one spec.
     *
     * @throws NullPointerException     if {@code events} is {@code null}
     * @throws IllegalArgumentException if an odds value is outside 0-100, or a kind is missing or repeated
     */
    public RandomEventRoster {
        Objects.requireNonNull(events, "events must not be null");
        if (badChancePercent < 0 || badChancePercent > 100 || goodChancePercent < 0 || goodChancePercent > 100) {
            throw new IllegalArgumentException("chances must be 0-100 percent");
        }
        events = List.copyOf(events);
        Set<RandomEventKind> seen = EnumSet.noneOf(RandomEventKind.class);
        for (EventSpec spec : events) {
            if (!seen.add(spec.kind())) {
                throw new IllegalArgumentException("duplicate event spec: " + spec.kind());
            }
        }
        if (seen.size() != RandomEventKind.values().length) {
            throw new IllegalArgumentException("every RandomEventKind needs exactly one spec");
        }
    }

    /**
     * Returns the spec for one event kind.
     *
     * @param kind the event
     * @return its spec
     */
    public EventSpec spec(RandomEventKind kind) {
        return events.stream().filter(spec -> spec.kind() == kind).findFirst().orElseThrow();
    }

    /**
     * BASPM's own magnitudes and durations, with BASPM's 5% and 20% odds
     * ({@code baspm.md} &sect;12.1; {@code design.md} &sect;2.7(c)).
     *
     * @return the roster
     */
    public static RandomEventRoster baspmClose() {
        return new RandomEventRoster(5, 20, List.of(
            new EventSpec(RandomEventKind.BASIC_TRAINING_EXTENSION, 1, 1, 0, 0),
            new EventSpec(RandomEventKind.EMPLOYEE_DISMISSED, 1, 1, 0, 0),
            new EventSpec(RandomEventKind.EMPLOYEE_FAILED_BASIC_TRAINING, 1, 1, 0, 0),
            new EventSpec(RandomEventKind.EMPLOYEE_RESIGNING, 1, 1, 0, 0),
            new EventSpec(RandomEventKind.EMPLOYEE_UNAVAILABLE, 1, 1, 1, 2),
            new EventSpec(RandomEventKind.MAX_LAUNCHES_ALLOWED, 1, 1, 1, 2),
            new EventSpec(RandomEventKind.MISSION_COMPONENTS_UNAVAILABLE, 1, 4, 1, 4),
            new EventSpec(RandomEventKind.MORALE_DROP, 10, 20, 0, 0),
            new EventSpec(RandomEventKind.NO_LAUNCHES_ALLOWED, 0, 0, 1, 2),
            new EventSpec(RandomEventKind.PERISHED_ASTRONAUT, 1, 1, 0, 0),
            new EventSpec(RandomEventKind.PRESTIGE_DROP, 4, 10, 0, 0),
            new EventSpec(RandomEventKind.RD_RESULTS_SETBACK, 0.80, 0.95, 1, 5),
            new EventSpec(RandomEventKind.RELIABILITY_SETBACK, 7, 15, 0, 0),
            new EventSpec(RandomEventKind.UNEXPECTED_COST, 7, 15, 0, 0),
            new EventSpec(RandomEventKind.UNEXPECTED_COST_AND_NO_LAUNCHES, 7, 15, 1, 2),
            new EventSpec(RandomEventKind.EXTRA_FUNDING, 7, 16, 0, 0),
            new EventSpec(RandomEventKind.MORALE_BOOST, 9, 21, 0, 0),
            new EventSpec(RandomEventKind.PRESTIGE_POINTS_BOOST, 4, 7.5, 0, 0),
            new EventSpec(RandomEventKind.RD_RESULTS_BOOST, 1.15, 1.25, 1, 8),
            new EventSpec(RandomEventKind.REDUCED_EXPENDITURES, 0.75, 0.95, 3, 12)));
    }
}
