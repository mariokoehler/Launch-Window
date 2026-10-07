package de.mkoehler.launchwindow.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mkoehler.launchwindow.actions.BackgroundCheckAction;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.IntelArea;
import de.mkoehler.launchwindow.model.IntelFact;
import de.mkoehler.launchwindow.model.IntelOperation;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.Personnel;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import de.mkoehler.launchwindow.model.RivalFolder;
import de.mkoehler.launchwindow.model.SetSpecialist;
import de.mkoehler.launchwindow.model.Trait;
import de.mkoehler.launchwindow.model.TraitState;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the intelligence service ({@code design.md} &sect;2.9): the level's one step a season, the
 * actions, the background check and the passive find, the turn's settlement, and what reaches the
 * client.
 *
 * @author Mario Koehler
 */
class IntelligenceTest {

    private static final ContentCatalog CATALOG =
        ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static PlayerSnapshot agency(String name, int level) {
        PlayerSnapshot snapshot = new PlayerSnapshot(name, Archetype.SOVIET, 25_000, 500, 0, 1);
        snapshot.getIntelligence().setLevel(level);
        snapshot.getIntelligence().setTargetLevel(level);
        return snapshot;
    }

    private static PlayerSnapshot copy(PlayerSnapshot snapshot) throws Exception {
        return MAPPER.readValue(MAPPER.writeValueAsBytes(snapshot), PlayerSnapshot.class);
    }

    private static SetSpecialist candidate(PlayerSnapshot snapshot, String id, Trait... traits) {
        SetSpecialist candidate = new SetSpecialist(id, "Test " + id, 30, 70, 50);
        candidate.setTraits(java.util.Arrays.stream(traits).map(trait -> new TraitState(trait, 3, false)).toList());
        snapshot.getSetSpecialistCandidates().add(candidate);
        return candidate;
    }

    @Test
    void theLevelMovesOneStepASeasonTowardItsTarget() {
        PlayerSnapshot snapshot = agency("A", 0);
        assertEquals(Intelligence.Outcome.TARGET_SET, Intelligence.setTarget(snapshot, 3));
        assertEquals(Intelligence.Outcome.INVALID_LEVEL, Intelligence.setTarget(snapshot, 4));
        assertEquals(0, snapshot.getIntelligence().getLevel(), "a change only takes effect at the resolution");
        for (int expected = 1; expected <= 3; expected++) {
            Intelligence.advanceSeason(snapshot, List.of(), expected);
            assertEquals(expected, snapshot.getIntelligence().getLevel());
        }
        Intelligence.setTarget(snapshot, 0);
        Intelligence.advanceSeason(snapshot, List.of(), 4);
        assertEquals(2, snapshot.getIntelligence().getLevel(), "winding down takes time too");
    }

    @Test
    void anAgencyInDebtLosesALevel() {
        PlayerSnapshot snapshot = agency("A", 2);
        snapshot.setFunds(-100);
        List<ResolutionEvent> events = Intelligence.advanceSeason(snapshot, List.of(), 1);
        assertEquals(1, snapshot.getIntelligence().getLevel());
        assertTrue(((IntelligenceLevelChanged) events.getLast()).debt());
    }

    @Test
    void theLevelIsPaidWithTheRunningCosts() {
        PlayerSnapshot snapshot = agency("A", 0);
        long base = BudgetProgress.runningCosts(snapshot, CATALOG);
        snapshot.getIntelligence().setLevel(2);
        assertEquals(base + Intelligence.COST_PER_SEASON[2], BudgetProgress.runningCosts(snapshot, CATALOG));
        assertEquals(Intelligence.COST_PER_SEASON[2],
            BudgetProgress.runningCostBreakdown(snapshot, CATALOG).intelligence());
    }

