package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * An agency's active contract ({@code design.md} &sect;2.11): awarded, and not yet flown to its end.
 * The fee is fixed at the award; the first launch of the configuration flies it.
 *
 * @param listingId       the listing it was awarded from
 * @param lineId          the line
 * @param clientId        the client, as this agency's side sees it, or {@code null}
 * @param programId       the program it flies on
 * @param configurationId the configuration it flies
 * @param tier            the tier won, 1 to 3
 * @param fee             what the client pays on success
 * @param awardedTurn     the turn it was awarded at
 * @param launchByTurn    the last turn it may launch in; it lapses at that turn's resolution if not
 * @param missionId       the mission flying it, or {@code null} before the launch
 * @author Mario Koehler
 */
public record ActiveContract(String listingId, String lineId, String clientId, String programId,
                             String configurationId, int tier, long fee, int awardedTurn, int launchByTurn,
                             String missionId) {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if an id but the client's or the mission's is {@code null}
     */
    public ActiveContract {
        Objects.requireNonNull(listingId, "listingId must not be null");
        Objects.requireNonNull(lineId, "lineId must not be null");
        Objects.requireNonNull(programId, "programId must not be null");
        Objects.requireNonNull(configurationId, "configurationId must not be null");
    }

    /**
     * Returns this contract flown by a mission, or back on the ground ({@code null}).
     *
     * @param mission the mission, or {@code null}
     * @return the contract
     */
    public ActiveContract withMission(String mission) {
        return new ActiveContract(listingId, lineId, clientId, programId, configurationId, tier, fee, awardedTurn,
            launchByTurn, mission);
    }
}
