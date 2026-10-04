package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.content.MissionComponent;
import de.mkoehler.launchwindow.content.Program;
import de.mkoehler.launchwindow.content.ProgramCategory;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.SetSkill;
import de.mkoehler.launchwindow.rules.ProgramOpening;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests Headquarters' view logic: program states, the price breakdown (its lines always add up to
 * the rule's price), visibility by agency, and the per-body counts.
 *
 * @author Mario Koehler
 */
class ProgramsTest {

    private static final List<Double> BANDS = List.of(3.0, 6.0, 9.0, 12.0);

    private static MissionComponent component(String id, long creationCost) {
        return new MissionComponent(id, SetSkill.CREWED_SPACECRAFT, 10, 95, 85, BANDS, 0, List.of(), 5, creationCost, 0,
            100, null, null);
    }

    /**
     * A small pack with what the real one lacks: a locked program, discounts that apply (stacked
     * past 100%), one from another agency's program, and a Moon category.
     */
    private static ContentCatalog catalog() {
        List<ProgramCategory> categories = List.of(new ProgramCategory("orbit", "Orbit"),
            new ProgramCategory("moon-probes", "Moon probes", CelestialBody.MOON));
        List<MissionComponent> components = List.of(component("capsule", 700), component("probe", 300),
            component("lander", 900));
        List<Program> programs = List.of(
            new Program("alpha", "orbit", 1_000, 50, List.of("capsule"), List.of(), List.of(), Archetype.SOVIET),
            new Program("beta", "orbit", 1_000, 60, List.of("capsule", "probe"),
                List.of(new Program.OpeningCostDiscount("alpha", 30), new Program.OpeningCostDiscount("gamma", 80)),
                List.of(), Archetype.SOVIET),
            new Program("gamma", "orbit", 500, 10, List.of("probe"), List.of(), List.of(), Archetype.SOVIET),
            new Program("delta", "moon-probes", 2_000, 90, List.of("lander"),
                List.of(new Program.OpeningCostDiscount("other-agency", 40)), List.of("first-orbit"), Archetype.SOVIET),
            new Program("other-agency", "orbit", 300, 5, List.of("probe"), List.of(), List.of(), Archetype.NASA));
        return new ContentCatalog(categories, programs, List.of(), components,
            List.of(new Goal("first-orbit", 100, 50, List.of())));
    }

    private static PlayerSnapshot soviet() {
        return new PlayerSnapshot("Vega", Archetype.SOVIET, 50_000, 0, 0, 1);
    }

    /**
     * States, and the agency filter: another agency's program never shows.
     */
    @Test
    void statesAndVisibility() {
        ContentCatalog catalog = catalog();
        PlayerSnapshot state = soviet();
        state.getOpenedProgramIds().add("alpha");
        assertEquals(List.of("alpha", "beta", "gamma", "delta"),
            Programs.visible(state, catalog).stream().map(Program::id).toList());
        assertEquals(Programs.State.OPEN, Programs.state(catalog.program("alpha").orElseThrow(), state));
        assertEquals(Programs.State.AVAILABLE, Programs.state(catalog.program("gamma").orElseThrow(), state));
        assertEquals(Programs.State.LOCKED, Programs.state(catalog.program("delta").orElseThrow(), state));
        assertEquals(List.of("first-orbit"), Programs.missingGoals(catalog.program("delta").orElseThrow(), state));
        state.getAchievedGoals().put("first-orbit", 3);
        assertEquals(Programs.State.AVAILABLE, Programs.state(catalog.program("delta").orElseThrow(), state));
    }

    /**
     * The breakdown's lines sum to {@link ProgramOpening#openingPrice} in every case: no discount,
     * one discount, stacked discounts capped at 100%, a discount from another agency that never
     * applies, and an owned component.
     */
    @Test
    void priceLinesAddUpToTheRule() {
        ContentCatalog catalog = catalog();
        PlayerSnapshot state = soviet();
        assertAddsUp(catalog, state, "beta");
        state.getOpenedProgramIds().add("alpha");
        state.getMissionComponents().put("capsule", new OwnedMissionComponent(40, 10));
        Programs.Price oneDiscount = assertAddsUp(catalog, state, "beta");
        assertEquals(1_000 - 300 + 300, oneDiscount.total(), "30% off, capsule owned, probe created");
        state.getOpenedProgramIds().add("gamma");
        Programs.Price capped = assertAddsUp(catalog, state, "beta");
        assertEquals(300, capped.total(), "30% + 80% capped at 100%: only the probe's creation is left");
        Programs.Price neverApplies = assertAddsUp(catalog, state, "delta");
        assertTrue(neverApplies.lines().stream().anyMatch(line -> "None".equals(line.text())));
    }

    private static Programs.Price assertAddsUp(ContentCatalog catalog, PlayerSnapshot state, String programId) {
        Program program = catalog.program(programId).orElseThrow();
        Programs.Price price = Programs.price(program, state, catalog);
        long sum = price.lines().stream().mapToLong(Programs.PriceLine::amount).sum();
        assertEquals(ProgramOpening.openingPrice(state, program, catalog), sum, programId);
        assertEquals(sum, price.total(), programId);
        return price;
    }

    /**
     * Bodies count only their own categories' programs for this agency.
     */
    @Test
    void bodyCounts() {
        ContentCatalog catalog = catalog();
        PlayerSnapshot state = soviet();
        state.getOpenedProgramIds().add("alpha");
        assertEquals(new Programs.Counts(1, 2, 0), Programs.bodyCounts(CelestialBody.EARTH, state, catalog));
        assertEquals(new Programs.Counts(0, 0, 1), Programs.bodyCounts(CelestialBody.MOON, state, catalog));
        assertEquals(new Programs.Counts(0, 0, 0), Programs.bodyCounts(CelestialBody.MARS, state, catalog));
        assertEquals("1 open · 2 available", Programs.bodyCounts(CelestialBody.EARTH, state, catalog).text());
    }

    /**
     * The real pack loads without a body field on its categories; they default to Earth (gap G9).
     */
    @Test
    void packCategoriesDefaultToEarth() {
        ContentCatalog pack = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
        assertTrue(pack.categories().stream().allMatch(category -> category.body() == CelestialBody.EARTH));
    }
}
