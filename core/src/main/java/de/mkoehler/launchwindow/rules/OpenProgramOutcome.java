package de.mkoehler.launchwindow.rules;

/**
 * <p>The outcome of {@link ProgramOpening#open}. A stale/already-open
 * program id or insufficient funds are both ordinary outcomes in an async
 * multiplayer game, not error conditions - see {@link ProgramOpening}'s own
 * class Javadoc.</p>
 *
 * @author Mario Koehler
 */
public enum OpenProgramOutcome {

    /**
     * The program was opened: its cost was deducted and every required
     * mission component not already owned was granted.
     */
    OPENED,

    /**
     * The given program id isn't in the content catalog.
     */
    UNKNOWN_PROGRAM,

    /**
     * This player already has the program open.
     */
    ALREADY_OPEN,

    /**
     * The player doesn't have enough funds to cover the program's
     * effective opening cost.
     */
    INSUFFICIENT_FUNDS,

    /**
     * One or more of the program's required goals hasn't been achieved or
     * cancelled yet - see {@code design.md} &sect;2.6(d).
     */
    GOALS_NOT_MET,

    /**
     * The player already has as many programs open as their Headquarters'
     * current level allows - see {@code baspm.md} &sect;10.9.
     */
    HEADQUARTERS_FULL,

    /**
     * The program belongs to another agency - see {@code design.md} &sect;5 (M3).
     */
    WRONG_AGENCY
}
