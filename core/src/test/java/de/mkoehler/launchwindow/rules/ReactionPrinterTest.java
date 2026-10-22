package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.model.FrontPage;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReactionPrinter} ({@code ui-spec.md} &sect;4.18.1, G17).
 *
 * @author Mario Koehler
 */
class ReactionPrinterTest {

    private static final FrontPages PAGES = FrontPages.load(Path.of("..", "assets", "content", FrontPages.FILE));
    private static final FrontPageReactions WORDS = FrontPageReactions.load(
        Path.of("..", "assets", "content", FrontPageReactions.FILE));

    private static final long SEED = 1234L;

    /** Orion made the first human in space in turn 20; the statements come in turn 21. */
    private static final FrontPage FIRST = new FrontPage("20-human-in-space", "human-in-space", 20, "space-first-flight",
        "Fair", List.of(), List.of(new FrontPage.Achiever("Orion Spaceworks", "Ada Orion", List.of("Daniel Hale"))),
        List.of());

    @Test
    void nobodySpeakingPrintsNothing() {
        List<ReactionPrinter.Agency> agencies = List.of(agency("Orion Spaceworks", 1, null),
            agency("Kestrel Aerospace", 1, null));
        assertTrue(ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES).isEmpty());
    }

    @Test
    void aStatementPrintsAFollowUpWithTheSilentDeclining() {
        List<ReactionPrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", 1, null),
            agency("Kestrel Aerospace", 1, statement(FrontPage.Stance.POLITE, "Well flown.")),
            agency("Baltic Star Agency", 1, null));
        List<FrontPage> printed = ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES);
        assertEquals(1, printed.size());
        FrontPage page = printed.get(0);
        assertEquals("20-human-in-space-reactions", page.id());
        assertEquals(FrontPage.Kind.REACTIONS, page.kind());
        assertEquals("20-human-in-space", page.reactsTo());
        assertEquals(21, page.gameTurn());
        assertEquals(FIRST.achievers(), page.achievers());
        assertFalse(page.tells("human-in-space"));
        assertEquals(FrontPageReactions.Mood.GRACIOUS, WORDS.first("human-in-space").orElseThrow()
            .story(page.storyId()).orElseThrow().mood());
        assertEquals(List.of("Kestrel Aerospace", "Baltic Star Agency"),
            page.reactions().stream().map(FrontPage.Reaction::agencyName).toList());
        FrontPage.Reaction told = page.reactions().get(0);
        assertEquals("Well flown.", told.quote());
        assertTrue(WORDS.linesOf(FrontPage.Stance.POLITE).quoted().stream().anyMatch(l -> l.id().equals(told.lineId())));
        assertNull(told.linkId());
        FrontPage.Reaction silent = page.reactions().get(1);
        assertEquals(FrontPage.Stance.DECLINED, silent.stance());
        assertTrue(WORDS.declined().one().stream().anyMatch(l -> l.id().equals(silent.lineId())));
    }

    @Test
    void achieversAndLateJoinersAreNotAsked() {
        List<ReactionPrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", 1, statement(FrontPage.Stance.ENTHUSIASTIC, "")),
            agency("Kestrel Aerospace", 1, statement(FrontPage.Stance.NEXT, "")),
            agency("Late Bureau", 21, statement(FrontPage.Stance.POLITE, "")));
        FrontPage page = ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES).get(0);
        assertEquals(List.of("Kestrel Aerospace"), page.reactions().stream().map(FrontPage.Reaction::agencyName).toList());
        assertFalse(ReactionPrinter.mayReact(FIRST, 21, "Orion Spaceworks", 1));
        assertFalse(ReactionPrinter.mayReact(FIRST, 21, "Late Bureau", 21));
        assertFalse(ReactionPrinter.mayReact(FIRST, 22, "Kestrel Aerospace", 1));
        assertTrue(ReactionPrinter.mayReact(FIRST, 21, "Kestrel Aerospace", 20));
        assertEquals(20, ReactionPrinter.firstTurn(21, 2));
    }

    @Test
    void onlyTheTurnBeforesFirstsGetFollowUps() {
        List<ReactionPrinter.Agency> agencies = List.of(agency("Orion Spaceworks", 1, null),
            agency("Kestrel Aerospace", 1, statement(FrontPage.Stance.POLITE, "")));
        assertTrue(ReactionPrinter.print(22, SEED, List.of(FIRST), agencies, WORDS, PAGES).isEmpty());
    }

    @Test
    void theSamePageIsPrintedEveryTime() {
        List<ReactionPrinter.Agency> agencies = crowd();
        assertEquals(ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES),
            ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES));
    }

    @Test
    void statementsAreToldInStanceOrderWithLinksWhereTheStanceChanges() {
        List<ReactionPrinter.Agency> agencies = List.of(
            agency("Orion Spaceworks", 1, null),
            agency("Alpha Agency", 1, statement(FrontPage.Stance.DOWNPLAY, "")),
            agency("Beta Agency", 1, statement(FrontPage.Stance.ENTHUSIASTIC, "")),
            agency("Gamma Agency", 1, statement(FrontPage.Stance.ENTHUSIASTIC, "Bravo!")));
        FrontPage page = ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES).get(0);
        assertEquals(List.of("Gamma Agency", "Beta Agency", "Alpha Agency"),
            page.reactions().stream().map(FrontPage.Reaction::agencyName).toList());
        assertNull(page.reactions().get(1).linkId(), "no link within a stance");
        String link = page.reactions().get(2).linkId();
        assertNotNull(link);
        assertTrue(WORDS.linksBetween(FrontPage.Stance.ENTHUSIASTIC, FrontPage.Stance.DOWNPLAY).stream()
            .anyMatch(l -> l.id().equals(link)));
        assertEquals(FrontPageReactions.Mood.MIXED, WORDS.first("human-in-space").orElseThrow()
            .story(page.storyId()).orElseThrow().mood());
        assertTrue(!page.reactions().get(0).lineId().equals(page.reactions().get(1).lineId()));
    }

    @Test
    void aCrowdedPageTellsQuotesFirstAndFoldsTheRest() {
        FrontPage page = ReactionPrinter.print(21, SEED, List.of(FIRST), crowd(), WORDS, PAGES).get(0);
        List<FrontPage.Reaction> told = page.reactions().stream().filter(r -> !WORDS.shared(r.lineId())).toList();
        List<FrontPage.Reaction> folded = page.reactions().stream().filter(r -> WORDS.shared(r.lineId())).toList();
        assertEquals(ReactionPrinter.MAX_TOLD, told.size());
        assertTrue(told.stream().allMatch(r -> !r.quote().isEmpty()), "quotes are told first");
        assertEquals(2, folded.size());
        assertEquals(folded.get(0).lineId(), folded.get(1).lineId(), "one stance's rest share a line");
        assertTrue(folded.stream().allMatch(r -> r.quote().isEmpty() && r.detailId() == null));
    }

    @Test
    void aCrowdedPageTakesTurnsAcrossTheStances() {
        List<ReactionPrinter.Agency> agencies = new ArrayList<>();
        agencies.add(agency("Orion Spaceworks", 1, null));
        for (String name : List.of("Polite A", "Polite B", "Polite C", "Polite D")) {
            agencies.add(agency(name, 1, statement(FrontPage.Stance.POLITE, "Well done.")));
        }
        agencies.add(agency("Next E", 1, statement(FrontPage.Stance.NEXT, "We follow.")));
        agencies.add(agency("Down F", 1, statement(FrontPage.Stance.DOWNPLAY, "A stunt.")));
        FrontPage page = ReactionPrinter.print(21, SEED, List.of(FIRST), agencies, WORDS, PAGES).get(0);
        List<FrontPage.Stance> told = page.reactions().stream().filter(r -> !WORDS.shared(r.lineId()))
            .map(FrontPage.Reaction::stance).toList();
        assertEquals(List.of(FrontPage.Stance.POLITE, FrontPage.Stance.POLITE, FrontPage.Stance.NEXT,
            FrontPage.Stance.DOWNPLAY), told, "every voice is told before a stance's second");
    }

    @Test
    void aRivalThatMatchedTheFirstIsAlwaysSaidToHave() {
        for (long seed = 0; seed < 20; seed++) {
            List<ReactionPrinter.Agency> agencies = List.of(agency("Orion Spaceworks", 1, null),
                new ReactionPrinter.Agency("Kestrel Aerospace", "Kai Kestrel", 1, Set.of("human-in-space"),
                    Map.of(FIRST.id(), statement(FrontPage.Stance.NEXT, ""))));
            FrontPage page = ReactionPrinter.print(21, seed, List.of(FIRST), agencies, WORDS, PAGES).get(0);
            String detail = page.reactions().get(0).detailId();
            assertTrue(WORDS.first("human-in-space").orElseThrow()
                .detailsOf(FrontPageReactions.DetailKind.MATCHED).stream().anyMatch(l -> l.id().equals(detail)));
        }
    }

    @Test
    void theMoodFollowsTheMix() {
        assertEquals(FrontPageReactions.Mood.GRACIOUS, FrontPageReactions.Mood.of(List.of(FrontPage.Stance.POLITE,
            FrontPage.Stance.NO_COMMENT)));
        assertEquals(FrontPageReactions.Mood.CHALLENGE, FrontPageReactions.Mood.of(List.of(FrontPage.Stance.POLITE,
            FrontPage.Stance.NEXT)));
        assertEquals(FrontPageReactions.Mood.MIXED, FrontPageReactions.Mood.of(List.of(FrontPage.Stance.ENTHUSIASTIC,
            FrontPage.Stance.DOWNPLAY)));
        assertEquals(FrontPageReactions.Mood.DISMISSIVE, FrontPageReactions.Mood.of(List.of(FrontPage.Stance.NEXT,
            FrontPage.Stance.DOWNPLAY)));
        assertEquals(FrontPageReactions.Mood.SILENT, FrontPageReactions.Mood.of(List.of(FrontPage.Stance.NO_COMMENT)));
    }

    /** Orion, four rivals with quotes and two polite ones without. */
    private static List<ReactionPrinter.Agency> crowd() {
        List<ReactionPrinter.Agency> agencies = new ArrayList<>();
        agencies.add(agency("Orion Spaceworks", 1, null));
        agencies.add(agency("Polite One", 1, statement(FrontPage.Stance.POLITE, "")));
        agencies.add(agency("Polite Two", 1, statement(FrontPage.Stance.POLITE, "")));
        agencies.add(agency("Quote A", 1, statement(FrontPage.Stance.ENTHUSIASTIC, "A great day.")));
        agencies.add(agency("Quote B", 1, statement(FrontPage.Stance.NEXT, "We will follow.")));
        agencies.add(agency("Quote C", 1, statement(FrontPage.Stance.DOWNPLAY, "A stunt.")));
        agencies.add(agency("Quote D", 1, statement(FrontPage.Stance.POLITE, "Well done.")));
        return agencies;
    }

    private static FrontPage.Statement statement(FrontPage.Stance stance, String quote) {
        return new FrontPage.Statement(FIRST.id(), stance, quote);
    }

    private static ReactionPrinter.Agency agency(String name, int firstTurn, FrontPage.Statement statement) {
        return new ReactionPrinter.Agency(name, "Director of " + name, firstTurn, Set.of("earth-orbit-satellite"),
            statement == null ? Map.of() : Map.of(FIRST.id(), statement));
    }
}
