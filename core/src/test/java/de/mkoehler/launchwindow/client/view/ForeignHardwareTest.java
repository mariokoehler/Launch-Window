package de.mkoehler.launchwindow.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.rules.IntelFinding;
import de.mkoehler.launchwindow.rules.Intelligence;
import de.mkoehler.launchwindow.rules.IntelligenceBoard;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Foreign hardware ({@code design.md} &sect;2.9): every program and rocket of both packs has its
 * words, and the rules teach only what the other side has open, at the level's chance.
 *
 * @author Mario Koehler
 */
class ForeignHardwareTest {

    private static final ContentCatalog CATALOG = pack("standard");
    private static final ForeignHardware WORDS = words();

    private static ContentCatalog pack(String name) {
        return ContentPackLoader.load(Path.of("..", "assets", "content", name));
    }

    private static ForeignHardware words() {
        try {
            return new ObjectMapper().readValue(Path.of("..", "assets", "content", ForeignHardware.FILE).toFile(),
                ForeignHardware.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<String> items(ContentCatalog catalog) {
        List<String> ids = new ArrayList<>();
        catalog.programs().forEach(program -> ids.add(program.id()));
        catalog.missionComponents().stream().filter(component -> component.rocket() != null)
            .forEach(component -> ids.add(component.id()));
        return ids;
    }

    private static PlayerSnapshot agency(Archetype archetype, int level) {
        PlayerSnapshot agency = new PlayerSnapshot(archetype.name(), archetype, 10_000, 0, 3_000, 5);
        agency.getIntelligence().setLevel(level);
        return agency;
    }

    @Test
    void everyProgramAndRocketOfBothPacksHasItsWords() {
        for (String name : List.of("standard", "early-era")) {
            for (String id : items(pack(name))) {
                assertNotNull(WORDS.items().get(id), id + " has no words");
            }
        }
        assertEquals(items(CATALOG).size(), WORDS.items().size(), "words for ids that no longer exist");
    }

    @Test
    void rolesFitAndCounterpartsAreOnTheOtherSide() {
        WORDS.items().forEach((id, item) -> {
            assertTrue(item.role().length() <= ForeignHardware.MAX_ROLE, id + "'s role is too long");
            if (item.counterpart() != null) {
                assertNotNull(WORDS.items().get(item.counterpart()), id + "'s counterpart");
                assertTrue(ForeignHardware.side(id, CATALOG) != ForeignHardware.side(item.counterpart(), CATALOG),
                    id + "'s counterpart is on its own side");
            }
        });
    }

    @Test
    void onlyWhatTheOtherSideHasOpenIsInThePool() {
        PlayerSnapshot nasa = agency(Archetype.NASA, 1);
        PlayerSnapshot soviet = agency(Archetype.SOVIET, 0);
        soviet.getOpenedProgramIds().add("vostok");
        OwnedMissionComponent proton = new OwnedMissionComponent(50, 50);
        soviet.getMissionComponents().put("proton", proton);
        soviet.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(50, 50));
        assertEquals(List.of("proton", "vostok"), Intelligence.hardwarePool(nasa, List.of(nasa, soviet), CATALOG));

        proton.setActive(false);
        assertEquals(List.of("vostok"), Intelligence.hardwarePool(nasa, List.of(nasa, soviet), CATALOG));

        nasa.getIntelligence().getForeignHardware().put("vostok", 3);
        assertTrue(Intelligence.hardwarePool(nasa, List.of(nasa, soviet), CATALOG).isEmpty());
    }

    @Test
    void aGameWithNoOtherSideLearnsNothing() {
        Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
        agencies.put("a", agency(Archetype.NASA, 3));
        PlayerSnapshot same = agency(Archetype.NASA, 3);
        same.getOpenedProgramIds().add("mercury");
        agencies.put("b", same);
        for (long seed = 0; seed < 200; seed++) {
            assertTrue(IntelligenceBoard.settle(agencies, CATALOG, seed, 5).values().stream().flatMap(List::stream)
                .noneMatch(finding -> finding.kind() == IntelFinding.Kind.FOREIGN_HARDWARE));
        }
    }

    @Test
    void eachLevelLearnsAtItsChance() {
        for (int level = 0; level <= Intelligence.MAX_LEVEL; level++) {
            Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
            agencies.put("spy", agency(Archetype.NASA, level));
            PlayerSnapshot soviet = agency(Archetype.SOVIET, 0);
            soviet.getOpenedProgramIds().add("vostok");
            agencies.put("rival", soviet);
            int learned = 0;
            int rolls = 4_000;
            for (long seed = 0; seed < rolls; seed++) {
                learned += (int) IntelligenceBoard.settle(agencies, CATALOG, seed, 5).getOrDefault("spy", List.of())
                    .stream().filter(finding -> finding.kind() == IntelFinding.Kind.FOREIGN_HARDWARE).count();
            }
            assertEquals(Intelligence.HARDWARE_PERCENT[level], 100.0 * learned / rolls, 3.0, "level " + level);
        }
    }

    @Test
    void whatIsLearnedIsKeptWhenTheServiceShrinks() {
        PlayerSnapshot nasa = agency(Archetype.NASA, 1);
        nasa.getIntelligence().setTargetLevel(0);
        List<IntelFinding> found = null;
        for (long seed = 0; found == null; seed++) {
            Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
            agencies.put("spy", nasa);
            PlayerSnapshot soviet = agency(Archetype.SOVIET, 0);
            soviet.getOpenedProgramIds().add("vostok");
            agencies.put("rival", soviet);
            found = IntelligenceBoard.settle(agencies, CATALOG, seed, 5).get("spy");
            if (found != null && found.stream().noneMatch(f -> f.kind() == IntelFinding.Kind.FOREIGN_HARDWARE)) {
                found = null;
            }
        }
        Intelligence.advanceSeason(nasa, found, 5);
        assertEquals(0, nasa.getIntelligence().getLevel());
        assertEquals(5, nasa.getIntelligence().getForeignHardware().get("vostok"));
        assertTrue(ForeignHardware.known("vostok", nasa, CATALOG, false));
    }

    @Test
    void rowsShowWhatThePlayerHasEarned() {
        PlayerSnapshot nasa = agency(Archetype.NASA, 1);
        ForeignHardware.Row unknown = WORDS.row("vostok-capsule", nasa, CATALOG, false);
        assertFalse(unknown.known());
        assertNull(unknown.role());
        assertFalse(unknown.opens());

        assertTrue(WORDS.row("vostok-capsule", nasa, CATALOG, true).known(), "the decided race opens everything");

        nasa.getIntelligence().getForeignHardware().put("korabl-sputnik-2", 3);
        ForeignHardware.Row shared = WORDS.row("vostok-capsule", nasa, CATALOG, false);
        assertTrue(shared.known(), "a known program explains every component it needs");
        assertEquals("korabl-sputnik-2", shared.fileId());

        ForeignHardware.Row own = WORDS.row("mercury", nasa, CATALOG, false);
        assertTrue(own.known());
        assertFalse(own.foreign());
        assertNull(own.linkLead(), "your own side's hardware has no counterpart line");

        nasa.getIntelligence().getForeignHardware().put("proton", 3);
        ForeignHardware.Row rocket = WORDS.row("proton", nasa, CATALOG, false);
        assertEquals("their counterpart to your ", rocket.linkLead());
    }

    @Test
    void aPartNamesItsProgram() {
        PlayerSnapshot nasa = agency(Archetype.NASA, 1);
        Program voskhod = CATALOG.program("voskhod").orElseThrow();
        nasa.getIntelligence().getForeignHardware().put("voskhod", 3);
        assertEquals("their counterpart to your ",
            WORDS.row(voskhod.requiredMissionComponentIds().getFirst(), nasa, CATALOG, false).linkLead());
        MissionComponent suit = CATALOG.missionComponents().stream().filter(component -> voskhod
            .requiredMissionComponentIds().indexOf(component.id()) > 0).findFirst().orElseThrow();
        assertEquals("part of their ", WORDS.row(suit.id(), nasa, CATALOG, false).linkLead());
    }
}
