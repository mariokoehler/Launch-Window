package de.mkoehler.launchwindow.rules;

import java.util.Objects;

/**
 * Reports one astronaut's season of PR duty ({@code design.md} &sect;2.2(c)): the prestige it
 * earned after diminishing returns and any press story, the story itself if the papers picked it
 * up, and how much rust their flying skills took.
 *
 * @param astronautId   the astronaut
 * @param name          their name, for the turn report
 * @param yield         what the tour earned before any press story, after diminishing returns
 * @param prestige      what it actually earned: the yield, plus or minus a press story's effect
 * @param pressStoryId  the press story, from {@code PrPress}, or {@code null}
 * @param rust          the points Piloting and Fitness each lost this season (0 in the first seasons)
 * @author Mario Koehler
 */
public record PrDutyPerformed(String astronautId, String name, long yield, long prestige, String pressStoryId,
                              double rust) implements ResolutionEvent {

    /**
     * Checks the ids.
     *
     * @throws NullPointerException if {@code astronautId} or {@code name} is {@code null}
     */
    public PrDutyPerformed {
        Objects.requireNonNull(astronautId, "astronautId must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
