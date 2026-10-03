package de.mkoehler.launchwindow.model;

/**
 * <p>One player's ownership record for a mission component they've been
 * granted: its current reliability, and the floor it can never be pushed
 * back below.</p>
 *
 * <p>The floor is the transfer-inclusive starting value (baseline
 * reliability plus whatever one-time reliability-transfer bonus applied
 * when this component was first granted), not the component's own bare
 * baseline - see {@code design.md} &sect;2.4(c) for why. A future
 * mission-simulation slice's post-mission reliability change must clamp
 * against {@link #getFloorReliability()}, not the component's own
 * baseline.</p>
 *
 * <p>Per-player state, mutable - matching every other class in
 * {@code de.mkoehler.launchwindow.model} (see {@code design.md} &sect;3.4's
 * "state is mutable" decision), unlike the immutable content-definition
 * types in {@code de.mkoehler.launchwindow.content} this one is paired
 * with.</p>
 *
 * <p>A component stays owned when the last program using it closes: it goes
 * inactive, keeping its reliability, and pays no upkeep until a program using it
 * opens again - BASPM's {@code IsActive} ({@code baspm.md} &sect;10.9).</p>
 *
 * @author Mario Koehler
 */
public class OwnedMissionComponent {

    private double currentReliability;
    private double floorReliability;
    private boolean active = true;

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

    /**
     * Returns whether the component is active: some open program (or, for a
     * rocket, its open rocket program) uses it.
     *
     * @return {@code true} while active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the component is active.
     *
     * @param active {@code true} while some open program uses it
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}