package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.SetSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ContentPackLoader}: a pack written field-for-field from the
 * content records loads back into an equal catalog, and the strict mapper
 * rejects the mistakes a hand-edited pack is likely to contain.
 *
 * @author Mario Koehler
 */
class ContentPackLoaderTest {

    private static final ProgramCategory CATEGORY = new ProgramCategory("crewed-capsules", "Crewed capsules");
    private static final MissionComponent CAPSULE = new MissionComponent("capsule", SetSkill.CREWED_SPACECRAFT,
        3, 90, 82, List.of(3.0, 6.0, 9.0, 12.0), 20,
        List.of(new MissionComponent.ReliabilityTransferSource("booster", 30)), 158);
    private static final MissionComponent BOOSTER = new MissionComponent("booster", SetSkill.HUMAN_RATED_ROCKETS,
        3, 92, 85, List.of(2.0, 4.0, 6.0, 8.0), 0, List.of(), 188);
    private static final Goal FIRST_FLIGHT = new Goal("first-flight", 1000, 500, List.of());
    private static final Goal FIRST_TEST = new Goal("first-test", 100, 50, List.of());
    private static final Program PROGRAM = new Program("capsule-program", "crewed-capsules", 1345, 135,
        List.of("capsule", "booster"), List.of(), List.of("first-test"));
    private static final MissionConfiguration CONFIG = new MissionConfiguration("capsule-flight", "capsule-program",
        List.of(new MissionSeason(List.of(
            new MissionStep("launch",
                List.of(new MissionStep.ComponentInvolvement("booster", 80),
                    new MissionStep.ComponentInvolvement("capsule", 20)),
                List.of(new MissionStep.ControllerInvolvement(0, 60)),
                List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true, 40, 5),
            new MissionStep("splashdown",
                List.of(new MissionStep.ComponentInvolvement("capsule", 100)),
                List.of(),
                List.of(new MissionStep.CrewInvolvement(0, AstronautSkill.PILOTING, 100)), true, 60, 5,
                List.of("first-flight"))))),
        1, 1, 2243, 1000,
        List.of(new MissionConfiguration.GoalModifier("first-test", "capsule", 15)),
        List.of());

    @TempDir
    Path packDirectory;

    @Test
    void writtenPackLoadsBackEqual() throws IOException {
        writePack(List.of(CATEGORY), List.of(PROGRAM), List.of(CAPSULE, BOOSTER), List.of(CONFIG),
            List.of(FIRST_FLIGHT, FIRST_TEST));

        ContentCatalog catalog = ContentPackLoader.load(packDirectory);

        assertEquals(CATEGORY, catalog.category("crewed-capsules").orElseThrow());
        assertEquals(PROGRAM, catalog.program("capsule-program").orElseThrow());
        assertEquals(CAPSULE, catalog.missionComponent("capsule").orElseThrow());
        assertEquals(BOOSTER, catalog.missionComponent("booster").orElseThrow());
        assertEquals(CONFIG, catalog.missionConfiguration("capsule-flight").orElseThrow());
        assertEquals(FIRST_FLIGHT, catalog.goal("first-flight").orElseThrow());
    }

    @Test
    void missingListDefaultsToEmpty() throws IOException {
        writePack(List.of(CATEGORY), List.of(), List.of(), List.of(), List.of());
        Files.writeString(packDirectory.resolve(ContentPackLoader.GOALS_FILE),
            "[{\"id\":\"g\",\"firstTimePrestige\":10,\"secondTimePrestige\":5}]");

        assertTrue(ContentPackLoader.load(packDirectory).goal("g").orElseThrow().cancelsGoalIds().isEmpty());
    }

    @Test
    void missingNumberIsRejected() throws IOException {
        writePack(List.of(CATEGORY), List.of(), List.of(), List.of(), List.of());
        Files.writeString(packDirectory.resolve(ContentPackLoader.GOALS_FILE),
            "[{\"id\":\"g\",\"firstTimePrestige\":10}]");

        assertThrows(IllegalArgumentException.class, () -> ContentPackLoader.load(packDirectory));
    }

    @Test
    void unknownFieldIsRejected() throws IOException {
        writePack(List.of(CATEGORY), List.of(), List.of(), List.of(), List.of());
        Files.writeString(packDirectory.resolve(ContentPackLoader.CATEGORIES_FILE),
            "[{\"id\":\"c\",\"name\":\"C\",\"colour\":\"red\"}]");

        assertThrows(IllegalArgumentException.class, () -> ContentPackLoader.load(packDirectory));
    }

    @Test
    void recordValidationFailureIsAContentError() throws IOException {
        writePack(List.of(CATEGORY), List.of(), List.of(), List.of(), List.of());
        Files.writeString(packDirectory.resolve(ContentPackLoader.GOALS_FILE),
            "[{\"id\":\"g\",\"firstTimePrestige\":-1,\"secondTimePrestige\":5}]");

        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> ContentPackLoader.load(packDirectory));
        assertTrue(e.getMessage().contains(ContentPackLoader.GOALS_FILE), e.getMessage());
    }

    @Test
    void danglingReferenceIsRejected() throws IOException {
        writePack(List.of(CATEGORY), List.of(PROGRAM), List.of(CAPSULE), List.of(), List.of(FIRST_TEST));

        assertThrows(IllegalArgumentException.class, () -> ContentPackLoader.load(packDirectory));
    }

    @Test
    void missingFileIsAnIoError() {
        assertThrows(UncheckedIOException.class, () -> ContentPackLoader.load(packDirectory));
    }

    private void writePack(List<ProgramCategory> categories, List<Program> programs,
            List<MissionComponent> components, List<MissionConfiguration> configurations, List<Goal> goals)
            throws IOException {
        write(ContentPackLoader.CATEGORIES_FILE, categories);
        write(ContentPackLoader.PROGRAMS_FILE, programs);
        write(ContentPackLoader.MISSION_COMPONENTS_FILE, components);
        write(ContentPackLoader.MISSION_CONFIGURATIONS_FILE, configurations);
        write(ContentPackLoader.GOALS_FILE, goals);
    }

    private void write(String fileName, Object value) throws IOException {
        ContentPackLoader.mapper().writerWithDefaultPrettyPrinter()
            .writeValue(packDirectory.resolve(fileName).toFile(), value);
    }
}
