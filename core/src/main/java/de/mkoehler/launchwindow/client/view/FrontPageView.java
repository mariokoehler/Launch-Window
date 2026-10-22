package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.FrontPageReactions;
import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.Goal;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * The words on a front page ({@code ui-spec.md} &sect;4.18): its story with the tokens filled in,
 * the folio line, the race box and the game line under the page. No drawing here, so it can be
 * tested without a window.
 *
 * @author Mario Koehler
 */
public final class FrontPageView {

    private FrontPageView() {
    }

    /**
     * Everything a page shows.
     *
     * @param headline the headline, in capitals
     * @param shortHeadline the short headline, in capitals
     * @param deck     the subheadline
     * @param caption  the photo's caption
     * @param brief    the "In brief" lines
     * @param paragraphs the article, in order; the last {@code droppable} may be left out to fit
     * @param droppable how many of the last paragraphs may be left out
     * @param folio    the folio's left part, "VOL. VII · No. 26"
     * @param season   the folio's middle, "SPRING 1961"
     * @param weather  the ear's weather line
     * @param race     the race box's rows
     * @param photo    the photo's path under {@code assets/ui/}
     */
    public record Page(String headline, String shortHeadline, String deck, String caption, List<String> brief,
                       List<String> paragraphs, int droppable, String folio, String season, String weather,
                       List<RaceRow> race, String photo) {
    }

    /**
     * One row of the race box.
     *
     * @param first the First's name
     * @param who   who made it and when, or "still open"
     * @param done  whether it's made
     */
    public record RaceRow(String first, String who, boolean done) {
    }

    /**
     * Builds a page's words.
     *
     * @param page    the printed page
     * @param all     every page printed in the game, for the race box
     * @param words   the pages' words
     * @param inPack  whether the game's pack has a goal
     * @param names   a goal's display name
     * @return the words, or empty if the words file doesn't know the page's First
     */
    public static Optional<Page> of(FrontPage page, List<FrontPage> all, FrontPages words,
                                    java.util.function.Predicate<String> inPack, Function<String, String> names) {
        Optional<FrontPages.First> first = words.first(page.goalId());
        if (first.isEmpty()) {
            return Optional.empty();
        }
        Optional<FrontPages.Story> found = first.get().story(page.storyId());
        Map<String, String> tokens = tokens(page, names);
        String headline = found.map(s -> fill(s.headline(), tokens)).orElse(names.apply(page.goalId()));
        String shortHeadline = found.map(s -> fill(s.headlineShort(), tokens)).orElse(headline);
        List<String> paragraphs = new ArrayList<>();
        int droppable = 0;
        if (found.isPresent()) {
            FrontPages.Story story = found.get();
            for (String paragraph : story.paragraphs(!page.foldedGoalIds().isEmpty(), page.rivals().size())) {
                paragraphs.add(fill(paragraph, tokens));
            }
            droppable = story.close().size();
        }
        GameDate date = GameDate.ofTurn(page.gameTurn());
        String folio = "VOL. " + roman(date.year() - GameDate.START_YEAR + 1) + " · No. " + page.gameTurn();
        return Optional.of(new Page(headline.toUpperCase(Locale.ROOT), shortHeadline.toUpperCase(Locale.ROOT),
            found.map(s -> fill(s.deck(), tokens)).orElse(""), fill(first.get().caption(), tokens), first.get().brief(),
            paragraphs, droppable, folio, date.label().toUpperCase(Locale.ROOT), page.weather(),
            race(page, all, words, inPack, names), "frontpage/" + page.goalId() + ".jpg"));
    }

