package de.mkoehler.launchwindow.client.view;

import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A memorial page's words ({@code ui-spec.md} &sect;4.18.3): the article put together from the story and lines the
 * server picked ({@link de.mkoehler.launchwindow.rules.MemorialPrinter}) and the crew's public facts. The lead, the
 * agency's statement, a sentence on each person, the first the flight had reached, the stand-down; then the
 * other agencies' condolences under their own small head, and the close.
 *
 * @author Mario Koehler
 */
public final class MemorialView {

    private MemorialView() {
    }

    /**
     * A memorial page's words.
     *
     * @param headline      the headline, in capitals
     * @param shortHeadline a shorter one, for when the long one doesn't fit two lines
     * @param deck          the deck
     * @param folio         the volume and number, as the dateline shows them
     * @param date          the season, in capitals
     * @param weather       the ear's weather line
     * @param crew          the crew, in seat order
     * @param article       the paragraphs before the condolences
     * @param head          the condolences' small head, or {@code null} when nobody sent any
     * @param condolences   the condolences' paragraphs, maybe none
     * @param close         the closing paragraph
     */
    public record Page(String headline, String shortHeadline, String deck, String folio, String date, String weather,
                       List<FrontPage.Lost> crew, List<String> article, String head, List<String> condolences,
                       String close) {

        /**
         * Returns every paragraph in reading order, the head among them, for measuring.
         *
         * @return the paragraphs
         */
        public List<String> all() {
            List<String> all = new ArrayList<>(article);
            if (head != null) {
                all.add(head);
            }
            all.addAll(condolences);
            all.add(close);
            return all;
        }
    }

