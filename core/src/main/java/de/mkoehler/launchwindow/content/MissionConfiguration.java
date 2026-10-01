package de.mkoehler.launchwindow.content;

import java.util.Objects;

/**
 * <p>One way of flying a {@link Program} - the third level of the
 * four-level containment hierarchy (see {@code design.md} &sect;2.3(c)).
 * Deliberately minimal: duration only. Prestige, goals granted, and the
 * "extended duration" variant's own higher-risk/higher-reward shape (see
 * {@code baspm.md} &sect;10.1) are not modeled yet - see {@code design.md}
 * &sect;2.5 (mission simulation) and &sect;2.6 (prestige/goals). Adding
 * those fields later is not a migration concern for this type, since it
 * isn't persisted anywhere yet (see {@link ContentCatalog}'s own class
 * Javadoc).</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id                a stable identifier, unique within a
 *                          {@link ContentCatalog}
 * @param programId         the {@link Program#id()} this configuration
 *                          belongs to
 * @param durationInSeasons how many seasons flying this configuration takes
 * @author Mario Koehler
 */
public record MissionConfiguration(String id, String programId, int durationInSeasons) {

    /**
     * Validates every field.
     *
     * @throws NullPointerException     if {@code id}/{@code programId} is {@code null}
     * @throws IllegalArgumentException if {@code id}/{@code programId} is
     *                                   blank, or {@code durationInSeasons}
     *                                   is less than 1
     */
    public MissionConfiguration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (programId.isBlank()) {
            throw new IllegalArgumentException("programId must not be blank");
        }
        if (durationInSeasons < 1) {
            throw new IllegalArgumentException("durationInSeasons must be at least 1: " + durationInSeasons);
        }
    }
}
