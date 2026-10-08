package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Building;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.PersonnelFile;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.Retention;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests poaching ({@code design.md} &sect;2.2(d), phase 5): making an offer, the standing orders,
 * the turn's settlement across two agencies, and what each side's pass and report get.
 *
 * @author Mario Koehler
 */
class PoachingTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String NASA = "rival-nasa";
    private static final String SOVIET = "rival-soviet";
    private static final int TURN = 12;

    private static PlayerSnapshot agency(String name, Archetype archetype) {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, archetype, 25_000, 500, 0, 1);
        snapshot.getIntelligence().setLevel(Intelligence.PERSONNEL_FILE_LEVEL);
        snapshot.getIntelligence().setTargetLevel(Intelligence.PERSONNEL_FILE_LEVEL);
        return snapshot;
    }

    /** NASA, with a file on the Soviets' engineer; the Soviets, with that engineer. */
    private static Map<String, PlayerSnapshot> game(int loyalty, Retention retention, Trait... traits) {
        PlayerSnapshot nasa = agency("NASA", Archetype.NASA);
        PlayerSnapshot soviet = agency("Soviet Space Agency", Archetype.SOVIET);
        SetSpecialist engineer = new SetSpecialist("p5", "Ivan Kozlov", 34, 20, 50);
        engineer.setSalary(36);
        engineer.setTenureSeasons(6);
        engineer.setLoyalty(loyalty);
        engineer.setDisappointedSeasons(2);
        engineer.setRetention(retention);
        engineer.setWorkSeasons(8);
        List<TraitState> states = new ArrayList<>();
        for (Trait trait : traits) {
            states.add(new TraitState(trait, 2, true));
        }
        engineer.setTraits(states);
        engineer.getCareer().add(new CareerEntry(3, CareerEntry.Kind.HIRED, "", "calm", "", 0, 0));
        soviet.getSetSpecialists().add(engineer);
        nasa.getIntelligence().folder(SOVIET, "Soviet Space Agency").getPersonnelFiles().add(new PersonnelFile("p5",
            "Ivan Kozlov", "", "SET", 34, TURN - 1, List.of(), traits.length > 0 ? traits[0] : null, "Low", null,
            true, 36, 5, null));
        Map<String, PlayerSnapshot> game = new TreeMap<>();
        game.put(NASA, nasa);
        game.put(SOVIET, soviet);
        return game;
    }

    /** Settles the turn, through the saved findings file's JSON as the server does, and runs both passes. */
    private static Map<String, List<ResolutionEvent>> resolve(Map<String, PlayerSnapshot> game, long seed)
            throws Exception {
        Map<String, List<IntelFinding>> settled = IntelligenceBoard.settle(game, CATALOG, seed, TURN);
        Map<String, List<IntelFinding>> findings = MAPPER.readValue(MAPPER.writeValueAsBytes(settled),
            new TypeReference<>() { });
        Map<String, List<ResolutionEvent>> events = new TreeMap<>();
        // The target's pass first, so the poacher's copy can't be the object the target removed.
        for (String id : List.of(SOVIET, NASA)) {
            events.put(id, Intelligence.advanceSeason(game.get(id), findings.getOrDefault(id, List.of()), TURN));
        }
        return events;
    }

    private static OfferSettled line(List<ResolutionEvent> events) {
        return events.stream().filter(OfferSettled.class::isInstance).map(OfferSettled.class::cast).findFirst()
            .orElseThrow();
    }

    @Test
    void anOfferNeedsAFileOnAnEngineerOrControllerAndThereIsOneASeason() {
        Map<String, PlayerSnapshot> game = game(50, Retention.DEFAULT);
        PlayerSnapshot nasa = game.get(NASA);
        nasa.getIntelligence().folder(SOVIET, null).getPersonnelFiles().add(new PersonnelFile("p1", "Pilot", "",
            "ASTRONAUT", 30, TURN - 1, List.of(), null));
        assertEquals(Poaching.Outcome.CANT_APPROACH_GROUP, Poaching.queueOffer(nasa, SOVIET, "p1", 50));
        assertEquals(Poaching.Outcome.NO_FILE, Poaching.queueOffer(nasa, SOVIET, "p9", 50));
        assertEquals(Poaching.Outcome.INVALID_RAISE, Poaching.queueOffer(nasa, SOVIET, "p5", 30));
        nasa.getIntelligence().setLevel(2);
        assertEquals(Poaching.Outcome.LEVEL_TOO_LOW, Poaching.queueOffer(nasa, SOVIET, "p5", 50));
        nasa.getIntelligence().setLevel(3);
        nasa.getIntelligence().setActionsUsed(3);
        // No action needed: every action is used and the offer still goes.
        assertEquals(Poaching.Outcome.QUEUED, Poaching.queueOffer(nasa, SOVIET, "p5", 50));
        assertEquals(Poaching.Outcome.OFFER_USED, Poaching.queueOffer(nasa, SOVIET, "p5", 100));
        assertEquals(Intelligence.Outcome.INVALID_OPERATION, Intelligence.queue(nasa, IntelOperation.Kind.OFFER,
            SOVIET, null, "SET", "p5"));
        String id = Poaching.queuedOffers(nasa).getFirst().id();
        assertEquals(Intelligence.Outcome.CANCELLED, Intelligence.cancel(nasa, id));
        assertEquals(3, nasa.getIntelligence().getActionsUsed(), "cancelling an offer gives no action back");
    }

    @Test
    void aMatchedOfferRaisesTheSalaryForGoodAndBothReportsSaySo() throws Exception {
        Map<String, PlayerSnapshot> game = game(10, Retention.KEEP);
        Poaching.queueOffer(game.get(NASA), SOVIET, "p5", 50);
        Map<String, List<ResolutionEvent>> events = resolve(game, 1);

        SetSpecialist kozlov = game.get(SOVIET).getSetSpecialists().getFirst();
        assertEquals(54, kozlov.getSalary(), 1e-9);
        assertEquals(15, kozlov.getLoyalty());
        assertEquals(TURN, kozlov.getLastApproachedTurn());
        assertEquals(CareerEntry.Kind.OFFER, kozlov.getCareer().getLast().kind());
        assertEquals("MATCHED", kozlov.getCareer().getLast().outcome());
        OfferSettled theirs = line(events.get(SOVIET));
        assertFalse(theirs.made());
        assertEquals(OfferResult.Outcome.MATCHED, theirs.outcome());
        assertEquals("KEEP", theirs.retention());
        OfferSettled ours = line(events.get(NASA));
        assertTrue(ours.made());
        assertEquals(OfferResult.Outcome.MATCHED, ours.outcome());
        assertEquals(TURN, Poaching.file(game.get(NASA), SOVIET, "p5").lastApproachedTurn());
        assertEquals(54, Poaching.file(game.get(NASA), SOVIET, "p5").salary(), 1e-9);
        assertTrue(events.get(NASA).stream().noneMatch(IntelligenceFound.class::isInstance));
    }

    @Test
    void someoneWhoTakesItMovesWithANewIdAndOnlyTheFiledTraitKnown() throws Exception {
        // Low loyalty, disappointed, low morale, never matched: 90 % at +100 %. Find a seed where they go.
        for (long seed = 1; seed < 50; seed++) {
            Map<String, PlayerSnapshot> game = game(0, Retention.LET_GO, Trait.RESTLESS, Trait.PERFECTIONIST);
            PlayerSnapshot nasa = game.get(NASA);
            long funds = nasa.getFunds();
            String nextId = "p" + nasa.getNextPersonnelId();
            Poaching.queueOffer(nasa, SOVIET, "p5", 100);
            Map<String, List<ResolutionEvent>> events = resolve(game, seed);
            if (line(events.get(NASA)).outcome() != OfferResult.Outcome.JOINED) {
                continue;
            }
            assertTrue(game.get(SOVIET).getSetSpecialists().isEmpty());
            SetSpecialist kozlov = nasa.getSetSpecialists().getFirst();
            assertEquals(nextId, kozlov.getId());
            assertEquals(72, kozlov.getSalary(), 1e-9);
            assertEquals(funds - 72, nasa.getFunds());
            assertEquals(Careers.LOYALTY_START, kozlov.getLoyalty());
            assertEquals(0, kozlov.getWorkSeasons());
            assertEquals(0, kozlov.getTenureSeasons());
            assertEquals(List.of(true, false), kozlov.getTraits().stream().map(TraitState::revealed).toList());
            assertEquals(CareerEntry.Kind.JOINED, kozlov.getCareer().getLast().kind());
            assertEquals("Soviet Space Agency", kozlov.getCareer().getLast().ref());
            assertNull(Poaching.file(nasa, SOVIET, "p5"), "no file on our own employee");
            OfferSettled ours = line(events.get(NASA));
            assertEquals(nextId, ours.personId());
            assertEquals(72, ours.signingBonus());
            assertEquals(OfferResult.Outcome.JOINED, line(events.get(SOVIET)).outcome());
            assertEquals("LET_GO", line(events.get(SOVIET)).retention());
            // A report line never carries the person: no traits, no affinity, no ambition to come.
            String report = MAPPER.writeValueAsString(events);
            assertFalse(report.contains("PERFECTIONIST"), report);
            assertFalse(report.contains("traits"), report);
            return;
        }
        throw new AssertionError("nobody took a 90 % offer in 49 seeds");
    }

    @Test
    void aPoachedEngineerLeavesAnRndTeamThroughTheWholeSeasonPass() throws Exception {
        // The real pass, both sides, with the target running no service at all: the engineer was
        // captured in an R&D team before the intelligence phase took them, and the R&D, teams and
        // careers phases after it must cope.
        for (long seed = 1; seed < 50; seed++) {
            Map<String, PlayerSnapshot> game = game(0, Retention.LET_GO, Trait.RESTLESS);
            PlayerSnapshot soviet = game.get(SOVIET);
            soviet.getIntelligence().setLevel(0);
            soviet.getIntelligence().setTargetLevel(0);
            SetSpecialist kozlov = soviet.getSetSpecialists().getFirst();
            String component = CATALOG.missionComponents().iterator().next().id();
            soviet.getMissionComponents().put(component, new de.mkoehler.launchwindow.model.OwnedMissionComponent(40, 30));
            kozlov.setAssignmentState(de.mkoehler.launchwindow.model.AssignmentState.ASSIGNED);
            kozlov.setRdTargetComponentId(component);
            kozlov.setRdSlot(0);
            SetSpecialist colleague = new SetSpecialist("p6", "Colleague", 40, 70, 50);
            colleague.setAssignmentState(de.mkoehler.launchwindow.model.AssignmentState.ASSIGNED);
            colleague.setRdTargetComponentId(component);
            colleague.setRdSlot(1);
            soviet.getSetSpecialists().add(colleague);
            Poaching.queueOffer(game.get(NASA), SOVIET, "p5", 100);
            Map<String, List<IntelFinding>> findings = IntelligenceBoard.settle(game, CATALOG, seed, TURN);
            if (findings.get(NASA).getFirst().offer().outcome() != OfferResult.Outcome.JOINED) {
                continue;
            }
            Map<String, Long> salaries = new TreeMap<>();
            for (String id : List.of(SOVIET, NASA)) {
                SeasonResolver.advanceSeason(game.get(id), new SeasonResolver.Context(CATALOG,
                    de.mkoehler.launchwindow.content.BudgetSchedule.baspmClose(), new de.mkoehler.launchwindow.content.RandomEventRoster(0, 0,
                    de.mkoehler.launchwindow.content.RandomEventRoster.baspmClose().events()), new GoalTally(), TURN, 5, findings.get(id)),
                    new java.util.Random(1), new SeasonResolver.Observer() {
                        @Override
                        public void before(SeasonResolver.Phase phase, PlayerSnapshot snapshot) {
                            if (phase == SeasonResolver.Phase.BUDGET) {
                                salaries.put(id, BudgetProgress.runningCostBreakdown(snapshot, CATALOG).salaries());
                            }
                        }
                    });
            }
            assertEquals(List.of("p6"), soviet.getSetSpecialists().stream().map(SetSpecialist::getId).toList());
            assertEquals(Math.round(colleague.getSalary()), salaries.get(SOVIET), "the target stops paying at once");
            SetSpecialist recruit = game.get(NASA).getSetSpecialists().getFirst();
            assertEquals(72, salaries.get(NASA), "the poacher pays the new salary from this resolution");
            assertEquals(1, recruit.getTenureSeasons());
            assertEquals(Careers.LOYALTY_START + Careers.LOYALTY_PER_SEASON, recruit.getLoyalty());
            assertEquals(1, recruit.getCareer().stream().filter(line -> line.kind() == CareerEntry.Kind.HIRED).count());
            assertNull(recruit.getRdTargetComponentId());
            return;
        }
        throw new AssertionError("nobody took a 90 % offer in 49 seeds");
    }

    @Test
    void offersLapseForTheNewTheRecentlyApproachedAndAFullRoster() throws Exception {
        Map<String, PlayerSnapshot> game = game(0, Retention.LET_GO);
        game.get(SOVIET).getSetSpecialists().getFirst().setTenureSeasons(2);
        Poaching.queueOffer(game.get(NASA), SOVIET, "p5", 100);
        Map<String, List<ResolutionEvent>> events = resolve(game, 1);
        assertEquals(OfferResult.Outcome.LAPSED_NOT_ELIGIBLE, line(events.get(NASA)).outcome());
        assertTrue(events.get(SOVIET).stream().noneMatch(OfferSettled.class::isInstance), "a lapse isn't told");

        game = game(0, Retention.LET_GO);
        game.get(SOVIET).getSetSpecialists().getFirst().setLastApproachedTurn(TURN - 2);
        Poaching.queueOffer(game.get(NASA), SOVIET, "p5", 100);
        assertEquals(OfferResult.Outcome.LAPSED_NOT_ELIGIBLE, line(resolve(game, 1).get(NASA)).outcome());

        game = game(0, Retention.LET_GO);
        PlayerSnapshot nasa = game.get(NASA);
        int room = BuildingUpgrades.capacity(nasa, Building.SET_CENTER, CATALOG.buildings());
        for (int i = 0; i < room; i++) {
            nasa.getSetSpecialists().add(new SetSpecialist("own" + i, "Own " + i, 30, 70, 50));
        }
        Poaching.queueOffer(nasa, SOVIET, "p5", 100);
        assertEquals(OfferResult.Outcome.LAPSED_NO_ROOM, line(resolve(game, 1).get(NASA)).outcome());
        assertEquals(1, game.get(SOVIET).getSetSpecialists().size());
    }

    @Test
    void theDefaultMatchesUpToItsRaiseAndLetGoNeverDoes() {
        PlayerSnapshot soviet = game(50, Retention.DEFAULT).get(SOVIET);
        SetSpecialist kozlov = soviet.getSetSpecialists().getFirst();
        assertTrue(Poaching.matches(soviet, kozlov, PersonnelGroup.SET, 25));
        assertFalse(Poaching.matches(soviet, kozlov, PersonnelGroup.SET, 50));
        assertEquals(Poaching.Outcome.RETENTION_SET, Poaching.setDefault(soviet, PersonnelGroup.SET, 100));
        assertTrue(Poaching.matches(soviet, kozlov, PersonnelGroup.SET, 100));
        assertEquals(Poaching.Outcome.INVALID_RETENTION, Poaching.setDefault(soviet, PersonnelGroup.ASTRONAUT, 25));
        assertEquals(Poaching.Outcome.INVALID_RETENTION, Poaching.setDefault(soviet, PersonnelGroup.SET, 30));
        Poaching.setRetention(soviet, PersonnelGroup.SET, "p5", Retention.LET_GO);
        assertFalse(Poaching.matches(soviet, kozlov, PersonnelGroup.SET, 25));
    }

    @Test
    void theChanceAndTheRangeAFileShows() {
        // Middling loyalty, Restless, +25 %: 15 x (100 - loyalty) / 50 + 10 over loyalty 40 to 69.
        PersonnelFile file = new PersonnelFile("p5", "Kozlov", "", "SET", 34, 1, List.of(), Trait.RESTLESS,
            "Middling", null, false);
        assertArrayEquals(new int[] {19, 28}, Poaching.answerRange(25, file));
        assertEquals(15, Poaching.acceptPercent(25, 50, 70, false, List.of()));
        assertEquals(0, Poaching.acceptPercent(100, 100, 70, false, List.of()));
        assertEquals(Poaching.ACCEPT_MAX_PERCENT, Poaching.acceptPercent(100, 0, 10, true, List.of(Trait.RESTLESS)));
        assertEquals(5, Poaching.acceptPercent(25, 50, 70, false, List.of(Trait.STEADY)));
    }

    @Test
    void aFileTellsWhenSomeoneCanBeApproached() {
        PersonnelFile file = new PersonnelFile("p5", "Kozlov", "", "SET", 34, 10, List.of(), null, "Low", null, false,
            36, 2, 9);
        // Filed at turn 10 after 2 seasons: 3 by turn 11, 4 by turn 12, but approached at turn 9.
        assertEquals(Poaching.Blocker.TOO_NEW, Poaching.blocker(file, 11));
        assertEquals(Poaching.Blocker.APPROACHED_RECENTLY, Poaching.blocker(file, 12));
        assertNull(Poaching.blocker(file, Poaching.approachableAgain(9)));
        assertNotNull(Poaching.blocker(new PersonnelFile("p1", "Pilot", "", "ASTRONAUT", 30, 10, List.of(), null),
            20));
    }
}
