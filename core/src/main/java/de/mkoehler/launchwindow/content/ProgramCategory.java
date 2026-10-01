package de.mkoehler.launchwindow.content;

import java.util.Objects;

/**
 * <p>The top level of the four-level program containment hierarchy -
 * Program Category &rarr; {@link Program} &rarr; {@link MissionConfiguration}
 * &rarr; {@link MissionComponent} (see {@code design.md} &sect;2.3(c)). A
 * content-definition type: shared, read-only game data loaded from a
 * content pack (not yet implemented - see {@code design.md} &sect;5's M3),
 * never per-player state, and never sent over the network.</p>
 *
 * @param id   a stable identifier, unique within a {@link ContentCatalog}
 * @param name display name
 * @author Mario Koehler
 */
public record ProgramCategory(String id, String name) {

    /**
     * Validates that both fields are present and non-blank.
     *
     * @throws NullPointerException     if either field is {@code null}
     * @throws IllegalArgumentException if either field is blank
     */
    public ProgramCategory {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
