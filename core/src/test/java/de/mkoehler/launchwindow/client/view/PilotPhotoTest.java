package de.mkoehler.launchwindow.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.rules.PortraitCatalog;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The photo of the first human in orbit shows the agency's own pilot.
 *
 * @author Mario Koehler
 */
class PilotPhotoTest {

    private static final String PLAIN = "frontpage/human-in-orbit.jpg";

    private static FrontPage page(String goal, FrontPage.Achiever... achievers) {
        return new FrontPage("1-" + goal, goal, 1, "story", "weather", List.of(), List.of(achievers), List.of());
    }

    @Test
    void everyPortraitHasAVersionAndEveryVersionAFile() {
        for (PortraitCatalog.Portrait portrait : PortraitCatalog.all()) {
            String version = PilotPhoto.version(portrait);
            assertNotNull(version, portrait.ethnicity());
            assertTrue(Files.exists(Path.of("..", "assets", "ui", version)), version);
        }
    }

    @Test
    void thePilotsPortraitPicksTheVersion() {
        PortraitCatalog.Portrait portrait = PortraitCatalog.all().get(0);
        assertEquals(PilotPhoto.version(portrait),
            PilotPhoto.of(page(PilotPhoto.GOAL, new FrontPage.Achiever("A", "D", List.of("P"), portrait.id()))));
    }

    @Test
    void noPortraitGetsThePlainPhoto() {
        assertEquals(PLAIN, PilotPhoto.of(page(PilotPhoto.GOAL, new FrontPage.Achiever("A", "D", List.of("P")))));
        assertEquals(PLAIN, PilotPhoto.of(page(PilotPhoto.GOAL, new FrontPage.Achiever("A", "D", List.of("P"), "gone"))));
    }

    @Test
    void anotherGoalKeepsItsPhoto() {
        String id = PortraitCatalog.all().get(0).id();
        assertEquals("frontpage/crewed-lunar-landing.jpg",
            PilotPhoto.of(page("crewed-lunar-landing", new FrontPage.Achiever("A", "D", List.of("P"), id))));
    }
}
