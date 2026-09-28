package de.mkoehler.launchwindow.persistence;

import de.mkoehler.launchwindow.model.PlayerSnapshot;

/**
 * <p>The on-disk envelope for one resolved turn: the resulting
 * {@link PlayerSnapshot} plus the journal file name of the last action
 * applied to produce it (or {@code null} if no actions were pending that
 * turn). Recording the cutoff in the same file as the snapshot, rather than
 * separately, means both are saved by the one atomic write - a
 * crashed-and-retried resolution can then archive exactly the actions that
 * were actually applied, never a later action submitted during the crash
 * window (see {@link PlayerStateService#resolveTurn(long)}).</p>
 *
 * @author Mario Koehler
 */
public class ResolvedSnapshot {

    private PlayerSnapshot snapshot;
    private String lastAppliedActionFile;

    /**
     * No-argument constructor required for Jackson deserialization.
     */
    public ResolvedSnapshot() {
    }

    /**
     * Creates a new envelope.
     *
     * @param snapshot              the resolved snapshot
     * @param lastAppliedActionFile the file name of the last action applied
     *                              to produce it, or {@code null} if none
     *                              were pending
     */
    public ResolvedSnapshot(PlayerSnapshot snapshot, String lastAppliedActionFile) {
        this.snapshot = snapshot;
        this.lastAppliedActionFile = lastAppliedActionFile;
    }

    /**
     * Returns the resolved snapshot.
     *
     * @return the snapshot
     */
    public PlayerSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Sets the resolved snapshot.
     *
     * @param snapshot the snapshot
     */
    public void setSnapshot(PlayerSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * Returns the file name of the last action applied to produce this
     * snapshot, or {@code null} if none were pending.
     *
     * @return the last applied action's file name, or {@code null}
     */
    public String getLastAppliedActionFile() {
        return lastAppliedActionFile;
    }

    /**
     * Sets the file name of the last action applied to produce this
     * snapshot.
     *
     * @param lastAppliedActionFile the last applied action's file name, or
     *                              {@code null}
     */
    public void setLastAppliedActionFile(String lastAppliedActionFile) {
        this.lastAppliedActionFile = lastAppliedActionFile;
    }
}
