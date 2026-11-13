package de.mkoehler.launchwindow.rules;

/**
 * Reports that one person expects more pay after this turn's work ({@link PayExpectations}, {@code design.md}
 * &sect;2.2(c)): a flight, a First, the Expert trait, a fulfilled ambition or an R&amp;D stint.
 *
 * @param group       their department
 * @param personnelId their stable id
 * @param name        their display name
 * @param before      the expectation before
 * @param after       the expectation after (before any review's rise the same turn)
 * @param percent     the rise, rounded
 * @param reasons     what earned it, in the rule's order: {@code FLIGHT:<role>} ({@code CREW}, {@code CAPCOM},
 *                    {@code CONTROLLER}), {@code FIRST}, {@code EXPERT}, {@code AMBITION},
 *                    {@code RD_STINT:<component id>}
 * @author Mario Koehler
 */
public record ExpectationRose(
    PersonnelGroup group,
    String personnelId,
    String name,
    double before,
    double after,
    long percent,
    java.util.List<String> reasons
) implements ResolutionEvent {

    /**
     * Normalizes a missing list.
     */
    public ExpectationRose {
        reasons = reasons == null ? java.util.List.of() : java.util.List.copyOf(reasons);
    }
}
