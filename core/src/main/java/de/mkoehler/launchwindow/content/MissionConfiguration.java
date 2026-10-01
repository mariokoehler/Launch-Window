package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>One way of flying a {@link Program} - the third level of the
 * four-level containment hierarchy (see {@code design.md} &sect;2.3(c)).
 * Holds its own step-by-step resolution content - {@link #seasons()} - but
 * nothing about prestige or goals granted, nor the "extended duration"
 * variant's own higher-risk/higher-reward shape (see {@code baspm.md}
 * &sect;10.1); see {@code design.md} &sect;2.5(e) and &sect;2.6
 * (prestige/goals) for what is still deferred. Adding those fields later is
 * not a migration concern for this type, since it isn't persisted anywhere
 * yet (see {@link ContentCatalog}'s own class Javadoc).</p>
 *
 * <p>A content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id        a stable identifier, unique within a {@link ContentCatalog}
 * @param programId the {@link Program#id()} this configuration belongs to
 * @param seasons   this configuration's ordered step batches, one per
 *                  season it runs - see {@code baspm.md} &sect;8.1's
 *                  one-file-per-season shape; never empty.
 *                  {@link #durationInSeasons()} is {@code seasons.size()}
 * @author Mario Koehler
 */
public record MissionConfiguration(String id, String programId, List<MissionSeason> seasons) {

    /**
     * Validates every field and defensively copies {@code seasons} into an
     * unmodifiable view.
     *
     * @throws NullPointerException     if {@code id}/{@code programId}/
     *                                   {@code seasons} is {@code null}
     * @throws IllegalArgumentException if {@code id}/{@code programId} is
     *                                   blank, or {@code seasons} is empty
     */
    public MissionConfiguration {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(seasons, "seasons must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (programId.isBlank()) {
            throw new IllegalArgumentException("programId must not be blank");
        }
        if (seasons.isEmpty()) {
            throw new IllegalArgumentException("seasons must not be empty");
        }
        seasons = List.copyOf(seasons);
    }

    /**
     * How many seasons flying this configuration takes - derived from
     * {@link #seasons()} rather than stored separately, so the two can
     * never disagree.
     *
     * @return {@code seasons().size()}
     */
    public int durationInSeasons() {
        return seasons.size();
    }
}
