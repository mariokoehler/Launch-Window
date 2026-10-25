package de.mkoehler.launchwindow.content;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies the decisions, press stories and clients as pack files (review.md 2.2): every pack loads them
 * into its catalog, they count towards the pack's hash, and the packs carry the same copies, so an edit to
 * one pack's can't quietly miss the other.
 *
 * @author Mario Koehler
 */
class RulesContentTest {

    private static final Path CONTENT = Path.of("..", "assets", "content");
    private static final List<String> FILES = List.of(ContentPackLoader.DECISIONS_FILE,
        ContentPackLoader.PR_PRESS_FILE, ContentPackLoader.COMMERCIAL_CLIENTS_FILE);

    @Test
    void everyPackLoadsThemIntoItsCatalog() {
        for (String pack : List.of("standard", "early-era")) {
            RulesContent rules = ContentPackLoader.load(CONTENT.resolve(pack)).rules();
            assertFalse(rules.decisions().decisions().isEmpty(), pack + " has decisions");
            assertFalse(rules.press().stories().isEmpty(), pack + " has press stories");
            assertFalse(rules.clients().clients().isEmpty(), pack + " has clients");
        }
    }

    @Test
    void thePacksCarryTheSameCopies() throws IOException {
        for (String file : FILES) {
            assertEquals(Files.readString(CONTENT.resolve("standard").resolve(file)).replace("\r\n", "\n"),
                Files.readString(CONTENT.resolve("early-era").resolve(file)).replace("\r\n", "\n"),
                file + " differs between the packs: edit standard's and copy it to early-era");
        }
    }

    @Test
    void theyAreHashedWithThePack(@org.junit.jupiter.api.io.TempDir Path copy) throws IOException {
        Path pack = CONTENT.resolve("early-era");
        for (String file : ContentPackLoader.FILES) {
            Files.copy(pack.resolve(file), copy.resolve(file));
        }
        String before = ContentPackHash.of(copy);
        Path decisions = copy.resolve(ContentPackLoader.DECISIONS_FILE);
        Files.writeString(decisions, Files.readString(decisions).replaceFirst("\\{", "{ "));
        assertNotEquals(before, ContentPackHash.of(copy), "a changed decision changes the pack's hash");
    }

    @Test
    void aCatalogWithOtherRulesKeepsItsTree() {
        ContentCatalog pack = ContentPackLoader.load(CONTENT.resolve("early-era"));
        ContentCatalog without = pack.withRules(pack.rules().withDecisions(Decisions.EMPTY));
        assertSame(Decisions.EMPTY, without.rules().decisions());
        assertSame(pack.rules().press(), without.rules().press());
        assertEquals(pack.program("vostok").isPresent(), without.program("vostok").isPresent());
    }
}
