package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.rules.Findings;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@code assets/content/investigation-causes.json} ({@code design.md} &sect;2.14(c)): every phase and
 * group a failing step of the standard pack can land on has causes, a lost crew always has one that can
 * explain it, the words use only their tokens and never blame a crew, and no cause repeats before its group
 * is used up.
 *
 * @author Mario Koehler
 */
class InvestigationCausesTest {

    private static final InvestigationCauses CAUSES = InvestigationCauses.load(Path.of("..", "assets", "content",
        InvestigationCauses.FILE));
    private static final ContentCatalog STANDARD = ContentPackLoader.load(Path.of("..", "assets", "content",
        "standard"));
    private static final Pattern TOKEN = Pattern.compile("\\{([a-z]+)}");

    @Test
    void idsAreUniqueAndEveryCauseHasItsWords() {
        Set<String> ids = new HashSet<>();
        for (InvestigationCauses.Cause cause : CAUSES.causes()) {
            assertTrue(ids.add(cause.id()), "duplicate " + cause.id());
            assertFalse(cause.shortly().isBlank(), cause.id());
            assertFalse(cause.finding().isBlank(), cause.id());
            assertFalse(cause.fix().isBlank(), cause.id());
            for (String phase : cause.phases()) {
                assertTrue(phaseIds().contains(phase), cause.id() + " names an unknown phase " + phase);
            }
        }
        CAUSES.factors().forEach(f -> assertTrue(ids.add(f.id()), "duplicate " + f.id()));
        CAUSES.boardStories().forEach(s -> assertTrue(ids.add(s.id()), "duplicate " + s.id()));
        assertFalse(CAUSES.factors().isEmpty());
        assertFalse(CAUSES.boardStories().isEmpty());
    }

    @Test
    void theWordsUseOnlyTheirTokens() {
        for (InvestigationCauses.Cause cause : CAUSES.causes()) {
            assertTokens(cause.id(), Set.of("component"), cause.shortly(), cause.finding(), cause.fix());
        }
        for (InvestigationCauses.Factor factor : CAUSES.factors()) {
            assertTokens(factor.id(), Set.of(), factor.text());
        }
        for (InvestigationCauses.BoardStory story : CAUSES.boardStories()) {
            assertTokens(story.id(), Set.of("mission", "program", "component", "cause", "season", "agency", "crew"),
                story.headline(), story.deck(), story.lead(), story.close());
        }
    }

    /** The user's rule (2026-11-14): a report never blames the crew. */
    @Test
    void noCauseBlamesTheCrew() {
        Pattern blame = Pattern.compile("(?i)\\b(crew|pilot|astronaut|cosmonaut|human error)");
        for (InvestigationCauses.Cause cause : CAUSES.causes()) {
            String words = cause.shortly() + " " + cause.finding() + " " + cause.fix();
            assertFalse(blame.matcher(words).find(), cause.id() + ": " + words);
        }
    }

    /** Every step the standard pack can fail on finds causes for each component group it involves. */
    @Test
    void everyFailingStepFindsACause() {
        Set<String> missing = new TreeSet<>();
        for (MissionConfiguration configuration : STANDARD.missionConfigurations()) {
            for (MissionSeason season : configuration.seasons()) {
                for (MissionStep step : season.steps()) {
                    String phase = CAUSES.phaseOf(Findings.kind(step.id()),
                        Findings.body(STANDARD, configuration).name(), configuration.programId());
                    for (MissionStep.ComponentInvolvement involvement : step.componentInvolvements()) {
                        for (InvestigationCauses.Group group : groups(involvement.componentId())) {
                            if (CAUSES.eligible(group, phase, false).isEmpty()) {
                                missing.add(group + " in " + phase);
                            }
                            if (step.crewRisk() && CAUSES.eligible(group, phase, true).isEmpty()) {
                                missing.add(group + " in " + phase + " (a lost crew)");
                            }
                        }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(), "no causes for " + missing);
    }

    @Test
    void stepKindsFallIntoTheirPhases() {
        assertEquals("ground", CAUSES.phaseOf("countdown"));
        assertEquals("launch", CAUSES.phaseOf("ascent"));
        assertEquals("launch", CAUSES.phaseOf("atv-launch"));
        assertEquals("ground", CAUSES.phaseOf("atv-launch-preparations"));
        assertEquals("landing", CAUSES.phaseOf("retrofire-jettison"));
        assertEquals("separation", CAUSES.phaseOf("modules-separation"));
        assertEquals("eva", CAUSES.phaseOf("eva-7k-ok"));
        assertEquals("space", CAUSES.phaseOf("trans-lunar-coast"));
        assertEquals("surface-landing", CAUSES.phaseOf("powered-descent"));
        assertEquals("surface-liftoff", CAUSES.phaseOf("lunar-liftoff"));
        assertEquals("surface-landing", CAUSES.phaseOf("landing", "MOON", "apollo"));
        assertEquals("planet-entry", CAUSES.phaseOf("landing", "VENUS", "venera-lander"));
        assertEquals("winged", CAUSES.phaseOf("landing", "EARTH", "mig-e-154"));
        assertEquals("landing", CAUSES.phaseOf("descent-and-landing", "MOON", "zond-5"));
        assertEquals("surface-landing", CAUSES.phaseOf("descent-and-landing", "MOON", "luna-15"));
        assertEquals("winged", CAUSES.phaseOf("powered-ascent"));
    }

    @Test
    void noCauseRepeatsBeforeItsPoolIsUsedUp() {
        List<InvestigationCauses.Cause> pool = CAUSES.eligible(InvestigationCauses.Group.ROCKET, "launch", false);
        List<String> used = new ArrayList<>();
        SplittableRandom random = new SplittableRandom(7);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < pool.size(); i++) {
            assertTrue(seen.add(CAUSES.pick(InvestigationCauses.Group.ROCKET, "launch", false, random, used)),
                "repeated before the pool was used up");
        }
        String again = CAUSES.pick(InvestigationCauses.Group.ROCKET, "launch", false, random, used);
        assertTrue(seen.contains(again), "starts over once used up");
        assertEquals(1, used.stream().filter(id -> pool.stream().anyMatch(c -> c.id().equals(id))).count());
    }

    private List<InvestigationCauses.Group> groups(String componentId) {
        if (componentId.startsWith("@rocket")) {
            return List.of(InvestigationCauses.Group.ROCKET);
        }
        return List.of(InvestigationCauses.Group.of(STANDARD.missionComponent(componentId).orElseThrow()
            .associatedSkill()));
    }

    private static Set<String> phaseIds() {
        Set<String> ids = new HashSet<>();
        CAUSES.phases().forEach(p -> ids.add(p.id()));
        ids.add(CAUSES.defaultPhase());
        return ids;
    }

    private static void assertTokens(String id, Set<String> allowed, String... texts) {
        for (String text : texts) {
            Matcher matcher = TOKEN.matcher(text);
            while (matcher.find()) {
                assertTrue(allowed.contains(matcher.group(1)), id + " uses {" + matcher.group(1) + "}");
            }
        }
    }
}
