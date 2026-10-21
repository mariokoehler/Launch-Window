package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.CelestialBody;
import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.content.Finding;
import de.mkoehler.launchwindow.content.MissionConfiguration;
import de.mkoehler.launchwindow.content.MissionSeason;
import de.mkoehler.launchwindow.content.MissionStep;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.model.AssignmentState;
import de.mkoehler.launchwindow.model.Astronaut;
import de.mkoehler.launchwindow.model.AstronautSkill;
import de.mkoehler.launchwindow.model.MissionControlOfficer;
import de.mkoehler.launchwindow.model.MissionControlSkill;
import de.mkoehler.launchwindow.model.OwnedMissionComponent;
import de.mkoehler.launchwindow.model.PlayerSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests findings and publishing them ({@code design.md} &sect;2.13): the packs' mappings, the bonus in
 * the odds, the publish action and the turn's settlement.
 *
 * @author Mario Koehler
 */
class FindingsTest {

    private static final ContentCatalog EARLY = ContentPackLoader.load(Path.of("..", "assets", "content", "early-era"));

    private static ContentCatalog pack(String name) {
        return ContentPackLoader.load(Path.of("..", "assets", "content", name));
    }

    /**
     * A step's kind drops the numbering and the a/b of a two-craft flight.
     */
    @Test
    void kindDropsNumberingAndVariants() {
        assertEquals("reentry", Findings.kind("05-gemini-a-reentry"));
        assertEquals("reentry", Findings.kind("07-reentry-b"));
        assertEquals("reentry", Findings.kind("12-spc-b-reentry"));
        assertEquals("duration-level-ii", Findings.kind("09-duration-level-ii"));
        assertEquals("launch", Findings.kind("03-launch"));
    }

    /**
     * Every finding is a goal, and every kind each match names makes some step of the pack safer:
     * a kind that matches nothing is a typo. Prints what each finding matches, for review.
     *
     * @param name the pack
     */
    @ParameterizedTest
    @ValueSource(strings = {"early-era", "standard"})
    void everyKindMatchesAStep(String name) {
        ContentCatalog catalog = pack(name);
        assertFalse(catalog.findings().isEmpty());
        for (Finding finding : catalog.findings()) {
            assertTrue(catalog.goal(finding.goalId()).isPresent(), finding.goalId());
            Map<String, Integer> matched = new LinkedHashMap<>();
            for (Finding.StepMatch match : finding.matches()) {
                for (String kind : match.kinds()) {
                    Finding.StepMatch one = new Finding.StepMatch(List.of(kind), match.body(), match.crewed());
                    int count = 0;
                    for (MissionConfiguration configuration : catalog.missionConfigurations()) {
                        CelestialBody body = Findings.body(catalog, configuration);
                        for (MissionSeason season : configuration.seasons()) {
                            for (MissionStep step : season.steps()) {
                                if (one.matches(Findings.kind(step.id()), body, !step.crewInvolvements().isEmpty())) {
                                    count++;
                                }
                            }
                        }
                    }
                    matched.put(kind, count);
                }
            }
            System.out.println(name + " " + finding.goalId() + " " + matched);
            // In the early pack only some kinds have steps yet; every finding still has one.
            if (name.equals("standard")) {
                matched.forEach((kind, count) -> assertTrue(count > 0, finding.goalId() + " " + kind));
            } else {
                assertTrue(matched.values().stream().anyMatch(count -> count > 0), finding.goalId());
            }
        }
    }

    /**
     * A Soviet agency with Vostok, its capsule and rocket, both uncrewed Vostok flights flown, two
     * cosmonauts and ten controllers (as in {@code MissionAssessmentTest}).
     */
    private static PlayerSnapshot vostokAgency() {
        PlayerSnapshot state = new PlayerSnapshot("Vega", Archetype.SOVIET, 50_000, 0, 0, 1);
        state.getOpenedProgramIds().add("vostok");
        state.getMissionComponents().put("vostok-capsule", new OwnedMissionComponent(70.6, 8.7));
        state.getMissionComponents().put("r7-vostok", new OwnedMissionComponent(74.3, 3.0));
        state.getAchievedGoals().put("vostok-uncrewed-suborbital-flown", 3);
        state.getAchievedGoals().put("vostok-uncrewed-orbital-flown", 4);
        for (int i = 0; i < 2; i++) {
            Astronaut astronaut = new Astronaut("a" + i, "Cosmonaut " + i, 30, 70, 60);
            for (AstronautSkill skill : AstronautSkill.values()) {
                astronaut.setSkill(skill, 55.0 + 10 * i);
            }
            astronaut.setAssignmentState(AssignmentState.IDLE);
            state.getAstronauts().add(astronaut);
        }
        for (int i = 0; i < 10; i++) {
            MissionControlOfficer officer = new MissionControlOfficer("c" + i, "Controller " + i, 35, 70, 60);
            for (MissionControlSkill skill : MissionControlSkill.values()) {
                officer.setSkill(skill, 50.0 + i * 3);
            }
            officer.setAssignmentState(AssignmentState.IDLE);
            state.getMissionControlOfficers().add(officer);
        }
        return state;
    }

