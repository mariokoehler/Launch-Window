package de.mkoehler.launchwindow.client.view;

import java.util.Locale;

/**
 * Display names for content ids. <b>A stopgap:</b> content packs carry ids only until their display
 * text exists (gap G4, {@code ui-spec.md} &sect;6), so for now a name is made from the id itself:
 * {@code vostok-capsule} reads "Vostok Capsule". Replace with the pack's strings once G4 lands.
 *
 * @author Mario Koehler
 */
public final class ContentNames {

    /**
     * Not instantiable; this class only holds the naming.
     */
    private ContentNames() {
    }

    /**
     * Makes a readable name from a content id.
     *
     * @param id the id, words separated by hyphens
     * @return the words, each capitalized
     */
    public static String of(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        StringBuilder name = new StringBuilder();
        for (String word : id.split("-")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return name.toString();
    }
}
