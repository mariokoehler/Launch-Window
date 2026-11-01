package de.mkoehler.launchwindow.client.view;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.PlayerSnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What analysts say about the other side's hardware ({@code design.md} &sect;2.9, {@code ui-spec.md}
 * &sect;4.15): the words from {@value #FILE}, outside the pack, and which of them a player has earned.
 * Hardware of the player's own side is always known, so a game with nobody on the other side needs no
 * special case here.
 *
 * @param items the words by program or rocket id
 * @author Mario Koehler
 */
@JsonIgnoreProperties({"_comment"})
public record ForeignHardware(Map<String, Item> items) {

    /** The words' file, under the assets' {@code content/} folder. */
    public static final String FILE = "foreign-hardware.json";
    /** The longest role that fits a dossier row on one line. */
    public static final int MAX_ROLE = 46;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static volatile ForeignHardware standard;

    /**
     * Copies the map; a missing one reads as empty.
     */
    public ForeignHardware {
        items = items == null ? Map.of() : Map.copyOf(items);
    }

    /**
     * One program's or rocket's words.
     *
     * @param role        what it does, in a short plain line
     * @param counterpart the nearest equivalent on the other side, or {@code null}
     */
    public record Item(String role, String counterpart) {
    }

    /**
     * How a dossier or the turn report shows a piece of hardware.
     *
     * @param id       the dossier's id, a program, rocket or component
     * @param name     its name
     * @param fileId   the program or rocket whose file it opens, or {@code null} with none to open
     * @param known    whether the player has its words
     * @param foreign  whether it is the other side's
     * @param role     what it does, or {@code null} when it isn't known
     * @param linkLead the second line's words before its name, or {@code null} with no second line
     * @param linkName the second line's name
     */
    public record Row(String id, String name, String fileId, boolean known, boolean foreign, String role,
                      String linkLead, String linkName) {

        /**
         * Returns whether the row opens a file.
         *
         * @return true when known and foreign
         */
        public boolean opens() {
            return known && foreign && fileId != null;
        }
    }

    /**
     * Returns the words every client reads, from the classpath.
     *
     * @return the words
     */
    public static ForeignHardware standard() {
        ForeignHardware loaded = standard;
        if (loaded == null) {
            try (InputStream in = ForeignHardware.class.getClassLoader().getResourceAsStream("content/" + FILE)) {
                loaded = in == null ? new ForeignHardware(Map.of()) : MAPPER.readValue(in, ForeignHardware.class);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            standard = loaded;
        }
        return loaded;
    }

    /**
     * Returns which side an item belongs to.
     *
     * @param id      a program or rocket id
     * @param catalog the content pack
     * @return its archetype, or {@code null} for an unknown id or one open to both
     */
    public static Archetype side(String id, ContentCatalog catalog) {
        return catalog.program(id).map(Program::agency)
            .orElseGet(() -> catalog.missionComponent(id).map(MissionComponent::agency).orElse(null));
    }

    /**
     * Returns whether the player knows an item.
     *
     * @param id      a program or rocket id
     * @param state   the player's state
     * @param catalog the content pack
     * @param decided whether the race is decided, which opens everything
     * @return true for their own side's, for what they learned, and for everything once decided
     */
    public static boolean known(String id, PlayerSnapshot state, ContentCatalog catalog, boolean decided) {
        Archetype side = side(id, catalog);
        return decided || side == null || side == state.getArchetype()
            || state.getIntelligence().getForeignHardware().containsKey(id);
    }

    /**
     * Returns whether the player has the words for a dossier's id, and how to show them. A rocket and
     * a program speak for themselves. Any other component speaks through a program that needs it, a
     * known one first: as that program, if it is the program's first component (the craft itself), or
     * as a part of it.
     *
     * @param id      a program, rocket or component id
     * @param state   the player's state
     * @param catalog the content pack
     * @param decided whether the race is decided
     * @return the row
     */
    public Row row(String id, PlayerSnapshot state, ContentCatalog catalog, boolean decided) {
        Objects.requireNonNull(id, "id must not be null");
        String name = ContentNames.of(id);
        String item = id;
        boolean part = false;
        if (catalog.program(id).isEmpty() && catalog.missionComponent(id).filter(c -> c.rocket() != null).isEmpty()) {
            List<Program> programs = catalog.programs().stream()
                .filter(program -> program.requiredMissionComponentIds().contains(id)).toList();
            if (programs.isEmpty()) {
                return new Row(id, name, null, false, false, null, null, null);
            }
            Program program = programs.stream().filter(each -> known(each.id(), state, catalog, decided)).findFirst()
                .orElse(programs.getFirst());
            item = program.id();
            part = !program.requiredMissionComponentIds().getFirst().equals(id);
        }
        Item words = items.get(item);
        Archetype side = side(item, catalog);
        boolean foreign = side != null && side != state.getArchetype();
        if (!known(item, state, catalog, decided) || words == null) {
            return new Row(id, name, item, false, foreign, null, null, null);
        }
        if (!foreign) {
            return new Row(id, name, item, true, false, words.role(), null, null);
        }
        if (part) {
            return new Row(id, name, item, true, true, words.role(), "part of their ", ContentNames.of(item));
        }
        return words.counterpart() == null ? new Row(id, name, item, true, true, words.role(), null, null)
            : new Row(id, name, item, true, true, words.role(), "their counterpart to your ",
                ContentNames.of(words.counterpart()));
    }
}
