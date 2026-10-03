package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.CloseProgramOutcome;
import de.mkoehler.launchwindow.rules.ProgramOpening;

/**
 * Closes a payload program - the inverse of {@link OpenProgramAction}, with no refund ({@link ProgramOpening#close}).
 *
 * <p>Journaled as {@code "closeProgram"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class CloseProgramAction extends PlayerAction {

    private String programId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public CloseProgramAction() {
    }

    /**
     * Creates the action.
     *
     * @param programId the program
     */
    public CloseProgramAction(String programId) {
        setProgramId(programId);
    }

    /**
     * Returns the program.
     *
     * @return the program
     */
    public String getProgramId() {
        return programId;
    }

    /**
     * Sets the program.
     *
     * @param programId the program
     */
    public void setProgramId(String programId) {
        this.programId = programId;
    }

    @Override
    public ActionResult apply(PlayerSnapshot snapshot, ActionContext context) {
        return ActionResult.of(ProgramOpening.close(snapshot, programId, context.catalog()), CloseProgramOutcome.CLOSED);
    }
}
