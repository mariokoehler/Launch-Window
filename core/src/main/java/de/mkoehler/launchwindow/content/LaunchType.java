package de.mkoehler.launchwindow.content;

/**
 * <p>Where a launch delivers its payload, and whether it carries crew: the four
 * destinations BASPM's {@code LaunchTypeT} names, each with a human-rated
 * variant ({@code baspm.md} &sect;10.9). A human-rated launch only accepts a
 * rocket whose skill is {@code HUMAN_RATED_ROCKETS}.</p>
 *
 * @author Mario Koehler
 */
public enum LaunchType {

    /** A suborbital hop. */
    SUBORBITAL(false),
    /** A crewed suborbital hop. */
    SUBORBITAL_HR(true),
    /** Low Earth orbit. */
    LEO(false),
    /** Low Earth orbit, crewed. */
    LEO_HR(true),
    /** Trans-lunar injection. */
    TLI(false),
    /** Trans-lunar injection, crewed. */
    TLI_HR(true),
    /** Trans-planetary injection. */
    TPI(false),
    /** Trans-planetary injection, crewed. */
    TPI_HR(true);

    private final boolean humanRated;

    LaunchType(boolean humanRated) {
        this.humanRated = humanRated;
    }

    /**
     * Returns whether this launch carries crew and so needs a human-rated rocket.
     *
     * @return {@code true} for the {@code _HR} variants
     */
    public boolean humanRated() {
        return humanRated;
    }
}