    private static MissionAssessment.Assessment assessOrbital(PlayerSnapshot state) {
        MissionConfiguration orbital = EARLY.missionConfiguration("vostok-orbital").orElseThrow();
        MissionStaffing.Staffing staffing = MissionStaffing.autoAssign(state, orbital);
        return MissionAssessment.assess(state, EARLY, orbital, staffing.crewSlotIds(), staffing.controlSlotIds(),
            List.of("r7-vostok"));
    }

    /**
     * A finding the agency holds adds its points to the base chance of every step it matches, and of
     * no other.
     */
    @Test
    void heldFindingRaisesItsSteps() {
        assertBonus(state -> state.getAchievedGoals().put("organisms-in-microgravity", 2));
    }

    /**
     * A published finding counts as known, though the agency never earned it.
     */
    @Test
    void publishedFindingRaisesItsSteps() {
        assertBonus(state -> state.getPublicFindings().put("organisms-in-microgravity", 2));
    }

    private static void assertBonus(java.util.function.Consumer<PlayerSnapshot> learn) {
        MissionAssessment.Assessment before = assessOrbital(vostokAgency());
        PlayerSnapshot knowing = vostokAgency();
        learn.accept(knowing);
        MissionAssessment.Assessment after = assessOrbital(knowing);
        int raised = 0;
        for (int i = 0; i < before.steps().size(); i++) {
            MissionAssessment.StepOdds plain = before.steps().get(i);
            MissionAssessment.StepOdds safer = after.steps().get(i);
            assertTrue(plain.findings().isEmpty());
            if (safer.findings().isEmpty()) {
                assertEquals(plain.success(), safer.success(), 1e-12, plain.step().id());
            } else {
                assertEquals(List.of("organisms-in-microgravity"), safer.findings());
                assertEquals(Math.min(1, plain.success() + Findings.BONUS_POINTS / 100), safer.success(), 1e-9,
                    plain.step().id());
                raised++;
            }
        }
        assertTrue(raised > 0);
        assertTrue(after.completion() > before.completion());
    }

    /**
     * Publishing needs the goal held and nobody to have published it; asking twice, or withdrawing
     * nothing, changes nothing.
     */
    @Test
    void publishAction() {
        PlayerSnapshot state = vostokAgency();
        assertEquals(Findings.PublishOutcome.NOT_A_FINDING, Findings.publish(state, "human-in-orbit", true, EARLY));
        assertEquals(Findings.PublishOutcome.NOT_KNOWN, Findings.publish(state, "magnetic-field-study", true, EARLY));
        state.getAchievedGoals().put("magnetic-field-study", 2);
        assertTrue(Findings.publishable(state, "magnetic-field-study", EARLY));
        assertEquals(Findings.PublishOutcome.QUEUED, Findings.publish(state, "magnetic-field-study", true, EARLY));
        assertFalse(Findings.publishable(state, "magnetic-field-study", EARLY));
        assertEquals(Findings.PublishOutcome.ALREADY_QUEUED,
            Findings.publish(state, "magnetic-field-study", true, EARLY));
        assertEquals(Findings.PublishOutcome.WITHDRAWN, Findings.publish(state, "magnetic-field-study", false, EARLY));
        assertEquals(Findings.PublishOutcome.NOT_QUEUED, Findings.publish(state, "magnetic-field-study", false, EARLY));
        state.getPublicFindings().put("magnetic-field-study", 3);
        assertEquals(Findings.PublishOutcome.ALREADY_PUBLIC,
            Findings.publish(state, "magnetic-field-study", true, EARLY));
    }

    private static PlayerSnapshot agency(String name, boolean holds) {
        PlayerSnapshot state = new PlayerSnapshot(name, Archetype.NASA, 0, 100, 0, 1);
        if (holds) {
            state.getAchievedGoals().put("organisms-in-microgravity", 3);
        }
        return state;
    }

