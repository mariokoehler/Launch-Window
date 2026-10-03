package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelActions;

/**
 * Puts an idle SET specialist on R&amp;D for a component ({@link PersonnelActions#assignResearch}).
 *
 * <p>Journaled as {@code "assignResearch"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class AssignResearchAction extends PlayerAction {

    private String specialistId;
    private String componentId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public AssignResearchAction() {
    }

    /**
     * Creates the action.
     *
     * @param specialistId the specialist
     * @param componentId the component
     */
    public AssignResearchAction(String specialistId, String componentId) {
        setSpecialistId(specialistId);
        setComponentId(componentId);
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

    /**
     * Returns the component.
     *
     * @return the component
     */
    public String getComponentId() {
        return componentId;
    }

    /**
     * Sets the component.
     *
     * @param componentId the component
     */
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(PersonnelActions.assignResearch(snapshot, specialistId, componentId, context.catalog()),
            PersonnelActions.ResearchOutcome.ASSIGNED);
    }
}
