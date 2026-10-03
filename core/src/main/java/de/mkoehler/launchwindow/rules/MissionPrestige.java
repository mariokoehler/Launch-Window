package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.model.FlightTier;

import java.util.Objects;

/**
 * <p>The pure prestige arithmetic for mission flights: which
 * {@link FlightTier} a launch pays at, what a tier's total prestige is,
 * and what one step's share of it pays. Headless, no state - the
 * {@link MissionProgress} resolver owns the per-season accumulation and
 * applies these numbers (see {@code design.md} &sect;2.6(d)).</p>
 *
 * <p><b>The repeat-tier ratios are Launch Window's own, not BASPM's.</b>
 * BASPM's own shape is a fixed uniform decay ({@code baspm.md} &sect;13.4a:
 * 50% / 30%); {@code design.md} &sect;1.2 and &sect;4 say to keep the shape
 * but re-derive the numbers. These are unconfirmed defaults chosen to be
 * flatter than BASPM's, so repeat flights stay worth flying.</p>
 *
 * @author Mario Koehler
 */
public final class MissionPrestige {

    /** Percentage of {@code firstTimePrestige} a second-time flight pays. Unconfirmed default. */
    public static final int SECOND_TIME_PERCENT = 60;

    /** Percentage of {@code firstTimePrestige} a later-time flight pays. Unconfirmed default. */
    public static final int LATER_TIME_PERCENT = 40;

    /**
     * Not instantiable; this class only exposes static methods.
     */
    private MissionPrestige() {
    }

    /**
     * Picks the repeat tier a launch pays at, from how many successful
     * completions of the same configuration the player already has.
     *
     * @param successfulFlightCount the player's prior successful completions
     *                              of this configuration; never negative
     * @return the tier for that count
     * @throws IllegalArgumentException if {@code successfulFlightCount} is negative
     */
    public static FlightTier tierFor(int successfulFlightCount) {
        if (successfulFlightCount < 0) {
            throw new IllegalArgumentException("successfulFlightCount must not be negative: " + successfulFlightCount);
        }
        if (successfulFlightCount == 0) {
            return FlightTier.FIRST_TIME;
        }
        return successfulFlightCount == 1 ? FlightTier.SECOND_TIME : FlightTier.LATER_TIME;
    }

    /**
     * Returns the total prestige a configuration pays at one repeat tier,
     * before any step's share is taken. Integer division floors, so a tier
     * total is never rounded up.
     *
     * @param configuration the configuration being flown
     * @param tier          the repeat tier the flight pays at
     * @return the tier's total prestige, non-negative
     * @throws NullPointerException if either argument is {@code null}
     */
    public static long tierTotal(MissionConfiguration configuration, FlightTier tier) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        Objects.requireNonNull(tier, "tier must not be null");
        int percent = switch (tier) {
            case FIRST_TIME -> 100;
            case SECOND_TIME -> SECOND_TIME_PERCENT;
            case LATER_TIME -> LATER_TIME_PERCENT;
        };
        return configuration.firstTimePrestige() * percent / 100;
    }

    /**
     * Returns one step's payout from a tier total: its percentage share,
     * floored. Halving for a {@code MINOR_GLITCH} is the caller's job, not
     * this method's.
     *
     * @param tierTotal    the configuration's total at the flight's tier
     * @param sharePercent the step's {@link de.mkoehler.launchwindow.content.MissionStep#prestigeSharePercent()}
     * @return the step's payout, non-negative
     */
    public static long stepPayout(long tierTotal, int sharePercent) {
        return tierTotal * sharePercent / 100;
    }
}
