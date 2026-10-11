package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * A program's unlock set is complete ({@code design.md} &sect;2.11): its category is a contract
 * line for the agency for good, and it qualifies for the line's listings while the program is open.
 *
 * @param lineId    the line
 * @param programId the program whose set was completed
 * @param newLine   whether the line itself is new to the agency, not only the program
 * @author Mario Koehler
 */
public record ContractLineUnlocked(String lineId, String programId, boolean newLine) implements ResolutionEvent {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if an id is {@code null}
     */
    public ContractLineUnlocked {
        Objects.requireNonNull(lineId, "lineId must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
    }
}
