package de.mkoehler.launchwindow.rules;

/**
 * Reports that everyone's pay expectation rose with a funding review ({@link PayExpectations}, {@code design.md}
 * &sect;2.2(c)).
 *
 * @param reviewNumber the review
 * @param percent      the rise, in percent
 * @param underpaid    how many are underpaid after it ({@link PayExpectations#isUnderpaid})
 * @param byGroup      the underpaid by department ({@link PersonnelGroup} name), departments with none left out
 * @author Mario Koehler
 */
public record ExpectationsRoseAtReview(
    int reviewNumber,
    long percent,
    int underpaid,
    java.util.Map<String, Integer> byGroup
) implements ResolutionEvent {

    /**
     * Normalizes a missing map.
     */
    public ExpectationsRoseAtReview {
        byGroup = byGroup == null ? java.util.Map.of() : java.util.Map.copyOf(byGroup);
    }
}
