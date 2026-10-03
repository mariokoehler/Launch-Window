package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.ContentCatalog;

import java.util.Objects;

/**
 * The game data a {@link PlayerAction} is validated and applied against: the game's
 * content pack and budget schedule. The same context must be used for every replay
 * of a player's journal, or replays could diverge ({@code design.md} &sect;5, M4).
 *
 * @param catalog  the game's content pack
 * @param schedule the game's budget schedule
 * @author Mario Koehler
 */
public record ActionContext(ContentCatalog catalog, BudgetSchedule schedule) {

    /**
     * Validates the context.
     *
     * @throws NullPointerException if an argument is {@code null}
     */
    public ActionContext {
        Objects.requireNonNull(catalog, "catalog must not be null");
        Objects.requireNonNull(schedule, "schedule must not be null");
    }
}
