package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.FrontPageReactions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FillerDeckTest {

    private static final List<FrontPageReactions.Filler> POOL = IntStream.range(0, 20)
        .mapToObj(i -> new FrontPageReactions.Filler("f" + i, "Headline " + i, List.of("Text " + i)))
        .toList();

    private static List<String> ids(List<FrontPageReactions.Filler> fillers) {
        return fillers.stream().map(FrontPageReactions.Filler::id).toList();
    }

    @Test
    void theItemsNotYetShownComeFirst() {
        Set<String> seen = new HashSet<>(ids(POOL).subList(0, 18));
        List<String> order = ids(FillerDeck.order(POOL, "page-1", null, seen));
        assertEquals(20, order.size());
        assertEquals(Set.of("f18", "f19"), new HashSet<>(order.subList(0, 2)));
    }

    @Test
    void aPageThatOpenedBeforeShowsTheSameItems() {
        List<String> order = ids(FillerDeck.order(POOL, "page-1", List.of("f7", "f3", "gone"), Set.of()));
        assertEquals(List.of("f7", "f3"), order);
        assertEquals(List.of(), ids(FillerDeck.order(POOL, "page-1", List.of(), Set.of())));
    }

    @Test
    void noItemRepeatsUntilEveryOneWasShown() {
        Set<String> seen = new LinkedHashSet<>();
        List<String> printed = new ArrayList<>();
        for (int page = 0; page < 13; page++) {
            List<String> picked = ids(FillerDeck.order(POOL, "page-" + page, null, seen)).subList(0, 3);
            printed.addAll(picked);
            seen = FillerDeck.afterShowing(POOL, seen, picked);
        }
        // Pages of three: the first 20 items are all different, and so are the next 19 (the deck ran out mid-page).
        assertEquals(20, new HashSet<>(printed.subList(0, 20)).size());
        assertEquals(19, new HashSet<>(printed.subList(20, 39)).size());
    }

    @Test
    void theDeckStartsOverWithTheRepeats() {
        Set<String> seen = new HashSet<>(ids(POOL).subList(0, 19));
        Set<String> next = FillerDeck.afterShowing(POOL, seen, List.of("f19", "f4"));
        assertEquals(Set.of("f4"), next);
        assertTrue(FillerDeck.afterShowing(POOL, new HashSet<>(ids(POOL).subList(0, 19)), List.of("f19")).isEmpty());
    }
}