    @Test
    void operationsNeedTheirLevelAndAnActionAndCancellingGivesItBack() {
        PlayerSnapshot snapshot = agency("A", 1);
        assertEquals(Intelligence.Outcome.LEVEL_TOO_LOW,
            Intelligence.queue(snapshot, IntelOperation.Kind.ASSESSMENT, "b", IntelArea.RND, null, null));
        snapshot.getIntelligence().setLevel(2);
        assertEquals(Intelligence.Outcome.INVALID_OPERATION,
            Intelligence.queue(snapshot, IntelOperation.Kind.ASSESSMENT, "b", null, null, null));
        assertEquals(Intelligence.Outcome.QUEUED,
            Intelligence.queue(snapshot, IntelOperation.Kind.ASSESSMENT, "b", IntelArea.RND, null, null));
        assertEquals(Intelligence.Outcome.LEVEL_TOO_LOW,
            Intelligence.queue(snapshot, IntelOperation.Kind.PERSONNEL_FILE, "b", null, "SET", null));
        assertEquals(Intelligence.Outcome.QUEUED,
            Intelligence.queue(snapshot, IntelOperation.Kind.ASSESSMENT, "b", IntelArea.PLANS, null, null));
        assertEquals(Intelligence.Outcome.NO_ACTION_LEFT,
            Intelligence.queue(snapshot, IntelOperation.Kind.ASSESSMENT, "b", IntelArea.PERSONNEL, null, null));
        String first = snapshot.getIntelligence().getQueued().getFirst().id();
        assertEquals(Intelligence.Outcome.CANCELLED, Intelligence.cancel(snapshot, first));
        assertEquals(1, Intelligence.actionsLeft(snapshot));
        assertEquals(Intelligence.Outcome.UNKNOWN_OPERATION, Intelligence.cancel(snapshot, first));
    }

    @Test
    void aBackgroundCheckAnswersAtOnceAndTheSameWayEveryTime() throws Exception {
        PlayerSnapshot snapshot = agency("A", 2);
        candidate(snapshot, "c1", Trait.WORKAHOLIC, Trait.STEADY);
        PlayerSnapshot replayed = copy(snapshot);
        assertTrue(new BackgroundCheckAction("c1").apply(snapshot, null).applied());
        assertTrue(new BackgroundCheckAction("c1").apply(replayed, null).applied());
        Personnel checked = snapshot.getSetSpecialistCandidates().getFirst();
        assertEquals(checked.getTraits(), replayed.getSetSpecialistCandidates().getFirst().getTraits(),
            "replaying the journal reveals the same trait");
        assertEquals(1, checked.getTraits().stream().filter(TraitState::revealed).count());
        assertEquals(Personnel.FIND_CHECK, checked.getIntelFind());

        assertEquals(Intelligence.Outcome.CHECKED, Intelligence.backgroundCheck(snapshot, "c1"));
        assertEquals(2, checked.getTraits().stream().filter(TraitState::revealed).count(), "a second check finds another");
        assertEquals(Intelligence.Outcome.NO_ACTION_LEFT, Intelligence.backgroundCheck(snapshot, "c1"));
    }

    @Test
    void aCheckOnSomeoneWithoutTraitsSaysSoAndCantBeRepeated() {
        PlayerSnapshot snapshot = agency("A", 2);
        candidate(snapshot, "c1");
        assertEquals(Intelligence.Outcome.CHECKED, Intelligence.backgroundCheck(snapshot, "c1"));
        Personnel checked = snapshot.getSetSpecialistCandidates().getFirst();
        assertTrue(checked.isIntelExhausted());
        assertEquals(Intelligence.Outcome.NOTHING_LEFT_TO_FIND, Intelligence.backgroundCheck(snapshot, "c1"));
        assertEquals(1, Intelligence.actionsLeft(snapshot), "a refused check costs nothing");
        assertEquals(Intelligence.Outcome.UNKNOWN_CANDIDATE, Intelligence.backgroundCheck(snapshot, "nobody"));
        PlayerSnapshot none = agency("B", 0);
        candidate(none, "c1");
        assertEquals(Intelligence.Outcome.LEVEL_TOO_LOW, Intelligence.backgroundCheck(none, "c1"));
    }

    @Test
    void thePassiveFindLeavesThePoolsDrawUntouched() {
        PlayerSnapshot without = agency("A", 0);
        PlayerSnapshot with = agency("A", 1);
        HiringPool.refreshCandidates(without, new SplittableRandom(7));
        HiringPool.refreshCandidates(with, new SplittableRandom(7));
        int finds = 0;
        for (int i = 0; i < without.getSetSpecialistCandidates().size(); i++) {
            SetSpecialist plain = without.getSetSpecialistCandidates().get(i);
            SetSpecialist found = with.getSetSpecialistCandidates().get(i);
            assertEquals(plain.getSkills(), found.getSkills());
            assertEquals(plain.getTraits().stream().map(TraitState::trait).toList(),
                found.getTraits().stream().map(TraitState::trait).toList());
            assertNull(plain.getIntelFind());
            finds += found.getIntelFind() == null ? 0 : 1;
        }
        int total = 0;
        int hits = 0;
        for (int seed = 0; seed < 200; seed++) {
            PlayerSnapshot snapshot = agency("Pool " + seed, 1);
            HiringPool.refreshCandidates(snapshot, new SplittableRandom(seed));
            for (Personnel candidate : Intelligence.candidates(snapshot).toList()) {
                total++;
                hits += candidate.getIntelFind() == null ? 0 : 1;
            }
        }
        assertTrue(Math.abs(hits * 100.0 / total - Intelligence.PASSIVE_CANDIDATE_PERCENT) < 3,
            hits + " of " + total + ", " + finds + " in the first pool");
    }

