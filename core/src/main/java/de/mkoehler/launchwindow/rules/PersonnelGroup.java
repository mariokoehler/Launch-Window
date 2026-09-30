package de.mkoehler.launchwindow.rules;

/**
 * <p>Which of a {@link de.mkoehler.launchwindow.model.PlayerSnapshot}'s three
 * personnel rosters a {@link ResolutionEvent} concerns - a rules-engine-only
 * label, distinct from any single {@code Personnel} subclass, so an event
 * can name its roster without depending on the model classes themselves.</p>
 *
 * @author Mario Koehler
 */
public enum PersonnelGroup {

    /**
     * The astronaut/cosmonaut roster.
     */
    ASTRONAUT,

    /**
     * The SET (Scientist/Engineer/Technician) roster.
     */
    SET,

    /**
     * The Mission Control roster.
     */
    MISSION_CONTROL
}
