package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests each pack's display text ({@link ContentStrings}, gap G4) against the pack itself, both ways,
 * and {@link ContentNames}' lookups over the early-era pack's.
 *
 * @author Mario Koehler
 */
class ContentStringsTest {

    private static final Path PACK_DIR = Path.of("..", "assets", "content", "early-era");
    private static final ContentCatalog EARLY_PACK = ContentPackLoader.load(PACK_DIR);
    private static final ContentStrings EARLY_STRINGS = ContentStrings.load(PACK_DIR);
    private static final Map<String, ContentCatalog> CATALOGS = new HashMap<>();
    private static final Map<String, ContentStrings> TEXTS = new HashMap<>();

    private static ContentCatalog catalog(String pack) {
        return CATALOGS.computeIfAbsent(pack, name -> ContentPackLoader.load(Path.of("..", "assets", "content", name)));
    }

    private static ContentStrings text(String pack) {
        return TEXTS.computeIfAbsent(pack, name -> ContentStrings.load(Path.of("..", "assets", "content", name)));
    }

    /** Headings and list rows. */
    private static final int MAX_NAME = 40;
    /** Two or three lines under a heading. */
    private static final int MAX_DESCRIPTION = 260;
    /** What the history-note hover card holds comfortably. */
    private static final int MAX_NOTE = 460;

    @AfterEach
    void forgetInstalledText() {
        ContentNames.install(ContentStrings.EMPTY, null);
    }

    /**
     * The display text is never part of the frozen, hashed pack: a text fix must not change a running
     * game's pack hash.
     */
    @Test
    void theTextIsNotPartOfTheFrozenPack() {
        assertFalse(ContentPackLoader.FILES.contains(ContentStrings.FILE));
    }

    /**
     * Every category, program, component, configuration and goal has a name, and every step of every
     * configuration resolves to a name.
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void everythingInThePackHasText(String packName) {
        ContentCatalog pack = catalog(packName);
        ContentStrings strings = text(packName);
        pack.categories().forEach(c -> assertNamed(strings.categories().get(c.id()), "category " + c.id()));
        pack.programs().forEach(p -> assertNamed(strings.programs().get(p.id()), "program " + p.id()));
        for (MissionComponent component : pack.missionComponents()) {
            assertNamed(strings.components().get(component.id()), "component " + component.id());
        }
        for (MissionConfiguration configuration : pack.missionConfigurations()) {
            ContentStrings.ConfigurationText text = strings.configurations().get(configuration.id());
            assertNotNull(text, "configuration " + configuration.id());
            assertTrue(text.name() != null && !text.name().isBlank(), "configuration " + configuration.id());
            for (MissionSeason season : configuration.seasons()) {
                for (MissionStep step : season.steps()) {
                    assertNamed(strings.step(configuration.id(), step.id()),
                        "step " + step.id() + " of " + configuration.id());
                }
            }
        }
        for (String goalId : goalIds(pack)) {
            assertNamed(strings.goals().get(goalId), "goal " + goalId);
        }
    }

    /**
     * Every entry in the text belongs to something in the pack, so a mistyped key can't hide.
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void theTextHasNoStrayEntries(String packName) {
        ContentCatalog pack = catalog(packName);
        ContentStrings strings = text(packName);
        Set<String> categories = new HashSet<>();
        pack.categories().forEach(c -> categories.add(c.id()));
        Set<String> programs = new HashSet<>();
        pack.programs().forEach(p -> programs.add(p.id()));
        Set<String> configurations = new HashSet<>();
        Set<String> stepTypes = new HashSet<>();
        for (MissionConfiguration configuration : pack.missionConfigurations()) {
            configurations.add(configuration.id());
            Set<String> stepIds = new HashSet<>();
            configuration.seasons().forEach(season -> season.steps().forEach(step -> {
                stepIds.add(step.id());
                String type = ContentStrings.stepType(step.id());
                stepTypes.add(type);
                String[] craft = ContentStrings.craftStep(type);
                if (craft != null) {
                    stepTypes.add(craft[1]);
                }
            }));
            ContentStrings.ConfigurationText text = strings.configurations().get(configuration.id());
            if (text != null) {
                for (String override : text.steps().keySet()) {
                    assertTrue(stepIds.contains(override), configuration.id() + " has no step " + override);
                }
            }
        }
        assertKnown(strings.categories().keySet(), categories, "category");
        assertKnown(strings.programs().keySet(), programs, "program");
        assertKnown(strings.components().keySet(), componentIds(pack), "component");
        assertKnown(strings.configurations().keySet(), configurations, "configuration");
        assertKnown(strings.steps().keySet(), stepTypes, "step type");
        assertKnown(strings.goals().keySet(), goalIds(pack), "goal");
    }

    /**
     * Ids are unique across kinds, which {@link ContentNames}' single lookup relies on.
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void idsAreUniqueAcrossKinds(String packName) {
        ContentCatalog pack = catalog(packName);
        ContentStrings strings = text(packName);
        Map<String, String> seen = new HashMap<>();
        Map<String, Set<String>> kinds = Map.of("category", strings.categories().keySet(), "program",
            strings.programs().keySet(), "component", strings.components().keySet(), "configuration",
            strings.configurations().keySet(), "goal", strings.goals().keySet());
        kinds.forEach((kind, ids) -> {
            for (String id : ids) {
                String other = seen.put(id, kind);
                assertNull(other, id + " is both a " + kind + " and a " + other);
            }
        });
    }

    /**
     * Every text fits its place on screen, and every letter is one the client's fonts can draw.
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void everyTextFitsAndCanBeDrawn(String packName) {
        ContentCatalog pack = catalog(packName);
        ContentStrings strings = text(packName);
        for (ContentStrings.Text text : allTexts(strings)) {
            check(text.name(), MAX_NAME);
            check(text.description(), MAX_DESCRIPTION);
            check(text.historyNote(), MAX_NOTE);
        }
    }

    /**
     * The lookups: a configuration's full and relative names, a joint flight's craft-specific steps,
     * a step override, and the fallback to a name made from the id.
     */
    @Test
    void contentNamesLooksUpThePacksText() {
        ContentNames.install(EARLY_STRINGS, EARLY_PACK);
        assertEquals("Vostok capsule", ContentNames.of("vostok-capsule"));
        assertEquals("Vostok · Orbital flight", ContentNames.of("vostok-orbital"));
        assertEquals("Orbital flight", ContentNames.configuration("vostok-orbital"));
        assertEquals("Retrofire", ContentNames.step("vostok-orbital", "08-retrofire"));
        assertEquals("Craft A: Deployment", ContentNames.step("vostok-joint-orbital", "05-spc-a-deployment"));
        assertEquals("Craft B: Countdown", ContentNames.step("vostok-joint-orbital", "08-countdown"));
        assertNotNull(ContentNames.historyNote("vostok"));
        assertNotNull(ContentNames.stepDescription("vostok-orbital", "10-reentry"));
        assertEquals("No Such Thing", ContentNames.of("no-such-thing"));

        ContentNames.install(ContentStrings.EMPTY, null);
        assertEquals("Vostok Capsule", ContentNames.of("vostok-capsule"));
        assertEquals("Launch preparations", ContentNames.step(null, "01-launch-preparations"));
        assertNull(ContentNames.description("vostok"));
    }

