package de.mkoehler.launchwindow.rules;

/**
 * Reports that everyone's pay expectation rose with a funding review ({@link PayExpectations}, {@code design.md}
 * &sect;2.2(c)).
 *
 * @param reviewNumber the review
 * @param percent      the rise, in percent
 * @author Mario Koehler
 */
public record ExpectationsRoseAtReview(
    int reviewNumber,
    long percent
) implements ResolutionEvent {
}
