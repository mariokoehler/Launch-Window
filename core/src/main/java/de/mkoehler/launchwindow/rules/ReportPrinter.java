package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.InvestigationCauses;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.MemorialEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Prints the independent boards' reports issued in one resolved turn ({@code design.md} &sect;2.14(b),
 * {@code ui-spec.md} &sect;4.24): one page per {@link OfficialReportIssued} in any agency's turn report, read
 * from the reports, which survive a retried resolution, so a retry prints the same pages. Every agency reads
 * them. Flavour only.
 *
 * <p>The story and the weather line are drawn with the game seed salted by the page's id, never from the
 * season's random stream.</p>
 *
 * @author Mario Koehler
 */
public final class ReportPrinter {

    private static final long SALT = 0x2E90_2700_1967_0127L;

    private ReportPrinter() {
    }

    /**
     * Returns a report page's id.
     *
     * @param gameTurn the turn it was printed in
     * @param number   its number among the turn's reports, from 1
     * @return the id
     */
    public static String idOf(int gameTurn, int number) {
        return gameTurn + "-report-" + number;
    }

    /**
     * Prints the turn's report pages.
     *
     * @param gameTurn the turn just resolved
     * @param gameSeed the game's seed
     * @param turns    every agency with its report and memorial for the turn
     * @param words    the boards' words
     * @param pages    the Firsts' words, for the weather
     * @return the pages, by agency name and then in the report's order; empty in a turn that issued none
     */
    public static List<FrontPage> print(int gameTurn, long gameSeed, List<MemorialPrinter.Turn> turns,
                                        InvestigationCauses words, FrontPages pages) {
        List<MemorialPrinter.Turn> sorted = new ArrayList<>(turns);
        sorted.sort(Comparator.comparing(t -> t.agency().agencyName()));
        List<FrontPage> printed = new ArrayList<>();
        for (MemorialPrinter.Turn turn : sorted) {
            if (turn.report() == null) {
                continue;
            }
            for (ResolutionEvent event : turn.report().events()) {
                if (!(event instanceof OfficialReportIssued issued)) {
                    continue;
                }
                String id = idOf(gameTurn, printed.size() + 1);
                SplittableRandom random = random(gameSeed, id);
                List<FrontPage.Lost> crew = new ArrayList<>();
                for (int i = 0; i < issued.crewIds().size(); i++) {
                    String personnelId = issued.crewIds().get(i);
                    MemorialEntry entry = turn.memorial().stream().filter(e -> personnelId.equals(e.personnelId()))
                        .findFirst().orElse(null);
                    String name = i < issued.crewNames().size() ? issued.crewNames().get(i) : "";
                    crew.add(entry == null ? new FrontPage.Lost(name, 0, null, personnelId, 1, 0, -1, List.of())
                        : MemorialPrinter.lost(entry, issued.accidentTurn()));
                }
                FrontPage.Loss loss = new FrontPage.Loss(issued.incidentId(), issued.accidentTurn(),
                    turn.agency().rivalKey(), turn.agency().agencyName(), turn.agency().directorName(),
                    issued.missionConfigurationId(), issued.stepId(), crew, List.of(), List.of());
                List<InvestigationCauses.BoardStory> stories = words.boardStories();
                String storyId = stories.isEmpty() ? "" : stories.get(random.nextInt(stories.size())).id();
                List<String> weather = pages.weatherFor(GameDate.ofTurn(gameTurn).season());
                String weatherLine = weather.isEmpty() ? "" : weather.get(random.nextInt(weather.size()));
                printed.add(new FrontPage(id, "", gameTurn, storyId, weatherLine, List.of(),
                    List.of(new FrontPage.Achiever(turn.agency().agencyName(), turn.agency().directorName(),
                        crew.stream().map(FrontPage.Lost::name).toList())),
                    List.of(), FrontPage.Kind.REPORT, null, List.of(), null,
                    new FrontPage.Report(loss, issued.causeId(), issued.factorId(), issued.culpritComponentId())));
            }
        }
        return printed;
    }

    private static SplittableRandom random(long gameSeed, String pageId) {
        long hash = 1125899906842597L;
        for (int i = 0; i < pageId.length(); i++) {
            hash = 31 * hash + pageId.charAt(i);
        }
        return new SplittableRandom(new SplittableRandom(gameSeed ^ SALT).nextLong() ^ hash);
    }
}
