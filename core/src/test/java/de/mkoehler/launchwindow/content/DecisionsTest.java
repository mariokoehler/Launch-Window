package de.mkoehler.launchwindow.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the R&amp;D decisions file: every type has a pool for rockets and for payloads, ids are unique, defaults take nobody
 * and cost nothing, knowledge and diagnosis have exactly one right answer, follow-ups resolve one
 * step on, explainers exist, the odds fit who an option takes, and placeholders fit their option.
 *
 * @author Mario Koehler
 */
class DecisionsTest {

    private static final Decisions DECISIONS = Decisions.load(Path.of("..", "assets", "content", Decisions.FILE));

    /**
     * Every type has a pool for each kind of component: at least {@link #POOL_FLOOR} first steps a rocket
     * can draw and as many a payload can (the pools grown 2026-10-25); ids are unique.
     */
    @Test
    void everyTypeHasDecisions() {
        Set<String> ids = new HashSet<>();
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            assertTrue(ids.add(decision.id()), "duplicate id " + decision.id());
        }
        for (Decisions.Type type : Decisions.Type.values()) {
            for (Decisions.Fits kind : List.of(Decisions.Fits.ROCKET, Decisions.Fits.PAYLOAD)) {
                long count = DECISIONS.decisions().stream().filter(decision -> decision.type() == type
                    && decision.step() == 1 && (decision.fits() == kind || decision.fits() == Decisions.Fits.ANY))
                    .count();
                assertTrue(count >= POOL_FLOOR, type + " has " + count + " for a " + kind);
            }
        }
    }

    /** The fewest first steps a type offers each kind of component. */
    private static final int POOL_FLOOR = 4;

    /** A default exists, takes nobody and costs nothing, so a lapse can always apply it. */
    @Test
    void defaultsTakeNobodyAndCostNothing() {
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            Decisions.Option fallback = decision.defaultChoice();
            assertEquals(Decisions.Takes.NOBODY, fallback.takes(), decision.id());
            assertEquals(0, fallback.cost(), decision.id());
            assertTrue(decision.options().size() >= 2 && decision.options().size() <= 4, decision.id());
            for (Decisions.Option option : decision.options()) {
                assertFalse(option.outcomes().isEmpty(), decision.id() + "/" + option.id());
            }
        }
    }

    /** Knowledge and diagnosis have exactly one option whose outcome is good. */
    @Test
    void oneRightAnswer() {
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            if (decision.type() != Decisions.Type.KNOWLEDGE && decision.type() != Decisions.Type.DIAGNOSIS) {
                continue;
            }
            long right = decision.options().stream().filter(option -> option.outcomes().size() == 1
                && option.outcomes().get(0).tone() == Decisions.Tone.GOOD).count();
            assertEquals(1, right, decision.id());
        }
    }

    /**
     * A follow-up names a decision one step on, of a component kind that fits; only a first step is
     * offered on its own; a continuing outcome belongs to a decision with a follow-up.
     */
    @Test
    void followUpsResolve() {
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            if (decision.followUp() != null) {
                Decisions.Decision next = DECISIONS.byId(decision.followUp()).orElseThrow(() ->
                    new AssertionError(decision.id() + " follows up with an unknown " + decision.followUp()));
                assertEquals(decision.step() + 1, next.step(), decision.id());
                assertFalse(next.recall().isEmpty(), next.id() + " recalls nothing");
            }
            boolean continues = decision.options().stream().flatMap(option -> option.outcomes().stream())
                .anyMatch(Decisions.Outcome::continues);
            assertEquals(decision.followUp() != null, continues, decision.id());
        }
    }

    /** The odds and the people an option takes go together; placeholders fit who it takes. */
    @Test
    void oddsAndPlaceholdersFitTheOption() {
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            for (Decisions.Option option : decision.options()) {
                if (option.odds() == Decisions.Odds.SKILL) {
                    assertEquals(Decisions.Takes.OVERSEER, option.takes(), decision.id());
                }
                if (option.odds() == Decisions.Odds.RAPPORT) {
                    assertEquals(Decisions.Takes.PAIR, option.takes(), decision.id());
                }
                for (Decisions.Outcome outcome : option.outcomes()) {
                    String text = outcome.headline() + " " + outcome.text();
                    if (option.takes() == Decisions.Takes.NOBODY) {
                        assertFalse(text.contains("{name}"), decision.id() + "/" + outcome.id());
                    }
                    if (option.takes() != Decisions.Takes.PAIR) {
                        assertFalse(text.contains("{second}"), decision.id() + "/" + outcome.id());
                    }
                    String rest = text.replace("{name}", "").replace("{second}", "");
                    assertFalse(rest.contains("{") || rest.contains("}"), decision.id() + "/" + outcome.id());
                }
            }
            String story = decision.headline() + decision.text() + decision.stakes();
            assertFalse(story.contains("{"), decision.id());
        }
    }

    /** Every explainer a decision links exists. */
    @Test
    void explainersExist() throws IOException {
        JsonNode file = new ObjectMapper().readTree(Path.of("..", "assets", "content", "explainers.json").toFile());
        Set<String> known = new HashSet<>();
        JsonNode list = file.has("explainers") ? file.get("explainers") : file;
        list.forEach(node -> known.add(node.get("id").asText()));
        for (Decisions.Decision decision : DECISIONS.decisions()) {
            for (String explainer : decision.explainers()) {
                assertTrue(known.contains(explainer), decision.id() + " links " + explainer);
            }
        }
    }
}