    private static void assertNamed(ContentStrings.Text text, String what) {
        assertNotNull(text, what + " has no text");
        assertTrue(text.name() != null && !text.name().isBlank(), what + " has no name");
    }

    private static void assertKnown(Set<String> keys, Set<String> known, String kind) {
        for (String key : keys) {
            assertTrue(known.contains(key), "the text names an unknown " + kind + ": " + key);
        }
    }

    private static void check(String text, int max) {
        if (text == null) {
            return;
        }
        assertTrue(text.length() <= max, text.length() + " characters, more than " + max + ": " + text);
        for (char c : text.replace(" ", "").toCharArray()) {
            assertTrue(Theme.CHARACTERS.indexOf(c) >= 0, "'" + c + "' can't be drawn, in: " + text);
        }
    }

    private static Set<String> componentIds(ContentCatalog pack) {
        Set<String> ids = new HashSet<>();
        pack.missionComponents().forEach(c -> ids.add(c.id()));
        return ids;
    }

    private static Set<String> goalIds(ContentCatalog pack) {
        Set<String> ids = new HashSet<>();
        pack.goals().forEach(g -> ids.add(g.id()));
        return ids;
    }

    private static List<ContentStrings.Text> allTexts(ContentStrings strings) {
        List<ContentStrings.Text> texts = new java.util.ArrayList<>();
        texts.addAll(strings.categories().values());
        texts.addAll(strings.programs().values());
        texts.addAll(strings.components().values());
        texts.addAll(strings.steps().values());
        texts.addAll(strings.goals().values());
        for (ContentStrings.ConfigurationText configuration : strings.configurations().values()) {
            texts.add(new ContentStrings.Text(configuration.name(), configuration.description(),
                configuration.historyNote()));
            texts.addAll(configuration.steps().values());
        }
        return texts;
    }
}
