package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FrontPagePrinter} ({@code ui-spec.md} &sect;4.18, G14).
 *
 * @author Mario Koehler
 */
class FrontPagePrinterTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));

    private static final long SEED = 1234L;

    @Test
    void aTurnWithoutAFirstPrintsNothing() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", report(7, flight("m1", List.of("Daniel Hale"), first("m1", "human-in-space", false)))),
            agency("Kestrel Aerospace", null));
        assertTrue(FrontPagePrinter.print(7, SEED, agencies, PAGES, goal -> true).isEmpty());
    }

    @Test
    void oneAgencysFirstPrintsItsPage() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", report(26, flight("m1", List.of("Daniel Hale"),
                first("m1", "human-in-orbit", true)))),
            agency("Kestrel Aerospace", report(26)),
            agency("Baltic Star Agency", report(26)));
        List<FrontPage> pages = FrontPagePrinter.print(26, SEED, agencies, PAGES, goal -> true);
        assertEquals(1, pages.size());
        FrontPage page = pages.get(0);
        assertEquals("26-human-in-orbit", page.id());
        assertEquals("human-in-orbit", page.goalId());
        assertEquals(List.of("Orion Spaceworks"), page.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        assertEquals(List.of("Daniel Hale"), page.achievers().get(0).crewNames());
        assertEquals("Director of Orion Spaceworks", page.achievers().get(0).directorName());
        assertTrue(PAGES.first("human-in-orbit").orElseThrow().stories().stream()
            .anyMatch(s -> s.id().equals(page.storyId())));
        assertEquals(2, page.rivals().size());
        assertFalse(page.rivals().contains("Orion Spaceworks"));
        assertFalse(page.weather().isBlank());
    }

    @Test
    void theSamePageIsPrintedEveryTime() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", report(26, flight("m1", List.of("Daniel Hale"),
                first("m1", "human-in-orbit", true)))),
            agency("Kestrel Aerospace", report(26)), agency("Baltic Star Agency", report(26)),
            agency("Polar Star Bureau", report(26)));
        assertEquals(FrontPagePrinter.print(26, SEED, agencies, PAGES, goal -> true),
            FrontPagePrinter.print(26, SEED, new ArrayList<>(agencies.reversed()), PAGES, goal -> true));
    }

    @Test
    void twoAgenciesInOneTurnShareATiePage() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", report(52, flight("m1", List.of("Daniel Hale", "Lena Fischer"),
                first("m1", "crewed-lunar-landing", true)))),
            agency("Baltic Star Agency", report(52, flight("m9", List.of("Pavel Brandt"),
                first("m9", "crewed-lunar-landing", true)))),
            agency("Kestrel Aerospace", report(52)));
        List<FrontPage> pages = FrontPagePrinter.print(52, SEED, agencies, PAGES, goal -> true);
        assertEquals(1, pages.size());
        assertEquals("landing-tie", pages.get(0).storyId());
        assertEquals(List.of("Baltic Star Agency", "Orion Spaceworks"),
            pages.get(0).achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        assertEquals(List.of("Kestrel Aerospace"), pages.get(0).rivals());
    }

    @Test
    void earlierFirstsOfTheSameFlightAreFoldedIntoTheLatest() {
        // A first orbit credits human in space and the satellite through the cascade.
        List<FrontPagePrinter.Agency> agencies = List.of(agency("Orion Spaceworks", report(26,
            flight("m1", List.of("Daniel Hale"), first("m1", "human-in-orbit", true), first("m1", "human-in-space", true),
                first("m1", "earth-orbit-satellite", true)))));
        List<FrontPage> pages = FrontPagePrinter.print(26, SEED, agencies, PAGES, goal -> true);
        assertEquals(1, pages.size());
        assertEquals("human-in-orbit", pages.get(0).goalId());
        assertEquals(List.of("earth-orbit-satellite", "human-in-space"), pages.get(0).foldedGoalIds());
        assertTrue(pages.get(0).rivals().isEmpty());
    }

    @Test
    void firstsOfDifferentFlightsGetPagesOfTheirOwn() {
        List<FrontPagePrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", report(30, flight("m1", List.of("Daniel Hale"), first("m1", "human-in-orbit", true)))),
            agency("Kestrel Aerospace", report(30, flight("m2", List.of(), first("m2", "earth-orbit-satellite", true)))));
        List<FrontPage> pages = FrontPagePrinter.print(30, SEED, agencies, PAGES, goal -> true);
        assertEquals(List.of("earth-orbit-satellite", "human-in-orbit"), pages.stream().map(FrontPage::goalId).toList());
        assertTrue(pages.get(0).achievers().get(0).crewNames().isEmpty());
    }

    @Test
    void aFirstThePackLacksIsNotPrinted() {
        List<FrontPagePrinter.Agency> agencies = List.of(agency("Orion Spaceworks", report(40,
            flight("m1", List.of("Daniel Hale"), first("m1", "crewed-lunar-flyby", true)))));
        assertTrue(FrontPagePrinter.print(40, SEED, agencies, PAGES, goal -> !goal.startsWith("crewed")).isEmpty());
    }

    private static FrontPagePrinter.Agency agency(String name, TurnReport report) {
        return new FrontPagePrinter.Agency(name, "Director of " + name, report);
    }

    private record Flight(String id, List<String> crew, List<GoalSettled> goals) {
    }

    private static Flight flight(String id, List<String> crew, GoalSettled... goals) {
        return new Flight(id, crew, List.of(goals));
    }

    private static GoalSettled first(String mission, String goal, boolean firstInGame) {
        return new GoalSettled(mission, goal, false, 100, firstInGame);
    }

    private static TurnReport report(int turn, Flight... flights) {
        List<TurnReport.MissionFlown> missions = new ArrayList<>();
        List<ResolutionEvent> events = new ArrayList<>();
        for (Flight flight : flights) {
            missions.add(new TurnReport.MissionFlown(flight.id(), "config", 1, 1, true, flight.crew()));
            events.addAll(flight.goals());
        }
        return new TurnReport(TurnReport.VERSION, turn, 0, 0, 0, 0, List.of(), missions, events, null);
    }
}