    /**
     * Builds a follow-up page's words ({@code ui-spec.md} &sect;4.18.1): the story picked for the mood, then
     * every statement as the server laid it out, then the closing paragraphs, which may be dropped to fit.
     *
     * @param page      the printed follow-up
     * @param all       every page printed in the game, for the race box
     * @param reactions the follow-ups' words
     * @param words     the Firsts' words, for the photo's caption and the race order
     * @param inPack    whether the game's pack has a goal
     * @param names     a goal's display name
     * @return the words, or empty if the words files don't know the page's First
     */
    public static Optional<Page> ofReactions(FrontPage page, List<FrontPage> all, FrontPageReactions reactions,
                                             FrontPages words, java.util.function.Predicate<String> inPack,
                                             Function<String, String> names) {
        Optional<FrontPages.First> first = words.first(page.goalId());
        Optional<FrontPageReactions.First> text = reactions.first(page.goalId());
        if (first.isEmpty() || text.isEmpty()) {
            return Optional.empty();
        }
        Optional<FrontPageReactions.Story> found = text.get().story(page.storyId());
        Map<String, String> tokens = new HashMap<>(tokens(page, names));
        tokens.put("season", GameDate.ofTurn(page.gameTurn()).label().toUpperCase(Locale.ROOT));
        long spoke = page.reactions().stream().filter(r -> r.stance() != FrontPage.Stance.DECLINED).count();
        boolean one = spoke == 1;
        String headline = found.map(s -> fill(s.headline(), tokens)).orElse(names.apply(page.goalId()));
        String shortHeadline = found.map(s -> fill(s.headlineShort(), tokens)).orElse(headline);
        String deck = found.map(s -> fill(one && !isBlank(s.deckOne()) ? s.deckOne() : s.deck(), tokens)).orElse("");
        List<String> paragraphs = new ArrayList<>();
        found.ifPresent(s -> (one && !s.leadOne().isEmpty() ? s.leadOne() : s.lead())
            .forEach(p -> paragraphs.add(fill(p, tokens))));
        paragraphs.addAll(statements(page, reactions, text.get(), tokens));
        text.get().close().forEach(p -> paragraphs.add(fill(p, tokens)));
        GameDate date = GameDate.ofTurn(page.gameTurn());
        String folio = "VOL. " + roman(date.year() - GameDate.START_YEAR + 1) + " · No. " + page.gameTurn();
        return Optional.of(new Page(headline.toUpperCase(Locale.ROOT), shortHeadline.toUpperCase(Locale.ROOT), deck,
            fill(first.get().caption(), tokens), List.of(), paragraphs, text.get().close().size(), folio,
            date.label().toUpperCase(Locale.ROOT), page.weather(), race(page, all, words, inPack, names),
            "frontpage/" + page.goalId() + ".jpg"));
    }

    /**
     * Returns a follow-up's statements as paragraphs: one per statement told on its own, with the
     * sentence leading into it, and one per run of statements sharing an "also" or "declined" line.
     *
     * @param page      the follow-up
     * @param reactions the follow-ups' words
     * @param first     its First's words, for the detail clauses
     * @param base      the page's tokens
     * @return the paragraphs
     */
    static List<String> statements(FrontPage page, FrontPageReactions reactions, FrontPageReactions.First first,
                                   Map<String, String> base) {
        List<String> paragraphs = new ArrayList<>();
        List<FrontPage.Reaction> all = page.reactions();
        int i = 0;
        while (i < all.size()) {
            FrontPage.Reaction reaction = all.get(i);
            String line = reactions.text(reaction.lineId()).orElse("");
            if (reactions.shared(reaction.lineId())) {
                List<String> names = new ArrayList<>();
                int j = i;
                while (j < all.size() && Objects.equals(all.get(j).lineId(), reaction.lineId())) {
                    names.add(all.get(j).agencyName());
                    j++;
                }
                Map<String, String> tokens = new HashMap<>(base);
                tokens.put("rivals", join(names));
                paragraphs.add(fill(line, tokens));
                i = j;
                continue;
            }
            Map<String, String> tokens = new HashMap<>(base);
            tokens.put("rival", reaction.agencyName());
            tokens.put("rivalDirector", reaction.directorName());
            tokens.put("detail", reactions.text(reaction.detailId()).orElse(""));
            String told = fill(line, tokens);
            String link = reactions.text(reaction.linkId()).map(l -> fill(l, tokens) + " ").orElse("");
            // The quote goes in last, so a player's words are never read as a token.
            paragraphs.add(link + told.replace("{quote}", quoted(reaction.quote())));
            i++;
        }
        return paragraphs;
    }

