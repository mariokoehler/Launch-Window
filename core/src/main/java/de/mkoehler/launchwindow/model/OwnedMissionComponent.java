package de.mkoehler.launchwindow.model;

/**
 * <p>One player's ownership record for a mission component they've been
 * granted: its current reliability, and the floor it can never be pushed
 * back below.</p>
 *
 * <p>The floor is the transfer-inclusive starting value (baseline
 * reliability plus whatever one-time reliability-transfer bonus applied
 * when this component was first granted), not the component's own bare
 * baseline - see {@code design.md} &sect;2.4(c) for the open question this
 * settles (unconfirmed default) and why. A future mission-simulation
 * slice's post-mission reliability change must clamp against
 * {@link #getFloorReliability()}, not the component's own baseline.</p>
 *
 * <p>Per-player state, mutable - matching every other class in
 * {@code de.mkoehler.launchwindow.model} (see {@code design.md} &sect;3.4's
 * "state is mutable" decision), unlike the immutable content-definition
 * types in {@code de.mkoehler.launchwindow.content} this one is paired
 * with.</p>
 *
 * @author Mario Koehler
 */
public class OwnedMissionComponent {

    private double currentReliability;
    private double floorReliability;

    /**
     * No-argument constructor required for Jackson/Kryo deserialization.
     */
    public OwnedMissionComponent() {
    }

    /**
     * Creates a new ownership record.
     *
     * @param currentReliability the component's current reliability
     * @param floorReliability   the reliability it can never be pushed
     *                           back below
     */
    public OwnedMissionComponent(double currentReliability, double floorReliability) {
        this.currentReliability = currentReliability;
        this.floorReliability = floorReliability;
    }

    /**
     * Returns the component's current reliability.
     *
     * @return the current reliability
     */
    public double getCurrentReliability() {
        return currentReliability;
    }

    /**
     * Sets the component's current reliability.
     *
     * @param currentReliability the current reliability
     */
    public void setCurrentReliability(double currentReliability) {
        this.currentReliability = currentReliability;
    }

    /**
     * Returns the reliability this component can never be pushed back
     * below.
     *
     * @return the floor reliability
     */
    public double getFloorReliability() {
        return floorReliability;
    }

    /**
     * Sets the reliability this component can never be pushed back below.
     *
     * @param floorReliability the floor reliability
     */
    public void setFloorReliability(double floorReliability) {
        this.floorReliability = floorReliability;
    }
}
