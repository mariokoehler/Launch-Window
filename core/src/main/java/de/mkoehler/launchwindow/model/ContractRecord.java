package de.mkoehler.launchwindow.model;

/**
 * An agency's record in one contract line ({@code design.md} &sect;2.11): what it flew for clients.
 * The points it adds to a score are the rules' ({@code rules.Contracts#recordPoints}).
 *
 * @param flown  contracts flown successfully
 * @param failed contracts whose flight failed
 * @param lapsed contracts not launched in their window
 * @author Mario Koehler
 */
public record ContractRecord(int flown, int failed, int lapsed) {

    /** No contracts yet. */
    public static final ContractRecord NONE = new ContractRecord(0, 0, 0);

    /**
     * Returns this record with one more contract flown.
     *
     * @return the record
     */
    public ContractRecord withFlown() {
        return new ContractRecord(flown + 1, failed, lapsed);
    }

    /**
     * Returns this record with one more failed.
     *
     * @return the record
     */
    public ContractRecord withFailed() {
        return new ContractRecord(flown, failed + 1, lapsed);
    }

    /**
     * Returns this record with one more lapsed.
     *
     * @return the record
     */
    public ContractRecord withLapsed() {
        return new ContractRecord(flown, failed, lapsed + 1);
    }
}
