package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * <p>Declares that a player's funds should now be a given value.</p>
 *
 * <p>A placeholder action, not a game mechanic.</p>
 *
 * @author Mario Koehler
 */
public class SetFundsAction extends PlayerAction {

    private long funds;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public SetFundsAction() {
    }

    /**
     * Creates a new action declaring the player's funds should now be
     * {@code funds}.
     *
     * @param funds the declared funds value
     */
    public SetFundsAction(long funds) {
        this.funds = funds;
    }

    /**
     * Returns the declared funds value.
     *
     * @return the funds value
     */
    public long getFunds() {
        return funds;
    }

    /**
     * Sets the declared funds value.
     *
     * @param funds the funds value
     */
    public void setFunds(long funds) {
        this.funds = funds;
    }

    /**
     * Sets the given snapshot's funds to this action's declared value.
     *
     * @param snapshot the snapshot to mutate
     */
    @Override
    public void applyTo(PlayerSnapshot snapshot) {
        snapshot.setFunds(funds);
    }
}