    /**
     * Returns a stance's name as the statement dialog's row shows it (component 28).
     *
     * @param stance the stance
     * @return its name
     */
    public static String stanceTitle(FrontPage.Stance stance) {
        return switch (stance) {
            case ENTHUSIASTIC -> "Enthusiastic congratulations";
            case POLITE -> "Polite congratulations";
            case NEXT -> "“We’ll be next”";
            case DOWNPLAY -> "Downplay";
            case NO_COMMENT -> "No comment";
            case DECLINED -> "No statement";
        };
    }

    /**
     * Returns a stance's short name, for the strip's "Your statement: ..." button.
     *
     * @param stance the stance
     * @return its short name
     */
    public static String stanceName(FrontPage.Stance stance) {
        return switch (stance) {
            case ENTHUSIASTIC -> "Enthusiastic";
            case POLITE -> "Polite";
            default -> stanceTitle(stance);
        };
    }

    /**
     * Returns the muted line under a stance's name in the statement dialog.
     *
     * @param stance the stance
     * @return the line
     */
    public static String stanceHint(FrontPage.Stance stance) {
        return switch (stance) {
            case ENTHUSIASTIC -> "Warm, generous praise.";
            case POLITE -> "Correct, and brief.";
            case NEXT -> "Congratulate them, then promise to catch up.";
            case DOWNPLAY -> "It proves less than it seems.";
            case NO_COMMENT -> "A pointed refusal to say anything. (Staying silent is printed as “did not respond”.)";
            case DECLINED -> "";
        };
    }

    /**
     * Returns how a statement might read in the follow-up, for the dialog's preview: the stance's
     * first line, quoted or plain, with the player's names, without a detail clause. The printed page
     * may draw another line of the same stance.
     *
     * @param first     the First's page
     * @param stance    the stance
     * @param quote     the quote, cleaned, or {@code ""}
     * @param agency    the player's agency
     * @param director  its director
     * @param reactions the follow-ups' words
     * @param names     a goal's display name
     * @return the sentence, or {@code ""} if the words file has none
     */
    public static String sample(FrontPage first, FrontPage.Stance stance, String quote, String agency, String director,
                                FrontPageReactions reactions, Function<String, String> names) {
        FrontPageReactions.StanceLines lines = reactions.linesOf(stance);
        boolean quoted = quote != null && !quote.isBlank() && stance.quotable() && !lines.quoted().isEmpty();
        List<FrontPageReactions.Line> pool = quoted ? lines.quoted() : lines.plain();
        if (pool.isEmpty()) {
            return "";
        }
        Map<String, String> tokens = new HashMap<>(tokens(first, names));
        tokens.put("rival", agency);
        tokens.put("rivalDirector", director);
        tokens.put("detail", "");
        return fill(pool.get(0).text(), tokens).replace("{quote}", quoted(quote));
    }

