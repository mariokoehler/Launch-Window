package de.mkoehler.launchwindow.devtools.analysis;

import de.mkoehler.launchwindow.content.ContentCatalog;
import de.mkoehler.launchwindow.content.ContentPackLoader;
import de.mkoehler.launchwindow.model.Archetype;
import de.mkoehler.launchwindow.rules.BudgetProgress;
import de.mkoehler.launchwindow.rules.GoalSettled;
import de.mkoehler.launchwindow.rules.GoalTally;
import de.mkoehler.launchwindow.rules.ResolutionEvent;
import de.mkoehler.launchwindow.rules.RivalFirsts;
import de.mkoehler.launchwindow.rules.RivalFirstsAnswered;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * <p>Measures the answer to a rival's First, the "Sputnik shock" ({@code design.md} &sect;2.1(e),
 * {@link RivalFirsts}): the smart bot plays NASA against the smart bot playing the Soviets in one
 * game, sharing the goal tally as the server does, with the rule off and on. Each agency plays on its
 * own harness and its own random source, so the two don't share the bot's state or the season's
 * draws.</p>
 *
 * <p>Reports each side's Moon landing, who lands first, the margin between the two landings, and how
 * often the side behind after the review of season 32 (the lower funding rate) still lands first: the
 * snowball's grip. Run with {@code -Dexec.args="standard"} ({@code decisions} as a second argument plays
 * with the R&amp;D decisions); {@code -Dbot.seed} picks the seed.</p>
 *
 * @author Mario Koehler
 */
public final class RaceShock {

    private static final int GAMES = 200;
    private static final int SEASONS = 240;

    private RaceShock() {
    }

    /** One race's outcome. */
    private record Race(int nasa, int soviet, long nasaRate32, long sovietRate32, long[] behindRates,
                        long[] aheadRates, long nasaCredit, long sovietCredit) {

        int behindLands() {
            return nasaRate32 < sovietRate32 ? nasa : soviet;
        }

        int aheadLands() {
            return nasaRate32 < sovietRate32 ? soviet : nasa;
        }
    }

    /**
     * Plays the races and prints the comparison.
     *
     * @param args the pack, then optionally {@code decisions}
     */
    public static void main(String[] args) {
        ContentCatalog catalog = ContentPackLoader.load(Path.of("assets", "content",
            args.length > 0 ? args[0] : "standard"));
        // -Drules.raiseShare=0 turns off raises between reviews (design.md 2.1(f)), for measuring them.
        String raise = System.getProperty("rules.raiseShare");
        if (raise != null) {
            de.mkoehler.launchwindow.rules.BudgetProgress.setRaiseShare(Double.parseDouble(raise));
        }
        if (args.length > 1 && args[1].equals("decisions")) {
            de.mkoehler.launchwindow.content.Decisions.setStandard(de.mkoehler.launchwindow.content.Decisions.load(
                Path.of("assets", "content", de.mkoehler.launchwindow.content.Decisions.FILE)));
        }
        long seed = Long.getLong("bot.seed", 42L);
        String shares = System.getProperty("race.shares", "0," + RivalFirsts.SHARE);
        for (String share : shares.split(",")) {
            RivalFirsts.setShare(Double.parseDouble(share));
            List<Race> races = play(catalog, seed);
            report(share, races);
        }
        RivalFirsts.setShare(RivalFirsts.SHARE);
    }