    @Test
    void revealedCandidateTraitsReachTheClientAndHiddenOnesDont() {
        PlayerSnapshot snapshot = agency("A", 1);
        candidate(snapshot, "c1", Trait.WORKAHOLIC, Trait.STEADY);
        candidate(snapshot, "c2", Trait.LONER);
        Intelligence.backgroundCheck(snapshot, "c1");
        PlayerSnapshot client = Personality.forClient(snapshot, MAPPER);
        assertEquals(1, client.getSetSpecialistCandidates().get(0).getTraits().size());
        assertTrue(client.getSetSpecialistCandidates().get(0).getTraits().getFirst().revealed());
        assertEquals(0, client.getSetSpecialistCandidates().get(1).getTraits().size());
    }

    private static Map<String, PlayerSnapshot> rivals(int spyLevel, int targetLevel) {
        PlayerSnapshot spy = agency("Spy Bureau", spyLevel);
        PlayerSnapshot target = agency("Target Works", targetLevel);
        target.getMissionComponents().put("r7-sputnik", new OwnedMissionComponent(81.4, 50));
        target.getOpenedProgramIds().add("sputnik");
        Astronaut star = new Astronaut("p1", "Vera Star", 30, 70, 50);
        for (AstronautSkill skill : AstronautSkill.values()) {
            star.setSkill(skill, 66);
        }
        star.setTraits(List.of(new TraitState(Trait.GLORY_SEEKER, 3, false)));
        target.getAstronauts().add(star);
        return new java.util.TreeMap<>(Map.of("spy", spy, "target", target));
    }

    @Test
    void theSettlementIsTheSameEveryTimeAndRangesHoldTheTruth() {
        int dossiers = 0;
        int caught = 0;
        for (long seed = 0; seed < 300; seed++) {
            Map<String, PlayerSnapshot> agencies = rivals(3, 1);
            Intelligence.queue(agencies.get("spy"), IntelOperation.Kind.ASSESSMENT, "target", IntelArea.RND, null, null);
            Intelligence.queue(agencies.get("spy"), IntelOperation.Kind.ASSESSMENT, "target", IntelArea.PLANS, null,
                null);
            Map<String, List<IntelFinding>> first = IntelligenceBoard.settle(agencies, CATALOG, seed, 5);
            assertEquals(first, IntelligenceBoard.settle(agencies, CATALOG, seed, 5));
            for (IntelFinding finding : first.getOrDefault("spy", List.of())) {
                if (finding.kind() == IntelFinding.Kind.DOSSIER) {
                    dossiers++;
                    for (IntelFact fact : finding.dossier().facts()) {
                        if (fact.kind() == IntelFact.Kind.COMPONENT_RELIABILITY) {
                            assertTrue(fact.low() <= 81 && fact.high() >= 81, fact.toString());
                            assertEquals(2 * Intelligence.halfWidth(3, 1), fact.high() - fact.low());
                        }
                    }
                    if (finding.area() == IntelArea.PLANS) {
                        assertEquals(1, finding.level(), "a plans dossier shows their level");
                    }
                }
                if (finding.kind() == IntelFinding.Kind.CAUGHT) {
                    caught++;
                    assertEquals(1, finding.level(), "being caught shows their counter-intelligence");
                    IntelFinding theirs = first.get("target").stream()
                        .filter(other -> other.kind() == IntelFinding.Kind.CAUGHT_THEIRS).findFirst().orElseThrow();
                    assertEquals(3, theirs.level(), "the catcher learns the spy's level");
                    assertEquals("spy", theirs.rivalId());
                }
            }
        }
        double success = dossiers / 600.0 * 100;
        assertTrue(Math.abs(success - Intelligence.successPercent(1)) < 6, "success " + success);
        assertTrue(caught > 0);
    }

