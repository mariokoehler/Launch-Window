package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PrestigeConversion;

/**
 * Converts prestige into funds while in debt ({@link PrestigeConversion#convert}).
 *
 * <p>Journaled as {@code "convertPrestige"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class ConvertPrestigeAction extends PlayerAction {

    private long amount;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public ConvertPrestigeAction() {
    }

    /**
     * Creates the action.
     *
     * @param amount the funds asked for
     */
    public ConvertPrestigeAction(long amount) {
        setAmount(amount);
    }

    /**
     * Returns the funds asked for.
     *
     * @return the funds asked for
     */
    public long getAmount() {
        return amount;
    }

    /**
     * Sets the funds asked for.
     *
     * @param amount the funds asked for
     */
    public void setAmount(long amount) {
        this.amount = amount;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PrestigeConversion.convert(snapshot, context.catalog(), context.schedule(), amount),
            PrestigeConversion.Outcome.CONVERTED);
    }
}