    private static List<Race> play(ContentCatalog catalog, long seed) {
        ContentPackPlayouts nasaHarness = new ContentPackPlayouts(catalog, ContentPackPlayouts.Bot.SMART);
        ContentPackPlayouts sovietHarness = new ContentPackPlayouts(catalog, ContentPackPlayouts.Bot.SMART);
        List<Race> races = new ArrayList<>();
        for (int game = 0; game < GAMES; game++) {
            Random nasaRandom = new Random(seed * 1_000_003L + 2L * game);
            Random sovietRandom = new Random(seed * 1_000_003L + 2L * game + 1);
            ContentPackPlayouts.Game nasa = nasaHarness.startRaceGame(Archetype.NASA, SEASONS, nasaRandom);
            ContentPackPlayouts.Game soviet = sovietHarness.startRaceGame(Archetype.SOVIET, SEASONS, sovietRandom);
            GoalTally tally = new GoalTally();
            List<String> firsts = List.of();
            long nasaRate32 = 0;
            long sovietRate32 = 0;
            long[] nasaRates = new long[3];
            long[] sovietRates = new long[3];
            long[] credits = new long[2];
            for (int season = 0; season < SEASONS; season++) {
                List<ResolutionEvent> all = new ArrayList<>();
                ContentPackPlayouts.Game[] sides = {nasa, soviet};
                for (int side = 0; side < 2; side++) {
                    if (sides[side].completedSeason > 0) {
                        continue;
                    }
                    List<ResolutionEvent> events = sides[side].season(season, tally, firsts);
                    for (ResolutionEvent event : events) {
                        if (event instanceof RivalFirstsAnswered answered) {
                            credits[side] += answered.credit();
                        }
                    }
                    all.addAll(events);
                }
                tally.commit(all);
                Set<String> next = new LinkedHashSet<>();
                for (ResolutionEvent event : all) {
                    if (event instanceof GoalSettled settled && settled.firstInGame()) {
                        next.add(settled.goalId());
                    }
                }
                firsts = List.copyOf(next);
                int played = season + 1;
                if (played % BudgetProgress.SEASONS_PER_REVIEW == 0 && played >= 32 && played <= 64) {
                    int index = played / BudgetProgress.SEASONS_PER_REVIEW - 2;
                    nasaRates[index] = nasa.snapshot.getFundingRatePerSeason();
                    sovietRates[index] = soviet.snapshot.getFundingRatePerSeason();
                }
                if (played == 32) {
                    nasaRate32 = nasa.snapshot.getFundingRatePerSeason();
                    sovietRate32 = soviet.snapshot.getFundingRatePerSeason();
                }
                if (nasa.completedSeason > 0 && soviet.completedSeason > 0) {
                    break;
                }
            }
            int nasaLands = nasa.completedSeason > 0 ? nasa.completedSeason : SEASONS + 1;
            int sovietLands = soviet.completedSeason > 0 ? soviet.completedSeason : SEASONS + 1;
            boolean nasaBehind = nasaRate32 < sovietRate32;
            races.add(new Race(nasaLands, sovietLands, nasaRate32, sovietRate32,
                nasaBehind ? nasaRates : sovietRates, nasaBehind ? sovietRates : nasaRates, credits[0], credits[1]));
        }
        return races;
    }

    private static void report(String share, List<Race> races) {
        System.out.printf(Locale.ROOT, "%n== Share %s: %d races, NASA against the Soviets ==%n", share, races.size());
        System.out.println("NASA lands:   " + quartiles(races.stream().map(Race::nasa).toList()));
        System.out.println("Soviet lands: " + quartiles(races.stream().map(Race::soviet).toList()));
        System.out.println("First landing: " + quartiles(races.stream().map(race -> Math.min(race.nasa(), race.soviet()))
            .toList()));
        long nasaWins = races.stream().filter(race -> race.nasa() < race.soviet()).count();
        long sovietWins = races.stream().filter(race -> race.soviet() < race.nasa()).count();
        System.out.printf(Locale.ROOT, "Lands first: NASA %d, Soviets %d, same season %d%n", nasaWins, sovietWins,
            races.size() - nasaWins - sovietWins);
        System.out.println("Margin (second landing less first): "
            + quartiles(races.stream().map(race -> Math.abs(race.nasa() - race.soviet())).toList()));
        long comebacks = races.stream().filter(race -> race.behindLands() < race.aheadLands()).count();
        long ties = races.stream().filter(race -> race.behindLands() == race.aheadLands()).count();
        System.out.printf(Locale.ROOT, "Behind on funding after season 32, lands first anyway: %d of %d (%d ties)%n",
            comebacks, races.size(), ties);
        for (int review = 0; review < 3; review++) {
            int index = review;
            System.out.printf(Locale.ROOT, "Funding rate after season %d: behind %,.0f, ahead %,.0f%n",
                32 + 16 * review, races.stream().mapToLong(race -> race.behindRates()[index]).average().orElse(0),
                races.stream().mapToLong(race -> race.aheadRates()[index]).average().orElse(0));
        }
        System.out.printf(Locale.ROOT, "Credit for rivals' Firsts per game: NASA %,.0f, Soviets %,.0f prestige%n",
            races.stream().mapToLong(Race::nasaCredit).average().orElse(0),
            races.stream().mapToLong(Race::sovietCredit).average().orElse(0));
        List<Integer> slowest = races.stream().map(race -> Math.max(race.nasa(), race.soviet())).sorted().toList();
        System.out.printf(Locale.ROOT, "Second landing, average %.1f; slowest quarter's average %.1f%n",
            slowest.stream().mapToInt(Integer::intValue).average().orElse(0),
            slowest.subList(slowest.size() * 3 / 4, slowest.size()).stream().mapToInt(Integer::intValue).average()
                .orElse(0));
    }

    private static String quartiles(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        return String.format(Locale.ROOT, "quartiles %d / %d / %d, average %.1f, max %d", sorted.get(sorted.size() / 4),
            sorted.get(sorted.size() / 2), sorted.get(sorted.size() * 3 / 4),
            sorted.stream().mapToInt(Integer::intValue).average().orElse(0), sorted.getLast());
    }
}