    @Test
    void aPersonnelFileFindsTheGroupsBestAndAnUnknownTargetComesToNothing() {
        PersonnelFileProbe probe = new PersonnelFileProbe();
        for (long seed = 0; seed < 50 && probe.file == null; seed++) {
            Map<String, PlayerSnapshot> agencies = rivals(3, 0);
            Intelligence.queue(agencies.get("spy"), IntelOperation.Kind.PERSONNEL_FILE, "target", null, "ASTRONAUT",
                null);
            Intelligence.queue(agencies.get("spy"), IntelOperation.Kind.ASSESSMENT, "nobody", IntelArea.RND, null,
                null);
            List<IntelFinding> found = IntelligenceBoard.settle(agencies, CATALOG, seed, 5).get("spy");
            assertEquals(IntelFinding.Kind.NO_TARGET, found.get(1).kind());
            if (found.getFirst().kind() == IntelFinding.Kind.PERSONNEL_FILE) {
                probe.file = found.getFirst();
            }
        }
        assertNotNull(probe.file);
        assertEquals("Vera Star", probe.file.file().name());
        assertEquals(Trait.GLORY_SEEKER, probe.file.file().trait());
        assertEquals(AstronautSkill.values().length, probe.file.file().skills().size());
    }

    private static final class PersonnelFileProbe {
        private IntelFinding file;
    }

    @Test
    void thePassApplysFindingsAndFreesTheSeasonsActions() {
        PlayerSnapshot spy = agency("Spy Bureau", 2);
        Intelligence.queue(spy, IntelOperation.Kind.ASSESSMENT, "target", IntelArea.RND, null, null);
        Intelligence.queue(spy, IntelOperation.Kind.ASSESSMENT, "target", IntelArea.PLANS, null, null);
        IntelOperation blown = spy.getIntelligence().getQueued().getFirst();
        IntelFinding caught = IntelFinding.about(IntelFinding.Kind.CAUGHT, blown, "Target Works", 2);
        List<ResolutionEvent> events = Intelligence.advanceSeason(spy, List.of(caught), 9);
        assertEquals(500 - Intelligence.CAUGHT_PRESTIGE, spy.getPrestige());
        RivalFolder folder = spy.getIntelligence().getFolders().get("target");
        assertEquals(2, folder.getKnownLevel());
        assertEquals(RivalFolder.FROM_BEING_CAUGHT, folder.getKnownLevelSource());
        assertEquals(1, folder.getCatches().size());
        assertEquals(IntelFinding.Kind.NOT_SETTLED, ((IntelligenceFound) events.get(1)).finding().kind(),
            "an operation queued after the settlement comes to nothing, and says so");
        assertTrue(spy.getIntelligence().getQueued().isEmpty());
        assertEquals(2, Intelligence.actionsLeft(spy));
        assertFalse(events.stream().anyMatch(IntelligenceLevelChanged.class::isInstance));
    }

    @Test
    void foldersKeepOnlyTheNewestFilesAndCatches() {
        PlayerSnapshot spy = agency("Spy Bureau", 3);
        for (int i = 0; i < Intelligence.FILES_KEPT + 3; i++) {
            IntelFinding file = new IntelFinding(IntelFinding.Kind.PERSONNEL_FILE, "op" + i,
                IntelOperation.Kind.PERSONNEL_FILE, "target", "Target Works", null, null,
                new de.mkoehler.launchwindow.model.PersonnelFile("p" + i, "P " + i, null, "SET", 30, i, List.of(), null),
                null, -1);
            IntelFinding theirs = new IntelFinding(IntelFinding.Kind.CAUGHT_THEIRS, null, IntelOperation.Kind.ASSESSMENT,
                "target", "Target Works", IntelArea.RND, null, null, null, 2);
            Intelligence.advanceSeason(spy, List.of(file, theirs), i + 1);
        }
        RivalFolder folder = spy.getIntelligence().getFolders().get("target");
        assertEquals(Intelligence.FILES_KEPT, folder.getPersonnelFiles().size());
        assertEquals("p" + (Intelligence.FILES_KEPT + 2), folder.getPersonnelFiles().getLast().personId());
        assertTrue(folder.getCatches().size() <= Intelligence.CATCHES_KEPT);
    }

    @Test
    void aSeasonPassAtLevelZeroDoesNothing() {
        PlayerSnapshot snapshot = agency("A", 0);
        assertTrue(Intelligence.advanceSeason(snapshot, List.of(), 1).isEmpty());
        assertTrue(IntelligenceBoard.settle(Map.of("a", snapshot, "b", agency("B", 0)), CATALOG, 1, 1).isEmpty());
    }
}
