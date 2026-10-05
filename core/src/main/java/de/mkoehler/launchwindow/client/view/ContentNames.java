package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentStrings;
import de.mkoehler.launchwindow.content.MissionConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Display text for content ids, from the game's pack ({@link ContentStrings}, gap G4). The client
 * installs the pack's text once per session ({@link #install}); every lookup falls back to a name made
 * from the id itself ({@code vostok-capsule} reads "Vostok Capsule") when the pack has no text for it,
 * so a pack without text, or a test that installs none, still reads sensibly.
 *
 * <p>One lookup serves every kind of id (category, program, component, configuration, goal), which
 * relies on ids being unique across kinds; {@code ContentStringsTest} checks that. A configuration's
 * name in the pack is relative to its program ("Orbital flight"); {@link #of} gives the full name
 * ("Vostok · Orbital flight") for screens that show it on its own, and {@link #configuration} the
 * relative one for screens that already show the program.</p>
 *
 * @author Mario Koehler
 */
public final class ContentNames {

    private static volatile Installed installed = new Installed(ContentStrings.EMPTY, Map.of(), Map.of(), Map.of(),
        Map.of());

    /**
     * Not instantiable; this class only holds the naming.
     */
    private ContentNames() {
    }

    /**
     * Installs a pack's text for every later lookup. {@link ContentStrings#EMPTY} goes back to names
     * made from ids.
     *
     * @param strings the pack's text
     * @param catalog the pack, for which program each configuration belongs to; may be {@code null}
     */
    public static void install(ContentStrings strings, ContentCatalog catalog) {
        Map<String, String> names = new HashMap<>();
        Map<String, String> descriptions = new HashMap<>();
        Map<String, String> notes = new HashMap<>();
        for (Map<String, ContentStrings.Text> kind : java.util.List.of(strings.categories(), strings.programs(),
                strings.components(), strings.goals())) {
            kind.forEach((id, text) -> put(id, text.name(), text.description(), text.historyNote(), names,
                descriptions, notes));
        }
        Map<String, String> relative = new HashMap<>();
        strings.configurations().forEach((id, text) -> {
            relative.put(id, text.name());
            put(id, text.name(), text.description(), text.historyNote(), names, descriptions, notes);
        });
        if (catalog != null) {
            for (MissionConfiguration configuration : catalog.missionConfigurations()) {
                String own = relative.get(configuration.id());
                if (own != null) {
                    names.put(configuration.id(), of(configuration.programId(), names) + " · " + own);
                }
            }
        }
        installed = new Installed(strings, names, descriptions, notes, relative);
    }

    private static void put(String id, String name, String description, String note, Map<String, String> names,
            Map<String, String> descriptions, Map<String, String> notes) {
        if (name != null && !name.isBlank()) {
            names.put(id, name);
        }
        if (description != null && !description.isBlank()) {
            descriptions.put(id, description);
        }
        if (note != null && !note.isBlank()) {
            notes.put(id, note);
        }
    }

    /**
     * Returns the display name for a content id: the pack's name, or one made from the id. A
     * configuration gets its full name, with its program in front.
     *
     * @param id the id
     * @return the name
     */
    public static String of(String id) {
        return of(id, installed.names());
    }

    private static String of(String id, Map<String, String> names) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String name = names.get(id);
        return name != null ? name : fromId(id);
    }

    /**
     * Returns a configuration's name relative to its program ("Orbital flight"), for screens that
     * already show the program, or {@code null} if the pack has none.
     *
     * @param configurationId the configuration id
     * @return the name, or {@code null}
     */
    public static String configuration(String configurationId) {
        return installed.relative().get(configurationId);
    }

    /**
     * Returns a content id's short description.
     *
     * @param id the id
     * @return the description, or {@code null} if the pack has none
     */
    public static String description(String id) {
        return installed.descriptions().get(id);
    }

    /**
     * Returns a content id's history note, for the history-note icon ({@code ui-spec.md} component 15).
     *
     * @param id the id
     * @return the note, or {@code null} if the pack has none
     */
    public static String historyNote(String id) {
        return installed.notes().get(id);
    }

    /**
     * Returns a step's display name within a configuration.
     *
     * @param configurationId the configuration, or {@code null}
     * @param stepId          the step id, such as {@code 08-retrofire}
     * @return the name
     */
    public static String step(String configurationId, String stepId) {
        ContentStrings.Text text = installed.strings().step(configurationId, stepId);
        if (text != null && text.name() != null && !text.name().isBlank()) {
            return text.name();
        }
        String words = fromId(ContentStrings.stepType(stepId));
        return words.isEmpty() ? words : words.charAt(0) + words.substring(1).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns a step's description within a configuration.
     *
     * @param configurationId the configuration, or {@code null}
     * @param stepId          the step id
     * @return the description, or {@code null} if the pack has none
     */
    public static String stepDescription(String configurationId, String stepId) {
        ContentStrings.Text text = installed.strings().step(configurationId, stepId);
        return text == null || text.description() == null || text.description().isBlank() ? null : text.description();
    }

    /**
     * Makes a readable name from a content id.
     *
     * @param id the id, words separated by hyphens
     * @return the words, each capitalized
     */
    static String fromId(String id) {
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

    private record Installed(ContentStrings strings, Map<String, String> names, Map<String, String> descriptions,
                             Map<String, String> notes, Map<String, String> relative) {
    }
}
