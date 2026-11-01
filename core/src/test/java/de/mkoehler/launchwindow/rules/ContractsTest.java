package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.actions.ApplyContractAction;
import de.mkoehler.launchwindow.content.BudgetSchedule;
import de.mkoehler.launchwindow.content.CommercialClients;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.ActiveContract;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.ContractApplication;
import de.mkoehler.launchwindow.model.ContractListing;
import de.mkoehler.launchwindow.model.ContractRecord;
import de.mkoehler.launchwindow.model.InProgressMission;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.persistence.ActionContext;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests commercial contracts ({@code design.md} &sect;2.11): unlocking a line, applying, the
 * market's awards and board, and a contract from the award to its flight's end or its lapse.
 *
 * @author Mario Koehler
 */
class ContractsTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final ActionContext CONTEXT = new ActionContext(CATALOG, BudgetSchedule.baspmClose());
    private static final String LINE = "earth-orbit-satellites";
    private static final ContractListing LISTING = new ContractListing("c1-1", LINE, "n1", "s1", 2, 4);

    /** Every roll at the top of its range: every step succeeds. */
    private static final RandomGenerator ALWAYS = () -> -1L;
    /** Every roll at the bottom: the first step fails. */
    private static final RandomGenerator NEVER = () -> 0L;

    private static PlayerSnapshot nasa(String name, double reliability) {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, Archetype.NASA, 50_000, 500, 0, 1);
        snapshot.getOpenedProgramIds().add("explorer-1");
        snapshot.getMissionComponents().put("explorer-1-satellite", new OwnedMissionComponent(reliability, 3));
        snapshot.getMissionComponents().put("jupiter-c", new OwnedMissionComponent(reliability, 3));
        snapshot.getSuccessfulFlightCounts().put("explorer-1-orbit", 1);
        snapshot.setContractBoard(List.of(LISTING));
        return snapshot;
    }

    private static ActiveContract contract(String missionId) {
        return new ActiveContract(LISTING.id(), LINE, "n1", "explorer-1", "explorer-1-orbit", 1, 2808, 2, 5, missionId);
    }

    @Test
    void flyingTheUnlockSetOpensTheLineAndAnOpenProgramQualifies() {
        PlayerSnapshot snapshot = nasa("A", 80);
        snapshot.getSuccessfulFlightCounts().clear();
        assertTrue(Contracts.unlockedLines(snapshot, CATALOG).isEmpty());

        InProgressMission mission = new InProgressMission("m1", "explorer-1-orbit", List.of(), List.of());
        mission.setRocketIds(List.of("jupiter-c"));
        snapshot.getInProgressMissions().put("m1", mission);
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, ALWAYS, new GoalTally(), 1);

        assertEquals(List.of(new ContractLineUnlocked(LINE, "explorer-1", true)),
            events.stream().filter(ContractLineUnlocked.class::isInstance).toList());
        assertEquals(List.of(LINE), Contracts.qualifiedLines(snapshot, CATALOG));
        snapshot.getOpenedProgramIds().clear();
        assertEquals(List.of(LINE), Contracts.unlockedLines(snapshot, CATALOG), "unlocked for good");
        assertTrue(Contracts.qualifiedLines(snapshot, CATALOG).isEmpty(), "but it qualifies only while open");
    }

    @Test
    void applyingIsCheckedAndOneApplicationReplacesTheLast() {
        PlayerSnapshot snapshot = nasa("A", 80);
        assertEquals(ContractOutcome.UNKNOWN_LISTING, Contracts.apply(snapshot, "nope", null, CATALOG));
        assertEquals(ContractOutcome.NOT_QUALIFIED, Contracts.apply(snapshot, LISTING.id(), "mercury", CATALOG));
        snapshot.getOpenedProgramIds().clear();
        assertEquals(ContractOutcome.PROGRAM_NOT_OPEN, Contracts.apply(snapshot, LISTING.id(), null, CATALOG));
        snapshot.getOpenedProgramIds().add("explorer-1");

        assertTrue(new ApplyContractAction(LISTING.id(), null).apply(snapshot, CONTEXT).applied());
        assertEquals(new ContractApplication(LISTING.id(), "explorer-1"), snapshot.getContractApplication());
        assertTrue(new ApplyContractAction(null, null).apply(snapshot, CONTEXT).applied());
        assertNull(snapshot.getContractApplication(), "withdrawn");

        snapshot.setActiveContract(contract(null));
        assertEquals(ContractOutcome.CONTRACT_ACTIVE, Contracts.apply(snapshot, LISTING.id(), null, CATALOG));

        PlayerSnapshot soviet = new PlayerSnapshot("S", Archetype.SOVIET, 50_000, 500, 0, 1);
        soviet.setContractBoard(List.of(LISTING));
        assertEquals(ContractOutcome.NOT_QUALIFIED, Contracts.apply(soviet, LISTING.id(), null, CATALOG));
    }

    @Test
    void theBestRecordsWinTheTiersAndTheFourthMissesOut() {
        Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
        String[] names = {"Delta", "Bravo", "Alpha", "Charlie"};
        double[] reliability = {60, 90, 70, 80};
        for (int i = 0; i < names.length; i++) {
            PlayerSnapshot agency = nasa(names[i], reliability[i]);
            Contracts.apply(agency, LISTING.id(), null, CATALOG);
            agencies.put("k" + i, agency);
        }
        // A record lifts Delta past Alpha: 60 + 15 against 70.
        agencies.get("k0").getContractRecords().put(LINE, new ContractRecord(5, 0, 0));
        assertEquals(75, Contracts.score(agencies.get("k0"), "explorer-1", CATALOG));

        ContractMarket.Settlement settlement = ContractMarket.settle(agencies, Map.of(), List.of(LISTING), List.of(), CATALOG,
            CommercialClients.EMPTY, 7, 2);

        long launchCost = 585 + 1170;
        assertEquals(launchCost, Contracts.launchCost(agencies.get("k1"), "explorer-1-orbit", CATALOG));
        assertAward(settlement, "k1", 1, launchCost * 160 / 100);
        assertAward(settlement, "k3", 2, launchCost * 140 / 100);
        assertAward(settlement, "k0", 3, launchCost * 125 / 100);
        assertEquals(ContractFinding.Kind.MISSED, settlement.findings().get("k2").getFirst().kind());
        ContractFinding news = settlement.findings().get("k2").getLast();
        assertEquals(ContractFinding.Kind.NEWS, news.kind());
        assertEquals(List.of("Bravo", "Charlie", "Delta"), news.winners(), "alphabetical: the ranking stays sealed");
        assertTrue(settlement.board().stream().noneMatch(listing -> listing.id().equals(LISTING.id())));

        PlayerSnapshot winner = agencies.get("k1");
        List<ResolutionEvent> events = Contracts.advanceSeason(winner, settlement.turnFor("k1"), 2);
        assertEquals(new ActiveContract(LISTING.id(), LINE, "n1", "explorer-1", "explorer-1-orbit", 1,
            launchCost * 160 / 100, 2, 5, null), winner.getActiveContract());
        assertNull(winner.getContractApplication());
        assertEquals(ContractSettled.Kind.AWARDED, ((ContractSettled) events.getFirst()).kind());
    }

    private static void assertAward(ContractMarket.Settlement settlement, String key, int tier, long fee) {
        ContractFinding award = settlement.findings().get(key).getFirst();
        assertEquals(ContractFinding.Kind.AWARDED, award.kind());
        assertEquals(tier, award.tier());
        assertEquals(fee, award.fee());
    }

    @Test
    void anApplicationWhoseProgramClosedComesToNothing() {
        PlayerSnapshot agency = nasa("A", 80);
        Contracts.apply(agency, LISTING.id(), null, CATALOG);
        agency.getOpenedProgramIds().clear();
        ContractMarket.Settlement settlement = ContractMarket.settle(Map.of("k", agency), Map.of(), List.of(LISTING),
            List.of(), CATALOG, CommercialClients.EMPTY, 7, 2);
        assertEquals(ContractFinding.Kind.VOIDED, settlement.findings().get("k").getFirst().kind());
        assertTrue(settlement.board().isEmpty(), "no line anyone qualifies for, so nothing listed");
    }

    @Test
    void theBoardSeatsEveryoneWhoQualifiesAndAddsAnExtra() {
        Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            agencies.put("k" + i, nasa("A" + i, 80));
        }
        PlayerSnapshot busy = nasa("Busy", 80);
        busy.setActiveContract(contract("m"));
        agencies.put("busy", busy);
        agencies.put("none", new PlayerSnapshot("None", Archetype.NASA, 0, 0, 0, 1));

        ContractMarket.Settlement settlement = ContractMarket.settle(agencies, Map.of(), List.of(), List.of(), CATALOG,
            CommercialClients.EMPTY, 7, 2);
        // Five eligible agencies in one line: two listings seat them, plus 1 + 5 / 4 extras.
        assertEquals(4, settlement.board().size());
        assertEquals(4, settlement.posted());
        assertTrue(settlement.board().stream().allMatch(listing -> listing.lineId().equals(LINE)));
        ContractListing first = settlement.board().getFirst();
        assertEquals(3, first.postedTurn());
        assertEquals(2 + Contracts.LISTING_TURNS, first.lastTurn());

        ContractMarket.Settlement again = ContractMarket.settle(agencies, Map.of(), List.of(), List.of(), CATALOG,
            CommercialClients.EMPTY, 7, 2);
        assertEquals(settlement, again, "the same seed settles the same market");

        // Next turn the listings still stand, so nothing new is needed.
        ContractMarket.Settlement next = ContractMarket.settle(agencies, Map.of(), settlement.board(), List.of(), CATALOG,
            CommercialClients.EMPTY, 7, 3);
        assertEquals(settlement.board(), next.board());
        assertEquals(0, next.posted());
        // And once they expire, new ones replace them.
        ContractMarket.Settlement later = ContractMarket.settle(agencies, Map.of(), settlement.board(), List.of(), CATALOG,
            CommercialClients.EMPTY, 7, 5);
        assertTrue(later.board().stream().allMatch(listing -> listing.postedTurn() == 6));
    }

    @Test
    void theContractFlightPaysTheFeeNotPrestigeAndDoesNotCountAsARepeat() {
        PlayerSnapshot snapshot = nasa("A", 90);
        snapshot.setActiveContract(contract(null));
        InProgressMission mission = new InProgressMission("m1", "explorer-1-orbit", List.of(), List.of());
        mission.setRocketIds(List.of("jupiter-c"));
        mission.setLaunchCost(1755);
        snapshot.getInProgressMissions().put("m1", mission);
        Contracts.launched(snapshot, mission);
        assertTrue(mission.isContract());
        assertEquals("m1", snapshot.getActiveContract().missionId());

        long funds = snapshot.getFunds();
        long prestige = snapshot.getPrestige();
        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, ALWAYS, new GoalTally(), 3);

        assertEquals(funds + 2808, snapshot.getFunds());
        assertEquals(prestige, snapshot.getPrestige());
        assertEquals(1, snapshot.getSuccessfulFlightCounts().get("explorer-1-orbit"));
        assertEquals(1, snapshot.getCommercialFlightCounts().get("explorer-1-orbit"), "the Museum counts it");
        assertNull(snapshot.getActiveContract());
        assertEquals(new ContractRecord(1, 0, 0), snapshot.getContractRecords().get(LINE));
        ContractSettled paid = (ContractSettled) events.stream().filter(ContractSettled.class::isInstance)
            .findFirst().orElseThrow();
        assertEquals(ContractSettled.Kind.PAID, paid.kind());
        assertEquals(2808, paid.funds());
    }

    @Test
    void aFailedContractFlightIsInsuredButCostsItsPrestige() {
        PlayerSnapshot snapshot = nasa("A", 90);
        snapshot.setActiveContract(contract("m1"));
        InProgressMission mission = new InProgressMission("m1", "explorer-1-orbit", List.of(), List.of());
        mission.setRocketIds(List.of("jupiter-c"));
        mission.setLaunchCost(1755);
        mission.setContract(true);
        snapshot.getInProgressMissions().put("m1", mission);
        long funds = snapshot.getFunds();
        long prestige = snapshot.getPrestige();

        List<ResolutionEvent> events = MissionProgress.advanceSeason(snapshot, CATALOG, NEVER, new GoalTally(), 3);

        assertEquals(funds + 1755, snapshot.getFunds(), "the insurer refunds the launch cost");
        assertTrue(snapshot.getPrestige() < prestige, "the failure costs its prestige as usual");
        assertEquals(new ContractRecord(0, 1, 0), snapshot.getContractRecords().get(LINE));
        assertTrue(events.stream().anyMatch(event -> event instanceof ContractSettled settled
            && settled.kind() == ContractSettled.Kind.REFUNDED));
    }

    @Test
    void aScrubKeepsTheContractAndAMissedWindowLapsesIt() {
        PlayerSnapshot snapshot = nasa("A", 90);
        snapshot.setActiveContract(contract(null));
        InProgressMission mission = new InProgressMission("m1", "explorer-1-orbit", List.of(), List.of());
        mission.setRocketIds(List.of("jupiter-c"));
        snapshot.getInProgressMissions().put("m1", mission);
        Contracts.launched(snapshot, mission);
        assertEquals(MissionCancellation.Outcome.SCRUBBED, MissionCancellation.cancel(snapshot, "m1", CATALOG));
        assertNotNull(snapshot.getActiveContract());
        assertNull(snapshot.getActiveContract().missionId());

        assertTrue(Contracts.advanceSeason(snapshot, Contracts.Turn.NONE, 4).isEmpty(), "the window is open");
        long prestige = snapshot.getPrestige();
        List<ResolutionEvent> events = Contracts.advanceSeason(snapshot, Contracts.Turn.NONE, 5);
        assertEquals(ContractSettled.Kind.LAPSED, ((ContractSettled) events.getFirst()).kind());
        assertEquals(prestige - Contracts.LAPSE_PRESTIGE, snapshot.getPrestige());
        assertEquals(new ContractRecord(0, 0, 1), snapshot.getContractRecords().get(LINE));
        assertFalse(snapshot.getActiveContract() != null);
    }

    @Test
    void theRecordsPointsStayWithinTheirCap() {
        assertEquals(0, Contracts.recordPoints(ContractRecord.NONE));
        assertEquals(Contracts.RECORD_CAP, Contracts.recordPoints(new ContractRecord(9, 0, 0)));
        assertEquals(-Contracts.RECORD_CAP, Contracts.recordPoints(new ContractRecord(0, 2, 2)));
        assertEquals(3 - 6, Contracts.recordPoints(new ContractRecord(1, 0, 1)));
    }
}
