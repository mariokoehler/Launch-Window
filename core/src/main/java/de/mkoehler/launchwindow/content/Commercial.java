package de.mkoehler.launchwindow.content;

import java.util.List;
import java.util.Objects;

/**
 * <p>A program's commercial block ({@code design.md} &sect;2.11): the configurations an agency must
 * each have flown successfully to unlock the program's category as a contract line, and the
 * configuration its contracts fly, always one of them. Read from the pack's
 * {@code commercial.json}; a program without one has no contracts.</p>
 *
 * @param programId                the program
 * @param unlockConfigurationIds   the unlock set, in the program's order
 * @param contractConfigurationId  the configuration contracts fly; one of the unlock set
 * @author Mario Koehler
 */
public record Commercial(String programId, List<String> unlockConfigurationIds, String contractConfigurationId) {

    /**
     * Checks the fields.
     *
     * @throws NullPointerException     if a field is {@code null}
     * @throws IllegalArgumentException if the unlock set is empty or leaves out the contract's
     *                                  configuration
     */
    public Commercial {
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(contractConfigurationId, "contractConfigurationId must not be null");
        unlockConfigurationIds = List.copyOf(Objects.requireNonNull(unlockConfigurationIds,
            "unlockConfigurationIds must not be null"));
        if (unlockConfigurationIds.isEmpty()) {
            throw new IllegalArgumentException("program " + programId + " has an empty unlock set");
        }
        if (!unlockConfigurationIds.contains(contractConfigurationId)) {
            throw new IllegalArgumentException("program " + programId + "'s contract configuration "
                + contractConfigurationId + " isn't in its unlock set");
        }
    }
}
