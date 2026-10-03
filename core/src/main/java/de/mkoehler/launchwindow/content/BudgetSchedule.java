package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>The game-wide budget economy's content: starting values, the bracket
 * table each funding review reads, and the prestige-to-funds conversion rate.
 * See {@code design.md} &sect;2.1(d).</p>
 *
 * <p>The bracket table's shape and values are close to BASPM's own
 * (Normal difficulty), per the user's instruction of 2026-10-03 to stay close
 * to BASPM's numbers for defaults. A review reads {@code reviews.get(n - 1)} for
 * review {@code n}, and repeats the last bracket past the table's end.</p>
 *
 * <p>A content-definition type: shared, read-only game data, never per-player
 * state, and never sent over the network.</p>
 *
 * @param initialFunds        the funds a new player starts with
 * @param initialPrestige     the prestige a new player starts with
 * @param initialFundingRate  the funding granted per season before the first review
 * @param prestigePerFund     prestige points spent per fund converted while in debt; {@code design.md} &sect;2.1(d)
 * @param reviews             the bracket table, one entry per successive review; never empty
 * @author Mario Koehler
 */
public record BudgetSchedule(long initialFunds, long initialPrestige, long initialFundingRate,
                             long prestigePerFund, List<Review> reviews) {

    /**
     * One review's bracket: funding interpolates linearly between the two
     * ends, is flat at each end, and rounds up.
     *
     * @param minPrestige the prestige at or below which the review pays {@code minFunding}
     * @param minFunding  the funding per season at the bottom of the bracket
     * @param maxPrestige the prestige at or above which the review pays {@code maxFunding}
     * @param maxFunding  the funding per season at the top of the bracket
     */
    public record Review(long minPrestige, long minFunding, long maxPrestige, long maxFunding) {

        /**
         * Validates the bracket.
         *
         * @throws IllegalArgumentException if any value is negative, or
         *                                   {@code maxPrestige} is not above {@code minPrestige}
         */
        public Review {
            if (minPrestige < 0 || minFunding < 0 || maxFunding < 0) {
                throw new IllegalArgumentException("review values must not be negative");
            }
            if (maxPrestige <= minPrestige) {
                throw new IllegalArgumentException("maxPrestige must exceed minPrestige: " + maxPrestige);
            }
        }
    }

    /**
     * Validates every field and defensively copies {@code reviews}.
     *
     * @throws NullPointerException     if {@code reviews} is {@code null}
     * @throws IllegalArgumentException if a starting value is negative,
     *                                   {@code prestigePerFund} is not positive,
     *                                   or {@code reviews} is empty
     */
    public BudgetSchedule {
        Objects.requireNonNull(reviews, "reviews must not be null");
        if (initialFunds < 0 || initialPrestige < 0 || initialFundingRate < 0) {
            throw new IllegalArgumentException("starting budget values must not be negative");
        }
        if (prestigePerFund <= 0) {
            throw new IllegalArgumentException("prestigePerFund must be positive: " + prestigePerFund);
        }
        reviews = List.copyOf(reviews);
        if (reviews.isEmpty()) {
            throw new IllegalArgumentException("reviews must not be empty");
        }
    }

    /**
     * Returns the bracket a review number reads, repeating the last bracket past
     * the table's end.
     *
     * @param reviewNumber the one-based review number; must be positive
     * @return the bracket for that review
     * @throws IllegalArgumentException if {@code reviewNumber} is not positive
     */
    public Review reviewFor(int reviewNumber) {
        if (reviewNumber < 1) {
            throw new IllegalArgumentException("reviewNumber must be positive: " + reviewNumber);
        }
        return reviews.get(Math.min(reviewNumber, reviews.size()) - 1);
    }
}
