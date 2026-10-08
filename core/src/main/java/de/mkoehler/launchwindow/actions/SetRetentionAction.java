package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.Poaching;

/**
 * Sets how the agency answers rival offers to one person: the agency default, keep at any price,
 * or let go ({@link Poaching}, {@code design.md} &sect;2.2(d)).
 *
 * @author Mario Koehler
 */
public class SetRetentionAction extends PlayerAction {

    private PersonnelGroup group;
    private String personnelId;
    private Retention retention;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public SetRetentionAction() {
    }

    /**
     * Creates the action.
     *
     * @param group       the person's group
     * @param personnelId the person
     * @param retention   the standing order
     */
    public SetRetentionAction(PersonnelGroup group, String personnelId, Retention retention) {
        setGroup(group);
        setPersonnelId(personnelId);
        setRetention(retention);
    }

    /**
     * Returns the person's group.
     *
     * @return the group
     */
    public PersonnelGroup getGroup() {
        return group;
    }

    /**
     * Sets the person's group.
     *
     * @param group the group
     */
    public void setGroup(PersonnelGroup group) {
        this.group = group;
    }

    /**
     * Returns the person.
     *
     * @return the id
     */
    public String getPersonnelId() {
        return personnelId;
    }

    /**
     * Sets the person.
     *
     * @param personnelId the id
     */
    public void setPersonnelId(String personnelId) {
        this.personnelId = personnelId;
    }

    /**
     * Returns the standing order.
     *
     * @return the order
     */
    public Retention getRetention() {
        return retention;
    }

    /**
     * Sets the standing order.
     *
     * @param retention the order
     */
    public void setRetention(Retention retention) {
        this.retention = retention;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Poaching.setRetention(snapshot, group, personnelId, retention),
            Poaching.Outcome.RETENTION_SET);
    }
}
