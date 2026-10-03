package de.mkoehler.launchwindow.actions;

import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import de.mkoehler.launchwindow.persistence.ActionResult;
import de.mkoehler.launchwindow.persistence.PlayerAction;
import de.mkoehler.launchwindow.rules.OpenProgramOutcome;
import de.mkoehler.launchwindow.rules.ProgramOpening;

/**
 * Opens a payload program, paying its opening price ({@link ProgramOpening#open}).
 *
 * <p>Journaled as {@code "openProgram"}; see {@code design.md} &sect;5, M4.</p>
 *
 * @author Mario Koehler
 */
public class OpenProgramAction extends PlayerAction {

    private String programId;

    /**
     * No-argument constructor for Jackson and Kryo.
     */
    public OpenProgramAction() {
    }

    /**
     * Creates the action.
     *
     * @param programId the program
     */
    public OpenProgramAction(String programId) {
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
        return ActionResult.of(ProgramOpening.open(snapshot, programId, context.catalog()), OpenProgramOutcome.OPENED);
    }
}
