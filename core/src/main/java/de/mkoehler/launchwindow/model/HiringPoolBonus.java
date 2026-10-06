package de.mkoehler.launchwindow.model;

import java.util.Objects;

/**
 * A press story's lasting boost to one group's hiring pool ({@code design.md} &sect;2.2(c), user
 * decision 2026-10-06): extra candidates, some of them from the best quality band, for a number of
 * pool refreshes. Each refresh counts one down; at zero the bonus is gone.
 *
 * @param group              the group whose pool grows, by {@code PersonnelGroup} name
 * @param extra              extra candidates on top of the usual batch, standouts included
 * @param standout           how many of the extra candidates come from the best quality band
 * @param refreshesRemaining the pool refreshes still to come that it covers
 * @author Mario Koehler
 */
public record HiringPoolBonus(String group, int extra, int standout, int refreshesRemaining) {

    /**
     * Checks the group.
     *
     * @throws NullPointerException if {@code group} is {@code null}
     */
    public HiringPoolBonus {
        Objects.requireNonNull(group, "group must not be null");
    }
}