    /**
     * Sets a player's quote as the paper prints it: in curly quotes, ending in a full stop unless it
     * ends in other punctuation.
     *
     * @param quote the quote
     * @return the printed quote
     */
    static String quoted(String quote) {
        String text = quote == null ? "" : quote.strip();
        if (text.isEmpty()) {
            return "";
        }
        char last = text.charAt(text.length() - 1);
        if (".!?…".indexOf(last) < 0) {
            text = text + ".";
        }
        return "“" + text + "”";
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /**
     * Returns the race box as it stood when a page was printed: every First the pack has, who made
     * it and when, from the pages printed up to that one.
     *
     * @param page   the page
     * @param all    every page printed in the game
     * @param words  the pages' words, for the Firsts' order
     * @param inPack whether the game's pack has a goal
     * @param names  a goal's display name
     * @return one row per First
     */
    public static List<RaceRow> race(FrontPage page, List<FrontPage> all, FrontPages words,
                                     java.util.function.Predicate<String> inPack, Function<String, String> names) {
        List<RaceRow> rows = new ArrayList<>();
        for (FrontPages.First first : words.firsts()) {
            if (!inPack.test(first.goal())) {
                continue;
            }
            Optional<FrontPage> made = all.stream()
                .filter(p -> p.gameTurn() <= page.gameTurn() && p.tells(first.goal()))
                .min(Comparator.comparingInt(FrontPage::gameTurn));
            rows.add(made.map(p -> new RaceRow(names.apply(first.goal()),
                    join(p.achievers().stream().map(FrontPage.Achiever::agencyName).toList()) + " · "
                        + GameDate.ofTurn(p.gameTurn()).label(), true))
                .orElse(new RaceRow(names.apply(first.goal()), "still open", false)));
        }
        return rows;
    }

    /**
     * Returns the game line under the page: who got there first and what the First pays now.
     *
     * @param page    the page
     * @param you     this player's agency name
     * @param catalog the game's content, for the prestige
     * @param names   a goal's display name
     * @return the line
     */
    public static String gameLine(FrontPage page, String you, ContentCatalog catalog, Function<String, String> names) {
        Optional<Goal> goal = catalog.goal(page.goalId());
        String name = names.apply(page.goalId());
        long first = goal.map(Goal::firstTimePrestige).orElse(0L);
        long later = goal.map(Goal::secondTimePrestige).orElse(0L);
        if (page.madeBy(you)) {
            List<String> others = page.achievers().stream().map(FrontPage.Achiever::agencyName)
                .filter(n -> !n.equals(you)).toList();
            if (others.isEmpty()) {
                return "Your agency got there first: " + name + " paid " + whole(first) + " prestige.";
            }
            return "You share this First with " + join(others) + ": each of you got " + whole(first)
                + " prestige.";
        }
        String who = join(page.achievers().stream().map(FrontPage.Achiever::agencyName).toList());
        return who + (page.achievers().size() > 1 ? " got there first together. " : " got there first. ")
            + name + " now pays " + whole(later) + " prestige to whoever follows.";
    }

    /**
     * Fills a text's tokens.
     *
     * @param text   the text
     * @param tokens the tokens' values, by name
     * @return the text
     */
    static String fill(String text, Map<String, String> tokens) {
        if (text == null) {
            return "";
        }
        String filled = text;
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            filled = filled.replace("{" + token.getKey() + "}", token.getValue());
        }
        return filled;
    }

    /**
     * Returns a page's token values.
     *
     * @param page  the page
     * @param names a goal's display name
     * @return the values, by token name
     */
    static Map<String, String> tokens(FrontPage page, Function<String, String> names) {
        List<String> crew = new ArrayList<>();
        List<String> commanders = new ArrayList<>();
        for (FrontPage.Achiever achiever : page.achievers()) {
            crew.addAll(achiever.crewNames());
            if (!achiever.crewNames().isEmpty()) {
                commanders.add(achiever.crewNames().get(0));
            }
        }
        List<String> folded = page.foldedGoalIds().stream().map(g -> "“" + names.apply(g) + "”").toList();
        return Map.of(
            "agency", join(page.achievers().stream().map(FrontPage.Achiever::agencyName).toList()),
            "director", join(page.achievers().stream().map(FrontPage.Achiever::directorName).toList()),
            "season", GameDate.ofTurn(page.gameTurn()).label().toUpperCase(Locale.ROOT),
            "crew", crew.isEmpty() ? "the crew" : join(crew),
            "commander", commanders.isEmpty() ? "the pilot" : join(commanders),
            "rival1", page.rivals().isEmpty() ? "" : page.rivals().get(0),
            "rival2", page.rivals().size() < 2 ? "" : page.rivals().get(1),
            "also", join(folded));
    }

    /**
     * Joins names as a sentence does: "A", "A and B", "A, B and C".
     *
     * @param names the names
     * @return the list
     */
    static String join(List<String> names) {
        if (names.isEmpty()) {
            return "";
        }
        if (names.size() == 1) {
            return names.get(0);
        }
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    private static String whole(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    /**
     * Returns a number in Roman numerals, for the volume.
     *
     * @param number a positive number
     * @return the numerals
     */
    static String roman(int number) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        int rest = Math.max(1, number);
        for (int i = 0; i < values.length; i++) {
            while (rest >= values[i]) {
                out.append(numerals[i]);
                rest -= values[i];
            }
        }
        return out.toString();
    }
}
