package de.mkoehler.launchwindow.content;

import de.mkoehler.launchwindow.client.ui.Theme;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Theories} and the bundled theories of the day ({@code ui-spec.md} &sect;4.18.2).
 *
 * @author Mario Koehler
 */
class TheoriesTest {

    private static final Theories THEORIES = Theories.load(Path.of("..", "assets", "content", Theories.FILE));

    @Test
    void everyTheoryIsCompleteForItsReach() {
        assertTrue(THEORIES.theories().size() >= 8);
        Set<String> ids = new HashSet<>();
        for (Theories.Theory theory : THEORIES.theories()) {
            assertTrue(ids.add(theory.id()), "duplicate id " + theory.id());
            assertFalse(theory.raisedBy().isEmpty(), theory.id());
            assertFalse(theory.answeredBy().isEmpty(), theory.id());
            for (String text : List.of(theory.title(), theory.question(), theory.settledBy(), theory.answer(),
                    theory.historyNote())) {
                assertFalse(text == null || text.isBlank(), theory.id());
            }
            if (theory.reach() == Theories.Reach.PUBLIC) {
                assertFalse(theory.headline() == null || theory.headline().isBlank(), theory.id());
                assertTrue(theory.headlineShort().length() <= theory.headline().length(), theory.id());
                assertTrue(theory.deck().contains("{agency}"), theory.id());
                assertFalse(theory.article().isEmpty(), theory.id());
                assertTrue(theory.article().get(0).startsWith("{season} — "), theory.id());
            } else {
                assertTrue(theory.notice().contains("{agency}"), theory.id());
            }
        }
    }

    /** A theory a pack can't raise or settle never shows, so every goal must be one of the standard pack's. */
    @Test
    void everyGoalIsAGoalOfTheStandardPack() {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("..", "assets", "content", "standard"));
        for (Theories.Theory theory : THEORIES.theories()) {
            List<String> goals = new ArrayList<>(theory.raisedBy());
            goals.addAll(theory.answeredBy());
            for (String goal : goals) {
                assertTrue(catalog.goal(goal).isPresent(), theory.id() + ": " + goal);
            }
            assertTrue(theory.playable(g -> catalog.goal(g).isPresent()), theory.id());
        }
    }

    @Test
    void theWordsUseOnlyKnownTokensAndDrawableLetters() {
        Pattern token = Pattern.compile("\\{([A-Za-z0-9]+)}");
        for (Theories.Theory theory : THEORIES.theories()) {
            List<String> texts = new ArrayList<>(List.of(theory.title(), theory.question(), theory.settledBy(),
                theory.answer(), theory.historyNote()));
            for (String text : new String[] {theory.headline(), theory.headlineShort(), theory.deck(), theory.notice()}) {
                if (text != null) {
                    texts.add(text);
                }
            }
            texts.addAll(theory.article());
            for (String text : texts) {
                Matcher matcher = token.matcher(text);
                while (matcher.find()) {
                    assertTrue(Set.of("agency", "season").contains(matcher.group(1)), "unknown token in: " + text);
                }
                text.chars().forEach(c -> assertTrue(c == ' ' || Theme.CHARACTERS.indexOf(c) >= 0,
                    "'" + (char) c + "' isn't in the fonts: " + text));
            }
        }
    }

    @Test
    void aTheoryIsRaisedOnTheFirstOfItsGoals() {
        Theories.Theory dust = THEORIES.theory("deep-dust").orElseThrow();
        assertEquals(java.util.Optional.of(14), dust.raisedOn(Map.of("lunar-probe", 14, "earth-orbit-satellite", 3)));
        assertTrue(dust.raisedOn(Map.of("earth-orbit-satellite", 3)).isEmpty());
        assertFalse(dust.playable(g -> g.equals("lunar-probe")));
    }
}
