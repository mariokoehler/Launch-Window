package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.Intelligence;

/**
 * Queues an intelligence operation against a rival for the resolution, using one action: an agency
 * assessment of one area, or a personnel file ({@link Intelligence#queue}, {@code design.md} &sect;2.9).
 *
 * @author Mario Koehler
 */
public class QueueIntelOperationAction extends PlayerAction {

    private IntelOperation.Kind kind;
    private String targetId;
    private IntelArea area;
    private String group;
    private String personId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public QueueIntelOperationAction() {
    }

    /**
     * An agency assessment.
     *
     * @param targetId the rival's player id
     * @param area     the area
     * @return the action
     */
    public static QueueIntelOperationAction assessment(String targetId, IntelArea area) {
        QueueIntelOperationAction action = new QueueIntelOperationAction();
        action.setKind(IntelOperation.Kind.ASSESSMENT);
        action.setTargetId(targetId);
        action.setArea(area);
        return action;
    }

    /**
     * A personnel file.
     *
     * @param targetId the rival's player id
     * @param group    the group, by {@code PersonnelGroup} name
     * @param personId a person named in an earlier file, or {@code null} for the group's most skilled
     *                 without one
     * @return the action
     */
    public static QueueIntelOperationAction personnelFile(String targetId, String group, String personId) {
        QueueIntelOperationAction action = new QueueIntelOperationAction();
        action.setKind(IntelOperation.Kind.PERSONNEL_FILE);
        action.setTargetId(targetId);
        action.setGroup(group);
        action.setPersonId(personId);
        return action;
    }

    /**
     * Returns the operation's kind.
     *
     * @return the kind
     */
    public IntelOperation.Kind getKind() {
        return kind;
    }

    /**
     * Sets the operation's kind.
     *
     * @param kind the kind
     */
    public void setKind(IntelOperation.Kind kind) {
        this.kind = kind;
    }

    /**
     * Returns the rival's player id.
     *
     * @return the id
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Sets the rival's player id.
     *
     * @param targetId the id
     */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Returns an assessment's area.
     *
     * @return the area, or {@code null}
     */
    public IntelArea getArea() {
        return area;
    }

    /**
     * Sets an assessment's area.
     *
     * @param area the area
     */
    public void setArea(IntelArea area) {
        this.area = area;
    }

    /**
     * Returns a personnel file's group.
     *
     * @return the group, or {@code null}
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets a personnel file's group.
     *
     * @param group the group, by {@code PersonnelGroup} name
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Returns a personnel file's named person.
     *
     * @return the person's id, or {@code null}
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets a personnel file's named person.
     *
     * @param personId the person's id, or {@code null}
     */
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(Intelligence.queue(snapshot, kind, targetId, area, group, personId),
            Intelligence.Outcome.QUEUED);
    }
}
