package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.HireOutcome;
import de.mkoehler.launchwindow.rules.HiringPool;
import de.mkoehler.launchwindow.rules.PersonnelGroup;

import java.util.SplittableRandom;

/**
 * Hires a candidate from the current pool, capped by the group's building ({@link HiringPool#hire}). Uses the action's seed for the new hire's first training skill.
 *
 * <p>Journaled as {@code "hire"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class HireAction extends PlayerAction {

    private PersonnelGroup group;
    private String candidateId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public HireAction() {
    }

    /**
     * Creates the action.
     *
     * @param group the group
     * @param candidateId the candidate
     */
    public HireAction(PersonnelGroup group, String candidateId) {
        setGroup(group);
        setCandidateId(candidateId);
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
     * Returns the candidate.
     *
     * @return the candidate
     */
    public String getCandidateId() {
        return candidateId;
    }

    /**
     * Sets the candidate.
     *
     * @param candidateId the candidate
     */
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(HiringPool.hire(snapshot, group, candidateId, new SplittableRandom(getSeed()),
            context.catalog().buildings()), HireOutcome.HIRED);
    }
}