    /**
     * Builds a memorial page's words.
     *
     * @param page  the printed memorial page
     * @param words the memorial pages' words
     * @return the words, or empty if the page carries no memorial
     */
    public static Optional<Page> of(FrontPage page, MemorialPages words) {
        FrontPage.Memorial memorial = page.memorial();
        if (memorial == null || memorial.loss() == null) {
            return Optional.empty();
        }
        FrontPage.Loss loss = memorial.loss();
        List<FrontPage.Lost> crew = loss.crew();
        List<String> names = crew.stream().map(FrontPage.Lost::name).toList();
        MemorialPages.Place place = words.place(loss.stepId());
        Optional<MemorialPages.FirstPhrase> first = words.first(loss.firstGoalIds());
        Map<String, String> tokens = new HashMap<>();
        tokens.put("agency", loss.agencyName());
        tokens.put("director", loss.directorName());
        tokens.put("season", GameDate.ofTurn(page.gameTurn()).label().toUpperCase(Locale.ROOT));
        tokens.put("crew", FrontPageView.join(names));
        tokens.put("name", names.isEmpty() ? "" : names.get(0));
        tokens.put("count", words.number(crew.size()));
        tokens.put("where", place.where());
        tokens.put("during", place.during());
        tokens.put("tail", place.tail());
        tokens.put("first", first.map(MemorialPages.FirstPhrase::first).orElse(""));
        tokens.put("after", first.map(MemorialPages.FirstPhrase::after).orElse(""));
        tokens.put("quote", FrontPageView.quoted(memorial.quote()));

        MemorialPages.Headlines headlines = words.headlines();
        boolean one = crew.size() == 1;
        String headline = FrontPageView.fill(one ? headlines.one() : headlines.many(), tokens).toUpperCase(Locale.ROOT);
        String shortHeadline = FrontPageView.fill(one ? headlines.shortOne() : headlines.shortMany(), tokens)
            .toUpperCase(Locale.ROOT);
        String deck = FrontPageView.fill(first.isPresent() ? words.decks().first() : words.decks().plain(), tokens);

        List<String> article = new ArrayList<>();
        if (!words.leads().isEmpty()) {
            article.add(FrontPageView.fill(words.leads().get(0).text(), tokens));
        }
        MemorialPages.Story story = words.story(page.storyId()).orElse(words.spokesman());
        boolean quoted = !memorial.quote().isBlank() && story.quoted() != null && !story.quoted().isBlank();
        article.add(FrontPageView.fill(quoted ? story.quoted() : story.plain(), tokens));
        List<String> people = new ArrayList<>();
        for (FrontPage.Lost lost : crew) {
            people.add(person(lost, memorial.tone(), words));
        }
        if (!people.isEmpty()) {
            article.add(String.join(" ", people));
        }
        if (first.isPresent()) {
            article.add(FrontPageView.fill(words.withheld().text(), tokens));
        }
        article.add(FrontPageView.fill(words.standDown().text(), tokens));

        List<String> condolences = new ArrayList<>();
        List<String> also = new ArrayList<>();
        StringBuilder told = new StringBuilder();
        for (FrontPage.Reaction reaction : page.reactions()) {
            if (reaction.lineId() == null || reaction.lineId().equals(words.condolences().also().id())) {
                also.add(reaction.agencyName());
                continue;
            }
            Optional<MemorialPages.CondolenceLine> line = words.condolenceLine(reaction.lineId());
            if (line.isEmpty()) {
                also.add(reaction.agencyName());
                continue;
            }
            Map<String, String> rival = new HashMap<>(tokens);
            rival.put("rival", reaction.agencyName());
            rival.put("rivalDirector", reaction.directorName());
            rival.put("quote", FrontPageView.quoted(reaction.quote()));
            if (!told.isEmpty()) {
                told.append(' ');
            }
            told.append(FrontPageView.fill(line.get().text(), rival));
        }
        if (!told.isEmpty()) {
            condolences.add(told.toString());
        }
        if (!also.isEmpty()) {
            Map<String, String> rest = new HashMap<>(tokens);
            rest.put("rivals", FrontPageView.join(also));
            condolences.add(FrontPageView.fill(words.condolences().also().text(), rest));
        }
        String close = FrontPageView.fill(story.close(), tokens);
        GameDate date = GameDate.ofTurn(page.gameTurn());
        String folio = "VOL. " + FrontPageView.roman(date.year() - GameDate.START_YEAR + 1) + " · No. " + page.gameTurn();
        return Optional.of(new Page(headline, shortHeadline, deck, folio, date.label().toUpperCase(Locale.ROOT),
            page.weather(), crew, article, condolences.isEmpty() ? null : words.condolences().head(), condolences,
            close));
    }

    /**
     * Returns the sentence on one person: who they were for a statement about the people (a revealed trait, or
     * the years served), else their flights.
     *
     * @param lost  the person
     * @param tone  the statement's tone, or {@code null} for the spokesman's
     * @param words the words
     * @return the sentence
     */
    static String person(FrontPage.Lost lost, FrontPage.Stance tone, MemorialPages words) {
        MemorialPages.People people = words.people();
        Map<String, String> tokens = new HashMap<>();
        tokens.put("name", lost.name());
        tokens.put("age", String.valueOf(lost.age()));
        tokens.put("nth", words.ordinal(lost.flights()));
        int years = lost.seasons() / de.mkoehler.launchwindow.model.Season.values().length;
        tokens.put("years", words.number(years) + (years == 1 ? " year" : " years"));
        boolean firstFlight = lost.flights() <= 1;
        if (tone == FrontPage.Stance.PEOPLE) {
            for (String trait : lost.traits()) {
                String line = people.traits().get(trait);
                if (line != null) {
                    return FrontPageView.fill(line, tokens);
                }
            }
            if (years >= 1) {
                return FrontPageView.fill(firstFlight ? people.servedFirst() : people.served(), tokens);
            }
        }
        return FrontPageView.fill(firstFlight ? people.first() : people.again(), tokens);
    }

    /**
     * Returns the history note on the memorial page's strip.
     *
     * @param words the words
     * @return the note, maybe blank
     */
    public static String historyNote(MemorialPages words) {
        return words.historyNotes().page();
    }
}
