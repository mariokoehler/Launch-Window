package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.Archetype;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the contract board's clients ({@code design.md} &sect;2.11): every line a side can fly
 * contracts in has clients for it, ids are unique, and a line's clients don't repeat until all ran.
 *
 * @author Mario Koehler
 */
class CommercialClientsTest {

    private static final CommercialClients CLIENTS =
        CommercialClients.load(Path.of("..", "assets", "content", "standard", CommercialClients.FILE));

    @Test
    void everyLineASideFliesHasClientsForIt() {
        for (String pack : List.of("standard", "early-era")) {
            ContentCatalog catalog = ContentPackLoader.load(Path.of("..", "assets", "content", pack));
            for (Commercial commercial : catalog.commercials()) {
                Program program = catalog.program(commercial.programId()).orElseThrow();
                for (Archetype side : Archetype.values()) {
                    if (!program.availableTo(side)) {
                        continue;
                    }
                    long count = CLIENTS.clients().stream()
                        .filter(client -> client.lineId().equals(program.categoryId()) && client.side() == side)
                        .count();
                    assertTrue(count >= 2, side + " has " + count + " clients in " + program.categoryId());
                }
            }
        }
    }

    @Test
    void idsAreUnique() {
        assertEquals(CLIENTS.clients().size(),
            new HashSet<>(CLIENTS.clients().stream().map(CommercialClients.Client::id).toList()).size());
    }

    @Test
    void aLinesClientsRunOnceBeforeAnyRepeats() {
        List<String> used = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(3);
        long count = CLIENTS.clients().stream()
            .filter(client -> client.lineId().equals("lunar-probes") && client.side() == Archetype.NASA).count();
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            picked.add(CLIENTS.pick("lunar-probes", Archetype.NASA, random, used));
        }
        assertEquals(count, new HashSet<>(picked).size());
        CLIENTS.pick("lunar-probes", Archetype.NASA, random, used);
        assertEquals(1, used.size(), "once all ran, they start over");
    }
}
