package de.mkoehler.launchwindow.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * <p>One player-submitted action, persisted as its own append-only journal
 * file (see {@code design.md} &sect;3.7). Every subtype is an idempotent
 * "declare current intent" statement, not a delta - applying the same
 * action twice, or applying a later action of the same kind after an
 * earlier one, always converges on the same result. This is what lets
 * {@link SnapshotReplay} replay a whole day's journal in order with no
 * separate cancel/retract mechanism.</p>
 *
 * @author Mario Koehler
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SetFundsAction.class, name = "setFunds")
})
public abstract class PlayerAction {

    /**
     * Applies this action's declared intent onto the given snapshot,
     * mutating it in place.
     *
     * @param snapshot the snapshot to mutate
     */
    public abstract void applyTo(PlayerSnapshot snapshot);
}
