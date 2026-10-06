package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.PrPress;

import java.util.List;
import java.util.Objects;

/**
 * Reports one astronaut's season of PR duty ({@code design.md} &sect;2.2(c)): the prestige it
 * earned after diminishing returns and any press story, the story itself if the papers picked it
 * up, what else the story did, and how much rust their flying skills took.
 *
 * @param astronautId   the astronaut
 * @param name          their name, for the turn report
 * @param yield         what the tour earned before any press story, after diminishing returns
 * @param prestige      what it actually earned: the yield, plus or minus a press story's effect
 * @param pressStoryId  the press story, from {@code PrPress}, or {@code null}
 * @param rust          the points Piloting and Fitness each lost this season (0 in the first seasons)
 * @param pressEffects  the story's other effects as applied ({@link PressEffects#apply}); empty
 *                      without a story, and in reports from before effects existed
 * @author Mario Koehler
 */
public record PrDutyPerformed(String astronautId, String name, long yield, long prestige, String pressStoryId,
                              double rust, List<PrPress.Effect> pressEffects) implements ResolutionEvent {

    /**
     * Checks the ids; missing effects read as none.
     *
     * @throws NullPointerException if {@code astronautId} or {@code name} is {@code null}
     */
    public PrDutyPerformed {
        Objects.requireNonNull(astronautId, "astronautId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        pressEffects = pressEffects == null ? List.of() : List.copyOf(pressEffects);
    }

    /**
     * A season with no press effects besides prestige.
     *
     * @param astronautId  the astronaut
     * @param name         their name
     * @param yield        the yield
     * @param prestige     what it earned
     * @param pressStoryId the press story, or {@code null}
     * @param rust         the rust
     */
    public PrDutyPerformed(String astronautId, String name, long yield, long prestige, String pressStoryId,
                           double rust) {
        this(astronautId, name, yield, prestige, pressStoryId, rust, List.of());
    }
}
