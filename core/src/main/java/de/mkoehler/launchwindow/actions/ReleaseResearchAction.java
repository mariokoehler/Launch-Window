package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelActions;

/**
 * Takes a SET specialist off R&amp;D - the inverse of {@link AssignResearchAction} ({@link PersonnelActions#releaseResearch}).
 *
 * <p>Journaled as {@code "releaseResearch"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class ReleaseResearchAction extends PlayerAction {

    private String specialistId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public ReleaseResearchAction() {
    }

    /**
     * Creates the action.
     *
     * @param specialistId the specialist
     */
    public ReleaseResearchAction(String specialistId) {
        setSpecialistId(specialistId);
    }

    /**
     * Returns the specialist.
     *
     * @return the specialist
     */
    public String getSpecialistId() {
        return specialistId;
    }

    /**
     * Sets the specialist.
     *
     * @param specialistId the specialist
     */
    public void setSpecialistId(String specialistId) {
        this.specialistId = specialistId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PersonnelActions.releaseResearch(snapshot, specialistId),
            PersonnelActions.ResearchOutcome.RELEASED);
    }
}
