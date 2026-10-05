package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.client.view.ContentNames;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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
 * Tests the early-era pack's display text ({@link ContentStrings}, gap G4) against the pack itself,
 * both ways, and {@link ContentNames}' lookups over it.
 *
 * @author Mario Koehler
 */
class ContentStringsTest {

    private static final Path PACK_DIR = Path.of("..", "assets", "content", "early-era");
    private static final ContentCatalog PACK = ContentPackLoader.load(PACK_DIR);
    private static final ContentStrings STRINGS = ContentStrings.load(PACK_DIR);

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
    @Test
    void everythingInThePackHasText() {
        PACK.categories().forEach(c -> assertNamed(STRINGS.categories().get(c.id()), "category " + c.id()));
        PACK.programs().forEach(p -> assertNamed(STRINGS.programs().get(p.id()), "program " + p.id()));
        for (MissionComponent component : PACK.missionComponents()) {
            assertNamed(STRINGS.components().get(component.id()), "component " + component.id());
        }
        for (MissionConfiguration configuration : PACK.missionConfigurations()) {
            ContentStrings.ConfigurationText text = STRINGS.configurations().get(configuration.id());
            assertNotNull(text, "configuration " + configuration.id());
            assertTrue(text.name() != null && !text.name().isBlank(), "configuration " + configuration.id());
            for (MissionSeason season : configuration.seasons()) {
                for (MissionStep step : season.steps()) {
                    assertNamed(STRINGS.step(configuration.id(), step.id()),
                        "step " + step.id() + " of " + configuration.id());
                }
            }
        }
        for (String goalId : goalIds()) {
            assertNamed(STRINGS.goals().get(goalId), "goal " + goalId);
        }
    }

    /**
     * Every entry in the text belongs to something in the pack, so a mistyped key can't hide.
     */
    @Test
    void theTextHasNoStrayEntries() {
        Set<String> categories = new HashSet<>();
        PACK.categories().forEach(c -> categories.add(c.id()));
        Set<String> programs = new HashSet<>();
        PACK.programs().forEach(p -> programs.add(p.id()));
        Set<String> configurations = new HashSet<>();
        Set<String> stepTypes = new HashSet<>();
        for (MissionConfiguration configuration : PACK.missionConfigurations()) {
            configurations.add(configuration.id());
            Set<String> stepIds = new HashSet<>();
            configuration.seasons().forEach(season -> season.steps().forEach(step -> {
                stepIds.add(step.id());
                String type = ContentStrings.stepType(step.id());
                stepTypes.add(type);
                stepTypes.add(type.replaceFirst("^spc-[ab]-", ""));
            }));
            ContentStrings.ConfigurationText text = STRINGS.configurations().get(configuration.id());
            if (text != null) {
                for (String override : text.steps().keySet()) {
                    assertTrue(stepIds.contains(override), configuration.id() + " has no step " + override);
                }
            }
        }
        assertKnown(STRINGS.categories().keySet(), categories, "category");
        assertKnown(STRINGS.programs().keySet(), programs, "program");
        assertKnown(STRINGS.components().keySet(), componentIds(), "component");
        assertKnown(STRINGS.configurations().keySet(), configurations, "configuration");
        assertKnown(STRINGS.steps().keySet(), stepTypes, "step type");
        assertKnown(STRINGS.goals().keySet(), goalIds(), "goal");
    }

    /**
     * Ids are unique across kinds, which {@link ContentNames}' single lookup relies on.
     */
    @Test
    void idsAreUniqueAcrossKinds() {
        Map<String, String> seen = new HashMap<>();
        Map<String, Set<String>> kinds = Map.of("category", STRINGS.categories().keySet(), "program",
            STRINGS.programs().keySet(), "component", STRINGS.components().keySet(), "configuration",
            STRINGS.configurations().keySet(), "goal", STRINGS.goals().keySet());
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
    @Test
    void everyTextFitsAndCanBeDrawn() {
        for (ContentStrings.Text text : allTexts()) {
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
        ContentNames.install(STRINGS, PACK);
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

    private static Set<String> componentIds() {
        Set<String> ids = new HashSet<>();
        PACK.missionComponents().forEach(c -> ids.add(c.id()));
        return ids;
    }

    private static Set<String> goalIds() {
        Set<String> ids = new HashSet<>();
        PACK.goals().forEach(g -> ids.add(g.id()));
        return ids;
    }

    private static List<ContentStrings.Text> allTexts() {
        List<ContentStrings.Text> texts = new java.util.ArrayList<>();
        texts.addAll(STRINGS.categories().values());
        texts.addAll(STRINGS.programs().values());
        texts.addAll(STRINGS.components().values());
        texts.addAll(STRINGS.steps().values());
        texts.addAll(STRINGS.goals().values());
        for (ContentStrings.ConfigurationText configuration : STRINGS.configurations().values()) {
            texts.add(new ContentStrings.Text(configuration.name(), configuration.description(),
                configuration.historyNote()));
            texts.addAll(configuration.steps().values());
        }
        return texts;
    }
}
