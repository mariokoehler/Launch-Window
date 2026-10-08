package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.PersonnelGroup;
import de.mkoehler.launchwindow.rules.Poaching;

/**
 * Sets the agency's standing order against poaching for one group: rival offers up to a raise are
 * matched ({@link Poaching}, {@code design.md} &sect;2.2(d)).
 *
 * @author Mario Koehler
 */
public class SetRetentionDefaultAction extends PlayerAction {

    private PersonnelGroup group;
    private int matchPercent;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public SetRetentionDefaultAction() {
    }

    /**
     * Creates the action.
     *
     * @param group        the group
     * @param matchPercent the highest raise matched, 0 for never
     */
    public SetRetentionDefaultAction(PersonnelGroup group, int matchPercent) {
        setGroup(group);
        setMatchPercent(matchPercent);
    }

    /**
     * Returns the group.
     *
     * @return the group
     */
    public PersonnelGroup getGroup() {
        return group;
    }

    /**
     * Sets the group.
     *
     * @param group the group
     */
    public void setGroup(PersonnelGroup group) {
        this.group = group;
    }

    /**
     * Returns the highest raise matched.
     *
     * @return the percentage, 0 for never
     */
    public int getMatchPercent() {
        return matchPercent;
    }

    /**
     * Sets the highest raise matched.
     *
     * @param matchPercent the percentage, 0 for never
     */
    public void setMatchPercent(int matchPercent) {
        this.matchPercent = matchPercent;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Poaching.setDefault(snapshot, group, matchPercent), Poaching.Outcome.RETENTION_SET);
    }
}