    /**
     * One publisher among four agencies, one rival knowing the finding already: it helps two of
     * three rivals and is paid that share of the cap; everyone hears, and the asks are cleared.
     */
    @Test
    void settlementPaysTheShareOfRivalsHelped() {
        Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
        agencies.put("a", agency("Alpha", true));
        agencies.put("b", agency("Bravo", true));
        agencies.put("c", agency("Charlie", false));
        agencies.put("d", agency("Delta", false));
        Findings.publish(agencies.get("a"), "organisms-in-microgravity", true, EARLY);
        FindingPublications.Preview preview = FindingPublications.preview("a", "organisms-in-microgravity", agencies,
            EARLY);
        long expected = Math.round(800 * Findings.PRESTIGE_SHARE * 2 / 3);
        assertEquals(new FindingPublications.Preview(expected, 2, 3), preview);

        FindingPublications.Settlement settlement = FindingPublications.settle(agencies, EARLY);
        FindingPublications.News mine = settlement.turnFor("a").getFirst();
        assertTrue(mine.byYou());
        assertEquals(expected, mine.prestige());
        assertEquals(List.of(), mine.publishers());

        List<ResolutionEvent> published = Findings.advanceSeason(agencies.get("a"), settlement.turnFor("a"), 5);
        assertEquals(100 + expected, agencies.get("a").getPrestige());
        assertEquals(new FindingPublished("organisms-in-microgravity", expected, 2, 3, List.of()), published.getFirst());
        assertTrue(agencies.get("a").getPendingPublications().isEmpty());
        assertEquals(List.of("organisms-in-microgravity"), agencies.get("a").getPublishedFindings());

        List<ResolutionEvent> learned = Findings.advanceSeason(agencies.get("c"), settlement.turnFor("c"), 5);
        assertEquals(new FindingLearned("organisms-in-microgravity", List.of("Alpha"), false), learned.getFirst());
        assertEquals(5, agencies.get("c").getPublicFindings().get("organisms-in-microgravity"));
        assertEquals(100, agencies.get("c").getPrestige());
        assertTrue(Findings.knows(agencies.get("c"), "organisms-in-microgravity"));
        FindingLearned knew = assertInstanceOf(FindingLearned.class,
            Findings.advanceSeason(agencies.get("b"), settlement.turnFor("b"), 5).getFirst());
        assertTrue(knew.knewIt());

        // Published, it can't be published again: a later ask comes to nothing.
        agencies.get("b").getPendingPublications().add("organisms-in-microgravity");
        assertTrue(FindingPublications.settle(agencies, EARLY).turnFor("b").isEmpty());
    }

    /**
     * Two agencies publishing the same finding at the same resolution split the payment.
     */
    @Test
    void samePublicationSplitsThePayment() {
        Map<String, PlayerSnapshot> agencies = new LinkedHashMap<>();
        agencies.put("a", agency("Alpha", true));
        agencies.put("b", agency("Bravo", true));
        agencies.put("c", agency("Charlie", false));
        agencies.get("a").getPendingPublications().add("organisms-in-microgravity");
        agencies.get("b").getPendingPublications().add("organisms-in-microgravity");
        FindingPublications.Settlement settlement = FindingPublications.settle(agencies, EARLY);
        long pot = Math.round(800 * Findings.PRESTIGE_SHARE * 1 / 2);
        assertEquals(Math.round(pot / 2.0), settlement.turnFor("a").getFirst().prestige());
        assertEquals(List.of("Bravo"), settlement.turnFor("a").getFirst().publishers());
        assertEquals(List.of("Alpha", "Bravo"), settlement.turnFor("c").getFirst().publishers());
    }

    /**
     * Old news pays nothing, and so does a game without rivals.
     */
    @Test
    void paymentNeedsRivalsHelped() {
        var goal = EARLY.goal("organisms-in-microgravity").orElseThrow();
        assertEquals(0, Findings.payment(goal, 0, 9));
        assertEquals(0, Findings.payment(goal, 0, 0));
        assertEquals(Math.round(800 * Findings.PRESTIGE_SHARE), Findings.payment(goal, 9, 9));
        assertEquals(Math.round(800 * Findings.PRESTIGE_SHARE * 6 / 9), Findings.payment(goal, 6, 9));
    }

    /**
     * Vostok's orbital flight has steps a finding makes safer.
     */
    @Test
    void orbitalFlightHasFindingSteps() {
        MissionConfiguration orbital = EARLY.missionConfiguration("vostok-orbital").orElseThrow();
        List<String> all = new ArrayList<>();
        orbital.seasons().forEach(season -> season.steps().forEach(step ->
            all.addAll(Findings.matching(EARLY, orbital, step))));
        assertFalse(all.isEmpty());
    }
}
