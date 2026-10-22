package de.mkoehler.launchwindow.rules;

import de.mkoehler.launchwindow.content.FrontPages;
import de.mkoehler.launchwindow.content.MemorialPages;
import de.mkoehler.launchwindow.model.CareerEntry;
import de.mkoehler.launchwindow.model.FrontPage;
import de.mkoehler.launchwindow.model.GameDate;
import de.mkoehler.launchwindow.model.MemorialEntry;
import de.mkoehler.launchwindow.model.Trait;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The memorial pages, "In memory" ({@code ui-spec.md} &sect;4.18.3, G19): a crew lost in flight is printed a turn
 * late, so no page spoils the replay. The turn that loses a crew records a {@link FrontPage.Loss} for it, which the
 * next turn's statements answer (the losing agency's in one of four tones, every other agency's condolences or
 * a tribute); the next resolution prints the page. Flavour only.
 *
 * <p>The story is drawn with the game seed salted by the page's id, never from the season's random stream, so a
 * retried resolution prints the same pages.</p>
 *
 * @author Mario Koehler
 */
public final class MemorialPrinter {

    /** Rivals' condolences told one by one; the rest share one sentence, so the page holds. */
    public static final int MAX_TOLD = 4;

    /** Mixed into the game seed, so the draw never shares another random stream. */
    private static final long SALT = 0x1E_6015_A10_1971L;

    private MemorialPrinter() {
    }

    /**
     * One agency in the game, as the pages name it.
     *
     * @param rivalKey     its key ({@code GameSeeds.rivalKey}), never its account id
     * @param agencyName   its name
     * @param directorName its director's name
     */
    public record Agency(String rivalKey, String agencyName, String directorName) {
    }

    /**
     * One agency's turn, for finding the crews it lost.
     *
     * @param agency   the agency
     * @param report   its report for the turn, or {@code null}
     * @param memorial its memorial after the turn
     */
    public record Turn(Agency agency, TurnReport report, List<MemorialEntry> memorial) {
    }

    /**
     * Returns a memorial page's id.
     *
     * @param gameTurn the turn it's printed in
     * @param lossId   the loss
     * @return the id
     */
    public static String idOf(int gameTurn, String lossId) {
        return gameTurn + "-memorial-" + lossId;
    }

    /**
     * Finds the crews lost in flight in a turn: each grounding ({@link CrewedFlightsGrounded}) with the departures
     * lost in that accident just before it, and the goals the flight had reached ({@link GoalsWithheld}). Each
     * person is told from the agency's memorial: age, flights, firsts, the seasons served and revealed traits.
     *
     * @param gameTurn the turn
     * @param turns    every agency's turn
     * @return the losses, by agency name, then in the report's order; ids {@code <turn>-loss-<n>}
     */
    public static List<FrontPage.Loss> losses(int gameTurn, List<Turn> turns) {
        List<Turn> sorted = new ArrayList<>(turns);
        sorted.sort(Comparator.comparing(t -> t.agency().agencyName()));
        List<FrontPage.Loss> losses = new ArrayList<>();
        for (Turn turn : sorted) {
            if (turn.report() == null) {
                continue;
            }
            List<PersonnelDeparture> lost = new ArrayList<>();
            Map<String, CrewedFlightsGrounded> grounded = new LinkedHashMap<>();
            Map<String, List<PersonnelDeparture>> crews = new LinkedHashMap<>();
            Map<String, GoalsWithheld> withheld = new LinkedHashMap<>();
            for (ResolutionEvent event : turn.report().events()) {
                if (event instanceof PersonnelDeparture departure
                        && departure.reason() == DepartureReason.LOST_IN_ACCIDENT) {
                    lost.add(departure);
                } else if (event instanceof CrewedFlightsGrounded ground) {
                    grounded.put(ground.missionInstanceId(), ground);
                    crews.put(ground.missionInstanceId(), List.copyOf(lost));
                    lost.clear();
                } else if (event instanceof GoalsWithheld goals) {
                    withheld.put(goals.missionInstanceId(), goals);
                }
            }
            for (CrewedFlightsGrounded ground : grounded.values()) {
                List<FrontPage.Lost> crew = new ArrayList<>();
                String stepId = "";
                for (PersonnelDeparture departure : crews.get(ground.missionInstanceId())) {
                    MemorialEntry entry = turn.memorial().stream()
                        .filter(e -> departure.personnelId().equals(e.personnelId())).findFirst().orElse(null);
                    if (entry == null) {
                        crew.add(new FrontPage.Lost(departure.name(), departure.ageAtExit(), null, departure.personnelId(),
                            1, 0, -1, List.of()));
                        continue;
                    }
                    stepId = entry.stepId() == null ? stepId : entry.stepId();
                    crew.add(lost(entry, gameTurn));
                }
                if (crew.isEmpty()) {
                    continue;
                }
                GoalsWithheld goals = withheld.get(ground.missionInstanceId());
                losses.add(new FrontPage.Loss(gameTurn + "-loss-" + (losses.size() + 1), gameTurn,
                    turn.agency().rivalKey(), turn.agency().agencyName(), turn.agency().directorName(),
                    ground.missionConfigurationId(), stepId, crew, goals == null ? List.of() : goals.goalIds(),
                    goals == null ? List.of() : goals.firstGoalIds()));
            }
        }
        return losses;
    }

