package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.AgencyNames;
import de.mkoehler.launchwindow.model.Archetype;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * Deals agency names for the Join screen's Randomize button ({@code ui-spec.md} &sect;4.2) like cards
 * from a shuffled deck: one deck per archetype (and one for both, before a style is picked), so no
 * name comes up twice until every name in its deck has. A fresh shuffle never starts with the name
 * that came last.
 *
 * @author Mario Koehler
 */
public final class NameSuggestions {

    private final AgencyNames names;
    private final Random random;
    private final Map<Archetype, Deque<String>> decks = new HashMap<>();
    private String last;

    /**
     * Builds the dealer.
     *
     * @param names  the name pools
     * @param random the shuffle's randomness
     */
    public NameSuggestions(AgencyNames names, Random random) {
        this.names = Objects.requireNonNull(names, "names must not be null");
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * Returns how many names an archetype's deck holds.
     *
     * @param archetype the archetype, or {@code null} for both pools
     * @return the count
     */
    public int size(Archetype archetype) {
        return names.forArchetype(archetype).size();
    }

    /**
     * Deals the next name for an archetype.
     *
     * @param archetype the archetype, or {@code null} for both pools
     * @return the name, or empty if the pool has none
     */
    public Optional<String> next(Archetype archetype) {
        Deque<String> deck = decks.computeIfAbsent(archetype, kind -> new ArrayDeque<>());
        if (deck.isEmpty()) {
            List<String> shuffled = new ArrayList<>(names.forArchetype(archetype));
            if (shuffled.isEmpty()) {
                return Optional.empty();
            }
            Collections.shuffle(shuffled, random);
            if (shuffled.size() > 1 && shuffled.getFirst().equals(last)) {
                Collections.swap(shuffled, 0, 1 + random.nextInt(shuffled.size() - 1));
            }
            deck.addAll(shuffled);
        }
        last = deck.pop();
        return Optional.of(last);
    }
}
