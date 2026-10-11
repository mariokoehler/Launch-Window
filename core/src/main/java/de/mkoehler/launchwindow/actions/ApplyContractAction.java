package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.ContractOutcome;
import de.mkoehler.launchwindow.rules.Contracts;

/**
 * Applies for a listing on the contract board, or withdraws the application ({@code design.md}
 * &sect;2.11): one sealed application per turn, decided when the turn resolves. A later application
 * this turn replaces the earlier one.
 *
 * @author Mario Koehler
 */
public class ApplyContractAction extends PlayerAction {

    private String listingId;
    private String programId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public ApplyContractAction() {
    }

    /**
     * Creates the action.
     *
     * @param listingId the listing, or {@code null} to withdraw
     * @param programId the program to fly it with, or {@code null} for the line's first qualifying one
     */
    public ApplyContractAction(String listingId, String programId) {
        this.listingId = listingId;
        this.programId = programId;
    }

    /**
     * Returns the listing.
     *
     * @return the listing, or {@code null} to withdraw
     */
    public String getListingId() {
        return listingId;
    }

    /**
     * Sets the listing.
     *
     * @param listingId the listing, or {@code null} to withdraw
     */
    public void setListingId(String listingId) {
        this.listingId = listingId;
    }

    /**
     * Returns the program.
     *
     * @return the program, or {@code null}
     */
    public String getProgramId() {
        return programId;
    }

    /**
     * Sets the program.
     *
     * @param programId the program, or {@code null}
     */
    public void setProgramId(String programId) {
        this.programId = programId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        ContractOutcome outcome = Contracts.apply(snapshot, listingId, programId, context.catalog());
        return new ActionResult(outcome == ContractOutcome.APPLIED || outcome == ContractOutcome.WITHDRAWN,
            outcome.name());
    }
}