    private static FrontPage.Lost lost(MemorialEntry entry, int gameTurn) {
        List<CareerEntry> career = entry.career() == null ? List.of() : entry.career();
        int flights = (int) career.stream().filter(c -> c.kind() == CareerEntry.Kind.FLIGHT && "CREW".equals(c.note()))
            .count();
        int firsts = (int) career.stream().filter(c -> c.kind() == CareerEntry.Kind.FIRST).count();
        List<String> traits = entry.traits() == null ? List.of() : entry.traits().stream().map(Trait::name).toList();
        return new FrontPage.Lost(entry.name(), entry.age(), entry.portraitId(), entry.personnelId(),
            Math.max(1, flights), firsts, entry.joinedTurn() < 0 ? -1 : gameTurn - entry.joinedTurn(), traits);
    }

    /**
     * Prints the memorial pages of the crews lost in the turn before.
     *
     * @param gameTurn   the turn just resolved, the one after the losses
     * @param gameSeed   the game's seed
     * @param losses     the crews lost in the turn before
     * @param statements this turn's statements, by agency key
     * @param agencies   every agency in the game
     * @param words      the memorial pages' words
     * @param pages      the Firsts' words, for the weather
     * @return the pages, in the losses' order
     */
    public static List<FrontPage> print(int gameTurn, long gameSeed, List<FrontPage.Loss> losses,
                                        Map<String, List<FrontPage.Statement>> statements, List<Agency> agencies,
                                        MemorialPages words, FrontPages pages) {
        List<Agency> sorted = new ArrayList<>(agencies);
        sorted.sort(Comparator.comparing(Agency::agencyName));
        List<FrontPage> printed = new ArrayList<>();
        for (FrontPage.Loss loss : losses) {
            String id = idOf(gameTurn, loss.id());
            SplittableRandom random = random(gameSeed, id);
            FrontPage.Statement eulogy = statementOf(statements.get(loss.rivalKey()), loss.id());
            boolean toned = eulogy != null && eulogy.stance() != null && eulogy.stance().tone();
            List<MemorialPages.Story> stories = toned ? words.storiesOf(eulogy.stance()) : List.of();
            String storyId = stories.isEmpty() ? words.spokesman().id() : stories.get(random.nextInt(stories.size())).id();
            FrontPage.Stance tone = stories.isEmpty() ? null : eulogy.stance();
            String quote = tone == null ? "" : eulogy.quote();

            // The condolences: quotes first, then by agency name; at most MAX_TOLD told one by one.
            List<FrontPage.Reaction> sent = new ArrayList<>();
            for (Agency agency : sorted) {
                if (agency.rivalKey().equals(loss.rivalKey())) {
                    continue;
                }
                FrontPage.Statement statement = statementOf(statements.get(agency.rivalKey()), loss.id());
                if (statement == null || statement.stance() == null || !statement.stance().condolence()) {
                    continue;
                }
                sent.add(new FrontPage.Reaction(agency.agencyName(), agency.directorName(), statement.stance(),
                    statement.quote(), null, null, null));
            }
            sent.sort(Comparator.comparing((FrontPage.Reaction r) -> r.quote().isEmpty()));
            List<FrontPage.Reaction> told = new ArrayList<>();
            for (int i = 0; i < sent.size(); i++) {
                FrontPage.Reaction r = sent.get(i);
                String line = i < MAX_TOLD
                    ? words.condolenceLine(r.stance(), !r.quote().isEmpty()).map(MemorialPages.CondolenceLine::id)
                        .orElse(words.condolences().also().id())
                    : words.condolences().also().id();
                told.add(new FrontPage.Reaction(r.agencyName(), r.directorName(), r.stance(),
                    i < MAX_TOLD ? r.quote() : "", line, null, null));
            }
            List<String> weather = pages.weatherFor(GameDate.ofTurn(gameTurn).season());
            String weatherLine = weather.isEmpty() ? "" : weather.get(random.nextInt(weather.size()));
            printed.add(new FrontPage(id, "", gameTurn, storyId, weatherLine, List.of(),
                List.of(new FrontPage.Achiever(loss.agencyName(), loss.directorName(),
                    loss.crew().stream().map(FrontPage.Lost::name).toList())),
                List.of(), FrontPage.Kind.MEMORIAL, null, told,
                new FrontPage.Memorial(loss, tone, quote, List.of(storyId))));
        }
        return printed;
    }

    private static FrontPage.Statement statementOf(List<FrontPage.Statement> statements, String lossId) {
        if (statements == null) {
            return null;
        }
        return statements.stream().filter(s -> s.pageId().equals(lossId)).reduce((a, b) -> b).orElse(null);
    }

    private static SplittableRandom random(long gameSeed, String pageId) {
        long hash = 1125899906842597L;
        for (int i = 0; i < pageId.length(); i++) {
            hash = 31 * hash + pageId.charAt(i);
        }
        return new SplittableRandom(new SplittableRandom(gameSeed ^ SALT).nextLong() ^ hash);
    }
}
