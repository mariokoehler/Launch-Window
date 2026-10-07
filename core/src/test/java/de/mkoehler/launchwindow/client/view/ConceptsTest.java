package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.client.ui.Theme;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the bundled concepts ({@code ui-spec.md} component 19): every id they name exists in the
 * pack, every explainer and sheet they open exists, their labels use only letters the fonts can
 * draw, and the chips come out as the mockups show them.
 *
 * @author Mario Koehler
 */
class ConceptsTest {

    private static final Path ASSETS = Path.of("..", "assets");
    private static final ContentCatalog PACK = ContentPackLoader.load(ASSETS.resolve("content").resolve("early-era"));

    private static Concepts concepts() throws IOException {
        return Concepts.parse(Files.readString(ASSETS.resolve("content").resolve(Concepts.FILE)));
    }

    @Test
    void everyConceptPointsAtRealContent() throws IOException {
        Explainers explainers = Explainers.parse(Files.readString(ASSETS.resolve("content").resolve(Explainers.FILE)));
        Set<String> stepTypes = new HashSet<>();
        PACK.missionConfigurations().forEach(configuration -> configuration.seasons().forEach(season ->
            season.steps().forEach(step -> stepTypes.add(Concepts.stepType(step.id())))));
        Set<String> ids = new HashSet<>();
        assertFalse(concepts().concepts().isEmpty());
        for (Concepts.Concept concept : concepts().concepts()) {
            assertTrue(ids.add(concept.id()), concept.id());
            assertTrue(concept.label() != null && !concept.label().isBlank(), concept.id());
            concept.label().chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                concept.id() + ": '" + (char) c + "'"));
            if (concept.shown()) {
                Explainers.Explainer explainer = explainers.byId(concept.explainer()).orElseThrow(
                    () -> new AssertionError(concept.id() + ": no explainer " + concept.explainer()));
                assertTrue(concept.sheet() >= 1 && concept.sheet() <= explainer.sheets().size(),
                    concept.id() + ": sheet " + concept.sheet());
            }
            concept.programs().forEach(id -> assertTrue(PACK.program(id).isPresent(), concept.id() + ": " + id));
            concept.configurations().forEach(id -> assertTrue(PACK.missionConfiguration(id).isPresent(),
                concept.id() + ": " + id));
            concept.steps().forEach(id -> assertTrue(stepTypes.contains(id), concept.id() + ": step " + id));
            concept.components().forEach(id -> assertTrue(PACK.missionComponent(id).isPresent(),
                concept.id() + ": " + id));
            concept.goals().forEach(id -> assertTrue(PACK.goal(id).isPresent(), concept.id() + ": " + id));
        }
    }

    @Test
    void theChipsMatchTheMockups() throws IOException {
        // The mockups' lists, re-homed (2026-10-07) when E-06 to E-19 got their explainers: flight ideas
        // on steps, capsule ideas on capsules, rocket ideas on rockets.
        Concepts concepts = concepts();
        assertEquals(List.of("Orbital speed", "Retrofire", "One-shot retrorocket", "Steering jets", "Launch escape"),
            labels(concepts.forProgram(PACK.program("vostok").orElseThrow())));
        assertEquals(List.of("Orbit", "Retrofire", "Heat shield", "The odds multiply", "Land or sea"),
            labels(concepts.forConfiguration(PACK.missionConfiguration("vostok-orbital").orElseThrow())));
        assertEquals(List.of("Orbital speed", "Staging", "Rocket equation", "Delta-v", "Rocket classes",
            "Cryogenic propellant", "Human rating", "Max Q", "Balloon tanks"), labels(concepts.forComponent("atlas")));
        assertEquals(List.of("Staging", "Rocket equation", "Delta-v", "Rocket classes", "Cryogenic propellant",
            "Launch latitude", "Guidance"), labels(concepts.forComponent("jupiter-c")));
    }

    @Test
    void noItemCarriesTooManyChips() throws IOException {
        // Six fit the narrow configuration column in three rows; a rocket's line has room for nine.
        Concepts concepts = concepts();
        PACK.programs().forEach(program -> assertTrue(concepts.forProgram(program).size() <= 6, program.id()));
        PACK.missionConfigurations().forEach(configuration -> assertTrue(
            concepts.forConfiguration(configuration).size() <= 6, configuration.id()));
        PACK.missionComponents().stream().filter(component -> component.rocket() != null).forEach(rocket ->
            assertTrue(concepts.forComponent(rocket.id()).size() <= 9, rocket.id()));
    }

    @Test
    void engineRestartsStayOffMercury() throws IOException {
        // Mercury's retropack was solid rockets: settling and relighting don't apply.
        assertFalse(labels(concepts().forConfiguration(PACK.missionConfiguration("mercury-orbital").orElseThrow()))
            .contains("Engine restarts"));
    }

    @Test
    void aJointFlightsCraftStepsCountAsTheirType() {
        assertEquals("retrofire", Concepts.stepType("12-spc-a-retrofire"));
        assertEquals("retrofire", Concepts.stepType("08-retrofire"));
    }

    private static List<String> labels(List<Concepts.Concept> concepts) {
        return concepts.stream().map(Concepts.Concept::label).toList();
    }
}
