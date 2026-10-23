package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.FrontPageReactions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * The order a page offers its filler items in ({@code ui-spec.md} &sect;4.18.1): none repeats until every one has
 * been printed, and a page shows the same items every time it opens.
 */
public final class FillerDeck {

    private FillerDeck() {
    }

    /**
     * Returns the items a page offers, in order: the ones it showed before if it has opened before; otherwise the
     * ones not yet shown, then the rest, each part in an order drawn from the page's id.
     *
     * @param pool   every filler item
     * @param pageId the page
     * @param shown  what the page showed before, or {@code null} if it hasn't opened before
     * @param seen   the items shown since the deck last ran out
     * @return the items to offer, in order
     */
    public static List<FrontPageReactions.Filler> order(List<FrontPageReactions.Filler> pool, String pageId,
                                                       List<String> shown, Set<String> seen) {
        if (shown != null) {
            Map<String, FrontPageReactions.Filler> byId = new HashMap<>();
            pool.forEach(filler -> byId.put(filler.id(), filler));
            List<FrontPageReactions.Filler> order = new ArrayList<>();
            for (String id : shown) {
                if (byId.containsKey(id)) {
                    order.add(byId.get(id));
                }
            }
            return order;
        }
        List<FrontPageReactions.Filler> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(pageId.hashCode()));
        List<FrontPageReactions.Filler> order = new ArrayList<>();
        shuffled.stream().filter(filler -> !seen.contains(filler.id())).forEach(order::add);
        shuffled.stream().filter(filler -> seen.contains(filler.id())).forEach(order::add);
        return order;
    }

    /**
     * Returns the items shown since the deck last ran out, once a page has shown its picks. When the picks use up
     * the items not yet shown, the deck starts over, holding only the picks that were already repeats.
     *
     * @param pool   every filler item
     * @param seen   the items shown before this page
     * @param picked what this page shows
     * @return the items shown since the deck last ran out
     */
    public static Set<String> afterShowing(List<FrontPageReactions.Filler> pool, Set<String> seen,
                                           List<String> picked) {
        Set<String> next = new LinkedHashSet<>(seen);
        next.addAll(picked);
        if (pool.stream().allMatch(filler -> next.contains(filler.id()))) {
            next.clear();
            picked.stream().filter(seen::contains).forEach(next::add);
        }
        return next;
    }
}
