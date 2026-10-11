package de.mkoehler.launchwindow.rules;

/**
 * The result of applying for a contract ({@link Contracts#apply}).
 *
 * @author Mario Koehler
 */
public enum ContractOutcome {

    /** The application is in, sealed until the turn resolves. */
    APPLIED,

    /** The application was withdrawn. */
    WITHDRAWN,

    /** The agency has an active contract: one at a time. */
    CONTRACT_ACTIVE,

    /** The agency hasn't unlocked the listing's line, or the program named isn't one of the line's. */
    NOT_QUALIFIED,

    /** The line is unlocked, but no program the agency flies it with is open. */
    PROGRAM_NOT_OPEN,

    /** The listing isn't on the board. */
    UNKNOWN_LISTING
}
