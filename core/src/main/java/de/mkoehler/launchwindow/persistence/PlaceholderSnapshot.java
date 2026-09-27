package de.mkoehler.launchwindow.persistence;

/**
 * Placeholder record for {@link JacksonRoundTripTest} only - proves the
 * Jackson dependency actually serializes/deserializes correctly end to end
 * for this project's flat-file JSON persistence model (design.md 3.7: one
 * snapshot file per player per turn, no database), before any real
 * snapshot/action-journal record types exist. Delete this once real
 * persistence types (e.g. a PlayerSnapshot, an Action subtype) exist and
 * have their own tests.
 *
 * @param agencyName the player's chosen agency name (design.md 3.8)
 * @param funds       current funds
 * @param season      the player's own personal season count (design.md 3.8 -
 *                    decoupled from the game's shared wall-clock schedule)
 * @author Mario Koehler
 */
public record PlaceholderSnapshot(String agencyName, int funds, int season) {
}
